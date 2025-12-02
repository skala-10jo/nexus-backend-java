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
 *
 * @author NEXUS Team
 * @since 2025-12-01
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
    private Integer exampleIndex;  // expressions.examples JSONB 배열의 인덱스

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private Integer correctCount = 0;

    @Column(name = "incorrect_count", nullable = false)
    @Builder.Default
    private Integer incorrectCount = 0;

    @Column(name = "last_attempted_at")
    private ZonedDateTime lastAttemptedAt;

    /**
     * 정답 처리
     */
    public void recordCorrect() {
        this.correctCount++;
        this.lastAttemptedAt = ZonedDateTime.now();
    }

    /**
     * 오답 처리
     */
    public void recordIncorrect() {
        this.incorrectCount++;
        this.lastAttemptedAt = ZonedDateTime.now();
    }

    /**
     * 총 시도 횟수
     */
    public int getTotalAttempts() {
        return correctCount + incorrectCount;
    }
}
