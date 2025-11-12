package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 번역-용어 매핑 엔티티
 *
 * 번역 시 탐지된 전문용어와 용어집의 매핑 정보를 저장합니다.
 * 원문에서의 위치 정보도 함께 저장하여 하이라이트에 활용합니다.
 */
@Entity
@Table(name = "translation_terms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 번역 참조
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translation_id", nullable = false)
    private Translation translation;

    /**
     * 용어집 참조
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "glossary_term_id", nullable = false)
    private GlossaryTerm glossaryTerm;

    /**
     * 원문에서의 시작 위치 (인덱스)
     */
    @Column(name = "position_start", nullable = false)
    private Integer positionStart;

    /**
     * 원문에서의 종료 위치 (인덱스)
     */
    @Column(name = "position_end", nullable = false)
    private Integer positionEnd;

    /**
     * 탐지된 실제 텍스트
     */
    @Column(name = "matched_text", nullable = false, length = 255)
    private String matchedText;

    /**
     * 생성일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
