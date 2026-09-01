package com.pdfFileReader.exception;

/** Kimlik dogrulama hatalari (401/409). */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }
}
