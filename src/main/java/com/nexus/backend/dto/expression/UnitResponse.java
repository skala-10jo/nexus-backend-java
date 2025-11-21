package com.nexus.backend.dto.expression;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unit Response DTO
 *
 * @author NEXUS Team
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitResponse {

    private String unit;
    private Long totalCount;
    private Long learnedCount;
}
