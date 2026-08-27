package com.pdfFileReader.domain.dto;

/** Gemini'nin dondurmesi beklenen tek not JSON'u. */
public record GeminiNote(
        String title,
        String description,
        String dueDate
) {
}
