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
    private Long totalCount;      // 전체 표현 개수
    private Long quizAttemptedCount;  // 퀴즈를 푼 표현 개수
    private Long correctCount;    // 정답 개수
    private Long incorrectCount;  // 오답 개수

    /**
     * 정답률 계산 (퀴즈를 푼 표현 기준)
     */
    public Double getAccuracyRate() {
        long totalAttempts = (correctCount != null ? correctCount : 0) + (incorrectCount != null ? incorrectCount : 0);
        if (totalAttempts == 0) return 0.0;
        return (correctCount != null ? correctCount : 0) * 100.0 / totalAttempts;
    }
}
