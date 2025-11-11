package com.nexus.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ScheduleRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    private Instant endTime;

    private Boolean allDay = false;

    private String color;

    private String location;

    // Project ID (many-to-one)
    private UUID projectId;

    // Multiple category IDs (many-to-many)
    private List<UUID> categoryIds = new ArrayList<>();
}
