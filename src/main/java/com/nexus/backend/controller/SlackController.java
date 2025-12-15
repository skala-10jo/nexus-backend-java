package com.nexus.backend.controller;

import com.nexus.backend.dto.request.SendSlackMessageRequest;
import com.nexus.backend.dto.request.SlackOAuthCallbackRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.SlackChannelResponse;
import com.nexus.backend.dto.response.SlackIntegrationResponse;
import com.nexus.backend.dto.response.SlackMessageResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.SlackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/slack")
@RequiredArgsConstructor
@Slf4j
public class SlackController {

    private final SlackService slackService;

    /**
     * Get Slack OAuth authorization URL
     */
    @GetMapping("/auth/url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAuthUrl(@AuthenticationPrincipal User user) {
        log.info("Generating Slack OAuth URL for user: {}", user.getId());
        String state = UUID.randomUUID().toString();
        String authUrl = slackService.getAuthorizationUrl(state);

        return ResponseEntity.ok(ApiResponse.success("Slack 인증 URL 생성 완료",
                Map.of("url", authUrl, "state", state)));
    }

    /**
     * Handle Slack OAuth callback
     */
    @PostMapping("/auth/callback")
    public ResponseEntity<ApiResponse<SlackIntegrationResponse>> handleCallback(
            @Valid @RequestBody SlackOAuthCallbackRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Handling Slack OAuth callback for user: {}", user.getId());
        SlackIntegrationResponse response = slackService.handleOAuthCallback(request.getCode(), user);
        return ResponseEntity.ok(ApiResponse.success("Slack 연동 완료", response));
    }

    /**
     * Get Slack integration status for current user
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SlackIntegrationResponse>> getIntegrationStatus(
            @AuthenticationPrincipal User user) {
        log.info("Getting Slack integration status for user: {}", user.getId());
        SlackIntegrationResponse integration = slackService.getIntegration(user);
        return ResponseEntity.ok(ApiResponse.success("Slack 연동 상태 조회 완료", integration));
    }

    /**
     * Disconnect Slack integration
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnectIntegration(
            @AuthenticationPrincipal User user) {
        log.info("Disconnecting Slack integration for user: {}", user.getId());
        slackService.disconnectIntegration(user);
        return ResponseEntity.ok(ApiResponse.success("Slack 연동 해제 완료", null));
    }

    /**
     * Get channels for current user's Slack workspace
     */
    @GetMapping("/channels")
    public ResponseEntity<ApiResponse<List<SlackChannelResponse>>> getChannels(
            @AuthenticationPrincipal User user) {
        log.info("Getting channels for user: {}", user.getId());
        List<SlackChannelResponse> channels = slackService.getChannels(user);
        return ResponseEntity.ok(ApiResponse.success("채널 목록 조회 완료", channels));
    }

    /**
     * Send a message to a Slack channel (with WebSocket broadcast)
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendMessage(
            @Valid @RequestBody SendSlackMessageRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Sending message to Slack channel: {} for user: {}",
                request.getChannelId(), user.getId());
        // Use sendMessageAndBroadcast to also notify WebSocket subscribers
        slackService.sendMessageAndBroadcast(request, user);
        return ResponseEntity.ok(ApiResponse.success("메시지 전송 완료", null));
    }

    /**
     * Get message history for a channel or DM
     */
    @GetMapping("/channels/{channelId}/history")
    public ResponseEntity<ApiResponse<List<SlackMessageResponse>>> getMessageHistory(
            @PathVariable String channelId,
            @AuthenticationPrincipal User user) {
        log.info("Getting message history for channel: {} and user: {}", channelId, user.getId());
        List<SlackMessageResponse> messages = slackService.getMessageHistory(channelId, user);
        return ResponseEntity.ok(ApiResponse.success("메시지 이력 조회 완료", messages));
    }
}
