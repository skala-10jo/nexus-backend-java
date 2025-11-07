package com.nexus.backend.dto.response;

import com.nexus.backend.entity.GlossaryExtractionJob;
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
public class ExtractionJobResponse {

    private UUID id;
    private UUID documentId;
    private String status;
    private Integer progress;
    private Integer termsExtracted;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public static ExtractionJobResponse from(GlossaryExtractionJob job) {
        return ExtractionJobResponse.builder()
                .id(job.getId())
                .documentId(job.getDocument().getId())
                .status(job.getStatus())
                .progress(job.getProgress())
                .termsExtracted(job.getTermsExtracted())
                .errorMessage(job.getErrorMessage())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
