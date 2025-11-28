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

/**
 * Video-specific metadata extension entity.
 * Has a 1:1 relationship with File entity where fileType = VIDEO.
 *
 * @author NEXUS Team
 * @version 1.0
 * @since 2025-01-18
 */
@Entity
@Table(name = "video_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFile {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private File file;

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

    @Column(name = "stt_status", nullable = false, length = 20)
    @Builder.Default
    private String sttStatus = "pending";

    @Column(name = "translation_status", nullable = false, length = 20)
    @Builder.Default
    private String translationStatus = "pending";

    @Column(name = "source_language", length = 10)
    private String sourceLanguage;

    @Column(name = "target_language", length = 10)
    private String targetLanguage;

    @Column(name = "original_subtitle_path", length = 500)
    private String originalSubtitlePath;

    @Column(name = "translated_subtitle_path", length = 500)
    private String translatedSubtitlePath;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Note: VideoSubtitle는 기존 VideoDocument 구조와 연결되어 있으므로
    // 새로운 File 구조와는 직접 연결하지 않습니다.
    // 필요 시 VideoSubtitleRepository로 쿼리하여 조회할 수 있습니다.
}
