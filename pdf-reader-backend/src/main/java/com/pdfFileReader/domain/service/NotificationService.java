package com.pdfFileReader.domain.service;

import com.pdfFileReader.domain.dto.ContractAnalysisResponse;
import com.pdfFileReader.domain.dto.UpdateNotificationRequest;
import com.pdfFileReader.domain.entity.Notification;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    String extractText(MultipartFile file);
    List<Notification> processAndSaveNotifications(MultipartFile file);
    ContractAnalysisResponse analyzeContract(MultipartFile file);

    /** Tum bildirimleri son tarihe gore artan sirada dondurur. */
    List<Notification> findAll();

    /** Sadece gonderilen (null olmayan) alanlari gunceller. */
    Notification update(UUID id, UpdateNotificationRequest request);

    void delete(UUID id);

    /**
     * Yaklasan ve gecikmis bildirimleri dondurur: dueDate'i bugun ile
     * bugun+days arasinda olanlar + bugunden gecmis ama kapatilmamis
     * (COMPLETED/CLOSED olmayan) tum bildirimler. Son tarihe gore sirali.
     */
    List<Notification> findUpcoming(int days);

    /** Süresi gecmis IN_PROGRESS bildirimleri DUE_DATE'e ceker, adedi dondurur. */
    int markOverdue();
}
