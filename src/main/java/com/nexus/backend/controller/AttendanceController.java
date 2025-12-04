package com.nexus.backend.controller;

import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.AttendanceResponse;
import com.nexus.backend.security.JwtTokenProvider;
import com.nexus.backend.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final JwtTokenProvider jwtTokenProvider;

    private UUID getUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token.substring(7));
    }

    /**
     * 출석 체크
     */
    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(
            @RequestHeader("Authorization") String token
    ) {
        UUID userId = getUserIdFromToken(token);
        AttendanceResponse attendance = attendanceService.checkIn(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<AttendanceResponse>builder()
                        .success(true)
                        .message("Check-in successful")
                        .data(attendance)
                        .build()
        );
    }

    /**
     * 오늘 출석 여부 확인
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> isTodayCheckedIn(
            @RequestHeader("Authorization") String token
    ) {
        UUID userId = getUserIdFromToken(token);
        boolean checkedIn = attendanceService.isTodayCheckedIn(userId);
        return ResponseEntity.ok(
                ApiResponse.<Map<String, Boolean>>builder()
                        .success(true)
                        .message("Today's attendance status retrieved")
                        .data(Map.of("checkedIn", checkedIn))
                        .build()
        );
    }

    /**
     * 전체 출석 기록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAllAttendances(
            @RequestHeader("Authorization") String token
    ) {
        UUID userId = getUserIdFromToken(token);
        List<AttendanceResponse> attendances = attendanceService.getAllAttendances(userId);
        return ResponseEntity.ok(
                ApiResponse.<List<AttendanceResponse>>builder()
                        .success(true)
                        .message("Attendance records retrieved successfully")
                        .data(attendances)
                        .build()
        );
    }

    /**
     * 특정 기간 출석 기록 조회
     */
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendancesByDateRange(
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        UUID userId = getUserIdFromToken(token);
        List<AttendanceResponse> attendances = attendanceService.getAttendancesByDateRange(
                userId, startDate, endDate);
        return ResponseEntity.ok(
                ApiResponse.<List<AttendanceResponse>>builder()
                        .success(true)
                        .message("Attendance records retrieved successfully")
                        .data(attendances)
                        .build()
        );
    }

    /**
     * 총 출석 일수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getTotalAttendanceCount(
            @RequestHeader("Authorization") String token
    ) {
        UUID userId = getUserIdFromToken(token);
        long count = attendanceService.getTotalAttendanceCount(userId);
        return ResponseEntity.ok(
                ApiResponse.<Map<String, Long>>builder()
                        .success(true)
                        .message("Total attendance count retrieved")
                        .data(Map.of("totalCount", count))
                        .build()
        );
    }
}
