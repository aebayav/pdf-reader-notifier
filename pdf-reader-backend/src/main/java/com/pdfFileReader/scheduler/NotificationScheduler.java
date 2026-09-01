package com.pdfFileReader.scheduler;

import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.mail.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Gunluk calisan notifier: suresi gecmis bildirimleri DUE_DATE'e ceker ve
 * yaklasan bildirimlerin ozetini loglar.
 */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final int daysAhead;

    public NotificationScheduler(
            NotificationService notificationService,
            EmailService emailService,
            @Value("${app.notifier.days-ahead:7}") int daysAhead
    ) {
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.daysAhead = daysAhead;
    }

    @Scheduled(cron = "${app.notifier.cron:0 0 9 * * *}")
    public void runDailyCheck() {
        log.info("Notifier gunluk kontrolu basladi");

        int marked = notificationService.markOverdue();

        List<Notification> upcoming = notificationService.findUpcomingAllUsers(daysAhead);
        long overdueCount = upcoming.stream()
                .filter(n -> n.getDueDate().isBefore(LocalDate.now()))
                .count();

        log.info("Notifier ozeti: {} yaklasan bildirim ({} gecikmis), {} yeni gecikme isaretlendi",
                upcoming.size() - overdueCount, overdueCount, marked);

        for (Notification notification : upcoming) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), notification.getDueDate());
            log.info("  - [{}] {} gun {} | {}",
                    notification.getDueDate(), daysLeft < 0 ? -daysLeft : daysLeft,
                    daysLeft < 0 ? "GECTI" : "kaldi",
                    notification.getTitle());
        }

        // E-posta ozeti (SMTP yapilandirilmamissa EmailService sessizce atlar)
        emailService.sendDailySummary(upcoming, marked);
    }
}
