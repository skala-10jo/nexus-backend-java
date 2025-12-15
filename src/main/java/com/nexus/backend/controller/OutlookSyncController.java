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
            Map<String, Integer> syncResult = calendarSyncService.syncUserCalendar(user.getId());
            int totalOutlookSchedules = calendarSyncService.getOutlookScheduleCount(user.getId());

            int syncedCount = syncResult.getOrDefault("syncedCount", 0);
            int updatedCount = syncResult.getOrDefault("updatedCount", 0);
            int deletedCount = syncResult.getOrDefault("deletedCount", 0);

            Map<String, Object> result = Map.of(
                    "syncedCount", syncedCount,
                    "updatedCount", updatedCount,
                    "deletedCount", deletedCount,
                    "totalOutlookSchedules", totalOutlookSchedules
            );

            // 동적 메시지 생성
            StringBuilder message = new StringBuilder("일정 동기화 완료");
            if (syncedCount > 0 || updatedCount > 0 || deletedCount > 0) {
                message.append(" (");
                boolean needComma = false;
                if (syncedCount > 0) {
                    message.append(syncedCount).append("개 추가");
                    needComma = true;
                }
                if (updatedCount > 0) {
                    if (needComma) message.append(", ");
                    message.append(updatedCount).append("개 수정");
                    needComma = true;
                }
                if (deletedCount > 0) {
                    if (needComma) message.append(", ");
                    message.append(deletedCount).append("개 삭제");
                }
                message.append(")");
            }

            return ResponseEntity.ok(ApiResponse.success(
                    message.toString(),
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
