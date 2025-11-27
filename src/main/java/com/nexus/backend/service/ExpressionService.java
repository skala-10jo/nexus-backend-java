package com.nexus.backend.service;

import com.nexus.backend.dto.expression.ChapterResponse;
import com.nexus.backend.dto.expression.ExpressionResponse;
import com.nexus.backend.dto.expression.MarkLearnedRequest;
import com.nexus.backend.dto.expression.UnitResponse;
import com.nexus.backend.entity.Expression;
import com.nexus.backend.entity.User;
import com.nexus.backend.entity.UserExpression;
import com.nexus.backend.repository.ExpressionRepository;
import com.nexus.backend.repository.UserExpressionRepository;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Expression Service
 *
 * @author NEXUS Team
 * @since 2025-01-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressionService {

    private final ExpressionRepository expressionRepository;
    private final UserExpressionRepository userExpressionRepository;
    private final UserRepository userRepository;

    /**
     * 모든 Unit 목록 조회 (사용자별 학습 통계 포함)
     */
    @Transactional(readOnly = true)
    public List<UnitResponse> getAllUnits(UUID userId) {
        log.info("모든 Unit 목록 조회 시작: userId={}", userId);

        // 모든 Unit 목록 조회
        List<String> units = expressionRepository.findAllUnits();

        // 사용자의 학습한 표현 목록 조회
        List<UserExpression> learnedExpressions = userExpressionRepository.findLearnedByUserId(userId);
        Map<String, Long> learnedCountByUnit = learnedExpressions.stream()
                .collect(Collectors.groupingBy(
                        ue -> ue.getExpression().getUnit(),
                        Collectors.counting()
                ));

        // Unit별 응답 생성
        List<UnitResponse> responses = new ArrayList<>();
        for (String unit : units) {
            Long totalCount = expressionRepository.countByUnit(unit);
            Long learnedCount = learnedCountByUnit.getOrDefault(unit, 0L);

            responses.add(UnitResponse.builder()
                    .unit(unit)
                    .totalCount(totalCount)
                    .learnedCount(learnedCount)
                    .build());
        }

        log.info("Unit 목록 조회 완료: count={}", responses.size());
        return responses;
    }

    /**
     * Unit 내 Chapter 목록 조회 (사용자별 학습 통계 포함)
     */
    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersByUnit(String unit, UUID userId) {
        log.info("Unit 내 Chapter 목록 조회 시작: unit={}, userId={}", unit, userId);

        // 해당 Unit의 Chapter 목록 조회
        List<String> chapters = expressionRepository.findChaptersByUnit(unit);

        // 사용자의 학습한 표현 목록 조회
        List<UserExpression> learnedExpressions = userExpressionRepository.findLearnedByUserId(userId);
        Map<String, Long> learnedCountByChapter = learnedExpressions.stream()
                .filter(ue -> ue.getExpression().getUnit().equals(unit))
                .collect(Collectors.groupingBy(
                        ue -> ue.getExpression().getChapter(),
                        Collectors.counting()
                ));

        // Chapter별 응답 생성
        List<ChapterResponse> responses = new ArrayList<>();
        for (String chapter : chapters) {
            Long totalCount = expressionRepository.countByUnitAndChapter(unit, chapter);
            Long learnedCount = learnedCountByChapter.getOrDefault(chapter, 0L);

            responses.add(ChapterResponse.builder()
                    .chapter(chapter)
                    .totalCount(totalCount)
                    .learnedCount(learnedCount)
                    .build());
        }

        log.info("Chapter 목록 조회 완료: count={}", responses.size());
        return responses;
    }

    /**
     * Unit별 표현 목록 조회 (순서대로, 개수 제한)
     */
    @Transactional(readOnly = true)
    public List<ExpressionResponse> getExpressionsByUnit(String unit, int limit, UUID userId) {
        log.info("Unit별 표현 조회 시작: unit={}, limit={}, userId={}", unit, limit, userId);

        // 순서대로 표현 조회
        List<Expression> expressions = expressionRepository.findByUnitOrderByExpression(unit, limit);

        // 사용자의 학습 상태 조회
        Map<UUID, Boolean> learnedMap = userExpressionRepository.findLearnedByUserId(userId).stream()
                .collect(Collectors.toMap(
                        ue -> ue.getExpression().getId(),
                        UserExpression::getIsLearned
                ));

        // DTO 변환
        List<ExpressionResponse> responses = expressions.stream()
                .map(expr -> {
                    Boolean isLearned = learnedMap.getOrDefault(expr.getId(), false);
                    return ExpressionResponse.from(expr, isLearned);
                })
                .collect(Collectors.toList());

        log.info("표현 조회 완료: count={}", responses.size());
        return responses;
    }

    /**
     * Unit + Chapter별 표현 목록 조회 (순서대로, 개수 제한)
     */
    @Transactional(readOnly = true)
    public List<ExpressionResponse> getExpressionsByUnitAndChapter(String unit, String chapter, int limit, UUID userId) {
        log.info("Unit + Chapter별 표현 조회 시작: unit={}, chapter={}, limit={}, userId={}", unit, chapter, limit, userId);

        // 순서대로 표현 조회
        List<Expression> expressions = expressionRepository.findByUnitAndChapterOrderByExpression(unit, chapter, limit);

        // 사용자의 학습 상태 조회
        Map<UUID, Boolean> learnedMap = userExpressionRepository.findLearnedByUserId(userId).stream()
                .collect(Collectors.toMap(
                        ue -> ue.getExpression().getId(),
                        UserExpression::getIsLearned
                ));

        // DTO 변환
        List<ExpressionResponse> responses = expressions.stream()
                .map(expr -> {
                    Boolean isLearned = learnedMap.getOrDefault(expr.getId(), false);
                    return ExpressionResponse.from(expr, isLearned);
                })
                .collect(Collectors.toList());

        log.info("표현 조회 완료: count={}", responses.size());
        return responses;
    }

    /**
     * 표현 학습 완료 처리
     */
    @Transactional
    public void markAsLearned(MarkLearnedRequest request, UUID userId) {
        log.info("표현 학습 완료 처리 시작: userId={}, expressionIds={}", userId, request.getExpressionIds());

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 각 표현에 대해 학습 완료 처리
        for (UUID expressionId : request.getExpressionIds()) {
            Expression expression = expressionRepository.findById(expressionId)
                    .orElseThrow(() -> new IllegalArgumentException("표현을 찾을 수 없습니다: " + expressionId));

            // 기존 기록 조회 또는 새로 생성
            UserExpression userExpression = userExpressionRepository
                    .findByUserIdAndExpressionId(userId, expressionId)
                    .orElse(UserExpression.builder()
                            .user(user)
                            .expression(expression)
                            .isLearned(false)
                            .build());

            // 학습 완료로 업데이트
            userExpression.setIsLearned(true);
            userExpressionRepository.save(userExpression);
        }

        log.info("표현 학습 완료 처리 완료: count={}", request.getExpressionIds().size());
    }
}
