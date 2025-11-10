package com.nexus.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {

    @NotEmpty(message = "수신자는 필수입니다")
    private List<String> toRecipients;

    @NotBlank(message = "제목은 필수입니다")
    private String subject;

    @NotBlank(message = "본문은 필수입니다")
    private String body;

    @Builder.Default
    private String bodyType = "HTML"; // HTML or Text

    @Builder.Default
    private List<String> ccRecipients = new ArrayList<>();

    @Builder.Default
    private List<String> bccRecipients = new ArrayList<>();

    @Builder.Default
    private List<String> attachmentPaths = new ArrayList<>();

    private UUID projectId; // Optional: link email to project
}
