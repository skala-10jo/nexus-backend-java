package com.nexus.backend.dto.response;

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
public class EmailResponse {

    private UUID id;
    private String messageId;
    private String subject;
    private String fromAddress;
    private String fromName;
    private String bodyPreview;
    private Boolean hasAttachments;
    private Boolean isRead;
    private String folder;
    private LocalDateTime receivedDateTime;
    private LocalDateTime sentDateTime;
    private UUID projectId;
    private String projectName;
    private LocalDateTime syncedAt;
}
