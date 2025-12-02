package com.nexus.backend.service;

import com.nexus.backend.dto.expression.ChapterResponse;
import com.nexus.backend.dto.expression.ExpressionResponse;
import com.nexus.backend.dto.expression.MarkLearnedRequest;
import com.nexus.backend.dto.expression.MistakeResponse;
import com.nexus.backend.dto.expression.QuizResultRequest;
import com.nexus.backend.dto.expression.UnitResponse;
import com.nexus.backend.entity.Expression;
import com.nexus.backend.entity.User;
import com.nexus.backend.entity.UserExpression;
import com.nexus.backend.entity.UserExpressionQuizResult;
import com.nexus.backend.repository.ExpressionRepository;
import com.nexus.backend.repository.UserExpressionRepository;
import com.nexus.backend.repository.UserExpressionQuizResultRepository;
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
    private final UserExpressionQuizResultRepository quizResultRepository;
    private final UserRepository userRepository;

    /**
     * 모든 Unit 목록 조회 (사용자별 퀴즈 통계 포함)
     */
    @Transactional(readOnly = true)
    public List<UnitResponse> getAllUnits(UUID userId) {
        log.info("모든 Unit 목록 조회 시작: userId={}", userId);

        // 모든 Unit 목록 조회
        List<String> units = expressionRepository.findAllUnits();

        // 사용자의 퀴즈 통계 조회
        List<Object[]> quizStats = quizResultRepository.getQuizStatsByUnit(userId);
        Map<String, Object[]> statsMap = quizStats.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],  // unit
                        row -> row
                ));

        // Unit별 응답 생성
        List<UnitResponse> responses = new ArrayList<>();
        for (String unit : units) {
            Long totalCount = expressionRepository.countByUnit(unit);
            Object[] stats = statsMap.get(unit);

            Long attemptedCount = 0L;
            Long correctCount = 0L;
            Long incorrectCount = 0L;

            if (stats != null) {
                attemptedCount = ((Number) stats[1]).longValue();
                correctCount = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
                incorrectCount = stats[3] != null ? ((Number) stats[3]).longValue() : 0L;
            }

            responses.add(UnitResponse.builder()
                    .unit(unit)
                    .totalCount(totalCount)
                    .quizAttemptedCount(attemptedCount)
                    .correctCount(correctCount)
                    .incorrectCount(incorrectCount)
                    .build());
        }

        log.info("Unit 목록 조회 완료: count={}", responses.size());
        return responses;
    }

    /**
     * Unit 내 Chapter 목록 조회 (사용자별 퀴즈 통계 포함)
     */
    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersByUnit(String unit, UUID userId) {
        log.info("Unit 내 Chapter 목록 조회 시작: unit={}, userId={}", unit, userId);

        // 해당 Unit의 Chapter 목록 조회
        List<String> chapters = expressionRepository.findChaptersByUnit(unit);

        // 사용자의 퀴즈 통계 조회
        List<Object[]> quizStats = quizResultRepository.getQuizStatsByChapter(userId, unit);
        Map<String, Object[]> statsMap = quizStats.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],  // chapter
                        row -> row
                ));

        // Chapter별 응답 생성
        List<ChapterResponse> responses = new ArrayList<>();
        for (String chapter : chapters) {
            Long totalCount = expressionRepository.countByUnitAndChapter(unit, chapter);
            Object[] stats = statsMap.get(chapter);

            Long attemptedCount = 0L;
            Long correctCount = 0L;
            Long incorrectCount = 0L;

            if (stats != null) {
                attemptedCount = ((Number) stats[1]).longValue();
                correctCount = stats[2] != null ? ((Number) stats[2]).longValue() : 0L;
                incorrectCount = stats[3] != null ? ((Number) stats[3]).longValue() : 0L;
            }

            responses.add(ChapterResponse.builder()
                    .chapter(chapter)
                    .totalCount(totalCount)
                    .quizAttemptedCount(attemptedCount)
                    .correctCount(correctCount)
                    .incorrectCount(incorrectCount)
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

    /**
     * 퀴즈 결과 저장
     */
    @Transactional
    public void saveQuizResult(QuizResultRequest request, UUID userId) {
        log.info("퀴즈 결과 저장 시작: userId={}, expressionId={}, exampleIndex={}, isCorrect={}",
                userId, request.getExpressionId(), request.getExampleIndex(), request.getIsCorrect());

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 표현 조회
        Expression expression = expressionRepository.findById(request.getExpressionId())
                .orElseThrow(() -> new IllegalArgumentException("표현을 찾을 수 없습니다: " + request.getExpressionId()));

        // 기존 결과 조회 또는 새로 생성
        UserExpressionQuizResult result = quizResultRepository
                .findByUserIdAndExpressionIdAndExampleIndex(userId, request.getExpressionId(), request.getExampleIndex())
                .orElse(UserExpressionQuizResult.builder()
                        .user(user)
                        .expression(expression)
                        .exampleIndex(request.getExampleIndex())
                        .build());

        // 정답/오답 기록
        if (request.getIsCorrect()) {
            result.recordCorrect();
        } else {
            result.recordIncorrect();
        }

        quizResultRepository.save(result);

        log.info("퀴즈 결과 저장 완료: resultId={}", result.getId());
    }

    /**
     * 오답 목록 조회
     */
    @Transactional(readOnly = true)
    public List<MistakeResponse> getMistakes(UUID userId) {
        log.info("오답 목록 조회 시작: userId={}", userId);

        List<UserExpressionQuizResult> mistakes = quizResultRepository.findMistakesByUserId(userId);

        List<MistakeResponse> responses = mistakes.stream()
                .map(MistakeResponse::from)
                .collect(Collectors.toList());

        log.info("오답 목록 조회 완료: count={}", responses.size());
        return responses;
    }

    /**
     * 오답 개수 조회
     */
    @Transactional(readOnly = true)
    public Long getMistakeCount(UUID userId) {
        return quizResultRepository.countMistakesByUserId(userId);
    }

    /**
     * 모든 퀴즈 결과 삭제 (Clear All)
     */
    @Transactional
    public void clearAllQuizResults(UUID userId) {
        log.info("퀴즈 결과 전체 삭제 시작: userId={}", userId);
        quizResultRepository.deleteByUserId(userId);
        log.info("퀴즈 결과 전체 삭제 완료");
    }

    /**
     * 개별 퀴즈 결과 삭제
     */
    @Transactional
    public void deleteQuizResult(UUID userId, UUID expressionId, Integer exampleIndex) {
        log.info("퀴즈 결과 삭제 시작: userId={}, expressionId={}, exampleIndex={}", userId, expressionId, exampleIndex);
        quizResultRepository.deleteByUserIdAndExpressionIdAndExampleIndex(userId, expressionId, exampleIndex);
        log.info("퀴즈 결과 삭제 완료");
    }
}
