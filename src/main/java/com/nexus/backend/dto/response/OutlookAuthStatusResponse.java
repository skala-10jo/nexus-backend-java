package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutlookAuthStatusResponse {

    private Boolean isConnected;
    private String outlookEmail;
    private LocalDateTime tokenExpiresAt;
    private String message;
}
