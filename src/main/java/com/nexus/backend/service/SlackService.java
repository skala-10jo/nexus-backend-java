package com.nexus.backend.service;

import com.nexus.backend.dto.request.SendSlackMessageRequest;
import com.nexus.backend.dto.response.SlackChannelResponse;
import com.nexus.backend.dto.response.SlackIntegrationResponse;
import com.nexus.backend.entity.SlackIntegration;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.SlackIntegrationRepository;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.slack.api.model.Conversation;
import com.slack.api.model.ConversationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
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
        String scopes = "channels:read,chat:write,users:read,team:read,im:read,mpim:read";
        return String.format(
                "https://slack.com/oauth/v2/authorize?client_id=%s&scope=%s&redirect_uri=%s&state=%s",
                clientId, scopes, redirectUri, state
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
     * Get list of channels, DMs, and group DMs for a workspace
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

            // Fetch all conversation types: channels, private channels, DMs, and group DMs
            ConversationsListRequest channelsRequest = ConversationsListRequest.builder()
                    .token(integration.getBotAccessToken())
                    .excludeArchived(true)
                    .types(Arrays.asList(
                            ConversationType.PUBLIC_CHANNEL,
                            ConversationType.PRIVATE_CHANNEL,
                            ConversationType.IM,
                            ConversationType.MPIM
                    ))
                    .build();

            ConversationsListResponse channelsResponse = slack.methods().conversationsList(channelsRequest);

            if (!channelsResponse.isOk()) {
                throw new RuntimeException("Failed to fetch channels: " + channelsResponse.getError());
            }

            // Log what we got from conversationsList
            log.info("ConversationsList returned {} total conversations", channelsResponse.getChannels().size());

            // Process all conversations including channels and DMs
            channelsResponse.getChannels().stream()
                    .map(channel -> {
                        String displayName = channel.getName();
                        boolean isDM = false;

                        // Log details about each conversation
                        log.info("Conversation: id={}, name={}, isIm={}, isMpim={}, isPrivate={}, user={}",
                                channel.getId(), channel.getName(), channel.isIm(), channel.isMpim(),
                                channel.isPrivate(), channel.getUser());

                        // Check if it's a DM (im or mpim type)
                        if (channel.isIm()) {
                            isDM = true;
                            // For DMs, get user name
                            if (channel.getUser() != null) {
                                displayName = getUserDisplayName(integration.getBotAccessToken(), channel.getUser());
                                log.info("Found DM with user {}, displaying as: {}", channel.getUser(), displayName);
                            } else {
                                displayName = "Direct Message";
                            }
                        } else if (channel.isMpim()) {
                            isDM = true;
                            displayName = "Group DM";
                            log.info("Found Group DM: {}", channel.getId());
                        }

                        return SlackChannelResponse.builder()
                                .id(channel.getId())
                                .name(displayName)
                                .isPrivate(channel.isPrivate() || isDM)
                                .isMember(channel.isMember())
                                .build();
                    })
                    .forEach(allChannels::add);

            log.info("Fetched {} total conversations (channels + DMs)", allChannels.size());
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
     * Send a message to a Slack channel
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
            var response = slack.methods(integration.getBotAccessToken())
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
}
