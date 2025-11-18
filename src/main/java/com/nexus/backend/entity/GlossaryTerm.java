package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "glossary_terms",
    uniqueConstraints = @UniqueConstraint(
        name = "glossary_terms_user_korean_unique",
        columnNames = {"user_id", "korean_term"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlossaryTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "korean_term", nullable = false, length = 255)
    private String koreanTerm;

    @Column(name = "english_term", length = 255)
    private String englishTerm;

    @Column(name = "vietnamese_term", length = 255)
    private String vietnameseTerm;

    @Column(length = 100)
    private String abbreviation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(name = "example_sentence", columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(length = 100)
    private String domain;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(nullable = false, length = 20)
    private String status = "AUTO_EXTRACTED";  // AUTO_EXTRACTED, USER_ADDED, USER_EDITED (data source)

    @Column(name = "is_verified")
    private Boolean isVerified = false;  // Verification state (independent from status)

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
