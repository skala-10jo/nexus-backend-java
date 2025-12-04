package com.nexus.backend.service;

import com.nexus.backend.dto.response.AttendanceResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.entity.UserAttendance;
import com.nexus.backend.repository.UserAttendanceRepository;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final UserAttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    /**
     * 오늘 출석 체크
     */
    @Transactional
    public AttendanceResponse checkIn(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate today = LocalDate.now();

        // 이미 출석했는지 확인
        if (attendanceRepository.existsByUserIdAndAttendanceDate(userId, today)) {
            throw new RuntimeException("Already checked in today");
        }

        UserAttendance attendance = UserAttendance.builder()
                .user(user)
                .attendanceDate(today)
                .build();

        attendance = attendanceRepository.save(attendance);
        return AttendanceResponse.from(attendance);
    }

    /**
     * 오늘 출석 여부 확인
     */
    public boolean isTodayCheckedIn(UUID userId) {
        return attendanceRepository.existsByUserIdAndAttendanceDate(userId, LocalDate.now());
    }

    /**
     * 사용자의 전체 출석 기록 조회
     */
    public List<AttendanceResponse> getAllAttendances(UUID userId) {
        return attendanceRepository.findByUserIdOrderByAttendanceDateDesc(userId)
                .stream()
                .map(AttendanceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 기간 출석 기록 조회
     */
    public List<AttendanceResponse> getAttendancesByDateRange(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return attendanceRepository.findByUserIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        userId, startDate, endDate)
                .stream()
                .map(AttendanceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 총 출석 일수
     */
    public long getTotalAttendanceCount(UUID userId) {
        return attendanceRepository.countByUserId(userId);
    }
}
