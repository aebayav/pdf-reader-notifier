package com.pdfFileReader.domain.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.exception.GeminiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void buildPromptDoesNotTruncateContractSizedText() {
        String longText = "x".repeat(250_000);

        String prompt = service.buildPrompt(longText);

        assertTrue(prompt.contains(longText),
                "250K karakterlik sozlesme metni TEK istekte eksiksiz gitmeli (parcalanmamali)");
        assertFalse(prompt.contains("kisaltildi"), "kisaltma notu olmamali");
    }

    @Test
    void buildPromptTruncatesOnlyBeyondModelLimit() {
        String hugeText = "y".repeat(1_200_000);

        String prompt = service.buildPrompt(hugeText);

        assertTrue(prompt.contains("kisaltildi"), "1M siniri asilinca kisaltma notu dusmeli");
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

    @Test
    void readKeyFromEnvFileParsesGEMINI_API_KEY(@TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path envFile = tempDir.resolve(".env");
        java.nio.file.Files.writeString(envFile, """
                # yorum satiri
                GEMINI_API_KEY=test-key-123

                """);

        assertEquals("test-key-123", service.readKeyFromEnvFile(envFile));
    }

    @Test
    void readKeyFromEnvFileHandlesMissingAndEmpty(@TempDir java.nio.file.Path tempDir) throws Exception {
        assertEquals("", service.readKeyFromEnvFile(tempDir.resolve("yok.env")),
                "olmayan dosya bos donmeli");

        java.nio.file.Path envFile = tempDir.resolve("bos.env");
        java.nio.file.Files.writeString(envFile, "BASKA_DEGISKEN=deger\n");
        assertEquals("", service.readKeyFromEnvFile(envFile),
                "GEMINI_API_KEY satiri yoksa bos donmeli");
    }
}
