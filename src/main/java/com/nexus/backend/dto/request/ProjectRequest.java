package com.nexus.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    @NotBlank(message = "Source language is required")
    private String sourceLanguage;

    @NotBlank(message = "Target language is required")
    private String targetLanguage;
}
