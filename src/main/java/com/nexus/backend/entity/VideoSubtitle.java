package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
    name = "video_subtitles",
    uniqueConstraints = @UniqueConstraint(columnNames = {"video_file_id", "sequence_number"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSubtitle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_file_id", nullable = false)
    private VideoFile videoFile;

    // 시퀀스 및 타이밍
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "start_time_ms", nullable = false)
    private Long startTimeMs;

    @Column(name = "end_time_ms", nullable = false)
    private Long endTimeMs;

    // 텍스트
    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "translated_text", columnDefinition = "TEXT")
    private String translatedText;

    // 원본 언어 코드 (ko, en, ja, vi 등)
    @Column(name = "original_language", length = 10, nullable = false)
    @Builder.Default
    private String originalLanguage = "ko";

    // 다국어 번역 저장 (JSONB: {"en": "...", "ja": "...", "vi": "..."})
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "translations", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> translations = new java.util.HashMap<>();

    // 화자 정보 (선택사항)
    @Column(name = "speaker_id")
    private Integer speakerId;

    // 신뢰도 점수 (STT 결과)
    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    // 탐지된 전문용어 (JSONB - 배열 형태로 저장)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detected_terms", columnDefinition = "jsonb")
    private List<Object> detectedTerms;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
