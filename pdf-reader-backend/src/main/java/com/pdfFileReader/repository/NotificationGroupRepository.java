package com.pdfFileReader.repository;

import com.pdfFileReader.domain.entity.NotificationGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationGroupRepository extends JpaRepository<NotificationGroup, UUID> {
    List<NotificationGroup> findAllByUserIdOrderByCreatedAtAsc(UUID userId);
    boolean existsByIdAndUserId(UUID id, UUID userId);
}