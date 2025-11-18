package com.nexus.backend.controller;

import com.nexus.backend.dto.request.VideoUploadRequest;
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
 * REST controller for file operations using the new file structure.
 * Provides endpoints for document and video management.
 *
 * @author NEXUS Team
 * @version 1.0
 * @since 2025-01-18
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;

    /**
     * Upload a document file.
     * POST /api/files/documents
     *
     * @param file      the file to upload
     * @param principal authenticated user
     * @return FileResponse
     */
    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<FileResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        log.info("Document upload request: user={}, filename={}", user.getUsername(), file.getOriginalFilename());

        FileResponse response = fileService.uploadDocument(file, user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("문서 업로드 완료", response));
    }

    /**
     * Upload a video file.
     * POST /api/files/videos
     *
     * @param file            the file to upload
     * @param sourceLanguage  source language code
     * @param targetLanguage  target language code
     * @param documentIds     optional document IDs for context (comma-separated)
     * @param principal       authenticated user
     * @return FileResponse
     */
    @PostMapping("/videos")
    public ResponseEntity<ApiResponse<FileResponse>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sourceLanguage") String sourceLanguage,
            @RequestParam("targetLanguage") String targetLanguage,
            @RequestParam(value = "documentIds", required = false) String documentIds,
            @AuthenticationPrincipal User user
    ) {
        log.info("Video upload request: user={}, filename={}", user.getUsername(), file.getOriginalFilename());

        VideoUploadRequest request = new VideoUploadRequest();
        request.setSourceLanguage(sourceLanguage);
        request.setTargetLanguage(targetLanguage);
        // Parse documentIds if needed

        FileResponse response = fileService.uploadVideo(file, request, user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("영상 업로드 완료", response));
    }

    /**
     * Get all documents for the current user.
     * GET /api/files/documents?page=0&size=20&sortBy=uploadDate&sortOrder=desc
     *
     * @param page      page number (default: 0)
     * @param size      page size (default: 20)
     * @param sortBy    sort field (default: uploadDate)
     * @param sortOrder sort order (default: desc)
     * @param principal authenticated user
     * @return paginated documents
     */
    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<Page<FileResponse>>> getDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal User user
    ) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<FileResponse> documents = fileService.getDocuments(user.getId(), pageable);

        return ResponseEntity.ok(ApiResponse.success("문서 목록 조회 완료", documents));
    }

    /**
     * Get all videos for the current user.
     * GET /api/files/videos?page=0&size=20&sortBy=uploadDate&sortOrder=desc
     *
     * @param page      page number (default: 0)
     * @param size      page size (default: 20)
     * @param sortBy    sort field (default: uploadDate)
     * @param sortOrder sort order (default: desc)
     * @param principal authenticated user
     * @return paginated videos
     */
    @GetMapping("/videos")
    public ResponseEntity<ApiResponse<Page<FileResponse>>> getVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal User user
    ) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<FileResponse> videos = fileService.getVideos(user.getId(), pageable);

        return ResponseEntity.ok(ApiResponse.success("영상 목록 조회 완료", videos));
    }

    /**
     * Get file detail by ID.
     * GET /api/files/{id}
     *
     * @param id        file ID
     * @param principal authenticated user
     * @return file details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileDetailResponse>> getFileDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        FileDetailResponse response = fileService.getFileDetail(id, user.getId());

        return ResponseEntity.ok(ApiResponse.success("파일 상세 조회 완료", response));
    }

    /**
     * Delete a file by ID.
     * DELETE /api/files/{id}
     *
     * @param id        file ID
     * @param principal authenticated user
     * @return success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.info("Delete file request: user={}, fileId={}", user.getUsername(), id);

        fileService.deleteFile(id, user.getId());

        return ResponseEntity.ok(ApiResponse.success("파일 삭제 완료", null));
    }
}
