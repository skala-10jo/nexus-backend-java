package com.nexus.backend.repository;

import com.nexus.backend.entity.UserExpressionQuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UserExpressionQuizResult Repository
 *
 * @author NEXUS Team
 * @since 2025-12-01
 */
@Repository
public interface UserExpressionQuizResultRepository extends JpaRepository<UserExpressionQuizResult, UUID> {

    /**
     * 사용자 + 표현 + 예문 인덱스로 조회
     */
    Optional<UserExpressionQuizResult> findByUserIdAndExpressionIdAndExampleIndex(
            UUID userId, UUID expressionId, Integer exampleIndex);

    /**
     * 사용자의 모든 퀴즈 결과 조회
     */
    List<UserExpressionQuizResult> findByUserId(UUID userId);

    /**
     * 사용자의 오답이 있는 퀴즈 결과 조회 (오답 횟수 > 0, 최근 시도 순)
     */
    @Query("SELECT r FROM UserExpressionQuizResult r " +
           "JOIN FETCH r.expression e " +
           "WHERE r.user.id = :userId AND r.incorrectCount > 0 " +
           "ORDER BY r.lastAttemptedAt DESC")
    List<UserExpressionQuizResult> findMistakesByUserId(@Param("userId") UUID userId);

    /**
     * 사용자의 특정 표현에 대한 모든 퀴즈 결과 조회
     */
    List<UserExpressionQuizResult> findByUserIdAndExpressionId(UUID userId, UUID expressionId);

    /**
     * 사용자의 오답 개수 카운트
     */
    @Query("SELECT COUNT(r) FROM UserExpressionQuizResult r " +
           "WHERE r.user.id = :userId AND r.incorrectCount > 0")
    Long countMistakesByUserId(@Param("userId") UUID userId);

    /**
     * 사용자의 모든 퀴즈 결과 삭제 (Clear All 기능)
     */
    void deleteByUserId(UUID userId);

    /**
     * 사용자의 특정 퀴즈 결과 삭제 (개별 삭제)
     */
    void deleteByUserIdAndExpressionIdAndExampleIndex(UUID userId, UUID expressionId, Integer exampleIndex);

    /**
     * Unit별 퀴즈 통계 조회 (퀴즈를 푼 표현 개수, 정답/오답 개수)
     */
    @Query("SELECT e.unit as unit, " +
           "COUNT(DISTINCT r.expression.id) as attemptedCount, " +
           "SUM(r.correctCount) as correctCount, " +
           "SUM(r.incorrectCount) as incorrectCount " +
           "FROM UserExpressionQuizResult r " +
           "JOIN r.expression e " +
           "WHERE r.user.id = :userId " +
           "GROUP BY e.unit")
    List<Object[]> getQuizStatsByUnit(@Param("userId") UUID userId);

    /**
     * 특정 Unit 내 Chapter별 퀴즈 통계 조회
     */
    @Query("SELECT e.chapter as chapter, " +
           "COUNT(DISTINCT r.expression.id) as attemptedCount, " +
           "SUM(r.correctCount) as correctCount, " +
           "SUM(r.incorrectCount) as incorrectCount " +
           "FROM UserExpressionQuizResult r " +
           "JOIN r.expression e " +
           "WHERE r.user.id = :userId AND e.unit = :unit " +
           "GROUP BY e.chapter")
    List<Object[]> getQuizStatsByChapter(@Param("userId") UUID userId, @Param("unit") String unit);

    /**
     * 사용자의 일별 퀴즈 통계 조회 (최근 N일)
     * delta_date 기준으로 일별 증분 합계 반환
     */
    @Query(value = "SELECT delta_date as quiz_date, " +
           "SUM(daily_correct_delta) as correct_count, " +
           "SUM(daily_incorrect_delta) as incorrect_count " +
           "FROM user_expression_quiz_results " +
           "WHERE user_id = :userId " +
           "AND delta_date >= CURRENT_DATE - CAST(:days AS INTEGER) + 1 " +
           "AND delta_date IS NOT NULL " +
           "GROUP BY delta_date " +
           "ORDER BY quiz_date ASC",
           nativeQuery = true)
    List<Object[]> getDailyQuizStats(@Param("userId") UUID userId, @Param("days") int days);
}
