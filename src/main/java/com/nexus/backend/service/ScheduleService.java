package com.nexus.backend.service;

import com.nexus.backend.dto.request.ScheduleRequest;
import com.nexus.backend.dto.response.ScheduleResponse;
import com.nexus.backend.entity.Schedule;
import com.nexus.backend.entity.ScheduleCategory;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.ScheduleCategoryRepository;
import com.nexus.backend.repository.ScheduleRepository;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ScheduleCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAllSchedulesByUser(UUID userId) {
        return scheduleRepository.findByUserIdOrderByStartTimeAsc(userId)
                .stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedulesByDateRange(
            UUID userId,
            Instant startDate,
            Instant endDate
    ) {
        return scheduleRepository.findByUserIdAndStartTimeBetweenOrderByStartTimeAsc(
                        userId, startDate, endDate)
                .stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getUpcomingSchedules(UUID userId) {
        return scheduleRepository.findByUserIdAndEndTimeAfterOrderByStartTimeAsc(
                        userId, Instant.now())
                .stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getScheduleById(UUID scheduleId, UUID userId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        // 해당 일정이 현재 사용자의 것인지 확인
        if (!schedule.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to schedule");
        }

        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public ScheduleResponse createSchedule(UUID userId, ScheduleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch categories if category IDs provided
        List<ScheduleCategory> categories = new ArrayList<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (UUID categoryId : request.getCategoryIds()) {
                ScheduleCategory category = categoryRepository.findByIdAndUserId(categoryId, userId)
                        .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
                categories.add(category);
            }
        }

        Schedule schedule = Schedule.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .allDay(request.getAllDay() != null ? request.getAllDay() : false)
                .color(request.getColor())
                .location(request.getLocation())
                .categories(categories)
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        return ScheduleResponse.from(savedSchedule);
    }

    @Transactional
    public ScheduleResponse updateSchedule(UUID scheduleId, UUID userId, ScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        // 해당 일정이 현재 사용자의 것인지 확인
        if (!schedule.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to schedule");
        }

        // Update categories if provided
        if (request.getCategoryIds() != null) {
            List<ScheduleCategory> categories = new ArrayList<>();
            for (UUID categoryId : request.getCategoryIds()) {
                ScheduleCategory category = categoryRepository.findByIdAndUserId(categoryId, userId)
                        .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
                categories.add(category);
            }
            schedule.setCategories(categories);
        }

        schedule.setTitle(request.getTitle());
        schedule.setDescription(request.getDescription());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setAllDay(request.getAllDay() != null ? request.getAllDay() : false);
        schedule.setColor(request.getColor());
        schedule.setLocation(request.getLocation());

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        return ScheduleResponse.from(updatedSchedule);
    }

    @Transactional
    public void deleteSchedule(UUID scheduleId, UUID userId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        // 해당 일정이 현재 사용자의 것인지 확인
        if (!schedule.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to schedule");
        }

        scheduleRepository.delete(schedule);
    }
}
