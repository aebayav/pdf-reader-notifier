package com.pdfFileReader.controller;

import com.pdfFileReader.domain.dto.ContractAnalysisResponse;
import com.pdfFileReader.domain.dto.ExtractedTextResponse;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.service.GeminiService;
import com.pdfFileReader.domain.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final GeminiService geminiService;

    public NotificationController(NotificationService notificationService, GeminiService geminiService) {
        this.notificationService = notificationService;
        this.geminiService = geminiService;
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
}
