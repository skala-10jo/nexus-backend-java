package com.nexus.backend.dto.response;

import com.nexus.backend.entity.GlossaryTerm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlossaryTermResponse {

    private UUID id;
    private UUID projectId;
    private String koreanTerm;
    private String englishTerm;
    private String vietnameseTerm;
    private String japaneseTerm;
    private String chineseTerm;
    private String abbreviation;
    private String definition;
    private String context;
    private String exampleSentence;
    private String note;
    private String domain;
    private BigDecimal confidenceScore;
    private String status;
    private Boolean isVerified;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GlossaryTermResponse from(GlossaryTerm term) {
        return GlossaryTermResponse.builder()
                .id(term.getId())
                .projectId(term.getProject() != null ? term.getProject().getId() : null)
                .koreanTerm(term.getKoreanTerm())
                .englishTerm(term.getEnglishTerm())
                .vietnameseTerm(term.getVietnameseTerm())
                .japaneseTerm(term.getJapaneseTerm())
                .chineseTerm(term.getChineseTerm())
                .abbreviation(term.getAbbreviation())
                .definition(term.getDefinition())
                .context(term.getContext())
                .exampleSentence(term.getExampleSentence())
                .note(term.getNote())
                .domain(term.getDomain())
                .confidenceScore(term.getConfidenceScore())
                .status(term.getStatus())
                .isVerified(term.getIsVerified())
                .usageCount(term.getUsageCount())
                .createdAt(term.getCreatedAt())
                .updatedAt(term.getUpdatedAt())
                .build();
    }
}
