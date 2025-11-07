package com.nexus.backend.dto.response;

import com.nexus.backend.entity.Schedule;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
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

    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .allDay(schedule.getAllDay())
                .color(schedule.getColor())
                .location(schedule.getLocation())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
