package com.pdfFileReader.domain.service;

import com.pdfFileReader.domain.dto.ContractAnalysisResponse;
import com.pdfFileReader.domain.dto.UpdateNotificationRequest;
import com.pdfFileReader.domain.entity.Notification;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    String extractText(MultipartFile file);
    List<Notification> processAndSaveNotifications(MultipartFile file, UUID userId);

    List<Notification> processAndSaveNotifications(MultipartFile file, UUID userId, UUID groupId);

    void saveGroup(List<Notification> notifications);
    ContractAnalysisResponse analyzeContract(MultipartFile file);

    /** Kullanicinin tum bildirimlerini son tarihe gore artan sirada dondurur. */
    List<Notification> findAll(UUID userId);

    /** Sadece gonderilen (null olmayan) alanlari gunceller; baskasinin kaydinda 404. */
    Notification update(UUID id, UUID userId, UpdateNotificationRequest request);

    void delete(UUID id, UUID userId);

    /**
     * Kullanicinin yaklasan ve gecikmis bildirimleri: dueDate'i bugun ile
     * bugun+days arasinda olanlar + bugunden gecmis ama kapatilmamis
     * (COMPLETED/CLOSED olmayan) tum bildirimler. Son tarihe gore sirali.
     */
    List<Notification> findUpcoming(int days, UUID userId);

    /** TUM kullanicilarin yaklasan bildirimleri (gunluk e-posta ozeti icin). */
    List<Notification> findUpcomingAllUsers(int days);

    /** Süresi gecmis IN_PROGRESS bildirimleri DUE_DATE'e ceker, adedi dondurur. */
    int markOverdue();
}
