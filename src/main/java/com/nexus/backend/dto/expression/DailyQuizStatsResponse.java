package com.nexus.backend.dto.expression;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Daily Quiz Stats Response DTO
 * 일별 퀴즈 통계 응답
 *
 * @author NEXUS Team
 * @since 2025-12-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyQuizStatsResponse {

    private LocalDate date;          // 날짜
    private Long correctCount;       // 정답 수
    private Long incorrectCount;     // 오답 수
    private Double accuracyRate;     // 정답률 (%)
    private Long totalAttempts;      // 총 시도 횟수

    /**
     * 정답률과 총 시도 횟수를 계산하여 설정
     * Builder 사용 후 호출 필요
     */
    public DailyQuizStatsResponse calculate() {
        long correct = correctCount != null ? correctCount : 0;
        long incorrect = incorrectCount != null ? incorrectCount : 0;
        long total = correct + incorrect;

        this.totalAttempts = total;
        this.accuracyRate = total > 0 ? Math.round(correct * 1000.0 / total) / 10.0 : 0.0;

        return this;
    }

    /**
     * 빌더로 객체 생성 후 계산된 필드를 자동으로 설정하는 정적 팩토리 메서드
     */
    public static DailyQuizStatsResponse of(LocalDate date, Long correctCount, Long incorrectCount) {
        return DailyQuizStatsResponse.builder()
                .date(date)
                .correctCount(correctCount)
                .incorrectCount(incorrectCount)
                .build()
                .calculate();
    }
}
