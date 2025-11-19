package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlackMessageResponse {

    private String text;
    private String user;
    private String username;
    private String timestamp;
    private String botId;
    private String channelId;
    private String userId;
    private String type;  // "text" or "file_notification"
}
