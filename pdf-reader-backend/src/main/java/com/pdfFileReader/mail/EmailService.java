package com.pdfFileReader.mail;

import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.User;
import com.pdfFileReader.util.EnvReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;

/**
 * Bildirimleri e-posta ile gonderir. SMTP ayarlari ortam degiskenlerinden
 * veya .env dosyasindan (EnvReader) gelir; yapilandirilmamissa sessizce
 * atlar. JavaMailSender'i kendisi kurar - baslatma yonteminden bagimsizdir.
 *
 * Kullanici bazli gonderim: kullanicinin kayitli e-postasi varsa o adrese,
 * yoksa MAIL_TO fallback adresine gonderilir.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final boolean enabled;
    private final String fromConfig;
    private final String fallbackTo;
    private final MailCredentials fixedCredentials;

    private volatile JavaMailSender cachedSender;

    @Autowired
    public EmailService(
            @Value("${app.mail.enabled:true}") boolean enabled,
            @Value("${app.mail.from:}") String from,
            @Value("${app.mail.to:}") String to
    ) {
        this(enabled, from, to, null);
    }

    /** Testlerin .env'e dokunmadan sabit kimliklerle calisabilmesi icin. */
    EmailService(boolean enabled, String from, String to, MailCredentials credentials) {
        this.enabled = enabled;
        this.fromConfig = from;
        this.fallbackTo = to;
        this.fixedCredentials = credentials;
    }

    record MailCredentials(String host, int port, String username, String password) {
    }

    private MailCredentials credentials() {
        if (fixedCredentials != null) {
            return fixedCredentials;
        }
        return new MailCredentials(
                EnvReader.read("MAIL_HOST"),
                Integer.parseInt(resolvedPort()),
                EnvReader.read("MAIL_USERNAME"),
                EnvReader.read("MAIL_PASSWORD")
        );
    }

    String resolvedHost() {
        MailCredentials c = credentials();
        return c.host() == null ? "" : c.host();
    }

    String resolvedPort() {
        String port = EnvReader.read("MAIL_PORT");
        return port.isBlank() ? "587" : port;
    }

    String resolvedUsername() {
        return credentials().username() == null ? "" : credentials().username();
    }

    String resolvedPassword() {
        return credentials().password() == null ? "" : credentials().password();
    }

    String resolvedFrom() {
        return EnvReader.or(fromConfig, "MAIL_FROM");
    }

    String resolvedFallbackTo() {
        return EnvReader.or(fallbackTo, "MAIL_TO");
    }

    boolean isConfigured() {
        return enabled
                && !resolvedHost().isBlank()
                && !resolvedUsername().isBlank()
                && !resolvedPassword().isBlank()
                && !resolvedFrom().isBlank();
    }

    private JavaMailSender sender() {
        if (cachedSender == null) {
            synchronized (this) {
                if (cachedSender == null) {
                    MailCredentials c = credentials();

                    JavaMailSenderImpl impl = new JavaMailSenderImpl();
                    impl.setHost(c.host());
                    impl.setPort(c.port());
                    impl.setUsername(c.username());
                    impl.setPassword(c.password());

                    Properties props = new Properties();
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    impl.setJavaMailProperties(props);

                    cachedSender = impl;
                }
            }
        }
        return cachedSender;
    }

    /**
     * Kullaniciya ozel ozet e-postasi gonderir. Kullanicinin kayitli
     * e-postasi yoksa fallback (MAIL_TO) adresine gonderilir. Her ikisi
     * de bos ise gonderilmez.
     */
    public void sendDailySummaryToUser(User user, List<Notification> upcoming, int newlyMarked) {
        if (!isConfigured()) {
            log.info("SMTP yapilandirilmadigi icin eposta bildirimi atlandi (.env: MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD/MAIL_FROM)");
            return;
        }

        String recipient = resolveRecipient(user);
        if (recipient.isBlank()) {
            log.info("Kullanici icin e-posta adresi bulunamadi, atlandi: {}", user.getUsername());
            return;
        }

        if (upcoming.isEmpty() && newlyMarked == 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        long overdue = upcoming.stream()
                .filter(n -> n.getDueDate() != null && n.getDueDate().isBefore(today))
                .count();
        long soon = upcoming.size() - overdue;

        StringBuilder body = new StringBuilder();
        body.append("PDF Reader Notifier - Gunluk Bildirim Ozeti (").append(user.getUsername()).append(")\n")
                .append("-----------------------------------------\n\n")
                .append(soon).append(" bildirim yaklasiyor, ")
                .append(overdue).append(" gecikmis");

        if (newlyMarked > 0) {
            body.append(" (").append(newlyMarked).append(" yeni gecikme isaretlendi)");
        }
        body.append(".\n\n");

        for (Notification notification : upcoming) {
            long daysLeft = ChronoUnit.DAYS.between(today, notification.getDueDate());
            body.append(daysLeft < 0 ? String.format("- [GECTI %d gun] ", -daysLeft)
                            : daysLeft == 0 ? "- [BUGUN] "
                            : String.format("- [%d gun kaldi] ", daysLeft))
                    .append(notification.getTitle())
                    .append(" (Son Tarih: ")
                    .append(notification.getDueDate())
                    .append(")\n");
        }

        body.append("\nTum bildirimler icin uygulamaya goz atabilirsiniz.\n");

        send(recipient,
                "PDF Reader Notifier: " + soon + " yaklasan, " + overdue + " gecikmis",
                body.toString());
    }

    /**
     * Toplu (eski uyumluluk) ozet – hala kullanilabilir; MAIL_TO adresine gider.
     *
     * @deprecated Kullanici bazli {@link #sendDailySummaryToUser} tercih edilmeli.
     */
    @Deprecated
    public void sendDailySummary(List<Notification> upcoming, int newlyMarked) {
        String recipient = resolvedFallbackTo();
        if (!isConfigured() || recipient.isBlank()) {
            log.info("SMTP veya MAIL_TO yapilandirilmadigi icin toplu eposta atlandi.");
            return;
        }

        if (upcoming.isEmpty() && newlyMarked == 0) {
            log.info("Bildirilecek bir sey yok, eposta gonderilmedi");
            return;
        }

        LocalDate today = LocalDate.now();
        long overdue = upcoming.stream()
                .filter(n -> n.getDueDate() != null && n.getDueDate().isBefore(today))
                .count();
        long soon = upcoming.size() - overdue;

        StringBuilder body = new StringBuilder();
        body.append("PDF Reader Notifier - Gunluk Bildirim Ozeti\n")
                .append("-----------------------------------------\n\n")
                .append(soon).append(" bildirim yaklasiyor, ")
                .append(overdue).append(" gecikmis");

        if (newlyMarked > 0) {
            body.append(" (").append(newlyMarked).append(" yeni gecikme isaretlendi)");
        }
        body.append(".\n\n");

        for (Notification notification : upcoming) {
            long daysLeft = ChronoUnit.DAYS.between(today, notification.getDueDate());
            body.append(daysLeft < 0 ? String.format("- [GECTI %d gun] ", -daysLeft)
                            : daysLeft == 0 ? "- [BUGUN] "
                            : String.format("- [%d gun kaldi] ", daysLeft))
                    .append(notification.getTitle())
                    .append(" (Son Tarih: ")
                    .append(notification.getDueDate())
                    .append(")\n");
        }

        body.append("\nTum bildirimler icin uygulamaya goz atabilirsiniz.\n");

        send(recipient,
                "PDF Reader Notifier: " + soon + " yaklasan, " + overdue + " gecikmis",
                body.toString());
    }

    public void sendTestMail() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "SMTP yapilandirilmamis. .env dosyasina MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD, "
                            + "MAIL_FROM ve MAIL_TO degerlerini ekleyip backend'i yeniden baslatin.");
        }

        String recipient = resolvedFallbackTo();
        if (recipient.isBlank()) {
            throw new IllegalStateException("MAIL_TO adresi ayarlanmamis.");
        }

        boolean sent = send(recipient, "PDF Reader Notifier: SMTP testi basarili",
                "SMTP ayarlari calisiyor. Iyi takipler!");

        if (!sent) {
            throw new IllegalStateException(
                    "Eposta gonderilemedi - SMTP sunucusu reddetti. Backend loglarini kontrol edin "
                            + "(host: " + resolvedHost() + ", kullanici: " + resolvedUsername() + ")");
        }
    }

    /** Gonderim basariliysa true doner; MailException loglanip yutulur. */
    private boolean send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolvedFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            sender().send(message);
            log.info("Eposta gonderildi: '{}' -> {}", subject, to);
            return true;
        } catch (MailException | NumberFormatException e) {
            log.error("Eposta gonderilemedi: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Kullanicinin kendi e-postasını tercih eder; yoksa fallback MAIL_TO adresi.
     */
    private String resolveRecipient(User user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return resolvedFallbackTo();
    }
}
