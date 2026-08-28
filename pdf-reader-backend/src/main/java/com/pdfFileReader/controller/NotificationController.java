package com.pdfFileReader.controller;

import com.pdfFileReader.domain.dto.ContractAnalysisResponse;
import com.pdfFileReader.domain.dto.ExtractedTextResponse;
import com.pdfFileReader.domain.dto.UpdateNotificationRequest;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.service.GeminiService;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.mail.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final GeminiService geminiService;
    private final EmailService emailService;

    public NotificationController(NotificationService notificationService, GeminiService geminiService, EmailService emailService) {
        this.notificationService = notificationService;
        this.geminiService = geminiService;
        this.emailService = emailService;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<Notification>> uploadPdf(@RequestParam("file") MultipartFile file) {
        List<Notification> result = notificationService.processAndSaveNotifications(file);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/extract-text")
    public ResponseEntity<ExtractedTextResponse> extractText(@RequestParam("file") MultipartFile file) {
        String text = notificationService.extractText(file);

        return ResponseEntity.ok(new ExtractedTextResponse(text));
    }

    @PostMapping("/analyze-contract")
    public ResponseEntity<ContractAnalysisResponse> analyzeContract(@RequestParam("file") MultipartFile file) {
        ContractAnalysisResponse result = notificationService.analyzeContract(file);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/ai-upload")
    public ResponseEntity<List<Notification>> aiUpload(@RequestParam("file") MultipartFile file) {
        List<Notification> result = geminiService.analyzeAndCreateNotifications(file);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Notification>> upcomingNotifications(
            @RequestParam(defaultValue = "7") int days
    ) {
        return ResponseEntity.ok(notificationService.findUpcoming(days));
    }

    @GetMapping
    public ResponseEntity<List<Notification>> listNotifications() {
        return ResponseEntity.ok(notificationService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Notification> updateNotification(
            @PathVariable UUID id,
            @RequestBody UpdateNotificationRequest request
    ) {
        return ResponseEntity.ok(notificationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id) {
        notificationService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mail-test")
    public ResponseEntity<ApiExceptionHandler.ApiError> sendTestMail() {
        try {
            emailService.sendTestMail();
            return ResponseEntity.ok(new ApiExceptionHandler.ApiError("OK", "Test epostasi gonderildi"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiExceptionHandler.ApiError("MAIL_NOT_CONFIGURED", e.getMessage()));
        }
    }
}
