package com.nexus.backend.repository;

import com.nexus.backend.entity.UserAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAttendanceRepository extends JpaRepository<UserAttendance, UUID> {

    List<UserAttendance> findByUserIdOrderByAttendanceDateDesc(UUID userId);

    List<UserAttendance> findByUserIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
        UUID userId,
        LocalDate startDate,
        LocalDate endDate
    );

    Optional<UserAttendance> findByUserIdAndAttendanceDate(UUID userId, LocalDate attendanceDate);

    boolean existsByUserIdAndAttendanceDate(UUID userId, LocalDate attendanceDate);

    long countByUserId(UUID userId);
}
