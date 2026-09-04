package com.pdfFileReader.scheduler;

import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.User;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.mail.EmailService;
import com.pdfFileReader.repository.AuthTokenRepository;
import com.pdfFileReader.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Gunluk calisan notifier: suresi gecmis bildirimleri DUE_DATE'e ceker,
 * kullanici bazinda e-posta ozeti gonderir ve suresi dolmus token'lari temizler.
 */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final int daysAhead;

    public NotificationScheduler(
            NotificationService notificationService,
            EmailService emailService,
            UserRepository userRepository,
            AuthTokenRepository authTokenRepository,
            @Value("${app.notifier.days-ahead:7}") int daysAhead
    ) {
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.daysAhead = daysAhead;
    }

    @Scheduled(cron = "${app.notifier.cron:0 0 9 * * *}")
    public void runDailyCheck() {
        log.info("Notifier gunluk kontrolu basladi");

        int marked = notificationService.markOverdue();

        // Kullanici bazli e-posta
        List<User> users = userRepository.findAll();
        int totalUpcoming = 0;

        for (User user : users) {
            List<Notification> upcoming = notificationService.findUpcoming(daysAhead, user.getId());
            if (!upcoming.isEmpty()) {
                totalUpcoming += upcoming.size();
                long overdueCount = upcoming.stream()
                        .filter(n -> n.getDueDate() != null && n.getDueDate().isBefore(LocalDate.now()))
                        .count();
                log.info("  Kullanici [{}]: {} yaklasan ({} gecikmis)",
                        user.getUsername(), upcoming.size(), overdueCount);

                // Her kullaniciya kendi bildirimleriyle e-posta at
                emailService.sendDailySummaryToUser(user, upcoming, 0);
            }
        }

        log.info("Notifier ozeti: {} kullanici, toplam {} yaklasan bildirim, {} yeni gecikme isaretlendi",
                users.size(), totalUpcoming, marked);

        // Genel log (eski davranis ile uyumlu)
        List<Notification> allUpcoming = notificationService.findUpcomingAllUsers(daysAhead);
        for (Notification notification : allUpcoming) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), notification.getDueDate());
            log.info("  - [{}] {} gun {} | {}",
                    notification.getDueDate(), daysLeft < 0 ? -daysLeft : daysLeft,
                    daysLeft < 0 ? "GECTI" : "kaldi",
                    notification.getTitle());
        }
    }

    /**
     * Suresi dolmus auth token'lari temizler. Her gece 02:00'de calisir.
     * Tablo sismesini onler.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now();
        authTokenRepository.deleteByExpiresAtBefore(cutoff);
        log.info("Suresi dolmus auth token'lari temizlendi (cutoff={})", cutoff);
    }
}
