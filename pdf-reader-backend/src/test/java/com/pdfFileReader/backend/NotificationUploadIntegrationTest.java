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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                "1. test ihale bildirimi 14.03.2026",
                "2. test kesin hesap bildirimi 19.06.2026",
                "Bu test bildirim surecinde taraflarin tum yukumlulukleri ilgili mevzuat hukumlerine gore degerlendirilecektir."
        );

        long before = notificationRepository.count();

        List<Notification> saved = notificationService.processAndSaveNotifications(pdf);

        assertFalse(saved.isEmpty(), "bildirim cikarilamadi");
        assertEquals(2, saved.size());
        assertEquals(before + saved.size(), notificationRepository.count(), "DB'ye kaydedilmedi");

        for (Notification notification : saved) {
            assertNotNull(notification.getId(), "kaydedilen bildirime ID atanmadi");
            assertEquals("IN_PROGRESS", notification.getStatus().name());
            assertNotNull(notification.getDescription(), "paragraf aciklamasi yazilmali");
        }
    }

    @Test
    void uploadingSamePdfTwiceDoesNotDuplicate() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("bildirim.pdf",
                "1. test ihale bildirimi 14.03.2026",
                "2. test kesin hesap bildirimi 19.06.2026",
                "Bu test bildirim surecinde taraflarin tum yukumlulukleri ilgili mevzuat hukumlerine gore degerlendirilecektir."
        );

        List<Notification> first = notificationService.processAndSaveNotifications(pdf);
        assertFalse(first.isEmpty(), "ilk yukleme kayit uretmeli");

        long countAfterFirst = notificationRepository.count();

        List<Notification> second = notificationService.processAndSaveNotifications(pdf);

        assertTrue(second.isEmpty(), "ayni belge ikinci kez islenmemeli, uretilen: " + second);
        assertEquals(countAfterFirst, notificationRepository.count(), "duplicate kayit eklendi");
    }

    @Test
    void differentDocumentsWithSameLinesAreNotSkipped() throws Exception {
        MockMultipartFile pdfA = TestPdfFactory.createPdf("a.pdf",
                "Tarih : 20.10.2025",
                "Bu birinci test belgesidir ve yalnizca mükerrer kontrolunun belge bazli calistigini dogrulamak icin olusturulmustur."
        );
        MockMultipartFile pdfB = TestPdfFactory.createPdf("b.pdf",
                "Tarih : 20.10.2025",
                "Bu ikinci test belgesidir ve farkli icerige sahip olmasina ragmen ilk belge ile ayni tarih satirini icerir."
        );

        List<Notification> first = notificationService.processAndSaveNotifications(pdfA);
        assertFalse(first.isEmpty(), "ilk belge kayit uretmeli");

        List<Notification> second = notificationService.processAndSaveNotifications(pdfB);

        assertFalse(second.isEmpty(), "FARKLI belge ayni satiri icerse bile atlanmamali: " + second);
        assertTrue(second.stream().anyMatch(n -> n.getTitle().startsWith("Tarih : 20.10.2025")),
                "farkli belgenin tarih notu kaydedilmeli: " + second);
    }
}
