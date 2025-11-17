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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "video_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    // 영상 메타데이터
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "video_codec", length = 50)
    private String videoCodec;

    @Column(name = "audio_codec", length = 50)
    private String audioCodec;

    @Column(name = "resolution", length = 20)
    private String resolution;

    @Column(name = "frame_rate", precision = 5, scale = 2)
    private BigDecimal frameRate;

    @Column(name = "has_audio", nullable = false)
    @Builder.Default
    private Boolean hasAudio = true;

    // 처리 상태
    @Column(name = "stt_status", nullable = false, length = 20)
    @Builder.Default
    private String sttStatus = "pending";

    @Column(name = "translation_status", nullable = false, length = 20)
    @Builder.Default
    private String translationStatus = "pending";

    // 언어 설정
    @Column(name = "source_language", length = 10)
    private String sourceLanguage;

    @Column(name = "target_language", length = 10)
    private String targetLanguage;

    // 결과 파일 경로
    @Column(name = "original_subtitle_path", length = 500)
    private String originalSubtitlePath;

    @Column(name = "translated_subtitle_path", length = 500)
    private String translatedSubtitlePath;

    // 에러 정보
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // 자막 세그먼트 (1:N 관계)
    @OneToMany(mappedBy = "videoDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VideoSubtitle> subtitles = new ArrayList<>();

    // 선택된 용어집 문서들 (M:N 관계)
    @OneToMany(mappedBy = "videoDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VideoTranslationGlossary> glossaries = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
