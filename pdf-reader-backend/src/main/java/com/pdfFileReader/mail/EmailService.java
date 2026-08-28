package com.pdfFileReader.mail;

import com.pdfFileReader.domain.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Bildirimleri e-posta ile gonderir. SMTP ayarlari .env uzerinden gelir;
 * yapilandirilmamissa (host/kullanici/to bos) sessizce atlar, hata firlatmaz.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final String to;
    private final String host;

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.enabled:true}") boolean enabled,
            @Value("${app.mail.from:}") String from,
            @Value("${app.mail.to:}") String to,
            @Value("${spring.mail.host:}") String host
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.enabled = enabled;
        this.from = from;
        this.to = to;
        this.host = host;
    }

    private boolean isConfigured() {
        return enabled
                && mailSender != null
                && !isBlank(host)
                && !isBlank(from)
                && !isBlank(to);
    }

    public void sendDailySummary(List<Notification> upcoming, int newlyMarked) {
        if (!isConfigured()) {
            log.info("SMTP yapilandirilmadigi icin eposta bildirimi atlandi (.env: MAIL_HOST/MAIL_FROM/MAIL_TO)");
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

        send("PDF Reader Notifier: " + soon + " yaklasan, " + overdue + " gecikmis", body.toString());
    }

    public void sendTestMail() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "SMTP yapilandirilmamis. .env dosyasina MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD, "
                            + "MAIL_FROM ve MAIL_TO degerlerini ekleyip backend'i yeniden baslatin.");
        }

        send("PDF Reader Notifier: SMTP testi basarili", "SMTP ayarlari calisiyor. Iyi takipler!");
    }

    private void send(String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Eposta gonderildi: '{}' -> {}", subject, to);
        } catch (MailException e) {
            log.error("Eposta gonderilemedi: {}", e.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
