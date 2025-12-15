package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detailed response DTO for File entity.
 * Used for detail views with complete file information.

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDetailResponse {

    // Common file fields
    private UUID id;
    private String fileType;  // "DOCUMENT", "VIDEO", "AUDIO"
    private String originalFilename;
    private String storedFilename;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime uploadDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Document-specific fields (populated only for DOCUMENT type)
    private DocumentFileDetail documentDetail;

    // Document content (text extracted from document pages)
    private List<ContentPageDto> contents;

    // Video-specific fields (populated only for VIDEO type)
    private VideoFileDetail videoDetail;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentFileDetail {
        private String language;
        private Integer pageCount;
        private Integer wordCount;
        private Integer characterCount;
        private Boolean isAnalyzed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoFileDetail {
        private Integer durationSeconds;
        private String videoCodec;
        private String audioCodec;
        private String resolution;
        private BigDecimal frameRate;
        private Boolean hasAudio;
        private String sttStatus;
        private String translationStatus;
        private String sourceLanguage;
        private String targetLanguage;
        private String originalSubtitlePath;
        private String translatedSubtitlePath;
        private String errorMessage;
    }
}
