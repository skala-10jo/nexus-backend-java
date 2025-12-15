package com.nexus.backend.controller;

import com.nexus.backend.dto.request.GlossaryTermRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.ExtractionJobResponse;
import com.nexus.backend.dto.response.GlossaryStatisticsResponse;
import com.nexus.backend.dto.response.GlossaryTermResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.GlossaryService;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/glossary")
@RequiredArgsConstructor
@Slf4j
public class GlossaryController {

    private final GlossaryService glossaryService;

    /**
     * Start glossary term extraction from a document
     */
    @PostMapping("/extract/{documentId}")
    public ResponseEntity<ApiResponse<ExtractionJobResponse>> startExtraction(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User user) {
        log.info("Starting glossary extraction for document: {} by user: {}", documentId, user.getId());
        ExtractionJobResponse response = glossaryService.startExtraction(documentId, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("용어집 추출 시작", response));
    }

    /**
     * Get extraction job status
     */
    @GetMapping("/extraction/{jobId}")
    public ResponseEntity<ApiResponse<ExtractionJobResponse>> getExtractionStatus(@PathVariable UUID jobId) {
        log.info("Getting extraction job status: {}", jobId);
        ExtractionJobResponse response = glossaryService.getExtractionStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success("추출 작업 상태 조회 완료", response));
    }

    /**
     * Get all glossary terms for current user (with optional project or document filter)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<GlossaryTermResponse>>> getTerms(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID documentId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal User user) {

        Page<GlossaryTermResponse> terms;
        if (documentId != null) {
            terms = glossaryService.findTermsByDocument(documentId, pageable);
        } else if (projectId != null) {
            terms = glossaryService.findTermsByProject(projectId, pageable);
        } else {
            terms = glossaryService.findAllTermsByUser(user, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success("용어집 목록 조회 완료", terms));
    }

    /**
     * Search glossary terms (with optional project or document filter)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<GlossaryTermResponse>>> searchTerms(
            @RequestParam String query,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID documentId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal User user) {

        Page<GlossaryTermResponse> terms;
        if (documentId != null) {
            log.info("Searching glossary terms in document: {} with query: {}", documentId, query);
            terms = glossaryService.searchTermsByDocument(documentId, query, pageable);
        } else if (projectId != null) {
            log.info("Searching glossary terms in project: {} with query: {}", projectId, query);
            terms = glossaryService.searchTermsByProject(projectId, query, pageable);
        } else {
            log.info("Searching all glossary terms for user: {} with query: {}", user.getId(), query);
            terms = glossaryService.searchAllTerms(user, query, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success("용어집 검색 완료", terms));
    }

    /**
     * Get glossary term detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GlossaryTermResponse>> getTermDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Getting glossary term detail: {} for user: {}", id, user.getId());
        GlossaryTermResponse term = glossaryService.getTermDetail(id, user);
        return ResponseEntity.ok(ApiResponse.success("용어 상세 조회 완료", term));
    }

    /**
     * Create glossary term manually
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GlossaryTermResponse>> createTerm(
            @RequestBody GlossaryTermRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Creating glossary term for user: {}", user.getId());
        GlossaryTermResponse term = glossaryService.createTerm(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("용어 생성 완료", term));
    }

    /**
     * Update glossary term
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GlossaryTermResponse>> updateTerm(
            @PathVariable UUID id,
            @RequestBody GlossaryTermRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Updating glossary term: {} for user: {}", id, user.getId());
        GlossaryTermResponse term = glossaryService.updateTerm(id, request, user);
        return ResponseEntity.ok(ApiResponse.success("용어 수정 완료", term));
    }

    /**
     * Delete glossary term
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTerm(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Deleting glossary term: {} for user: {}", id, user.getId());
        glossaryService.deleteTerm(id, user);
        return ResponseEntity.ok(ApiResponse.success("용어 삭제 완료", null));
    }

    /**
     * Delete multiple glossary terms
     */
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> deleteTerms(
            @RequestBody java.util.List<UUID> termIds,
            @AuthenticationPrincipal User user) {
        log.info("Deleting {} glossary terms for user: {}", termIds.size(), user.getId());
        glossaryService.deleteTerms(termIds, user);
        return ResponseEntity.ok(ApiResponse.success("용어 일괄 삭제 완료", null));
    }

    /**
     * Verify glossary term
     */
    @PutMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<GlossaryTermResponse>> verifyTerm(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Verifying glossary term: {} for user: {}", id, user.getId());
        GlossaryTermResponse term = glossaryService.verifyTerm(id, user);
        return ResponseEntity.ok(ApiResponse.success("용어 검증 완료", term));
    }

    /**
     * Unverify glossary term
     */
    @PutMapping("/{id}/unverify")
    public ResponseEntity<ApiResponse<GlossaryTermResponse>> unverifyTerm(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Unverifying glossary term: {} for user: {}", id, user.getId());
        GlossaryTermResponse term = glossaryService.unverifyTerm(id, user);
        return ResponseEntity.ok(ApiResponse.success("용어 검증 해제 완료", term));
    }

    /**
     * Get glossary statistics (with optional project filter)
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<GlossaryStatisticsResponse>> getStatistics(
            @RequestParam(required = false) UUID projectId,
            @AuthenticationPrincipal User user) {
        log.info("Getting glossary statistics for user: {} (projectId: {})", user.getId(), projectId);
        GlossaryStatisticsResponse statistics = glossaryService.getStatistics(user, projectId);
        return ResponseEntity.ok(ApiResponse.success("용어집 통계 조회 완료", statistics));
    }
}
