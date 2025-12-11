package com.nexus.backend.controller;

import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.CalendarSyncService;
import com.nexus.backend.service.EmailSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/outlook")
@RequiredArgsConstructor
public class OutlookSyncController {

    private final EmailSyncService emailSyncService;
    private final CalendarSyncService calendarSyncService;

    /**
     * Outlook 메일 동기화
     * POST /api/outlook/sync
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> syncEmails(
            @AuthenticationPrincipal User user
    ) {
        try {
            int syncedCount = emailSyncService.syncUserEmails(user.getId());
            return ResponseEntity.ok(ApiResponse.success(
                    syncedCount + "개의 메일이 동기화되었습니다",
                    "동기화 완료"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("메일 동기화 실패: " + e.getMessage()));
        }
    }

    /**
     * Outlook 캘린더 동기화
     * POST /api/outlook/calendar/sync
     */
    @PostMapping("/calendar/sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncCalendar(
            @AuthenticationPrincipal User user
    ) {
        try {
            int syncedCount = calendarSyncService.syncUserCalendar(user.getId());
            int totalOutlookSchedules = calendarSyncService.getOutlookScheduleCount(user.getId());

            Map<String, Object> result = Map.of(
                    "syncedCount", syncedCount,
                    "totalOutlookSchedules", totalOutlookSchedules
            );

            return ResponseEntity.ok(ApiResponse.success(
                    syncedCount + "개의 일정이 동기화되었습니다",
                    result
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("캘린더 동기화 실패: " + e.getMessage()));
        }
    }

    /**
     * Outlook 일정 동기화 상태 조회
     * GET /api/outlook/calendar/status
     */
    @GetMapping("/calendar/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCalendarSyncStatus(
            @AuthenticationPrincipal User user
    ) {
        try {
            int totalOutlookSchedules = calendarSyncService.getOutlookScheduleCount(user.getId());

            Map<String, Object> result = Map.of(
                    "totalOutlookSchedules", totalOutlookSchedules,
                    "isConnected", user.getOutlookAccessToken() != null
            );

            return ResponseEntity.ok(ApiResponse.success("조회 완료", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("상태 조회 실패: " + e.getMessage()));
        }
    }
}
