package com.nexus.backend.controller;

import com.nexus.backend.dto.request.MarkLearnedRequest;
import com.nexus.backend.dto.request.QuizResultRequest;
import com.nexus.backend.dto.response.ChapterResponse;
import com.nexus.backend.dto.response.DailyQuizStatsResponse;
import com.nexus.backend.dto.response.ExpressionResponse;
import com.nexus.backend.dto.response.MistakeResponse;
import com.nexus.backend.dto.response.UnitResponse;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.ExpressionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/expressions")
@RequiredArgsConstructor
public class ExpressionController {

    private final ExpressionService expressionService;

    /**
     * 모든 Unit 목록 조회 (사용자별 학습 통계 포함)
     */
    @GetMapping("/units")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> getAllUnits(
            @AuthenticationPrincipal User user) {
        log.info("Unit 목록 조회 요청: userId={}", user.getId());

        List<UnitResponse> units = expressionService.getAllUnits(user.getId());

        log.info("Unit 목록 조회 완료: count={}", units.size());
        return ResponseEntity.ok(ApiResponse.success(units));
    }

    /**
     * Unit 내 Chapter 목록 조회 (사용자별 학습 통계 포함)
     *
     * @param unit Unit 이름
     */
    @GetMapping("/chapters")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> getChaptersByUnit(
            @RequestParam String unit,
            @AuthenticationPrincipal User user) {
        log.info("Chapter 목록 조회 요청: unit={}, userId={}", unit, user.getId());

        List<ChapterResponse> chapters = expressionService.getChaptersByUnit(unit, user.getId());

        log.info("Chapter 목록 조회 완료: count={}", chapters.size());
        return ResponseEntity.ok(ApiResponse.success(chapters));
    }

    /**
     * Unit별 표현 목록 조회 (랜덤, 개수 제한)
     *
     * @param unit  Unit 이름
     * @param limit 조회 개수 (기본값: 5)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpressionResponse>>> getExpressionsByUnit(
            @RequestParam String unit,
            @RequestParam(required = false) String chapter,
            @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal User user) {
        log.info("표현 목록 조회 요청: unit={}, chapter={}, limit={}, userId={}", unit, chapter, limit, user.getId());

        List<ExpressionResponse> expressions;
        if (chapter != null && !chapter.isEmpty()) {
            expressions = expressionService.getExpressionsByUnitAndChapter(unit, chapter, limit, user.getId());
        } else {
            expressions = expressionService.getExpressionsByUnit(unit, limit, user.getId());
        }

        log.info("표현 목록 조회 완료: count={}", expressions.size());
        return ResponseEntity.ok(ApiResponse.success(expressions));
    }

    /**
     * 표현 학습 완료 처리
     */
    @PostMapping("/learned")
    public ResponseEntity<ApiResponse<Void>> markAsLearned(
            @RequestBody @Valid MarkLearnedRequest request,
            @AuthenticationPrincipal User user) {
        log.info("표현 학습 완료 처리 요청: userId={}, expressionIds={}", user.getId(), request.getExpressionIds());

        expressionService.markAsLearned(request, user.getId());

        log.info("표현 학습 완료 처리 완료");
        return ResponseEntity.ok(ApiResponse.success("표현 학습 완료 처리가 완료되었습니다", null));
    }

    /**
     * 퀴즈 결과 저장
     */
    @PostMapping("/quiz/result")
    public ResponseEntity<ApiResponse<Void>> saveQuizResult(
            @RequestBody @Valid QuizResultRequest request,
            @AuthenticationPrincipal User user) {
        log.info("퀴즈 결과 저장 요청: userId={}, expressionId={}", user.getId(), request.getExpressionId());

        expressionService.saveQuizResult(request, user.getId());

        return ResponseEntity.ok(ApiResponse.success("퀴즈 결과가 저장되었습니다", null));
    }

    /**
     * 오답 목록 조회
     */
    @GetMapping("/mistakes")
    public ResponseEntity<ApiResponse<List<MistakeResponse>>> getMistakes(
            @AuthenticationPrincipal User user) {
        log.info("오답 목록 조회 요청: userId={}", user.getId());

        List<MistakeResponse> mistakes = expressionService.getMistakes(user.getId());

        log.info("오답 목록 조회 완료: count={}", mistakes.size());
        return ResponseEntity.ok(ApiResponse.success(mistakes));
    }

    /**
     * 오답 개수 조회
     */
    @GetMapping("/mistakes/count")
    public ResponseEntity<ApiResponse<Long>> getMistakeCount(
            @AuthenticationPrincipal User user) {
        log.info("오답 개수 조회 요청: userId={}", user.getId());

        Long count = expressionService.getMistakeCount(user.getId());

        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * 모든 퀴즈 결과 삭제 (Clear All)
     */
    @DeleteMapping("/quiz/results")
    public ResponseEntity<ApiResponse<Void>> clearAllQuizResults(
            @AuthenticationPrincipal User user) {
        log.info("퀴즈 결과 전체 삭제 요청: userId={}", user.getId());

        expressionService.clearAllQuizResults(user.getId());

        return ResponseEntity.ok(ApiResponse.success("모든 퀴즈 결과가 삭제되었습니다", null));
    }

    /**
     * 개별 퀴즈 결과 삭제
     */
    @DeleteMapping("/quiz/result/{expressionId}/{exampleIndex}")
    public ResponseEntity<ApiResponse<Void>> deleteQuizResult(
            @PathVariable java.util.UUID expressionId,
            @PathVariable Integer exampleIndex,
            @AuthenticationPrincipal User user) {
        log.info("퀴즈 결과 삭제 요청: userId={}, expressionId={}, exampleIndex={}", user.getId(), expressionId, exampleIndex);

        expressionService.deleteQuizResult(user.getId(), expressionId, exampleIndex);

        return ResponseEntity.ok(ApiResponse.success("퀴즈 결과가 삭제되었습니다", null));
    }

    /**
     * 일별 퀴즈 통계 조회 (Performance 차트용)
     *
     * @param days 조회할 일 수 (기본값: 7)
     */
    @GetMapping("/quiz/daily-stats")
    public ResponseEntity<ApiResponse<List<DailyQuizStatsResponse>>> getDailyQuizStats(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal User user) {
        log.info("일별 퀴즈 통계 조회 요청: userId={}, days={}", user.getId(), days);

        List<DailyQuizStatsResponse> stats = expressionService.getDailyQuizStats(user.getId(), days);

        log.info("일별 퀴즈 통계 조회 완료: count={}", stats.size());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
