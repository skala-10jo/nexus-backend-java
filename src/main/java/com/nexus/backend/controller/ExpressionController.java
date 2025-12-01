package com.nexus.backend.controller;

import com.nexus.backend.dto.expression.ChapterResponse;
import com.nexus.backend.dto.expression.ExpressionResponse;
import com.nexus.backend.dto.expression.MarkLearnedRequest;
import com.nexus.backend.dto.expression.UnitResponse;
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

/**
 * Expression Controller
 *
 * @author NEXUS Team
 * @since 2025-01-21
 */
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
}
