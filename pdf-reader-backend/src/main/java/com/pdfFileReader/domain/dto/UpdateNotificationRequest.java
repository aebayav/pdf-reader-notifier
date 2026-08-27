package com.pdfFileReader.domain.dto;

import com.pdfFileReader.domain.entity.Status;

import java.time.LocalDate;

/**
 * Kismi guncelleme istegi: sadece null olmayan alanlar guncellenir.
 * Sadece durum degistirmek icin {"status":"COMPLETED"} gondermek yeterlidir.
 */
public record UpdateNotificationRequest(
        String title,
        String description,
        LocalDate dueDate,
        Status status
) {
}
