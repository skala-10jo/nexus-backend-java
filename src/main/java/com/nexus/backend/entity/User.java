package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 30)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "role", length = 100)
    private String role;

    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "ko";  // Default: Korean (ko, en, ja, vi, zh)

    // Outlook integration fields
    @Column(name = "outlook_email")
    private String outlookEmail;

    @Column(name = "outlook_access_token", columnDefinition = "TEXT")
    private String outlookAccessToken;

    @Column(name = "outlook_refresh_token", columnDefinition = "TEXT")
    private String outlookRefreshToken;

    @Column(name = "outlook_token_expires_at")
    private LocalDateTime outlookTokenExpiresAt;

    @Column(name = "outlook_delta_link", columnDefinition = "TEXT")
    private String outlookDeltaLink; // For incremental sync

    // Slack integration fields (same pattern as Outlook)
    @Column(name = "slack_workspace_id")
    private String slackWorkspaceId;

    @Column(name = "slack_workspace_name")
    private String slackWorkspaceName;

    @Column(name = "slack_access_token", columnDefinition = "TEXT")
    private String slackAccessToken;

    @Column(name = "slack_bot_user_id")
    private String slackBotUserId;

    @Column(name = "slack_bot_access_token", columnDefinition = "TEXT")
    private String slackBotAccessToken;

    @Column(name = "slack_user_access_token", columnDefinition = "TEXT")
    private String slackUserAccessToken;

    @Column(name = "slack_scope", columnDefinition = "TEXT")
    private String slackScope;

    @Column(name = "slack_is_active")
    private Boolean slackIsActive = false;

    @Column(name = "slack_token_expires_at")
    private LocalDateTime slackTokenExpiresAt;

    @Column(name = "slack_connected_at")
    private LocalDateTime slackConnectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
