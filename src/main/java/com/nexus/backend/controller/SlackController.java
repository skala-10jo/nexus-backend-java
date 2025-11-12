package com.nexus.backend.controller;

import com.nexus.backend.dto.request.SendSlackMessageRequest;
import com.nexus.backend.dto.request.SlackOAuthCallbackRequest;
import com.nexus.backend.dto.response.SlackChannelResponse;
import com.nexus.backend.dto.response.SlackIntegrationResponse;
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
    public ResponseEntity<Map<String, String>> getAuthUrl(@AuthenticationPrincipal User user) {
        log.info("Generating Slack OAuth URL for user: {}", user.getId());
        String state = UUID.randomUUID().toString();
        String authUrl = slackService.getAuthorizationUrl(state);

        return ResponseEntity.ok(Map.of(
                "url", authUrl,
                "state", state
        ));
    }

    /**
     * Handle Slack OAuth callback
     */
    @PostMapping("/auth/callback")
    public ResponseEntity<SlackIntegrationResponse> handleCallback(
            @Valid @RequestBody SlackOAuthCallbackRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Handling Slack OAuth callback for user: {}", user.getId());
        SlackIntegrationResponse response = slackService.handleOAuthCallback(request.getCode(), user);
        return ResponseEntity.ok(response);
    }

    /**
     * Get Slack integration status for current user
     */
    @GetMapping("/status")
    public ResponseEntity<SlackIntegrationResponse> getIntegrationStatus(
            @AuthenticationPrincipal User user) {
        log.info("Getting Slack integration status for user: {}", user.getId());
        SlackIntegrationResponse integration = slackService.getIntegration(user);
        return ResponseEntity.ok(integration);
    }

    /**
     * Disconnect Slack integration
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnectIntegration(
            @AuthenticationPrincipal User user) {
        log.info("Disconnecting Slack integration for user: {}", user.getId());
        slackService.disconnectIntegration(user);
        return ResponseEntity.ok(Map.of("message", "Slack integration disconnected successfully"));
    }

    /**
     * Get channels for current user's Slack workspace
     */
    @GetMapping("/channels")
    public ResponseEntity<List<SlackChannelResponse>> getChannels(
            @AuthenticationPrincipal User user) {
        log.info("Getting channels for user: {}", user.getId());
        List<SlackChannelResponse> channels = slackService.getChannels(user);
        return ResponseEntity.ok(channels);
    }

    /**
     * Send a message to a Slack channel
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendMessage(
            @Valid @RequestBody SendSlackMessageRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Sending message to Slack channel: {} for user: {}",
                request.getChannelId(), user.getId());
        slackService.sendMessage(request, user);
        return ResponseEntity.ok(Map.of("message", "Message sent successfully"));
    }

    /**
     * Get message history for a channel or DM
     */
    @GetMapping("/channels/{channelId}/history")
    public ResponseEntity<List<com.nexus.backend.dto.response.SlackMessageResponse>> getMessageHistory(
            @PathVariable String channelId,
            @AuthenticationPrincipal User user) {
        log.info("Getting message history for channel: {} and user: {}", channelId, user.getId());
        var messages = slackService.getMessageHistory(channelId, user);
        return ResponseEntity.ok(messages);
    }
}
