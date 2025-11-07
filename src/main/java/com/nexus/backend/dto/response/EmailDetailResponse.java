package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDetailResponse {

    private UUID id;
    private String messageId;
    private String subject;
    private String fromAddress;
    private String fromName;
    private String toRecipients;
    private String ccRecipients;
    private String bccRecipients;
    private String body;
    private String bodyPreview;
    private String bodyType;
    private Boolean hasAttachments;
    private Boolean isRead;
    private String conversationId;
    private String folder;
    private LocalDateTime receivedDateTime;
    private LocalDateTime sentDateTime;
    private UUID projectId;
    private String projectName;
    private List<EmailAttachmentResponse> attachments;
    private LocalDateTime syncedAt;
}
