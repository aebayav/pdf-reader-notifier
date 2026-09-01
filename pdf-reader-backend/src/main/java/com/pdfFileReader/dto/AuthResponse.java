package com.pdfFileReader.dto;

/** Giris/kayit yaniti: opaque bearer token. */
public record AuthResponse(String token, String userId) {
}
