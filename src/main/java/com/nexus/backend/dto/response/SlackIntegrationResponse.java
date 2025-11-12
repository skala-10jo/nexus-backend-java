package com.nexus.backend.dto.response;

import com.nexus.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlackIntegrationResponse {

    private String workspaceId;
    private String workspaceName;
    private String botUserId;
    private String scope;
    private Boolean isActive;
    private LocalDateTime connectedAt;

    public static SlackIntegrationResponse from(User user) {
        return SlackIntegrationResponse.builder()
                .workspaceId(user.getSlackWorkspaceId())
                .workspaceName(user.getSlackWorkspaceName())
                .botUserId(user.getSlackBotUserId())
                .scope(user.getSlackScope())
                .isActive(user.getSlackIsActive())
                .connectedAt(user.getSlackConnectedAt())
                .build();
    }
}
