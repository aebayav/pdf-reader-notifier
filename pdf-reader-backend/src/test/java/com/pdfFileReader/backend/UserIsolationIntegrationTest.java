package com.pdfFileReader.backend;

import com.pdfFileReader.domain.dto.UpdateNotificationRequest;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.User;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.exception.NotificationNotFoundException;
import com.pdfFileReader.repository.UserRepository;
import com.pdfFileReader.testutil.TestPdfFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bildirimler kullaniciya ozeldir: kimse baskasinin kaydini goremez/değiştiremez. */
@SpringBootTest
@Transactional
class UserIsolationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    private UUID userA;
    private UUID userB;

    @BeforeEach
    void createUsers() {
        userA = saveUser("izolasyon-a-" + System.currentTimeMillis());
        userB = saveUser("izolasyon-b-" + System.currentTimeMillis());
    }

    private UUID saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("x");
        userRepository.save(user);
        return user.getId();
    }

    @Test
    void userBCannotSeeUserANotifications() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("ozel.pdf",
                "Ozel bildirim 14.03.2026 tarihinde tamamlanacaktir.",
                "Bu satir A kullanicisina ait olmali ve asenkron test suresi boyunca taraflarin tum yukumlulukleri ilgili mevzuat hukumlerine gore degerlendirilecektir.");

        List<Notification> saved = notificationService.processAndSaveNotifications(pdf, userA);
        assertFalse(saved.isEmpty(), "A'nin bildirimi olusmali");

        List<Notification> forB = notificationService.findAll(userB);
        assertTrue(forB.stream().noneMatch(n -> n.getUserId().equals(userA)),
                "B, A'nin bildirimlerini gormemeli");

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.update(saved.get(0).getId(), userB,
                        new UpdateNotificationRequest("calinti", null, null, null)),
                "B, A'nin bildirimini guncelleyememeli");

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.delete(saved.get(0).getId(), userB),
                "B, A'nin bildirimini silememeli");
    }

    @Test
    void sameDocumentIsIndependentPerUser() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("ortak-belge.pdf",
                "Ortak belge bildirimi 20.05.2026 tarihinde yapilacaktir.",
                "Iki farkli kullanici ayni belgeyi bagimsiz olarak isleyebilmeli ve bu dogrulama icin yeterli uzunlukta aciklama metni bulunmalidir.");

        List<Notification> forA = notificationService.processAndSaveNotifications(pdf, userA);
        List<Notification> forB = notificationService.processAndSaveNotifications(pdf, userB);

        assertFalse(forA.isEmpty(), "A isledi");
        assertFalse(forB.isEmpty(), "B ayni belgeyi BAGIMSIZ isleyebilmeli (dedup kullanici bazinda)");
        assertEquals(forA.size(), forB.size());
    }
}
