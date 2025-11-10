package com.nexus.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendSlackMessageRequest {

    @NotBlank(message = "Channel ID is required")
    private String channelId;

    @NotBlank(message = "Message text is required")
    private String text;
}
