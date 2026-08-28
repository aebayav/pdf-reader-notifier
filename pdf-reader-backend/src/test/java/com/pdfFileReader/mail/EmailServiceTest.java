package com.pdfFileReader.mail;

import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.Status;
import com.pdfFileReader.mail.EmailService.MailCredentials;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceTest {

    /** Erisilemez SMTP: testlerde ag'a dusmeden hizli reddedilir. */
    private static final MailCredentials DEAD = new MailCredentials("127.0.0.1", 1, "k", "p");

    private EmailService disabledService() {
        return new EmailService(false, "bildirim@example.com", "kullanici@example.com", DEAD);
    }

    private EmailService configuredDeadService() {
        return new EmailService(true, "bildirim@example.com", "kullanici@example.com", DEAD);
    }

    @Test
    void isConfiguredReflectsEnabledAndCredentials() {
        assertFalse(disabledService().isConfigured(), "kapali servis configure edilmis sayilmamali");
        assertTrue(configuredDeadService().isConfigured(), "tum degerler varken configure sayilmali");
    }

    @Test
    void skipsSilentlyWhenNotConfigured() {
        EmailService service = disabledService();

        service.sendDailySummary(List.of(new Notification()), 1);
        service.sendDailySummary(List.of(), 0);
    }

    @Test
    void skipsWhenNothingToReport() {
        EmailService service = configuredDeadService();

        service.sendDailySummary(List.of(), 0);
    }

    @Test
    void testMailThrowsWhenNotConfigured() {
        EmailService service = disabledService();

        assertThrows(IllegalStateException.class, service::sendTestMail,
                "yapilandirilmamis SMTP'de test maili hata firlatmali");
    }

    @Test
    void testMailThrowsWhenSmtpRejects() {
        EmailService service = configuredDeadService();

        assertThrows(IllegalStateException.class, service::sendTestMail,
                "SMTP reddederse kullaniciya acik hata donmeli");
    }

    @Test
    void summaryDoesNotCrashWhenSmtpUnreachable() {
        EmailService service = configuredDeadService();

        Notification n = new Notification();
        n.setTitle("Yer Teslimi");
        n.setDueDate(LocalDate.now().plusDays(3));
        n.setStatus(Status.IN_PROGRESS);

        assertDoesNotThrow(() -> service.sendDailySummary(List.of(n), 1),
                "SMTP'ye ulasilamazsa gunluk ozet uygulamayi cokertmemeli");
    }
}
