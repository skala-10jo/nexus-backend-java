package com.nexus.backend.controller;

import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.FileDetailResponse;
import com.nexus.backend.dto.response.FileResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Backward compatibility controller for document operations.
 * Delegates to FileService (new structure).
 *
 * @deprecated Use FileController instead (/api/files)
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Deprecated
public class DocumentController {

    private final FileService fileService;

    /**
     * Upload a document file.
     * @deprecated Use POST /api/files/documents instead
     */
    @PostMapping("/upload")
    @Deprecated
    public ResponseEntity<ApiResponse<FileResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        log.warn("Using deprecated endpoint /api/documents/upload - use /api/files/documents instead");
        FileResponse response = fileService.uploadDocument(file, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("문서 업로드 완료", response));
    }

    /**
     * Get all documents for authenticated user.
     * @deprecated Use GET /api/files/documents instead
     */
    @GetMapping
    @Deprecated
    public ResponseEntity<ApiResponse<Page<FileResponse>>> getUserDocuments(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        log.warn("Using deprecated endpoint /api/documents - use /api/files/documents instead");
        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FileResponse> documents = fileService.getDocuments(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("문서 목록 조회 완료", documents));
    }

    /**
     * Get document by ID.
     * @deprecated Use GET /api/files/{id} instead
     */
    @GetMapping("/{id}")
    @Deprecated
    public ResponseEntity<ApiResponse<FileDetailResponse>> getDocumentById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.warn("Using deprecated endpoint /api/documents/{id} - use /api/files/{id} instead");
        FileDetailResponse response = fileService.getFileDetail(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("문서 조회 완료", response));
    }

    /**
     * Delete document by ID.
     * @deprecated Use DELETE /api/files/{id} instead
     */
    @DeleteMapping("/{id}")
    @Deprecated
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.warn("Using deprecated endpoint DELETE /api/documents/{id} - use DELETE /api/files/{id} instead");
        fileService.deleteFile(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("문서 삭제 완료", null));
    }
}
