package com.pdfFileReader.controller;

import com.pdfFileReader.exception.AuthException;
import com.pdfFileReader.exception.GeminiException;
import com.pdfFileReader.exception.NotificationNotFoundException;
import com.pdfFileReader.exception.PdfReadException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PdfReadException.class)
    public ResponseEntity<ApiError> handlePdfReadException(PdfReadException exception) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ApiError("PDF_READ_FAILED", exception.getMessage()));
    }

    @ExceptionHandler(GeminiException.class)
    public ResponseEntity<ApiError> handleGeminiException(GeminiException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("GEMINI_FAILED", exception.getMessage()));
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(NotificationNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError("NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuthException(AuthException exception) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("AUTH_FAILED", exception.getMessage()));
    }

    /**
     * Bean Validation (@Valid) hatalarini yakalar. Her alan hatasi
     * virgullerle birlestirilip tek bir mesaj olarak doner.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VALIDATION_ERROR", message));
    }

    public record ApiError(String code, String message) {
    }
}
