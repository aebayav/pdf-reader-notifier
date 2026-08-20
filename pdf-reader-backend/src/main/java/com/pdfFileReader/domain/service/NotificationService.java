package com.pdfFileReader.domain.service;

import com.pdfFileReader.domain.dto.ContractAnalysisResponse;
import com.pdfFileReader.domain.entity.Notification;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NotificationService {
    String extractText(MultipartFile file);
    List<Notification> processAndSaveNotifications(MultipartFile file);
    ContractAnalysisResponse analyzeContract(MultipartFile file);
}
