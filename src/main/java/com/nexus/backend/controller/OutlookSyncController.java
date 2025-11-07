package com.nexus.backend.controller;

import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.EmailSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outlook")
@RequiredArgsConstructor
public class OutlookSyncController {

    private final EmailSyncService emailSyncService;

    /**
     * Outlook 메일 동기화
     * POST /api/outlook/sync
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> syncEmails(
            @AuthenticationPrincipal User user
    ) {
        try {
            int syncedCount = emailSyncService.syncUserEmails(user.getId());
            return ResponseEntity.ok(ApiResponse.success(
                    syncedCount + "개의 메일이 동기화되었습니다",
                    "동기화 완료"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("메일 동기화 실패: " + e.getMessage()));
        }
    }
}
