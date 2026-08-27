package com.pdfFileReader.exception;

import java.util.UUID;

/** Istenen bildirim veritabaninda bulunamadi. */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID id) {
        super("Bildirim bulunamadi: " + id);
    }
}
