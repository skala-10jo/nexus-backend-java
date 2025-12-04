package com.nexus.backend.dto.response;

import com.nexus.backend.entity.UserAttendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    private UUID id;
    private LocalDate attendanceDate;

    public static AttendanceResponse from(UserAttendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .attendanceDate(attendance.getAttendanceDate())
                .build();
    }
}
