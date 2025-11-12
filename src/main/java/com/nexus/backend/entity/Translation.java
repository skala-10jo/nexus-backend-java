package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 번역 엔티티
 *
 * 사용자가 수행한 번역 기록을 저장합니다.
 * 프로젝트와 연결되어 컨텍스트 기반 번역 여부를 추적합니다.
 */
@Entity
@Table(name = "translations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 사용자 참조
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 프로젝트 참조 (선택)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    /**
     * 원문
     */
    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    /**
     * 번역문
     */
    @Column(name = "translated_text", nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    /**
     * 원본 언어 (ko, en, ja, vi 등)
     */
    @Column(name = "source_language", nullable = false, length = 10)
    private String sourceLanguage;

    /**
     * 목표 언어
     */
    @Column(name = "target_language", nullable = false, length = 10)
    private String targetLanguage;

    /**
     * 컨텍스트 사용 여부
     */
    @Column(name = "context_used", nullable = false)
    @Builder.Default
    private Boolean contextUsed = false;

    /**
     * 사용된 컨텍스트 요약
     */
    @Column(name = "context_summary", columnDefinition = "TEXT")
    private String contextSummary;

    /**
     * 탐지된 용어 개수
     */
    @Column(name = "terms_detected", nullable = false)
    @Builder.Default
    private Integer termsDetected = 0;

    /**
     * 생성일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 탐지된 용어 매핑
     */
    @OneToMany(mappedBy = "translation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TranslationTerm> translationTerms = new ArrayList<>();
}
