package com.pdfFileReader.mail;

import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private EmailService configuredService(JavaMailSender mailSender) {
        return new EmailService(providerOf(mailSender), true,
                "bildirim@example.com", "kullanici@example.com", "smtp.example.com");
    }

    private EmailService unconfiguredService() {
        return new EmailService(providerOf(null), true, "", "", "");
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> providerOf(JavaMailSender mailSender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        return provider;
    }

    @Test
    void sendsSummaryWhenConfiguredAndThereAreUpcoming() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = configuredService(mailSender);

        Notification n = new Notification();
        n.setTitle("Yer Teslimi");
        n.setDueDate(LocalDate.now().plusDays(3));
        n.setStatus(Status.IN_PROGRESS);

        service.sendDailySummary(List.of(n), 1);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void skipsSilentlyWhenNotConfigured() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = unconfiguredService();

        service.sendDailySummary(List.of(), 0);
        service.sendDailySummary(List.of(new Notification()), 1);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void skipsWhenNothingToReport() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = configuredService(mailSender);

        service.sendDailySummary(List.of(), 0);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testMailThrowsWhenNotConfigured() {
        EmailService service = unconfiguredService();

        assertThrows(IllegalStateException.class, service::sendTestMail,
                "yapilandirilmamis SMTP'de test maili hata firlatmali");
    }
}
