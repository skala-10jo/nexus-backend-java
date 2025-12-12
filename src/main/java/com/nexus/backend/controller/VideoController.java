package com.nexus.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.backend.dto.request.VideoUploadRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.FileResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.exception.ServiceException;
import com.nexus.backend.service.FileService;
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

/**
 * REST Controller for video file management.
 *
 * Endpoints:
 * - POST /api/videos/upload : Upload video file
 * - GET /api/videos : List user videos
 * - GET /api/videos/{id} : Get video details
 * - DELETE /api/videos/{id} : Delete video
 *
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final FileService fileService;
    private final ObjectMapper objectMapper;

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
            @AuthenticationPrincipal User user) {
        try {
            log.info("Video upload request: user={}, filename={}",
                    user.getUsername(), file.getOriginalFilename());

            // Parse VideoUploadRequest from JSON string
            VideoUploadRequest request = objectMapper.readValue(requestJson, VideoUploadRequest.class);

            // Validate file (delegated to Service)
            fileService.validateVideoFile(file);

            // Upload video
            FileResponse response = fileService.uploadVideo(file, request, user);

            log.info("Video upload successful: fileId={}", response.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("영상 업로드 완료", response));

        } catch (ServiceException ex) {
            log.warn("Video upload validation failed: user={}, error={}",
                    user.getUsername(), ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ex.getMessage()));
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
            @AuthenticationPrincipal User user) {
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
            @AuthenticationPrincipal User user) {
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
            @AuthenticationPrincipal User user) {
        try {
            log.info("Video stream request: user={}, videoId={}", user.getUsername(), id);

            // Get video stream resource from Service (all business logic delegated)
            FileService.VideoStreamResult streamResult = fileService.getVideoStreamResource(id, user.getId());

            log.debug("Streaming video: videoId={}, contentType={}", id, streamResult.contentType());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(streamResult.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + streamResult.filename() + "\"")
                    .body(streamResult.resource());

        } catch (ServiceException ex) {
            log.warn("Video stream access denied: videoId={}, error={}", id, ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception ex) {
            log.error("Failed to stream video: videoId={}, error={}", id, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
