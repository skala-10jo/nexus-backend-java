package com.nexus.backend.dto.response;

import com.nexus.backend.entity.SlackIntegration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlackIntegrationResponse {

    private UUID id;
    private String workspaceId;
    private String workspaceName;
    private String botUserId;
    private String scope;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SlackIntegrationResponse from(SlackIntegration integration) {
        return SlackIntegrationResponse.builder()
                .id(integration.getId())
                .workspaceId(integration.getWorkspaceId())
                .workspaceName(integration.getWorkspaceName())
                .botUserId(integration.getBotUserId())
                .scope(integration.getScope())
                .isActive(integration.getIsActive())
                .createdAt(integration.getCreatedAt())
                .updatedAt(integration.getUpdatedAt())
                .build();
    }
}
