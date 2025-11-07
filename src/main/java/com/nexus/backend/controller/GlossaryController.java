package com.nexus.backend.controller;

import com.nexus.backend.dto.request.GlossaryTermRequest;
import com.nexus.backend.dto.response.ExtractionJobResponse;
import com.nexus.backend.dto.response.GlossaryTermResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.GlossaryService;
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
    public ResponseEntity<ExtractionJobResponse> startExtraction(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User user) {
        log.info("Starting glossary extraction for document: {} by user: {}", documentId, user.getId());
        ExtractionJobResponse response = glossaryService.startExtraction(documentId, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Get extraction job status
     */
    @GetMapping("/extraction/{jobId}")
    public ResponseEntity<ExtractionJobResponse> getExtractionStatus(@PathVariable UUID jobId) {
        log.info("Getting extraction job status: {}", jobId);
        ExtractionJobResponse response = glossaryService.getExtractionStatus(jobId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all glossary terms for current user (with optional project filter)
     */
    @GetMapping
    public ResponseEntity<Page<GlossaryTermResponse>> getTerms(
            @RequestParam(required = false) UUID projectId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal User user) {

        if (projectId != null) {
            log.info("Getting glossary terms for project: {}", projectId);
            Page<GlossaryTermResponse> terms = glossaryService.findTermsByProject(projectId, pageable);
            return ResponseEntity.ok(terms);
        } else {
            log.info("Getting all glossary terms for user: {}", user.getId());
            Page<GlossaryTermResponse> terms = glossaryService.findAllTermsByUser(user, pageable);
            return ResponseEntity.ok(terms);
        }
    }

    /**
     * Search glossary terms (with optional project filter)
     */
    @GetMapping("/search")
    public ResponseEntity<Page<GlossaryTermResponse>> searchTerms(
            @RequestParam String query,
            @RequestParam(required = false) UUID projectId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal User user) {

        if (projectId != null) {
            log.info("Searching glossary terms in project: {} with query: {}", projectId, query);
            Page<GlossaryTermResponse> terms = glossaryService.searchTermsByProject(projectId, query, pageable);
            return ResponseEntity.ok(terms);
        } else {
            log.info("Searching all glossary terms for user: {} with query: {}", user.getId(), query);
            Page<GlossaryTermResponse> terms = glossaryService.searchAllTerms(user, query, pageable);
            return ResponseEntity.ok(terms);
        }
    }

    /**
     * Get glossary term detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<GlossaryTermResponse> getTermDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Getting glossary term detail: {} for user: {}", id, user.getId());
        GlossaryTermResponse term = glossaryService.getTermDetail(id, user);
        return ResponseEntity.ok(term);
    }

    /**
     * Create glossary term manually
     */
    @PostMapping
    public ResponseEntity<GlossaryTermResponse> createTerm(
            @RequestBody GlossaryTermRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Creating glossary term for user: {}", user.getId());
        GlossaryTermResponse term = glossaryService.createTerm(request, user);
        return ResponseEntity.ok(term);
    }

    /**
     * Update glossary term
     */
    @PutMapping("/{id}")
    public ResponseEntity<GlossaryTermResponse> updateTerm(
            @PathVariable UUID id,
            @RequestBody GlossaryTermRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Updating glossary term: {} for user: {}", id, user.getId());
        GlossaryTermResponse term = glossaryService.updateTerm(id, request, user);
        return ResponseEntity.ok(term);
    }

    /**
     * Delete glossary term
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTerm(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Deleting glossary term: {} for user: {}", id, user.getId());
        glossaryService.deleteTerm(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete multiple glossary terms
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteTerms(
            @RequestBody java.util.List<UUID> termIds,
            @AuthenticationPrincipal User user) {
        log.info("Deleting {} glossary terms for user: {}", termIds.size(), user.getId());
        glossaryService.deleteTerms(termIds, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verify glossary term
     */
    @PutMapping("/{id}/verify")
    public ResponseEntity<GlossaryTermResponse> verifyTerm(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Verifying glossary term: {} for user: {}", id, user.getId());
        GlossaryTermResponse term = glossaryService.verifyTerm(id, user);
        return ResponseEntity.ok(term);
    }
}
