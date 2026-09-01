package com.pdfFileReader.domain.service;

import com.pdfFileReader.domain.entity.Notification;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** Yapay zeka (Gemini) ile metin anlamlandirma servisi. */
public interface GeminiService {

    /**
     * PDF'ten metni cikarir, Gemini'ye yollar ve dondurdugu JSON'dan
     * bildirimler olusturup veritabanina kaydeder.
     */
    List<Notification> analyzeAndCreateNotifications(MultipartFile file, UUID userId);
}
