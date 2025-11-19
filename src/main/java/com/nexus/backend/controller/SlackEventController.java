package com.nexus.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.backend.dto.response.SlackMessageResponse;
import com.nexus.backend.service.SlackService;
import com.slack.api.Slack;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Controller for receiving Slack Event API webhooks.
 *
 * Handles real-time message events from Slack and broadcasts them via WebSocket.
 */
@RestController
@RequestMapping("/api/slack/events")
@RequiredArgsConstructor
@Slf4j
public class SlackEventController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final SlackService slackService;

    /**
     * Slack Event API endpoint.
     *
     * Receives events from Slack and broadcasts to WebSocket subscribers.
     *
     * @param payload Event payload from Slack
     * @return Response for Slack verification or acknowledgment
     */
    @PostMapping
    public ResponseEntity<?> handleSlackEvent(@RequestBody String payload) {
        try {
            log.info("Received Slack event: {}", payload);

            JsonNode event = objectMapper.readTree(payload);

            // Handle URL verification challenge (first-time setup)
            if (event.has("challenge")) {
                String challenge = event.get("challenge").asText();
                log.info("Responding to Slack challenge: {}", challenge);
                return ResponseEntity.ok(Map.of("challenge", challenge));
            }

            // Handle actual events
            if (event.has("event")) {
                JsonNode eventData = event.get("event");
                String eventType = eventData.get("type").asText();

                log.info("Event type: {}", eventType);

                // Handle message events
                if ("message".equals(eventType)) {
                    handleMessageEvent(eventData);
                }
            }

            // Acknowledge receipt
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to process Slack event: {}", e.getMessage(), e);
            return ResponseEntity.ok().build(); // Still acknowledge to Slack
        }
    }

    /**
     * Handle incoming message events from Slack.
     *
     * @param eventData Message event data
     */
    private void handleMessageEvent(JsonNode eventData) {
        try {
            // Ignore bot messages and message changes to avoid loops
            if (eventData.has("bot_id") ||
                eventData.has("subtype") && !"file_share".equals(eventData.get("subtype").asText())) {
                log.debug("Ignoring bot or edited message");
                return;
            }

            String channelId = eventData.get("channel").asText();
            String text = eventData.has("text") ? eventData.get("text").asText() : "";
            String slackUserId = eventData.has("user") ? eventData.get("user").asText() : "unknown";
            String ts = eventData.get("ts").asText();

            // Convert timestamp to LocalDateTime
            double timestamp = Double.parseDouble(ts);
            LocalDateTime messageTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli((long) (timestamp * 1000)),
                    ZoneId.systemDefault()
            );

            // Get real username from Slack API
            String username = slackService.getSlackUserDisplayName(slackUserId);

            log.info("Broadcasting message from Slack: channel={}, user={}, username={}, text={}",
                    channelId, slackUserId, username, text);

            // Create message response
            SlackMessageResponse message = SlackMessageResponse.builder()
                    .text(text)
                    .channelId(channelId)
                    .userId(slackUserId)
                    .username(username)
                    .timestamp(messageTime.toString())
                    .type("text")
                    .build();

            // Broadcast to WebSocket subscribers
            messagingTemplate.convertAndSend(
                    "/topic/slack/" + channelId,
                    message
            );

            log.info("Message broadcasted via WebSocket to channel: {}", channelId);

        } catch (Exception e) {
            log.error("Failed to handle message event: {}", e.getMessage(), e);
        }
    }
}
