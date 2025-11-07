package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCodeResponse {

    private String userCode;
    private String deviceCode;
    private String verificationUri;
    private String message;
    private Integer expiresIn;
    private Integer interval;
}
