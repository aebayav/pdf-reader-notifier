package com.pdfFileReader.controller;

import com.pdfFileReader.auth.AuthFilter;
import com.pdfFileReader.domain.dto.ContractAnalysisResponse;
import com.pdfFileReader.domain.dto.ExtractedTextResponse;
import com.pdfFileReader.domain.dto.UpdateNotificationRequest;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.ProcessingJob;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.domain.service.impl.JobService;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final JobService jobService;

    public NotificationController(
            NotificationService notificationService,
            EmailService emailService,
            JobService jobService
    ) {
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.jobService = jobService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ProcessingJob> upload(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        ProcessingJob job = jobService.enqueue(file, false, userId);

        return ResponseEntity.accepted().body(job);
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
    public ResponseEntity<ProcessingJob> aiUpload(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        ProcessingJob job = jobService.enqueue(file, true, userId);

        return ResponseEntity.accepted().body(job);
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<ProcessingJob> jobStatus(
            @PathVariable UUID id,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        return ResponseEntity.ok(jobService.getJob(id, userId));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<ProcessingJob>> recentJobs(
            @RequestParam(defaultValue = "10") int limit,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        return ResponseEntity.ok(jobService.recentJobs(limit, userId));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Notification>> upcomingNotifications(
            @RequestParam(defaultValue = "7") int days,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        return ResponseEntity.ok(notificationService.findUpcoming(days, userId));
    }

    @GetMapping
    public ResponseEntity<List<Notification>> listNotifications(
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        return ResponseEntity.ok(notificationService.findAll(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Notification> updateNotification(
            @PathVariable UUID id,
            @RequestBody UpdateNotificationRequest request,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        return ResponseEntity.ok(notificationService.update(id, userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        notificationService.delete(id, userId);

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
