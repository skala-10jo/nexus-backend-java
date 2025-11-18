package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for File entity.
 * Used for list views and basic file information.
 *
 * @author NEXUS Team
 * @version 1.0
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
}
