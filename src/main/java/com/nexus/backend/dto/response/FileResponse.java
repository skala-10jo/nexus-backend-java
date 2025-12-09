package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for File entity.
 * Used for list views and basic file information.
 *
 * @author NEXUS Team
 * @version 1.1
 * @since 2025-01-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    private UUID id;
    private String fileType;  // "DOCUMENT", "VIDEO", "AUDIO"
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime uploadDate;
    private String status;
    private LocalDateTime createdAt;

    // Document-specific fields (null for videos)
    private Boolean isAnalyzed;
    private Integer pageCount;
    private String language;

    // Video-specific fields (null for documents)
    private Integer durationSeconds;
    private String resolution;
    private String sttStatus;
    private String translationStatus;

    // Video-specific fields for frontend compatibility
    /** 영상 길이 (초) - durationSeconds의 alias */
    private Integer duration;

    /** 자막 존재 여부 (sttStatus가 completed이거나 자막 레코드가 있으면 true) */
    private Boolean hasSubtitles;

    /** 영상 썸네일 URL (null이면 frontend에서 기본 아이콘 표시) */
    private String thumbnailUrl;

    /** 원본 언어 코드 (ko, en, ja, vi 등) */
    private String originalLanguage;

    /** 사용 가능한 언어 목록 (원본 + 번역된 언어들) */
    private List<String> availableLanguages;
}
