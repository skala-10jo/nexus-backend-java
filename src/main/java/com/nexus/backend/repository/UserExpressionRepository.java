package com.nexus.backend.repository;

import com.nexus.backend.entity.UserExpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UserExpression Repository
 */
@Repository
public interface UserExpressionRepository extends JpaRepository<UserExpression, UUID> {

    /**
     * 사용자 + 표현으로 조회
     */
    Optional<UserExpression> findByUserIdAndExpressionId(UUID userId, UUID expressionId);

    /**
     * 사용자의 학습한 표현 목록
     */
    @Query("SELECT ue FROM UserExpression ue WHERE ue.user.id = :userId AND ue.isLearned = true")
    List<UserExpression> findLearnedByUserId(@Param("userId") UUID userId);

    /**
     * 사용자의 학습 통계 (총 개수)
     */
    @Query("SELECT COUNT(ue) FROM UserExpression ue WHERE ue.user.id = :userId AND ue.isLearned = true")
    Long countLearnedByUserId(@Param("userId") UUID userId);
}
