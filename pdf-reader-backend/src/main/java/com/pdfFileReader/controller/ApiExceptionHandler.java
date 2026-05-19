package com.pdfFileReader.controller;

import com.pdfFileReader.exception.PdfReadException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PdfReadException.class)
    public ResponseEntity<ApiError> handlePdfReadException(PdfReadException exception) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ApiError("PDF_READ_FAILED", exception.getMessage()));
    }

    public record ApiError(String code, String message) {
    }
}
