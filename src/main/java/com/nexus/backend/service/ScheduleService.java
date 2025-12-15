package com.nexus.backend.service;

import com.nexus.backend.dto.request.ScheduleRequest;
import com.nexus.backend.dto.response.ScheduleResponse;
import com.nexus.backend.entity.Project;
import com.nexus.backend.entity.Schedule;
import com.nexus.backend.entity.ScheduleCategory;
import com.nexus.backend.entity.User;
import com.nexus.backend.exception.ResourceNotFoundException;
import com.nexus.backend.exception.UnauthorizedException;
import com.nexus.backend.repository.ProjectRepository;
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
    private final ProjectRepository projectRepository;

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
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        // 해당 일정이 현재 사용자의 것인지 확인
        if (!schedule.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Unauthorized access to schedule");
        }

        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public ScheduleResponse createSchedule(UUID userId, ScheduleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Fetch project if project ID provided
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));

            // Verify project belongs to user
            if (!project.getUser().getId().equals(userId)) {
                throw new UnauthorizedException("Unauthorized access to project");
            }
        }

        // Fetch categories if category IDs provided
        List<ScheduleCategory> categories = new ArrayList<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (UUID categoryId : request.getCategoryIds()) {
                ScheduleCategory category = categoryRepository.findByIdAndUserId(categoryId, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("ScheduleCategory", "id", categoryId));
                categories.add(category);
            }
        }

        // Auto-link project by category name if project is not explicitly provided
        // This ensures schedule is linked to project when category with same name exists
        if (project == null && !categories.isEmpty()) {
            String categoryName = categories.get(0).getName();
            project = projectRepository.findByUserIdAndName(userId, categoryName).orElse(null);
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
                .project(project)
                .categories(categories)
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        return ScheduleResponse.from(savedSchedule);
    }

    @Transactional
    public ScheduleResponse updateSchedule(UUID scheduleId, UUID userId, ScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        // 해당 일정이 현재 사용자의 것인지 확인
        if (!schedule.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Unauthorized access to schedule");
        }

        // Update project if provided
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));

            // Verify project belongs to user
            if (!project.getUser().getId().equals(userId)) {
                throw new UnauthorizedException("Unauthorized access to project");
            }
        }

        // Update categories if provided
        List<ScheduleCategory> categories = new ArrayList<>();
        if (request.getCategoryIds() != null) {
            for (UUID categoryId : request.getCategoryIds()) {
                ScheduleCategory category = categoryRepository.findByIdAndUserId(categoryId, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("ScheduleCategory", "id", categoryId));
                categories.add(category);
            }
            schedule.setCategories(categories);
        }

        // Auto-link project by category name if project is not explicitly provided
        // This ensures schedule is linked to project when category with same name exists
        if (project == null && !categories.isEmpty()) {
            String categoryName = categories.get(0).getName();
            project = projectRepository.findByUserIdAndName(userId, categoryName).orElse(null);
        }
        schedule.setProject(project);

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
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        // 해당 일정이 현재 사용자의 것인지 확인
        if (!schedule.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Unauthorized access to schedule");
        }

        scheduleRepository.delete(schedule);
    }
}
