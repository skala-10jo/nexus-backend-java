package com.nexus.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    private String status;

    private List<UUID> documentIds;
}
