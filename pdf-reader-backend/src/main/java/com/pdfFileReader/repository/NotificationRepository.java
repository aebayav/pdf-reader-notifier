package com.pdfFileReader.repository;

import com.pdfFileReader.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByTitleAndDueDate(String title, LocalDate dueDate);
}
