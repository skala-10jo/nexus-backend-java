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
public class ScheduleCategoryResponse {

    private UUID id;
    private String name;
    private String color;
    private String icon;
    private String description;
    private Boolean isDefault;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Outlook 범주 연동 정보
    private Boolean isFromOutlook;

    // Statistics (optional)
    private Long scheduleCount;
}
