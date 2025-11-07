package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDetailResponse {
    private UUID id;
    private String originalFilename;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private LocalDateTime uploadDate;
    private String status;
    private Boolean isAnalyzed;
    private DocumentMetadataDto metadata;
}
