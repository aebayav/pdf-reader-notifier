package com.pdfFileReader.backend;

import com.pdfFileReader.domain.dto.UpdateNotificationRequest;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.Status;
import com.pdfFileReader.domain.entity.User;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.exception.NotificationNotFoundException;
import com.pdfFileReader.repository.NotificationRepository;
import com.pdfFileReader.repository.UserRepository;
import com.pdfFileReader.testutil.TestPdfFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class NotificationUploadIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void createTestUser() {
        User user = new User();
        user.setUsername("upload-test-" + System.currentTimeMillis());
        user.setPasswordHash("x");
        userRepository.save(user);
        userId = user.getId();
    }

    @Test
    void uploadSavesNotificationsToDatabase() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("bildirim.pdf",
                "1. test ihale bildirimi 14.03.2026",
                "2. test kesin hesap bildirimi 19.06.2026",
                "Bu test bildirim surecinde taraflarin tum yukumlulukleri ilgili mevzuat hukumlerine gore degerlendirilecektir."
        );

        long before = notificationRepository.count();

        List<Notification> saved = notificationService.processAndSaveNotifications(pdf, userId);

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

        List<Notification> first = notificationService.processAndSaveNotifications(pdf, userId);
        assertFalse(first.isEmpty(), "ilk yukleme kayit uretmeli");

        long countAfterFirst = notificationRepository.count();

        List<Notification> second = notificationService.processAndSaveNotifications(pdf, userId);

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

        List<Notification> first = notificationService.processAndSaveNotifications(pdfA, userId);
        assertFalse(first.isEmpty(), "ilk belge kayit uretmeli");

        List<Notification> second = notificationService.processAndSaveNotifications(pdfB, userId);

        assertFalse(second.isEmpty(), "FARKLI belge ayni satiri icerse bile atlanmamali: " + second);
        assertTrue(second.stream().anyMatch(n -> n.getTitle().startsWith("Tarih : 20.10.2025")),
                "farkli belgenin tarih notu kaydedilmeli: " + second);
    }

    @Test
    void findAllReturnsSortedByDueDate() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("crud.pdf",
                "Kesin kabul islemleri 30.09.2025 tarihinde tamamlanacaktir.",
                "Yer teslim tarihi 10.04.2025 olarak kararlastirilmistir.",
                "Ilk hak edis 25.01.2026 tarihinde yapilacaktir."
        );
        List<Notification> saved = notificationService.processAndSaveNotifications(pdf, userId);
        assertFalse(saved.isEmpty());

        List<Notification> all = notificationService.findAll(userId);

        for (int i = 1; i < all.size(); i++) {
            LocalDate previous = all.get(i - 1).getDueDate();
            LocalDate current = all.get(i).getDueDate();
            assertTrue(!previous.isAfter(current),
                    "liste son tarihe gore artan olmali: " + previous + " > " + current);
        }
    }

    @Test
    void updateChangesOnlyProvidedFields() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("crud2.pdf",
                "Yer teslim tarihi 10.04.2025 olarak kararlastirilmistir.",
                "Isin suresi yer tesliminden itibaren doksan gun olarak uygulanacaktir ve bu sure sonunda taraflar degerlendirme yapacaktir."
        );
        Notification saved = notificationService.processAndSaveNotifications(pdf, userId).get(0);

        Notification updated = notificationService.update(saved.getId(), userId,
                new UpdateNotificationRequest(null, null, null, Status.COMPLETED));

        assertEquals(Status.COMPLETED, updated.getStatus(), "sadece status degismeli");
        assertEquals(saved.getTitle(), updated.getTitle(), "title korunmali");
        assertEquals(saved.getDueDate(), updated.getDueDate(), "tarih korunmali");
    }

    @Test
    void updateUnknownIdThrowsNotFound() {
        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.update(UUID.randomUUID(), userId,
                        new UpdateNotificationRequest(null, null, null, Status.COMPLETED)));
    }

    @Test
    void deleteRemovesNotification() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("crud3.pdf",
                "Ceza tahakkuku 15.11.2025 tarihinde yapilir.",
                "Gecikme cezasi gunluk yuzde sifir virgul bir oraninda uygulanacaktir ve tum cezalar hak edislerden kesilecektir."
        );
        Notification saved = notificationService.processAndSaveNotifications(pdf, userId).get(0);
        long before = notificationRepository.count();

        notificationService.delete(saved.getId(), userId);

        assertEquals(before - 1, notificationRepository.count());
        assertFalse(notificationRepository.existsById(saved.getId()));
    }

    @Test
    void deleteUnknownIdThrowsNotFound() {
        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.delete(UUID.randomUUID(), userId));
    }

    @Test
    void findUpcomingReturnsNearAndOverdueOnly() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("notifier.pdf",
                "Yakin tarih bildirimi 03.05.2026 tarihinde yapilacaktir.",
                "Uzak tarih bildirimi 03.05.2027 tarihinde yapilacaktir ve bu sure icinde takip edilecektir."
        );
        List<Notification> saved = notificationService.processAndSaveNotifications(pdf, userId);
        assertFalse(saved.isEmpty());

        // Uzak tarihli olani manuel yakin tarihe cek (test kararliligi icin)
        Notification far = saved.stream()
                .filter(n -> n.getTitle().contains("Uzak"))
                .findFirst()
                .orElseThrow();
        LocalDate past = LocalDate.now().minusDays(1);
        notificationService.update(far.getId(), userId,
                new UpdateNotificationRequest(null, null, past, null));

        List<Notification> upcoming = notificationService.findUpcoming(7, userId);

        assertTrue(upcoming.stream().anyMatch(n -> n.getDueDate().equals(past)),
                "gecikmis bildirim gelmeli");
        assertTrue(upcoming.stream().noneMatch(n -> n.getDueDate().isAfter(LocalDate.now().plusDays(7))),
                "7 gunden uzak bildirim gelmemeli");
        assertTrue(upcoming.stream().noneMatch(n -> n.getStatus() == Status.CLOSED),
                "kapali bildirimler gelmemeli");
    }

    @Test
    void markOverdueMovesOnlyPastInProgressToDueDate() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("notifier2.pdf",
                "Ilk bildirim 15.11.2025 tarihinde yapilacaktir.",
                "Ikinci bildirim 20.01.2026 tarihinde yapilacaktir ve takip edilecektir."
        );
        List<Notification> saved = notificationService.processAndSaveNotifications(pdf, userId);

        // Ilk bildirimi gecmise cek, ikincisini COMPLETED yap
        Notification first = saved.get(0);
        notificationService.update(first.getId(), userId,
                new UpdateNotificationRequest(null, null, LocalDate.now().minusDays(2), null));
        Notification second = saved.get(1);
        notificationService.update(second.getId(), userId,
                new UpdateNotificationRequest(null, null, null, Status.COMPLETED));

        int marked = notificationService.markOverdue();

        // Not: paylasilan DB'de baska gecikmis kayitlar da olabileceginden
        // kesin sayi yerine kendi test satirlarimizin davranisini dogrulariz.
        assertTrue(marked >= 1, "en az bizim gecikmis test satirimiz isaretlenmeli");
        assertEquals(Status.DUE_DATE, notificationService.findAll(userId).stream()
                        .filter(n -> n.getId().equals(first.getId()))
                        .findFirst().orElseThrow().getStatus());
        assertEquals(Status.COMPLETED, notificationService.findAll(userId).stream()
                        .filter(n -> n.getId().equals(second.getId()))
                        .findFirst().orElseThrow().getStatus(),
                "COMPLETED bildirime dokunulmamali");
    }
}
