package com.nexus.backend.controller;

import com.nexus.backend.dto.request.SendSlackMessageRequest;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.UserRepository;
import com.nexus.backend.service.SlackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket controller for real-time Slack messaging.
 *
 * Handles STOMP messages from clients and broadcasts to subscribers.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SlackWebSocketController {

    private final SlackService slackService;
    private final UserRepository userRepository;

    /**
     * Handle message send from client via WebSocket.
     *
     * Client sends to: /app/slack/send
     * Server broadcasts to: /topic/slack/{channelId}
     *
     * @param request Message request
     * @param principal Authenticated principal
     */
    @MessageMapping("/slack/send")
    public void sendMessage(
            @Payload SendSlackMessageRequest request,
            Principal principal) {

        log.info("WebSocket message received: channel={}, principal={}",
                request.getChannelId(), principal != null ? principal.getName() : "anonymous");

        try {
            User user = null;

            // Try to get User from principal
            if (principal instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
                if (auth.getPrincipal() instanceof User) {
                    user = (User) auth.getPrincipal();
                    log.info("WebSocket user from principal: {}", user.getUsername());
                }
            }

            // Fallback: use username from request payload
            if (user == null && request.getUsername() != null && !request.getUsername().isEmpty()) {
                log.info("Using username from request payload: {}", request.getUsername());
                user = userRepository.findByUsername(request.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found: " + request.getUsername()));
            }

            if (user == null) {
                log.error("Cannot extract user from principal or request");
                return;
            }

            log.info("WebSocket authenticated user loaded: {}", user.getId());

            // Send message to Slack and broadcast via WebSocket
            slackService.sendMessageAndBroadcast(request, user);
        } catch (Exception e) {
            log.error("Failed to send WebSocket message: {}", e.getMessage(), e);
        }
    }
}
