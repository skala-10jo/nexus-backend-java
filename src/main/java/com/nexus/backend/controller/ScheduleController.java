package com.nexus.backend.controller;

import com.nexus.backend.dto.request.ScheduleRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.ScheduleResponse;
import com.nexus.backend.security.JwtTokenProvider;
import com.nexus.backend.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final JwtTokenProvider jwtTokenProvider;

    private UUID getUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token.substring(7));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getAllSchedules(
            @RequestHeader("Authorization") String token
    ) {
        UUID userId = getUserIdFromToken(token);
        List<ScheduleResponse> schedules = scheduleService.getAllSchedulesByUser(userId);
        return ResponseEntity.ok(
                ApiResponse.<List<ScheduleResponse>>builder()
                        .success(true)
                        .message("Schedules retrieved successfully")
                        .data(schedules)
                        .build()
        );
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedulesByDateRange(
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        UUID userId = getUserIdFromToken(token);
        List<ScheduleResponse> schedules = scheduleService.getSchedulesByDateRange(userId, start, end);
        return ResponseEntity.ok(
                ApiResponse.<List<ScheduleResponse>>builder()
                        .success(true)
                        .message("Schedules retrieved successfully")
                        .data(schedules)
                        .build()
        );
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getUpcomingSchedules(
            @RequestHeader("Authorization") String token
    ) {
        UUID userId = getUserIdFromToken(token);
        List<ScheduleResponse> schedules = scheduleService.getUpcomingSchedules(userId);
        return ResponseEntity.ok(
                ApiResponse.<List<ScheduleResponse>>builder()
                        .success(true)
                        .message("Upcoming schedules retrieved successfully")
                        .data(schedules)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getScheduleById(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id
    ) {
        UUID userId = getUserIdFromToken(token);
        ScheduleResponse schedule = scheduleService.getScheduleById(id, userId);
        return ResponseEntity.ok(
                ApiResponse.<ScheduleResponse>builder()
                        .success(true)
                        .message("Schedule retrieved successfully")
                        .data(schedule)
                        .build()
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ScheduleRequest request
    ) {
        UUID userId = getUserIdFromToken(token);
        ScheduleResponse schedule = scheduleService.createSchedule(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ScheduleResponse>builder()
                        .success(true)
                        .message("Schedule created successfully")
                        .data(schedule)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleRequest request
    ) {
        UUID userId = getUserIdFromToken(token);
        ScheduleResponse schedule = scheduleService.updateSchedule(id, userId, request);
        return ResponseEntity.ok(
                ApiResponse.<ScheduleResponse>builder()
                        .success(true)
                        .message("Schedule updated successfully")
                        .data(schedule)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id
    ) {
        UUID userId = getUserIdFromToken(token);
        scheduleService.deleteSchedule(id, userId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Schedule deleted successfully")
                        .build()
        );
    }
}
