package com.nexus.backend.controller;

import com.nexus.backend.dto.request.VideoUploadRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.VideoDocumentResponse;
import com.nexus.backend.dto.response.VideoSubtitleResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.VideoDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoDocumentController {

    private final VideoDocumentService videoDocumentService;
    private final ObjectMapper objectMapper;

    /**
     * 영상 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<VideoDocumentResponse>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("request") String requestJson,
            @AuthenticationPrincipal User user
    ) {
        try {
            // JSON 문자열을 VideoUploadRequest로 변환
            VideoUploadRequest request = objectMapper.readValue(requestJson, VideoUploadRequest.class);

            VideoDocumentResponse response = videoDocumentService.uploadVideo(file, request, user);

            return ResponseEntity.ok(
                    ApiResponse.success("영상이 성공적으로 업로드되었습니다.", response)
            );
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("영상 업로드에 실패했습니다: " + ex.getMessage())
            );
        }
    }

    /**
     * 영상 정보 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoDocumentResponse>> getVideoDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        try {
            VideoDocumentResponse response = videoDocumentService.getVideoDocument(id, user);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ex.getMessage())
            );
        }
    }

    /**
     * 자막 목록 조회
     */
    @GetMapping("/{id}/subtitles")
    public ResponseEntity<ApiResponse<List<VideoSubtitleResponse>>> getSubtitles(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        try {
            List<VideoSubtitleResponse> subtitles = videoDocumentService.getSubtitles(id, user);
            return ResponseEntity.ok(
                    ApiResponse.success("자막 목록 조회 완료", subtitles)
            );
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ex.getMessage())
            );
        }
    }

    /**
     * 내 영상 목록 조회
     */
    @GetMapping("/user/me")
    public ResponseEntity<ApiResponse<Page<VideoDocumentResponse>>> getUserVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal User user
    ) {
        Sort sort = sortOrder.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<VideoDocumentResponse> videos = videoDocumentService.getUserVideos(user.getId(), pageable);

        return ResponseEntity.ok(ApiResponse.success(videos));
    }
}
