package com.nexus.backend.repository;

import com.nexus.backend.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    // Outlook Calendar 동기화용 메서드
    Optional<Schedule> findByOutlookEventIdAndUserId(String outlookEventId, UUID userId);

    boolean existsByOutlookEventIdAndUserId(String outlookEventId, UUID userId);

    @Query("SELECT s.outlookEventId FROM Schedule s WHERE s.user.id = :userId AND s.isFromOutlook = true")
    List<String> findOutlookEventIdsByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.outlookEventId IN :outlookEventIds AND s.user.id = :userId")
    void deleteByOutlookEventIdsAndUserId(@Param("outlookEventIds") List<String> outlookEventIds, @Param("userId") UUID userId);

    List<Schedule> findByUserIdAndIsFromOutlookTrue(UUID userId);
}
