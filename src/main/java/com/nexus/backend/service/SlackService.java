package com.nexus.backend.service;

import com.nexus.backend.dto.request.SendSlackMessageRequest;
import com.nexus.backend.dto.response.SlackChannelResponse;
import com.nexus.backend.dto.response.SlackIntegrationResponse;
import com.nexus.backend.dto.response.SlackMessageResponse;
import com.nexus.backend.entity.SlackIntegration;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.SlackIntegrationRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackService {

    private final SlackIntegrationRepository slackIntegrationRepository;
    private final Slack slack = Slack.getInstance();

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

        return String.format(
                "https://slack.com/oauth/v2/authorize?client_id=%s&scope=%s&user_scope=%s&redirect_uri=%s&state=%s",
                clientId, botScopes, userScopes, redirectUri, state
        );
    }

    /**
     * Handle OAuth callback and save integration
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

            // Check if integration already exists for this workspace
            SlackIntegration integration = slackIntegrationRepository
                    .findByUserIdAndWorkspaceId(user.getId(), response.getTeam().getId())
                    .orElse(SlackIntegration.builder()
                            .user(user)
                            .workspaceId(response.getTeam().getId())
                            .build());

            // Update integration details
            integration.setWorkspaceName(response.getTeam().getName());
            integration.setAccessToken(response.getAccessToken());
            integration.setBotUserId(response.getBotUserId());
            integration.setBotAccessToken(response.getAccessToken());

            // Store user access token if available (for DM access)
            if (response.getAuthedUser() != null && response.getAuthedUser().getAccessToken() != null) {
                integration.setUserAccessToken(response.getAuthedUser().getAccessToken());
                log.info("Saved user access token for DM support");
            }

            integration.setScope(response.getScope());
            integration.setIsActive(true);

            integration = slackIntegrationRepository.save(integration);

            log.info("Slack integration created/updated for user {} and workspace {}",
                    user.getId(), response.getTeam().getId());

            return SlackIntegrationResponse.from(integration);

        } catch (IOException | SlackApiException e) {
            log.error("Error during Slack OAuth callback", e);
            throw new RuntimeException("Failed to complete Slack OAuth: " + e.getMessage());
        }
    }

    /**
     * Get all Slack integrations for a user
     */
    @Transactional(readOnly = true)
    public List<SlackIntegrationResponse> getUserIntegrations(User user) {
        return slackIntegrationRepository.findByUserId(user.getId())
                .stream()
                .map(SlackIntegrationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific integration
     */
    @Transactional(readOnly = true)
    public SlackIntegrationResponse getIntegration(UUID integrationId, User user) {
        SlackIntegration integration = slackIntegrationRepository
                .findByIdAndUserId(integrationId, user.getId())
                .orElseThrow(() -> new RuntimeException("Slack integration not found"));

        return SlackIntegrationResponse.from(integration);
    }

    /**
     * Delete Slack integration
     */
    @Transactional
    public void deleteIntegration(UUID integrationId, User user) {
        SlackIntegration integration = slackIntegrationRepository
                .findByIdAndUserId(integrationId, user.getId())
                .orElseThrow(() -> new RuntimeException("Slack integration not found"));

        slackIntegrationRepository.delete(integration);
        log.info("Deleted Slack integration {} for user {}", integrationId, user.getId());
    }

    /**
     * Get list of channels and DMs for a workspace
     */
    @Transactional(readOnly = true)
    public List<SlackChannelResponse> getChannels(UUID integrationId, User user) {
        SlackIntegration integration = slackIntegrationRepository
                .findByIdAndUserId(integrationId, user.getId())
                .orElseThrow(() -> new RuntimeException("Slack integration not found"));

        if (!integration.getIsActive()) {
            throw new RuntimeException("Slack integration is not active");
        }

        try {
            List<SlackChannelResponse> allChannels = new java.util.ArrayList<>();
            String token = integration.getBotAccessToken();

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
            if (integration.getUserAccessToken() != null) {
                try {
                    String userToken = integration.getUserAccessToken();

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

                        dmResponse.getChannels().forEach(conv -> {
                            log.info("DM Conversation: id={}, name={}, isIm={}, isMpim={}, user={}",
                                    conv.getId(), conv.getName(), conv.isIm(), conv.isMpim(), conv.getUser());
                        });

                        // Add all DMs to the list
                        dmResponse.getChannels().forEach(dm -> {
                            String displayName;
                            if (dm.isIm() && dm.getUser() != null) {
                                displayName = getUserDisplayName(userToken, dm.getUser());
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
     * Get user display name for DMs
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
     * Send a message to a Slack channel or DM
     */
    @Transactional(readOnly = true)
    public void sendMessage(UUID integrationId, SendSlackMessageRequest request, User user) {
        SlackIntegration integration = slackIntegrationRepository
                .findByIdAndUserId(integrationId, user.getId())
                .orElseThrow(() -> new RuntimeException("Slack integration not found"));

        if (!integration.getIsActive()) {
            throw new RuntimeException("Slack integration is not active");
        }

        try {
            // Determine if this is a DM based on channel ID prefix
            // D = Direct Message, G = Group DM (some older format), C = Channel
            boolean isDM = request.getChannelId().startsWith("D") || request.getChannelId().startsWith("G");

            // Use user token for DMs, bot token for channels
            String token = isDM && integration.getUserAccessToken() != null
                    ? integration.getUserAccessToken()
                    : integration.getBotAccessToken();

            log.info("Sending message to {} (isDM={}) using {} token",
                    request.getChannelId(), isDM, isDM ? "user" : "bot");

            var response = slack.methods(token)
                    .chatPostMessage(req -> req
                            .channel(request.getChannelId())
                            .text(request.getText())
                    );

            if (!response.isOk()) {
                throw new RuntimeException("Failed to send message: " + response.getError());
            }

            log.info("Message sent to Slack channel {} via integration {}",
                    request.getChannelId(), integrationId);

        } catch (IOException | SlackApiException e) {
            log.error("Error sending Slack message", e);
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Get message history for a channel or DM
     */
    @Transactional(readOnly = true)
    public List<SlackMessageResponse> getMessageHistory(UUID integrationId, String channelId, User user) {
        SlackIntegration integration = slackIntegrationRepository
                .findByIdAndUserId(integrationId, user.getId())
                .orElseThrow(() -> new RuntimeException("Slack integration not found"));

        if (!integration.getIsActive()) {
            throw new RuntimeException("Slack integration is not active");
        }

        try {
            // Determine if this is a DM
            boolean isDM = channelId.startsWith("D") || channelId.startsWith("G");

            // Use appropriate token
            String token = isDM && integration.getUserAccessToken() != null
                    ? integration.getUserAccessToken()
                    : integration.getBotAccessToken();

            var response = slack.methods(token).conversationsHistory(req -> req
                    .channel(channelId)
                    .limit(50));

            if (!response.isOk()) {
                throw new RuntimeException("Failed to fetch message history: " + response.getError());
            }

            log.info("Fetched {} messages from channel {}", response.getMessages().size(), channelId);

            return response.getMessages().stream()
                    .map(message -> {
                        String username = null;
                        if (message.getUser() != null) {
                            username = getUserDisplayName(token, message.getUser());
                        } else if (message.getBotId() != null) {
                            username = message.getUsername() != null ? message.getUsername() : "Bot";
                        }

                        return SlackMessageResponse.builder()
                                .text(message.getText())
                                .user(message.getUser())
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
