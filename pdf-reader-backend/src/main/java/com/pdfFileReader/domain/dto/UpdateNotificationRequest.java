package com.pdfFileReader.domain.dto;

import com.pdfFileReader.domain.entity.Status;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Kismi guncelleme istegi: sadece null olmayan alanlar guncellenir.
 * Sadece durum degistirmek icin {"status":"COMPLETED"} gondermek yeterlidir.
 */
public record UpdateNotificationRequest(
        @Size(min = 1, max = 200, message = "Baslik 1-200 karakter olmali.")
        String title,

        @Size(max = 2000, message = "Aciklama en fazla 2000 karakter olabilir.")
        String description,

        LocalDate dueDate,
        Status status
) {
}
