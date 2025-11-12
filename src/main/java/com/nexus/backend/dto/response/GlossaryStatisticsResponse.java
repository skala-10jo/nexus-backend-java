package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Glossary statistics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlossaryStatisticsResponse {

    /**
     * Total number of terms
     */
    private Long totalTerms;

    /**
     * Number of verified terms
     */
    private Long verifiedTerms;

    /**
     * Number of unverified terms
     */
    private Long unverifiedTerms;

    /**
     * Number of auto-extracted terms
     */
    private Long autoExtractedTerms;

    /**
     * Number of user-added terms
     */
    private Long userAddedTerms;

    /**
     * Number of user-edited terms
     */
    private Long userEditedTerms;
}
