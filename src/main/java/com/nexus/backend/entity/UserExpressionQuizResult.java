package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * UserExpressionQuizResult Entity
 * 사용자별 표현 퀴즈 결과 (예문별 정답/오답 횟수 추적)
 */
@Entity
@Table(name = "user_expression_quiz_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExpressionQuizResult {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expression_id", nullable = false)
    private Expression expression;

    @Column(name = "example_index", nullable = false)
    private Integer exampleIndex; // expressions.examples JSONB 배열의 인덱스

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private Integer correctCount = 0;

    @Column(name = "incorrect_count", nullable = false)
    @Builder.Default
    private Integer incorrectCount = 0;

    @Column(name = "last_attempted_at")
    private ZonedDateTime lastAttemptedAt;

    @Column(name = "daily_correct_delta", nullable = false)
    @Builder.Default
    private Integer dailyCorrectDelta = 0;

    @Column(name = "daily_incorrect_delta", nullable = false)
    @Builder.Default
    private Integer dailyIncorrectDelta = 0;

    @Column(name = "delta_date")
    private java.time.LocalDate deltaDate;

    /**
     * 정답 처리
     */
    public void recordCorrect() {
        this.correctCount++;
        this.lastAttemptedAt = ZonedDateTime.now();
        updateDailyDelta(true);
    }

    /**
     * 오답 처리
     */
    public void recordIncorrect() {
        this.incorrectCount++;
        this.lastAttemptedAt = ZonedDateTime.now();
        updateDailyDelta(false);
    }

    /**
     * 일별 증분 업데이트
     */
    private void updateDailyDelta(boolean isCorrect) {
        java.time.LocalDate today = java.time.LocalDate.now();

        // 날짜가 바뀌면 증분 리셋
        if (this.deltaDate == null || !this.deltaDate.equals(today)) {
            this.dailyCorrectDelta = 0;
            this.dailyIncorrectDelta = 0;
            this.deltaDate = today;
        }

        // 증분 추가
        if (isCorrect) {
            this.dailyCorrectDelta++;
        } else {
            this.dailyIncorrectDelta++;
        }
    }

    /**
     * 총 시도 횟수
     */
    public int getTotalAttempts() {
        return correctCount + incorrectCount;
    }
}
