package com.nexus.backend.dto.response;

import com.nexus.backend.entity.DocumentStatus;
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
public class DocumentResponse {
    private UUID id;
    private String originalFilename;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadDate;
    private String status;
    private Boolean isAnalyzed;
}
