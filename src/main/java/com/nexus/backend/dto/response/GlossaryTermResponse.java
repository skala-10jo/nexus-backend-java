package com.nexus.backend.dto.response;

import com.nexus.backend.entity.GlossaryTerm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlossaryTermResponse {

    private UUID id;
    private UUID projectId;
    private String koreanTerm;
    private String englishTerm;
    private String abbreviation;
    private String definition;
    private String context;
    private String domain;
    private BigDecimal confidenceScore;
    private String status;
    private Boolean isVerified;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DocumentInfo> documents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentInfo {
        private UUID id;
        private String originalFilename;
    }

    public static GlossaryTermResponse from(GlossaryTerm term) {
        List<DocumentInfo> documentInfos = term.getDocuments() != null
            ? term.getDocuments().stream()
                .map(doc -> DocumentInfo.builder()
                    .id(doc.getId())
                    .originalFilename(doc.getOriginalFilename())
                    .build())
                .collect(Collectors.toList())
            : List.of();

        return GlossaryTermResponse.builder()
                .id(term.getId())
                .projectId(term.getProject() != null ? term.getProject().getId() : null)
                .koreanTerm(term.getKoreanTerm())
                .englishTerm(term.getEnglishTerm())
                .abbreviation(term.getAbbreviation())
                .definition(term.getDefinition())
                .context(term.getContext())
                .domain(term.getDomain())
                .confidenceScore(term.getConfidenceScore())
                .status(term.getStatus())
                .isVerified(term.getIsVerified())
                .usageCount(term.getUsageCount())
                .createdAt(term.getCreatedAt())
                .updatedAt(term.getUpdatedAt())
                .documents(documentInfos)
                .build();
    }
}
