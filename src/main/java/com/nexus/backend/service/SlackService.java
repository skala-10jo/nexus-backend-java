package com.nexus.backend.service;

import com.nexus.backend.dto.request.SendSlackMessageRequest;
import com.nexus.backend.dto.response.SlackChannelResponse;
import com.nexus.backend.dto.response.SlackIntegrationResponse;
import com.nexus.backend.dto.response.SlackMessageResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.UserRepository;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.model.ConversationType;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.slack.api.model.Conversation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackService {

    private final UserRepository userRepository;
    private final Slack slack = Slack.getInstance();
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${slack.client-id}")
    private String clientId;

    @Value("${slack.client-secret}")
    private String clientSecret;

    @Value("${slack.redirect-uri}")
    private String redirectUri;

    /**
     * Generate Slack OAuth authorization URL
     */
    public String getAuthorizationUrl(String state) {
        // Bot scopes
        String botScopes = "channels:read,channels:history,chat:write,users:read,team:read";
        // User scopes for DM access, messaging, and history
        String userScopes = "channels:read,channels:history,im:read,im:history,mpim:read,mpim:history,chat:write,users:read";

        // URL encode the redirect URI
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        return String.format(
                "https://slack.com/oauth/v2/authorize?client_id=%s&scope=%s&user_scope=%s&redirect_uri=%s&state=%s",
                clientId, botScopes, userScopes, encodedRedirectUri, state
        );
    }

    /**
     * Handle OAuth callback and save integration to User entity
     */
    @Transactional
    public SlackIntegrationResponse handleOAuthCallback(String code, User user) {
        try {
            // Exchange code for access token
            OAuthV2AccessRequest request = OAuthV2AccessRequest.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .code(code)
                    .redirectUri(redirectUri)
                    .build();

            OAuthV2AccessResponse response = slack.methods().oauthV2Access(request);

            if (!response.isOk()) {
                throw new RuntimeException("Failed to exchange OAuth code: " + response.getError());
            }

            // Update user's Slack integration fields
            user.setSlackWorkspaceId(response.getTeam().getId());
            user.setSlackWorkspaceName(response.getTeam().getName());
            user.setSlackAccessToken(response.getAccessToken());
            user.setSlackBotUserId(response.getBotUserId());
            user.setSlackBotAccessToken(response.getAccessToken());

            // Store user access token if available (for DM access)
            if (response.getAuthedUser() != null && response.getAuthedUser().getAccessToken() != null) {
                user.setSlackUserAccessToken(response.getAuthedUser().getAccessToken());
                log.info("Saved user access token for DM support");
            }

            user.setSlackScope(response.getScope());
            user.setSlackIsActive(true);

            // Set connected timestamp if this is first connection
            if (user.getSlackConnectedAt() == null) {
                user.setSlackConnectedAt(LocalDateTime.now());
            }

            user = userRepository.save(user);

            log.info("Slack integration created/updated for user {} and workspace {}",
                    user.getId(), response.getTeam().getId());

            return SlackIntegrationResponse.from(user);

        } catch (IOException | SlackApiException e) {
            log.error("Error during Slack OAuth callback", e);
            throw new RuntimeException("Failed to complete Slack OAuth: " + e.getMessage());
        }
    }

    /**
     * Get Slack integration status for current user
     */
    @Transactional(readOnly = true)
    public SlackIntegrationResponse getIntegration(User user) {
        if (user.getSlackWorkspaceId() == null) {
            throw new RuntimeException("Slack integration not found");
        }
        return SlackIntegrationResponse.from(user);
    }

    /**
     * Disconnect Slack integration (clear all Slack fields)
     */
    @Transactional
    public void disconnectIntegration(User user) {
        user.setSlackWorkspaceId(null);
        user.setSlackWorkspaceName(null);
        user.setSlackAccessToken(null);
        user.setSlackBotUserId(null);
        user.setSlackBotAccessToken(null);
        user.setSlackUserAccessToken(null);
        user.setSlackScope(null);
        user.setSlackIsActive(false);
        user.setSlackTokenExpiresAt(null);
        user.setSlackConnectedAt(null);

        userRepository.save(user);
        log.info("Disconnected Slack integration for user {}", user.getId());
    }

    /**
     * Get list of channels and DMs for current user's workspace
     */
    @Transactional(readOnly = true)
    public List<SlackChannelResponse> getChannels(User user) {
        if (user.getSlackWorkspaceId() == null) {
            throw new RuntimeException("Slack integration not found");
        }

        if (!Boolean.TRUE.equals(user.getSlackIsActive())) {
            throw new RuntimeException("Slack integration is not active");
        }

        try {
            List<SlackChannelResponse> allChannels = new java.util.ArrayList<>();
            String token = user.getSlackBotAccessToken();

            // Fetch regular channels using conversationsList
            ConversationsListRequest channelsRequest = ConversationsListRequest.builder()
                    .token(token)
                    .excludeArchived(true)
                    .build();

            ConversationsListResponse channelsResponse = slack.methods().conversationsList(channelsRequest);

            if (!channelsResponse.isOk()) {
                throw new RuntimeException("Failed to fetch channels: " + channelsResponse.getError());
            }

            log.info("ConversationsList returned {} conversations", channelsResponse.getChannels().size());

            // Process regular channels
            channelsResponse.getChannels().stream()
                    .map(channel -> SlackChannelResponse.builder()
                            .id(channel.getId())
                            .name(channel.getName())
                            .isPrivate(channel.isPrivate())
                            .isMember(channel.isMember())
                            .isDM(false)
                            .build())
                    .forEach(allChannels::add);

            // Fetch DMs using user access token with types parameter
            if (user.getSlackUserAccessToken() != null) {
                try {
                    String userToken = user.getSlackUserAccessToken();

                    // OPTIMIZATION: Batch fetch all user display names (1 API call instead of N)
                    Map<String, String> userDisplayNames = fetchAllUserDisplayNames(userToken);

                    // Try fetching DMs with types=im,mpim using ConversationType enum
                    ConversationsListRequest dmRequest = ConversationsListRequest.builder()
                            .token(userToken)
                            .types(java.util.Arrays.asList(ConversationType.IM, ConversationType.MPIM))
                            .excludeArchived(true)
                            .limit(1000)
                            .build();

                    ConversationsListResponse dmResponse = slack.methods().conversationsList(dmRequest);

                    if (dmResponse.isOk()) {
                        log.info("DM conversations.list returned {} items", dmResponse.getChannels().size());

                        // Add all DMs to the list using batch-fetched user names
                        dmResponse.getChannels().forEach(dm -> {
                            String displayName;
                            if (dm.isIm() && dm.getUser() != null) {
                                // Use batch-fetched map instead of individual API calls
                                displayName = getUserDisplayNameFromMap(userDisplayNames, dm.getUser(), "Direct Message");
                            } else if (dm.isMpim()) {
                                displayName = "Group DM";
                            } else {
                                displayName = "Direct Message";
                            }

                            allChannels.add(SlackChannelResponse.builder()
                                    .id(dm.getId())
                                    .name(displayName)
                                    .isPrivate(true)
                                    .isMember(true)
                                    .isDM(true)
                                    .build());
                        });

                        log.info("Added {} DMs", dmResponse.getChannels().size());
                    } else {
                        log.warn("Failed to fetch DMs: error={}, needed={}",
                                dmResponse.getError(), dmResponse.getNeeded());
                    }
                } catch (Exception e) {
                    log.error("Error fetching DMs: {}", e.getMessage(), e);
                }
            } else {
                log.warn("No user access token available for DM fetching");
            }

            log.info("Fetched {} total conversations", allChannels.size());
            return allChannels;

        } catch (IOException | SlackApiException e) {
            log.error("Error fetching Slack channels", e);
            throw new RuntimeException("Failed to fetch channels: " + e.getMessage());
        }
    }

    /**
     * Batch fetch all workspace users' display names (OPTIMIZED)
     * Reduces N API calls to 1 API call
     */
    private Map<String, String> fetchAllUserDisplayNames(String token) {
        Map<String, String> userNames = new HashMap<>();
        try {
            var response = slack.methods(token).usersList(req -> req.limit(1000));
            if (response.isOk() && response.getMembers() != null) {
                response.getMembers().forEach(user -> {
                    String displayName = user.getProfile() != null ? user.getProfile().getDisplayName() : null;
                    String realName = user.getRealName();
                    String name = (displayName != null && !displayName.isEmpty()) ? displayName : realName;
                    if (name != null) {
                        userNames.put(user.getId(), name);
                    }
                });
                log.info("Batch fetched {} user display names", userNames.size());
            }
        } catch (Exception e) {
            log.warn("Failed to batch fetch users: {}", e.getMessage());
        }
        return userNames;
    }

    /**
     * Get user display name from cache map, or return default
     */
    private String getUserDisplayNameFromMap(Map<String, String> userMap, String userId, String defaultName) {
        return userMap.getOrDefault(userId, defaultName);
    }

    /**
     * Get user display name for DMs (legacy - for single lookups)
     */
    private String getUserDisplayName(String token, String userId) {
        try {
            var userInfo = slack.methods(token).usersInfo(req -> req.user(userId));
            if (userInfo.isOk() && userInfo.getUser() != null) {
                String realName = userInfo.getUser().getRealName();
                String displayName = userInfo.getUser().getProfile().getDisplayName();
                return displayName != null && !displayName.isEmpty() ? displayName : realName;
            }
        } catch (Exception e) {
            log.warn("Failed to get user display name for {}: {}", userId, e.getMessage());
        }
        return "Direct Message";
    }

    /**
     * Get Slack user display name using any available bot token
     * Used by Event API to get real names for incoming messages
     */
    public String getSlackUserDisplayName(String slackUserId) {
        // Find any user with active Slack integration to get a token
        List<User> slackUsers = userRepository.findAll().stream()
                .filter(u -> u.getSlackBotAccessToken() != null && Boolean.TRUE.equals(u.getSlackIsActive()))
                .toList();

        if (slackUsers.isEmpty()) {
            log.warn("No active Slack integration found to fetch user info");
            return "Slack User";
        }

        // Use the first available token (all should work for the same workspace)
        String token = slackUsers.get(0).getSlackBotAccessToken();
        return getUserDisplayName(token, slackUserId);
    }

    /**
     * Send a message to Slack and broadcast via WebSocket
     */
    @Transactional(readOnly = true)
    public void sendMessageAndBroadcast(SendSlackMessageRequest request, User user) {
        // Send to Slack
        sendMessage(request, user);

        // Broadcast to WebSocket subscribers
        SlackMessageResponse messageResponse = SlackMessageResponse.builder()
                .text(request.getText())
                .channelId(request.getChannelId())
                .userId(user.getId().toString())
                .username(user.getUsername())
                .timestamp(LocalDateTime.now().toString())
                .type("text")
                .build();

        messagingTemplate.convertAndSend(
                "/topic/slack/" + request.getChannelId(),
                messageResponse
        );

        log.info("Message broadcasted via WebSocket to channel: {}", request.getChannelId());
    }

    /**
     * Send a message to a Slack channel or DM (without WebSocket broadcast)
     */
    @Transactional(readOnly = true)
    public void sendMessage(SendSlackMessageRequest request, User user) {
        if (user.getSlackWorkspaceId() == null) {
            throw new RuntimeException("Slack integration not found");
        }

        if (!Boolean.TRUE.equals(user.getSlackIsActive())) {
            throw new RuntimeException("Slack integration is not active");
        }

        try {
            // Determine if this is a DM based on channel ID prefix
            // D = Direct Message, G = Group DM (some older format), C = Channel
            boolean isDM = request.getChannelId().startsWith("D") || request.getChannelId().startsWith("G");

            // Use user token for DMs, bot token for channels
            String token = isDM && user.getSlackUserAccessToken() != null
                    ? user.getSlackUserAccessToken()
                    : user.getSlackBotAccessToken();

            log.info("Sending message to {} (isDM={}) using {} token",
                    request.getChannelId(), isDM, isDM ? "user" : "bot");

            // For channel messages via bot, prepend sender's info for identification
            String messageText = request.getText();
            if (!isDM) {
                String senderName = user.getFullName() != null ? user.getFullName() : user.getUsername();
                messageText = "<" + senderName + "님이 보낸 메시지입니다.>\n" + messageText;
            }

            final String finalMessageText = messageText;
            var response = slack.methods(token)
                    .chatPostMessage(req -> req
                            .channel(request.getChannelId())
                            .text(finalMessageText)
                    );

            if (!response.isOk()) {
                throw new RuntimeException("Failed to send message: " + response.getError());
            }

            log.info("Message sent to Slack channel {} for user {}",
                    request.getChannelId(), user.getId());

        } catch (IOException | SlackApiException e) {
            log.error("Error sending Slack message", e);
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Get message history for a channel or DM
     */
    @Transactional(readOnly = true)
    public List<SlackMessageResponse> getMessageHistory(String channelId, User user) {
        if (user.getSlackWorkspaceId() == null) {
            throw new RuntimeException("Slack integration not found");
        }

        if (!Boolean.TRUE.equals(user.getSlackIsActive())) {
            throw new RuntimeException("Slack integration is not active");
        }

        try {
            // Determine if this is a DM
            boolean isDM = channelId.startsWith("D") || channelId.startsWith("G");

            // Use appropriate token
            String token = isDM && user.getSlackUserAccessToken() != null
                    ? user.getSlackUserAccessToken()
                    : user.getSlackBotAccessToken();

            var response = slack.methods(token).conversationsHistory(req -> req
                    .channel(channelId)
                    .limit(50));

            if (!response.isOk()) {
                throw new RuntimeException("Failed to fetch message history: " + response.getError());
            }

            log.info("Fetched {} messages from channel {}", response.getMessages().size(), channelId);

            // OPTIMIZATION: Batch fetch all user display names (1 API call instead of N)
            Map<String, String> userDisplayNames = fetchAllUserDisplayNames(token);

            // Get current user's Slack user ID using USER token (not bot token!)
            // auth.test with bot token returns bot's ID, not user's ID
            String currentUserSlackId = null;
            if (user.getSlackUserAccessToken() != null) {
                try {
                    var authResponse = slack.methods(user.getSlackUserAccessToken()).authTest(req -> req);
                    if (authResponse.isOk()) {
                        currentUserSlackId = authResponse.getUserId();
                        log.info("Current user's Slack ID: {}", currentUserSlackId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to get current user's Slack ID", e);
                }
            } else {
                log.warn("User access token not available, cannot determine message ownership");
            }

            final String finalCurrentUserSlackId = currentUserSlackId;

            return response.getMessages().stream()
                    .map(message -> {
                        String username = null;
                        String slackUserId = null;

                        if (message.getUser() != null) {
                            slackUserId = message.getUser();
                            // Use batch-fetched map instead of individual API calls
                            username = getUserDisplayNameFromMap(userDisplayNames, slackUserId, "Unknown User");
                        } else if (message.getBotId() != null) {
                            username = message.getUsername() != null ? message.getUsername() : "Bot";
                        }

                        // Check if this message is from the current user
                        // Compare Slack user ID with the current user's Slack ID from auth.test
                        String userId = null;
                        if (slackUserId != null && slackUserId.equals(finalCurrentUserSlackId)) {
                            // This message is from the current logged-in user
                            userId = user.getId().toString();
                        }

                        return SlackMessageResponse.builder()
                                .text(message.getText())
                                .user(message.getUser())
                                .userId(userId)
                                .username(username)
                                .timestamp(message.getTs())
                                .botId(message.getBotId())
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (IOException | SlackApiException e) {
            log.error("Error fetching message history", e);
            throw new RuntimeException("Failed to fetch message history: " + e.getMessage());
        }
    }
}
