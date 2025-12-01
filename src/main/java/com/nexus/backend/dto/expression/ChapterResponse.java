package com.nexus.backend.dto.expression;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chapter Response DTO
 *
 * @author NEXUS Team
 * @since 2025-01-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponse {

    private String chapter;
    private Long totalCount;
    private Long learnedCount;
}
