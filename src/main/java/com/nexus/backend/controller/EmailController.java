package com.nexus.backend.controller;

import com.nexus.backend.dto.request.AssignProjectRequest;
import com.nexus.backend.dto.request.UpdateReadStatusRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.EmailDetailResponse;
import com.nexus.backend.dto.response.EmailResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    /**
     * 메일 목록 조회
     * GET /api/emails?folder=Inbox&projectId=xxx&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmailResponse>>> getEmails(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "receivedDateTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<EmailResponse> emails = emailService.getEmails(
                user.getId(),
                folder,
                projectId,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success("메일 목록 조회 성공", emails));
    }

    /**
     * 메일 상세 조회
     * GET /api/emails/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailDetailResponse>> getEmailDetail(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        EmailDetailResponse email = emailService.getEmailDetail(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("메일 조회 성공", email));
    }

    /**
     * 메일 검색
     * GET /api/emails/search?query=keyword
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<EmailResponse>>> searchEmails(
            @AuthenticationPrincipal User user,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedDateTime"));
        Page<EmailResponse> emails = emailService.searchEmails(user.getId(), query, pageable);

        return ResponseEntity.ok(ApiResponse.success("메일 검색 성공", emails));
    }

    /**
     * 읽음 상태 변경
     * PUT /api/emails/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<EmailResponse>> updateReadStatus(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReadStatusRequest request
    ) {
        EmailResponse email = emailService.updateReadStatus(user.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("읽음 상태 변경 성공", email));
    }

    /**
     * 프로젝트 할당
     * PUT /api/emails/{id}/project
     */
    @PutMapping("/{id}/project")
    public ResponseEntity<ApiResponse<EmailResponse>> assignProject(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody AssignProjectRequest request
    ) {
        EmailResponse email = emailService.assignProject(user.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("프로젝트 할당 성공", email));
    }

    /**
     * 메일 삭제
     * DELETE /api/emails/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmail(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        emailService.deleteEmail(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("메일 삭제 성공", null));
    }

    /**
     * 안읽은 메일 개수
     * GET /api/emails/unread/count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal User user
    ) {
        long count = emailService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.success("안읽은 메일 개수 조회 성공", count));
    }
}
