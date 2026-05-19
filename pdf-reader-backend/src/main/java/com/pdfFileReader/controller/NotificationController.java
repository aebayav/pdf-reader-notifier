package com.pdfFileReader.controller;

import com.pdfFileReader.domain.entity.Notification;
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

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<Notification>> uploadPdf(@RequestParam("file") MultipartFile file) {

        List<Notification> result = notificationService.processAndSaveNotifications(file);

        
        return ResponseEntity.ok(result);
    }
}
