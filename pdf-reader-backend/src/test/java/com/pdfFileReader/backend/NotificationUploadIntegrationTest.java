package com.pdfFileReader.backend;

import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.repository.NotificationRepository;
import com.pdfFileReader.testutil.TestPdfFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class NotificationUploadIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void uploadSavesNotificationsToDatabase() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("bildirim.pdf",
                "1. ihale bildirimi 12.02.2026",
                "2. kesin hesap bildirimi 28.05.2026",
                "Bu bildirim surecinde taraflarin tum yukumlulukleri ilgili mevzuat hukumlerine gore degerlendirilecektir."
        );

        long before = notificationRepository.count();

        List<Notification> saved = notificationService.processAndSaveNotifications(pdf);

        assertFalse(saved.isEmpty(), "bildirim cikarilamadi");
        assertEquals(2, saved.size());
        assertEquals(before + saved.size(), notificationRepository.count(), "DB'ye kaydedilmedi");

        for (Notification notification : saved) {
            assertNotNull(notification.getId(), "kaydedilen bildirime ID atanmadi");
            assertEquals("IN_PROGRESS", notification.getStatus().name());
        }
    }
}
