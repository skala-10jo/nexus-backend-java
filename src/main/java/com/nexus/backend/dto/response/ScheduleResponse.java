package com.nexus.backend.dto.response;

import com.nexus.backend.entity.Schedule;
import com.nexus.backend.entity.ScheduleCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    private UUID id;
    private String title;
    private String description;
    private Instant startTime;
    private Instant endTime;
    private Boolean allDay;
    private String color;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Outlook Calendar 연동 정보
    private Boolean isFromOutlook;
    private String attendees;
    private String organizer;

    // Project information
    private ProjectInfo project;

    // Multiple categories
    @Builder.Default
    private List<CategoryInfo> categories = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectInfo {
        private UUID id;
        private String name;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        private UUID id;
        private String name;
        private String color;
        private String icon;
        private Boolean isFromOutlook;
    }

    public static ScheduleResponse from(Schedule schedule) {
        List<CategoryInfo> categoryInfos = schedule.getCategories().stream()
                .map(cat -> CategoryInfo.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .color(cat.getColor())
                        .icon(cat.getIcon())
                        .isFromOutlook(cat.getIsFromOutlook())
                        .build())
                .collect(Collectors.toList());

        ProjectInfo projectInfo = null;
        if (schedule.getProject() != null) {
            projectInfo = ProjectInfo.builder()
                    .id(schedule.getProject().getId())
                    .name(schedule.getProject().getName())
                    .description(schedule.getProject().getDescription())
                    .build();
        }

        return ScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .allDay(schedule.getAllDay())
                .color(schedule.getColor())
                .location(schedule.getLocation())
                .isFromOutlook(schedule.getIsFromOutlook())
                .attendees(schedule.getAttendees())
                .organizer(schedule.getOrganizer())
                .project(projectInfo)
                .categories(categoryInfos)
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
