package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document-specific metadata extension entity.
 * Has a 1:1 relationship with File entity where fileType = DOCUMENT.
 *
 * @author NEXUS Team
 * @version 1.0
 * @since 2025-01-18
 */
@Entity
@Table(name = "document_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFile {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private File file;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "character_count")
    private Integer characterCount;

    @Column(name = "is_analyzed", nullable = false)
    @Builder.Default
    private Boolean isAnalyzed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Note: DocumentContent는 기존 Document 구조와 연결되어 있으므로
    // 새로운 File 구조와는 직접 연결하지 않습니다.
    // 필요 시 DocumentContentRepository로 쿼리하여 조회할 수 있습니다.
}
