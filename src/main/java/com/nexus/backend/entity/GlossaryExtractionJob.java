package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "glossary_extraction_jobs",
    uniqueConstraints = @UniqueConstraint(
        name = "glossary_extraction_jobs_file_unique",
        columnNames = {"file_id"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlossaryExtractionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";  // PENDING, PROCESSING, COMPLETED, FAILED

    @Builder.Default
    private Integer progress = 0;

    @Column(name = "terms_extracted")
    @Builder.Default
    private Integer termsExtracted = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
