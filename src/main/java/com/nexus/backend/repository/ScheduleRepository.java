package com.nexus.backend.repository;

import com.nexus.backend.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    List<Schedule> findByUserIdOrderByStartTimeAsc(UUID userId);

    List<Schedule> findByUserIdAndStartTimeBetweenOrderByStartTimeAsc(
        UUID userId,
        Instant startTime,
        Instant endTime
    );

    List<Schedule> findByUserIdAndEndTimeAfterOrderByStartTimeAsc(
        UUID userId,
        Instant now
    );

    List<Schedule> findByProjectIdOrderByStartTimeAsc(UUID projectId);
}
