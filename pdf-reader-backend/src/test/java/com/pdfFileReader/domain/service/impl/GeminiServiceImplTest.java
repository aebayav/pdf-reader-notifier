package com.pdfFileReader.domain.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.exception.GeminiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiServiceImplTest {

    private GeminiServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GeminiServiceImpl(null, null, new ObjectMapper(),
                "test-key", "gemini-3.6-flash", "https://example.com");
    }

    @Test
    void buildPromptContainsRulesAndText() {
        String prompt = service.buildPrompt("Yer teslim tarihi 10.04.2025.");

        assertTrue(prompt.contains("Yer teslim tarihi 10.04.2025."), "metin prompt icinde olmali");
        assertTrue(prompt.contains("YYYY-AA-GG"), "tarih formati kurali olmali");
        assertTrue(prompt.contains("JSON dizisi"), "JSON kurali olmali");
    }

    @Test
    void parseToNotificationsBuildsValidNotifications() {
        String json = """
                [
                  {"title": "Yer Teslimi", "description": "Yer teslim tarihi 10.04.2025 olarak kararlastirilmistir.", "dueDate": "2025-04-10"},
                  {"title": "Kesin Kabul", "description": "Kesin kabul 30.09.2025 tarihinde yapilacaktir.", "dueDate": "30.09.2025"}
                ]
                """;

        List<Notification> notifications = service.parseToNotifications(json);

        assertEquals(2, notifications.size());
        assertEquals("Yer Teslimi", notifications.get(0).getTitle());
        assertEquals(LocalDate.of(2025, 4, 10), notifications.get(0).getDueDate());
        assertNotNull(notifications.get(0).getDescription());

        assertEquals("Kesin Kabul", notifications.get(1).getTitle());
        assertEquals(LocalDate.of(2025, 9, 30), notifications.get(1).getDueDate(),
                "GG.AA.YYYY formati da cozulmeli");
    }

    @Test
    void parseToNotificationsSkipsInvalidEntries() {
        String json = """
                [
                  {"title": "", "description": "x", "dueDate": "2025-04-10"},
                  {"title": "Tarih Yok", "description": "x", "dueDate": null},
                  {"title": "Bozuk Tarih", "description": "x", "dueDate": "bilinmiyor"},
                  {"title": "Gecerli", "description": "x", "dueDate": "2025-05-01"}
                ]
                """;

        List<Notification> notifications = service.parseToNotifications(json);

        assertEquals(1, notifications.size(), "bos baslik, eksik ve bozuk tarih elenmeli");
        assertEquals("Gecerli", notifications.get(0).getTitle());
        assertEquals(LocalDate.of(2025, 5, 1), notifications.get(0).getDueDate());
    }

    @Test
    void parseToNotificationsRejectsMalformedJson() {
        assertThrows(GeminiException.class, () -> service.parseToNotifications("bu bir JSON degil"));
    }

    @Test
    void parseToNotificationsAcceptsNullDescription() {
        String json = """
                [{"title": "Sadece Baslik", "dueDate": "2025-06-01"}]
                """;

        List<Notification> notifications = service.parseToNotifications(json);

        assertEquals(1, notifications.size());
        assertNull(notifications.get(0).getDescription());
    }
}
