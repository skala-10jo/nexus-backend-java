package com.nexus.backend.service;

import com.nexus.backend.dto.request.GlossaryTermRequest;
import com.nexus.backend.dto.request.PythonExtractionRequest;
import com.nexus.backend.dto.response.ExtractionJobResponse;
import com.nexus.backend.dto.response.GlossaryTermResponse;
import com.nexus.backend.entity.Document;
import com.nexus.backend.entity.GlossaryExtractionJob;
import com.nexus.backend.entity.GlossaryTerm;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.DocumentRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class GlossaryService {

    private final GlossaryTermRepository glossaryTermRepository;
    private final GlossaryExtractionJobRepository extractionJobRepository;
    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;

    @Value("${python.backend.url:http://localhost:8000}")
    private String pythonBackendUrl;

    @Transactional
    public ExtractionJobResponse startExtraction(UUID documentId, User user) {
        log.info("Starting glossary extraction for document: {}", documentId);

        // Find document
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Check if there's an active job (PENDING or PROCESSING)
        Optional<GlossaryExtractionJob> existingJob = extractionJobRepository.findByDocumentId(documentId);
        if (existingJob.isPresent()) {
            String status = existingJob.get().getStatus();
            if ("PENDING".equals(status) || "PROCESSING".equals(status)) {
                throw new RuntimeException("Extraction is already in progress for this document");
            }
            // Delete old FAILED or COMPLETED job to allow re-extraction
            extractionJobRepository.delete(existingJob.get());
            log.info("Deleted old extraction job (status: {}) for document: {}", status, documentId);
        }

        // Create extraction job
        GlossaryExtractionJob job = GlossaryExtractionJob.builder()
                .user(user)
                .document(document)
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

                PythonExtractionRequest request = PythonExtractionRequest.builder()
                        .jobId(savedJob.getId())
                        .documentId(documentId)
                        .filePath(document.getFilePath())
                        .userId(user.getId())
                        .projectId(document.getProject() != null ? document.getProject().getId() : null)
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
    @Transactional(readOnly = true)
    public Page<GlossaryTermResponse> findTermsByProject(UUID projectId, Pageable pageable) {
        return glossaryTermRepository.findByProjectId(projectId, pageable)
                .map(GlossaryTermResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<GlossaryTermResponse> searchTermsByProject(UUID projectId, String query, Pageable pageable) {
        return glossaryTermRepository.searchByProjectIdAndQuery(projectId, query, pageable)
                .map(GlossaryTermResponse::from);
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
                .abbreviation(request.getAbbreviation())
                .definition(request.getDefinition())
                .context(request.getContext())
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
        term.setAbbreviation(request.getAbbreviation());
        term.setDefinition(request.getDefinition());
        term.setContext(request.getContext());
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
        term.setStatus("USER_VERIFIED");

        term = glossaryTermRepository.save(term);
        log.info("Verified glossary term: {}", termId);

        return GlossaryTermResponse.from(term);
    }

    @Transactional
    public GlossaryTermResponse unverifyTerm(UUID termId, User user) {
        GlossaryTerm term = glossaryTermRepository.findByIdAndUserId(termId, user.getId())
                .orElseThrow(() -> new RuntimeException("Glossary term not found"));

        term.setIsVerified(false);
        // Restore status based on original source
        if ("USER_VERIFIED".equals(term.getStatus()) || "USER_EDITED".equals(term.getStatus())) {
            term.setStatus("EXTRACTED");
        }

        term = glossaryTermRepository.save(term);
        log.info("Unverified glossary term: {}", termId);

        return GlossaryTermResponse.from(term);
    }
}
