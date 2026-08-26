package com.pdfFileReader.repository;

import com.pdfFileReader.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Mükerrer yükleme kontrolü BELGE BAZINDA yapılır: aynı içerik hash'ine
     * sahip belge daha önce işlendiyse tüm notları atlanır. Başlık/açıklama
     * bazlı kontrol farklı sözleşmelerdeki aynı satırları yanlışlıkla atlardı.
     */
    boolean existsBySourceHash(String sourceHash);
}
