package com.nexus.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlackOAuthCallbackRequest {

    @NotBlank(message = "Authorization code is required")
    private String code;

    private String state;
}
