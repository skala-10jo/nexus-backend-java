package com.nexus.backend.controller;

import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.DeviceCodeResponse;
import com.nexus.backend.dto.response.OutlookAuthStatusResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.OutlookAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outlook/auth")
@RequiredArgsConstructor
public class OutlookAuthController {

    private final OutlookAuthService outlookAuthService;

    /**
     * Outlook 연동 상태 확인
     * GET /api/outlook/auth/status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<OutlookAuthStatusResponse>> getAuthStatus(
            @AuthenticationPrincipal User user
    ) {
        OutlookAuthStatusResponse status = outlookAuthService.getAuthStatus(user.getId());
        return ResponseEntity.ok(ApiResponse.success("인증 상태 조회 성공", status));
    }

    /**
     * Device Flow 인증 시작
     * POST /api/outlook/auth/initiate
     */
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<DeviceCodeResponse>> initiateAuth(
            @AuthenticationPrincipal User user
    ) {
        try {
            DeviceCodeResponse deviceCode = outlookAuthService.initiateDeviceFlow(user.getId()).get();
            return ResponseEntity.ok(ApiResponse.success("Device Flow 인증 시작", deviceCode));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Device Flow 인증 시작 실패: " + e.getMessage()));
        }
    }

    /**
     * Outlook 연동 해제
     * POST /api/outlook/auth/disconnect
     */
    @PostMapping("/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnect(
            @AuthenticationPrincipal User user
    ) {
        outlookAuthService.disconnect(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Outlook 연동이 해제되었습니다", null));
    }
}
