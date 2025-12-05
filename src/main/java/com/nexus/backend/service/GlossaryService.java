package com.nexus.backend.service;

import com.nexus.backend.dto.request.GlossaryTermRequest;
import com.nexus.backend.dto.request.PythonExtractionRequest;
import com.nexus.backend.dto.response.ExtractionJobResponse;
import com.nexus.backend.dto.response.GlossaryStatisticsResponse;
import com.nexus.backend.dto.response.GlossaryTermResponse;
import com.nexus.backend.entity.File;
import com.nexus.backend.entity.GlossaryExtractionJob;
import com.nexus.backend.entity.GlossaryTerm;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.FileRepository;
import com.nexus.backend.repository.GlossaryExtractionJobRepository;
import com.nexus.backend.repository.GlossaryTermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlossaryService {

    private final GlossaryTermRepository glossaryTermRepository;
    private final GlossaryExtractionJobRepository extractionJobRepository;
    private final FileRepository fileRepository;
    private final RestTemplate restTemplate;

    @Value("${python.backend.url:http://localhost:8000}")
    private String pythonBackendUrl;

    @Transactional
    public ExtractionJobResponse startExtraction(UUID fileId, User user) {
        log.info("Starting glossary extraction for file: {}", fileId);

        // Find file
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Check if there's an active job (PENDING or PROCESSING)
        Optional<GlossaryExtractionJob> existingJob = extractionJobRepository.findByFileId(fileId);
        if (existingJob.isPresent()) {
            String status = existingJob.get().getStatus();
            if ("PENDING".equals(status) || "PROCESSING".equals(status)) {
                throw new RuntimeException("Extraction is already in progress for this file");
            }
            // Delete old FAILED or COMPLETED job to allow re-extraction
            extractionJobRepository.delete(existingJob.get());
            extractionJobRepository.flush(); // Force immediate deletion before creating new job
            log.info("Deleted old extraction job (status: {}) for file: {}", status, fileId);
        }

        // Create extraction job
        GlossaryExtractionJob job = GlossaryExtractionJob.builder()
                .user(user)
                .file(file)
                .status("PENDING")
                .progress(0)
                .termsExtracted(0)
                .build();

        job = extractionJobRepository.save(job);
        log.info("Created extraction job: {}", job.getId());

        // Call Python API asynchronously
        final GlossaryExtractionJob savedJob = job;
        CompletableFuture.runAsync(() -> {
            try {
                String pythonUrl = pythonBackendUrl + "/api/ai/glossary/extract";

                // Get first project if file has projects
                UUID projectId = null;
                if (file.getProjects() != null && !file.getProjects().isEmpty()) {
                    projectId = file.getProjects().get(0).getId();
                }

                PythonExtractionRequest request = PythonExtractionRequest.builder()
                        .jobId(savedJob.getId())
                        .fileId(fileId)
                        .filePath(file.getFilePath())
                        .userId(user.getId())
                        .projectId(projectId)
                        .build();

                log.info("Calling Python API: {}", pythonUrl);
                restTemplate.postForObject(pythonUrl, request, Void.class);
                log.info("Python API called successfully");

            } catch (Exception e) {
                log.error("Failed to call Python API", e);
                // Update job status to FAILED
                savedJob.setStatus("FAILED");
                savedJob.setErrorMessage(e.getMessage());
                extractionJobRepository.save(savedJob);
            }
        });

        return ExtractionJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public ExtractionJobResponse getExtractionStatus(UUID jobId) {
        GlossaryExtractionJob job = extractionJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Extraction job not found"));

        return ExtractionJobResponse.from(job);
    }

    // User-level queries (all terms)
    @Transactional(readOnly = true)
    public Page<GlossaryTermResponse> findAllTermsByUser(User user, Pageable pageable) {
        return glossaryTermRepository.findByUserId(user.getId(), pageable)
                .map(GlossaryTermResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<GlossaryTermResponse> searchAllTerms(User user, String query, Pageable pageable) {
        return glossaryTermRepository.searchByUserIdAndQuery(user.getId(), query, pageable)
                .map(GlossaryTermResponse::from);
    }

    // Project-level queries (filtered)
    // Use project files approach instead of project_id to handle terms extracted before project assignment
    // Note: These use native queries which require snake_case column names for sorting
    @Transactional(readOnly = true)
    public Page<GlossaryTermResponse> findTermsByProject(UUID projectId, Pageable pageable) {
        Pageable nativePageable = convertToNativePageable(pageable);
        return glossaryTermRepository.findTermsByProjectFiles(projectId, nativePageable)
                .map(GlossaryTermResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<GlossaryTermResponse> searchTermsByProject(UUID projectId, String query, Pageable pageable) {
        Pageable nativePageable = convertToNativePageable(pageable);
        return glossaryTermRepository.searchTermsByProjectFiles(projectId, query, nativePageable)
                .map(GlossaryTermResponse::from);
    }

    // Document-level queries (filtered by source file)
    // Note: These use native queries which require snake_case column names for sorting
    @Transactional(readOnly = true)
    public Page<GlossaryTermResponse> findTermsByDocument(UUID documentId, Pageable pageable) {
        Pageable nativePageable = convertToNativePageable(pageable);
        return glossaryTermRepository.findBySourceFileId(documentId, nativePageable)
                .map(GlossaryTermResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<GlossaryTermResponse> searchTermsByDocument(UUID documentId, String query, Pageable pageable) {
        Pageable nativePageable = convertToNativePageable(pageable);
        return glossaryTermRepository.searchBySourceFileIdAndQuery(documentId, query, nativePageable)
                .map(GlossaryTermResponse::from);
    }

    /**
     * Convert Pageable with camelCase property names to snake_case column names for native queries.
     * e.g., "createdAt" -> "created_at"
     */
    private Pageable convertToNativePageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        Sort nativeSort = Sort.by(
            pageable.getSort().stream()
                .map(order -> {
                    String snakeCaseProperty = camelToSnake(order.getProperty());
                    return new Sort.Order(order.getDirection(), snakeCaseProperty);
                })
                .collect(Collectors.toList())
        );

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), nativeSort);
    }

    /**
     * Convert camelCase to snake_case.
     * e.g., "createdAt" -> "created_at"
     */
    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @Transactional(readOnly = true)
    public GlossaryTermResponse getTermDetail(UUID termId, User user) {
        GlossaryTerm term = glossaryTermRepository.findByIdAndUserId(termId, user.getId())
                .orElseThrow(() -> new RuntimeException("Glossary term not found"));

        return GlossaryTermResponse.from(term);
    }

    @Transactional
    public GlossaryTermResponse createTerm(GlossaryTermRequest request, User user) {
        // Check if term already exists for this user
        if (glossaryTermRepository.existsByUserIdAndKoreanTerm(user.getId(), request.getKoreanTerm())) {
            throw new RuntimeException("Term already exists in your glossary");
        }

        GlossaryTerm term = GlossaryTerm.builder()
                .project(null)  // Project is optional
                .user(user)
                .koreanTerm(request.getKoreanTerm())
                .englishTerm(request.getEnglishTerm())
                .vietnameseTerm(request.getVietnameseTerm())
                .abbreviation(request.getAbbreviation())
                .definition(request.getDefinition())
                .context(request.getContext())
                .exampleSentence(request.getExampleSentence())
                .note(request.getNote())
                .domain(request.getDomain())
                .status("USER_ADDED")
                .isVerified(true)
                .usageCount(0)
                .build();

        term = glossaryTermRepository.save(term);
        log.info("Created glossary term: {}", term.getId());

        return GlossaryTermResponse.from(term);
    }

    @Transactional
    public GlossaryTermResponse updateTerm(UUID termId, GlossaryTermRequest request, User user) {
        GlossaryTerm term = glossaryTermRepository.findByIdAndUserId(termId, user.getId())
                .orElseThrow(() -> new RuntimeException("Glossary term not found"));

        term.setKoreanTerm(request.getKoreanTerm());
        term.setEnglishTerm(request.getEnglishTerm());
        term.setVietnameseTerm(request.getVietnameseTerm());
        term.setAbbreviation(request.getAbbreviation());
        term.setDefinition(request.getDefinition());
        term.setContext(request.getContext());
        term.setExampleSentence(request.getExampleSentence());
        term.setNote(request.getNote());
        term.setDomain(request.getDomain());
        term.setStatus("USER_EDITED");

        term = glossaryTermRepository.save(term);
        log.info("Updated glossary term: {}", term.getId());

        return GlossaryTermResponse.from(term);
    }

    @Transactional
    public void deleteTerm(UUID termId, User user) {
        GlossaryTerm term = glossaryTermRepository.findByIdAndUserId(termId, user.getId())
                .orElseThrow(() -> new RuntimeException("Glossary term not found"));

        glossaryTermRepository.delete(term);
        log.info("Deleted glossary term: {}", termId);
    }

    @Transactional
    public void deleteTerms(java.util.List<UUID> termIds, User user) {
        log.info("Deleting {} terms for user: {}", termIds.size(), user.getId());

        for (UUID termId : termIds) {
            try {
                GlossaryTerm term = glossaryTermRepository.findByIdAndUserId(termId, user.getId())
                        .orElseThrow(() -> new RuntimeException("Glossary term not found: " + termId));
                glossaryTermRepository.delete(term);
            } catch (Exception e) {
                log.error("Failed to delete term {}: {}", termId, e.getMessage());
                // Continue with other deletions
            }
        }

        log.info("Completed batch deletion of glossary terms");
    }

    @Transactional
    public GlossaryTermResponse verifyTerm(UUID termId, User user) {
        GlossaryTerm term = glossaryTermRepository.findByIdAndUserId(termId, user.getId())
                .orElseThrow(() -> new RuntimeException("Glossary term not found"));

        term.setIsVerified(true);
        // Don't change status - it represents data source, not verification state

        term = glossaryTermRepository.save(term);
        log.info("Verified glossary term: {}", termId);

        return GlossaryTermResponse.from(term);
    }

    @Transactional
    public GlossaryTermResponse unverifyTerm(UUID termId, User user) {
        GlossaryTerm term = glossaryTermRepository.findByIdAndUserId(termId, user.getId())
                .orElseThrow(() -> new RuntimeException("Glossary term not found"));

        term.setIsVerified(false);
        // Don't change status - it represents data source, not verification state

        term = glossaryTermRepository.save(term);
        log.info("Unverified glossary term: {}", termId);

        return GlossaryTermResponse.from(term);
    }

    public GlossaryStatisticsResponse getStatistics(User user, UUID projectId) {
        long totalTerms;
        long verifiedTerms;
        long unverifiedTerms;
        long autoExtractedTerms;
        long userAddedTerms;
        long userEditedTerms;

        if (projectId != null) {
            // Project-level statistics (use project files approach)
            totalTerms = glossaryTermRepository.countTermsByProjectFiles(projectId);
            verifiedTerms = glossaryTermRepository.countTermsByProjectFilesAndIsVerified(projectId, true);
            unverifiedTerms = glossaryTermRepository.countTermsByProjectFilesAndIsVerified(projectId, false);
            autoExtractedTerms = glossaryTermRepository.countTermsByProjectFilesAndStatus(projectId, "AUTO_EXTRACTED");
            userAddedTerms = glossaryTermRepository.countTermsByProjectFilesAndStatus(projectId, "USER_ADDED");
            userEditedTerms = glossaryTermRepository.countTermsByProjectFilesAndStatus(projectId, "USER_EDITED");
        } else {
            // User-level statistics
            totalTerms = glossaryTermRepository.countByUserId(user.getId());
            verifiedTerms = glossaryTermRepository.countByUserIdAndIsVerified(user.getId(), true);
            unverifiedTerms = glossaryTermRepository.countByUserIdAndIsVerified(user.getId(), false);
            autoExtractedTerms = glossaryTermRepository.countByUserIdAndStatus(user.getId(), "AUTO_EXTRACTED");
            userAddedTerms = glossaryTermRepository.countByUserIdAndStatus(user.getId(), "USER_ADDED");
            userEditedTerms = glossaryTermRepository.countByUserIdAndStatus(user.getId(), "USER_EDITED");
        }

        log.info("Retrieved glossary statistics for user: {} (projectId: {})", user.getId(), projectId);

        return GlossaryStatisticsResponse.builder()
                .totalTerms(totalTerms)
                .verifiedTerms(verifiedTerms)
                .unverifiedTerms(unverifiedTerms)
                .autoExtractedTerms(autoExtractedTerms)
                .userAddedTerms(userAddedTerms)
                .userEditedTerms(userEditedTerms)
                .build();
    }
}
