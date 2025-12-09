package com.nexus.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.backend.dto.request.VideoUploadRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.FileResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.entity.VideoFile;
import com.nexus.backend.repository.VideoFileRepository;
import com.nexus.backend.service.FileService;
import com.nexus.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;

/**
 * REST Controller for video file management.
 *
 * Endpoints:
 * - POST   /api/videos/upload     : Upload video file
 * - GET    /api/videos             : List user videos
 * - GET    /api/videos/{id}        : Get video details
 * - DELETE /api/videos/{id}        : Delete video
 *
 * @author NEXUS Team
 * @version 1.0
 * @since 2025-01-19
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final VideoFileRepository videoFileRepository;
    private final FileStorageService fileStorageService;

    /**
     * Upload a video file.
     *
     * @param file        the video file (MultipartFile)
     * @param requestJson JSON string containing VideoUploadRequest
     * @param user        authenticated user
     * @return uploaded video information
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileResponse>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("request") String requestJson,
            @AuthenticationPrincipal User user
    ) {
        try {
            log.info("Video upload request: user={}, filename={}",
                    user.getUsername(), file.getOriginalFilename());

            // Parse VideoUploadRequest from JSON string
            VideoUploadRequest request = objectMapper.readValue(requestJson, VideoUploadRequest.class);

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("영상 파일이 비어있습니다"));
            }

            // Validate MIME type (video types)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("영상 파일만 업로드 가능합니다"));
            }

            // Upload video
            FileResponse response = fileService.uploadVideo(file, request, user);

            log.info("Video upload successful: fileId={}", response.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("영상 업로드 완료", response));

        } catch (Exception ex) {
            log.error("Video upload failed: user={}, error={}",
                    user.getUsername(), ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("영상 업로드 실패: " + ex.getMessage()));
        }
    }

    /**
     * List user videos with pagination.
     *
     * @param page      page number (default: 0)
     * @param size      page size (default: 20)
     * @param sortBy    sort field (default: uploadDate)
     * @param sortOrder sort direction (asc/desc, default: desc)
     * @param user      authenticated user
     * @return paginated video list
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FileResponse>>> getVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal User user
    ) {
        try {
            Sort sort = sortOrder.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageable = PageRequest.of(page, size, sort);

            Page<FileResponse> videos = fileService.getVideos(user.getId(), pageable);

            log.info("Retrieved {} videos for user: {}", videos.getTotalElements(), user.getUsername());

            return ResponseEntity.ok(ApiResponse.success("영상 목록 조회 완료", videos));

        } catch (Exception ex) {
            log.error("Failed to retrieve videos: user={}, error={}",
                    user.getUsername(), ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("영상 목록 조회 실패"));
        }
    }

    /**
     * Delete a video file.
     *
     * @param id   video file ID
     * @param user authenticated user
     * @return deletion result
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(
            @PathVariable java.util.UUID id,
            @AuthenticationPrincipal User user
    ) {
        try {
            log.info("Video delete request: user={}, videoId={}", user.getUsername(), id);

            fileService.deleteFile(id, user.getId());

            log.info("Video deleted successfully: videoId={}", id);

            return ResponseEntity.ok(ApiResponse.success("영상 삭제 완료", null));

        } catch (Exception ex) {
            log.error("Failed to delete video: videoId={}, user={}, error={}",
                    id, user.getUsername(), ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("영상 삭제 실패: " + ex.getMessage()));
        }
    }

    /**
     * Stream a video file.
     *
     * @param id   video file ID
     * @param user authenticated user
     * @return video file stream
     */
    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable java.util.UUID id,
            @AuthenticationPrincipal User user
    ) {
        try {
            log.info("Video stream request: user={}, videoId={}", user.getUsername(), id);

            // Find video file with file info
            VideoFile videoFile = videoFileRepository.findByIdWithFile(id)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            // Security check: verify user owns this video
            if (!videoFile.getFile().getUser().getId().equals(user.getId())) {
                log.warn("Unauthorized video stream attempt: user={}, videoId={}",
                        user.getUsername(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Load video file as resource
            String filePath = videoFile.getFile().getFilePath();
            Resource resource = fileStorageService.loadFileAsResource(filePath);

            // Determine content type
            String contentType = videoFile.getFile().getMimeType();
            if (contentType == null) {
                try {
                    contentType = Files.probeContentType(fileStorageService.getFilePath(filePath));
                } catch (IOException e) {
                    contentType = "video/mp4";
                }
            }

            log.debug("Streaming video: path={}, contentType={}", filePath, contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + videoFile.getFile().getOriginalFilename() + "\"")
                    .body(resource);

        } catch (Exception ex) {
            log.error("Failed to stream video: videoId={}, error={}", id, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
