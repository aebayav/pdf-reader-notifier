package com.pdfFileReader.repository;

import com.pdfFileReader.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Mükerrer yükleme kontrolü BELGE BAZINDA VE KULLANICI BAZINDA yapılır:
     * aynı kullanıcı aynı içerik hash'ine sahip belgeyi daha önce işlediyse
     * tüm notları atlanır; farklı kullanıcılar aynı belgeyi bağımsız işler.
     */
    boolean existsBySourceHashAndUserId(String sourceHash, UUID userId);

    /** Kullanicinin bildirimleri, son tarihe gore artan. */
    List<Notification> findAllByUserIdOrderByDueDateAsc(UUID userId);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
