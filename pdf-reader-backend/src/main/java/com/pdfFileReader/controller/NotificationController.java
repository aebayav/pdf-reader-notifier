package com.pdfFileReader.controller;

import com.pdfFileReader.auth.AuthFilter;
import com.pdfFileReader.domain.dto.ContractAnalysisResponse;
import com.pdfFileReader.domain.dto.CreateNotificationGroupRequest;
import com.pdfFileReader.domain.dto.ExtractedTextResponse;
import com.pdfFileReader.domain.dto.UpdateNotificationRequest;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.NotificationGroup;
import com.pdfFileReader.domain.entity.ProcessingJob;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.domain.service.impl.JobService;
import com.pdfFileReader.mail.EmailService;
import com.pdfFileReader.repository.NotificationGroupRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    /** Magic byte: tüm geçerli PDF dosyaları bu imzayla başlar. */
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-

    /** Kabul edilen maksimum dosya boyutu: 50 MB. */
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final JobService jobService;
    private final NotificationGroupRepository groupRepository;

    public NotificationController(
            NotificationService notificationService,
            EmailService emailService,
            JobService jobService,
            NotificationGroupRepository groupRepository
    ) {
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.jobService = jobService;
        this.groupRepository = groupRepository;
    }

    // ------------------------------------------------------------------ //
    // Upload endpointleri                                                  //
    // ------------------------------------------------------------------ //

    @PostMapping("/upload")
    public ResponseEntity<ProcessingJob> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "groupId", required = false) UUID groupId,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        validatePdfFile(file);
        validateGroup(groupId, userId);
        ProcessingJob job = jobService.enqueue(file, false, userId, groupId);
        return ResponseEntity.accepted().body(job);
    }

    @PostMapping("/ai-upload")
    public ResponseEntity<ProcessingJob> aiUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "groupId", required = false) UUID groupId,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        validatePdfFile(file);
        validateGroup(groupId, userId);
        ProcessingJob job = jobService.enqueue(file, true, userId, groupId);
        return ResponseEntity.accepted().body(job);
    }

    // ------------------------------------------------------------------ //
    // Analiz endpointleri (token zorunlu)                                 //
    // ------------------------------------------------------------------ //

    @PostMapping("/extract-text")
    public ResponseEntity<ExtractedTextResponse> extractText(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        validatePdfFile(file);
        String text = notificationService.extractText(file);
        return ResponseEntity.ok(new ExtractedTextResponse(text));
    }

    @PostMapping("/analyze-contract")
    public ResponseEntity<ContractAnalysisResponse> analyzeContract(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        validatePdfFile(file);
        ContractAnalysisResponse result = notificationService.analyzeContract(file);
        return ResponseEntity.ok(result);
    }

    // ------------------------------------------------------------------ //
    // Job sorgulama                                                        //
    // ------------------------------------------------------------------ //

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

    // ------------------------------------------------------------------ //
    // Bildirim CRUD                                                        //
    // ------------------------------------------------------------------ //

    @GetMapping("/upcoming")
    public ResponseEntity<List<Notification>> upcomingNotifications(
            @RequestParam(defaultValue = "7") int days,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        return ResponseEntity.ok(notificationService.findUpcoming(days, userId));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<NotificationGroup>> listGroups(
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        return ResponseEntity.ok(groupRepository.findAllByUserIdOrderByCreatedAtAsc(userId));
    }

    @PostMapping("/groups")
    public ResponseEntity<NotificationGroup> createGroup(
            @Valid @RequestBody CreateNotificationGroupRequest request,
            @RequestAttribute(AuthFilter.ATTR_USER_ID) UUID userId
    ) {
        NotificationGroup group = new NotificationGroup();
        group.setName(request.name().trim());
        group.setUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(groupRepository.save(group));
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
            @Valid @RequestBody UpdateNotificationRequest request,
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

    // ------------------------------------------------------------------ //
    // Mail testi                                                           //
    // ------------------------------------------------------------------ //

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

    // ------------------------------------------------------------------ //
    // Yardımcılar                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Dosyayı güvenlik açısından doğrular:
     * 1. Boyut ≤ 50 MB
     * 2. Magic byte %PDF- ile başlamalı (MIME spoofing'e karşı)
     * 3. Dosya adı sanitize edilir (path traversal önleme)
     */
    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dosya bos olamaz.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dosya boyutu 50 MB sinirini asiyor.");
        }

        // Magic byte kontrolü
        try {
            byte[] header = file.getBytes();
            if (header.length < PDF_MAGIC.length || !startsWith(header, PDF_MAGIC)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Gecersiz dosya formati: yalnizca PDF dosyalari kabul edilir.");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dosya okunamadi.");
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private void validateGroup(UUID groupId, UUID userId) {
        if (groupId != null && !groupRepository.existsByIdAndUserId(groupId, userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gecersiz grup.");
        }
    }
}
