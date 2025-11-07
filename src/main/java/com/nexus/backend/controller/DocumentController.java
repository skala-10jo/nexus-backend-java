package com.nexus.backend.controller;

import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.DocumentDetailResponse;
import com.nexus.backend.dto.response.DocumentResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {

        DocumentResponse response = documentService.uploadDocument(file, user);

        return ResponseEntity.ok(ApiResponse.success("문서가 성공적으로 업로드되었습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User user) {
        Sort sort = sortOrder.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DocumentResponse> documents = documentService.getDocuments(user.getId(), pageable);

        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> getDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User user) {
        DocumentDetailResponse response = documentService.getDocument(documentId, user.getId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User user) {
        Resource resource = documentService.downloadDocument(documentId, user.getId());

        // Get document details for filename
        DocumentDetailResponse document = documentService.getDocument(documentId, user.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User user) {
        documentService.deleteDocument(documentId, user.getId());

        return ResponseEntity.ok(ApiResponse.success("문서가 성공적으로 삭제되었습니다.", null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> searchDocuments(
            @RequestParam String query,
            @RequestParam(required = false) String fileType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        Pageable pageable = PageRequest.of(page, size);

        Page<DocumentResponse> documents = documentService.searchDocuments(user.getId(), query, pageable);

        return ResponseEntity.ok(ApiResponse.success(documents));
    }
}
