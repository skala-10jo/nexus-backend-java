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
     * Get all Slack integrations for current user
     */
    @GetMapping("/integrations")
    public ResponseEntity<List<SlackIntegrationResponse>> getIntegrations(
            @AuthenticationPrincipal User user) {
        log.info("Getting Slack integrations for user: {}", user.getId());
        List<SlackIntegrationResponse> integrations = slackService.getUserIntegrations(user);
        return ResponseEntity.ok(integrations);
    }

    /**
     * Get a specific Slack integration
     */
    @GetMapping("/integrations/{integrationId}")
    public ResponseEntity<SlackIntegrationResponse> getIntegration(
            @PathVariable UUID integrationId,
            @AuthenticationPrincipal User user) {
        log.info("Getting Slack integration: {} for user: {}", integrationId, user.getId());
        SlackIntegrationResponse integration = slackService.getIntegration(integrationId, user);
        return ResponseEntity.ok(integration);
    }

    /**
     * Delete a Slack integration
     */
    @DeleteMapping("/integrations/{integrationId}")
    public ResponseEntity<Map<String, String>> deleteIntegration(
            @PathVariable UUID integrationId,
            @AuthenticationPrincipal User user) {
        log.info("Deleting Slack integration: {} for user: {}", integrationId, user.getId());
        slackService.deleteIntegration(integrationId, user);
        return ResponseEntity.ok(Map.of("message", "Slack integration deleted successfully"));
    }

    /**
     * Get channels for a Slack workspace
     */
    @GetMapping("/integrations/{integrationId}/channels")
    public ResponseEntity<List<SlackChannelResponse>> getChannels(
            @PathVariable UUID integrationId,
            @AuthenticationPrincipal User user) {
        log.info("Getting channels for integration: {} and user: {}", integrationId, user.getId());
        List<SlackChannelResponse> channels = slackService.getChannels(integrationId, user);
        return ResponseEntity.ok(channels);
    }

    /**
     * Send a message to a Slack channel
     */
    @PostMapping("/integrations/{integrationId}/send")
    public ResponseEntity<Map<String, String>> sendMessage(
            @PathVariable UUID integrationId,
            @Valid @RequestBody SendSlackMessageRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Sending message to Slack channel: {} via integration: {}",
                request.getChannelId(), integrationId);
        slackService.sendMessage(integrationId, request, user);
        return ResponseEntity.ok(Map.of("message", "Message sent successfully"));
    }
}
