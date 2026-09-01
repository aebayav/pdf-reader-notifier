package com.pdfFileReader.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.util.UUID;

@Table(name = "notifications") // Create table annotiation
@Getter // Getter and setters via Lombok
@Setter
@NoArgsConstructor //Constructors via Lombok
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dueDate;

    @CreationTimestamp // Oluşturulma tarihini Spring otomatik yönetir
    @Column(updatable = false)
    private LocalDate createDate;

    @Enumerated(EnumType.STRING) // Enum ismini (Örn: "COMPLETED") DB'ye yazar
    private Status status;

    /** Kaynak belgenin icerik hash'i (mükerrer yükleme kontrolü için). */
    @Column(name = "source_hash", length = 64)
    private String sourceHash;

    /** Bildirimin sahibi kullanici (veri izolasyonu). */
    @Column(name = "user_id")
    private UUID userId;
}