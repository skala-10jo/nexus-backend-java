package com.nexus.backend.controller;

import com.nexus.backend.dto.request.SendEmailRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.EmailSendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailSendController {

    private final EmailSendService emailSendService;

    /**
     * 메일 발송
     * POST /api/emails/send
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendEmail(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SendEmailRequest request
    ) {
        try {
            emailSendService.sendEmail(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("메일 발송 성공", "메일이 발송되었습니다"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("메일 발송 실패: " + e.getMessage()));
        }
    }
}
