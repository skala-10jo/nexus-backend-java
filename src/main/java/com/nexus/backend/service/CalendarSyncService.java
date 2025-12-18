package com.nexus.backend.service;

import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.EventCollectionResponse;
import com.microsoft.graph.models.OutlookCategory;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.nexus.backend.entity.Project;
import com.nexus.backend.entity.Schedule;
import com.nexus.backend.entity.ScheduleCategory;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.ProjectRepository;
import com.nexus.backend.repository.ScheduleCategoryRepository;
import com.nexus.backend.repository.ScheduleRepository;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.backend.exception.BadRequestException;
import com.nexus.backend.exception.ResourceNotFoundException;
import com.nexus.backend.exception.ServiceException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CalendarSyncService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleCategoryRepository scheduleCategoryRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final OutlookAuthService outlookAuthService;
    private final ProjectCategorySyncService projectCategorySyncService;

    // Outlook 범주 색상 매핑 (preset 색상)
    private static final Map<String, String> OUTLOOK_COLOR_MAP = Map.ofEntries(
            Map.entry("preset0", "#E74C3C"),   // Red
            Map.entry("preset1", "#E67E22"),   // Orange
            Map.entry("preset2", "#F39C12"),   // Peach (Brown/Gold)
            Map.entry("preset3", "#F1C40F"),   // Yellow
            Map.entry("preset4", "#27AE60"),   // Green
            Map.entry("preset5", "#16A085"),   // Teal
            Map.entry("preset6", "#3498DB"),   // Olive (using Blue)
            Map.entry("preset7", "#2980B9"),   // Blue
            Map.entry("preset8", "#9B59B6"),   // Purple
            Map.entry("preset9", "#E91E63"),   // Cranberry (Pink)
            Map.entry("preset10", "#607D8B"),  // Steel (Gray-Blue)
            Map.entry("preset11", "#795548"),  // DarkSteel (Brown)
            Map.entry("preset12", "#9E9E9E"),  // Gray
            Map.entry("preset13", "#455A64"),  // DarkGray
            Map.entry("preset14", "#000000"),  // Black
            Map.entry("preset15", "#C0392B"),  // DarkRed
            Map.entry("preset16", "#D35400"),  // DarkOrange
            Map.entry("preset17", "#8E44AD"),  // DarkBrown (using Purple)
            Map.entry("preset18", "#B8860B"),  // DarkYellow
            Map.entry("preset19", "#1E8449"),  // DarkGreen
            Map.entry("preset20", "#117A65"),  // DarkTeal
            Map.entry("preset21", "#2E86AB"),  // DarkOlive
            Map.entry("preset22", "#1A5276"),  // DarkBlue
            Map.entry("preset23", "#6C3483"),  // DarkPurple
            Map.entry("preset24", "#AD1457")   // DarkCranberry
    );

    /**
     * 사용자의 Outlook 일정 동기화
     * 단방향: Outlook → 로컬 DB
     * @return Map with syncedCount, updatedCount, deletedCount
     */
    @Transactional
    public Map<String, Integer> syncUserCalendar(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getOutlookAccessToken() == null) {
            throw new BadRequestException("Outlook 계정이 연동되지 않았습니다");
        }

        try {
            log.info("Syncing calendar for user: {}", userId);

            GraphServiceClient graphClient = outlookAuthService.createGraphClient(user);

            // 1. 먼저 범주 동기화
            syncCategories(graphClient, user);

            // 2. 일정 동기화
            Map<String, Integer> result = syncEvents(graphClient, user);

            log.info("Calendar sync completed for user: {}, created={}, updated={}, deleted={}",
                    userId, result.get("syncedCount"), result.get("updatedCount"), result.get("deletedCount"));
            return result;

        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to sync calendar for user: {}", userId, e);
            throw new ServiceException("캘린더 동기화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Outlook 범주 동기화
     */
    private void syncCategories(GraphServiceClient graphClient, User user) {
        try {
            log.info("Syncing categories for user: {}", user.getId());

            var categoriesResponse = graphClient.me()
                    .outlook()
                    .masterCategories()
                    .get();

            if (categoriesResponse == null || categoriesResponse.getValue() == null) {
                log.info("No categories found for user: {}", user.getId());
                // Outlook에 카테고리가 없으면 모든 Outlook 연동 카테고리 삭제
                detectAndDeleteRemovedCategories(user.getId(), new HashSet<>());
                return;
            }

            // Outlook 범주 ID 수집 (삭제 감지용)
            Set<String> allOutlookCategoryIds = new HashSet<>();
            for (OutlookCategory outlookCategory : categoriesResponse.getValue()) {
                allOutlookCategoryIds.add(outlookCategory.getId());
            }

            // 삭제된 범주 감지 및 제거
            detectAndDeleteRemovedCategories(user.getId(), allOutlookCategoryIds);

            int syncedCount = 0;
            for (OutlookCategory outlookCategory : categoriesResponse.getValue()) {
                // 이미 존재하는 범주인지 확인
                Optional<ScheduleCategory> existingCategory = scheduleCategoryRepository
                        .findByOutlookCategoryIdAndUserId(outlookCategory.getId(), user.getId());

                if (existingCategory.isPresent()) {
                    // 기존 범주 업데이트
                    ScheduleCategory category = existingCategory.get();
                    category.setName(outlookCategory.getDisplayName());
                    category.setColor(mapOutlookColor(outlookCategory.getColor()));
                    scheduleCategoryRepository.save(category);

                    // 기존 카테고리에 대응하는 프로젝트가 없으면 생성
                    projectCategorySyncService.onCategoryCreated(user.getId(), outlookCategory.getDisplayName());
                } else {
                    // 이름으로 기존 범주 확인 (사용자가 직접 만든 같은 이름의 범주가 있을 수 있음)
                    Optional<ScheduleCategory> existingByName = scheduleCategoryRepository
                            .findByUserIdAndName(user.getId(), outlookCategory.getDisplayName());

                    if (existingByName.isPresent()) {
                        // 이름이 같은 범주가 있으면 Outlook ID만 연결
                        ScheduleCategory category = existingByName.get();
                        category.setOutlookCategoryId(outlookCategory.getId());
                        category.setIsFromOutlook(true);
                        category.setColor(mapOutlookColor(outlookCategory.getColor()));
                        scheduleCategoryRepository.save(category);

                        // 기존 카테고리에 대응하는 프로젝트가 없으면 생성
                        projectCategorySyncService.onCategoryCreated(user.getId(), outlookCategory.getDisplayName());
                    } else {
                        // 새 범주 생성
                        ScheduleCategory newCategory = ScheduleCategory.builder()
                                .user(user)
                                .name(outlookCategory.getDisplayName())
                                .color(mapOutlookColor(outlookCategory.getColor()))
                                .outlookCategoryId(outlookCategory.getId())
                                .isFromOutlook(true)
                                .isDefault(false)
                                .displayOrder(0)
                                .build();
                        scheduleCategoryRepository.save(newCategory);
                        syncedCount++;

                        // 카테고리 생성 시 동일 이름의 프로젝트 자동 생성
                        projectCategorySyncService.onCategoryCreated(user.getId(), outlookCategory.getDisplayName());
                        log.info("Auto-created project for Outlook category: {}", outlookCategory.getDisplayName());
                    }
                }
            }

            log.info("Synced {} new categories for user: {}", syncedCount, user.getId());

        } catch (Exception e) {
            log.error("Failed to sync categories for user: {}", user.getId(), e);
            // 범주 동기화 실패해도 일정 동기화는 계속 진행
        }
    }

    /**
     * Outlook에서 삭제된 범주를 DB에서도 제거
     */
    private void detectAndDeleteRemovedCategories(UUID userId, Set<String> outlookCategoryIds) {
        try {
            List<String> dbCategoryIds = scheduleCategoryRepository.findOutlookCategoryIdsByUserId(userId);

            List<String> categoryIdsToDelete = dbCategoryIds.stream()
                    .filter(id -> id != null && !outlookCategoryIds.contains(id))
                    .collect(Collectors.toList());

            if (!categoryIdsToDelete.isEmpty()) {
                log.info("Deleting {} categories removed from Outlook for user: {}",
                        categoryIdsToDelete.size(), userId);
                scheduleCategoryRepository.deleteByOutlookCategoryIdsAndUserId(categoryIdsToDelete, userId);
            }

        } catch (Exception e) {
            log.error("Failed to detect and delete removed categories for user: {}", userId, e);
        }
    }

    /**
     * Outlook 일정 동기화
     * @return Map with syncedCount, updatedCount, deletedCount
     */
    private Map<String, Integer> syncEvents(GraphServiceClient graphClient, User user) {
        try {
            // 현재 시간 기준 과거 1개월 ~ 미래 6개월 범위로 조회
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime startDateTime = now.minusMonths(1);
            OffsetDateTime endDateTime = now.plusMonths(6);

            log.info("Fetching events from {} to {} for user: {}", startDateTime, endDateTime, user.getId());

            // 모든 Outlook 이벤트 ID 가져오기 (삭제 감지용)
            Set<String> allOutlookEventIds = new HashSet<>();

            EventCollectionResponse eventsResponse = graphClient.me()
                    .calendarView()
                    .get(requestConfig -> {
                        requestConfig.queryParameters.startDateTime = startDateTime.toString();
                        requestConfig.queryParameters.endDateTime = endDateTime.toString();
                        requestConfig.queryParameters.top = 500;
                        requestConfig.queryParameters.orderby = new String[]{"start/dateTime"};
                        requestConfig.queryParameters.select = new String[]{
                                "id", "subject", "body", "start", "end", "isAllDay",
                                "location", "categories", "organizer", "attendees",
                                "showAs", "importance"
                        };
                    });

            if (eventsResponse == null || eventsResponse.getValue() == null) {
                log.info("No events found for user: {}", user.getId());
                // 일정이 없으면 모든 Outlook 일정 삭제
                int deletedCount = detectAndDeleteRemovedEvents(user.getId(), new HashSet<>());
                return Map.of("syncedCount", 0, "updatedCount", 0, "deletedCount", deletedCount);
            }

            // Outlook 이벤트 ID 수집
            for (Event event : eventsResponse.getValue()) {
                allOutlookEventIds.add(event.getId());
            }

            log.info("Found {} events in Outlook for user: {}", allOutlookEventIds.size(), user.getId());

            // 삭제된 일정 감지 및 제거
            int deletedCount = detectAndDeleteRemovedEvents(user.getId(), allOutlookEventIds);

            // 범주 캐싱 (일정별로 매번 조회하지 않도록)
            Map<String, ScheduleCategory> categoryCache = scheduleCategoryRepository
                    .findByUserIdOrderByDisplayOrder(user.getId())
                    .stream()
                    .collect(Collectors.toMap(
                            ScheduleCategory::getName,
                            c -> c,
                            (existing, replacement) -> existing
                    ));

            // 프로젝트 캐싱 (이름으로 자동 매칭용)
            Map<String, Project> projectCache = projectRepository.findByUserId(user.getId())
                    .stream()
                    .collect(Collectors.toMap(
                            Project::getName,
                            p -> p,
                            (existing, replacement) -> existing
                    ));

            int syncedCount = 0;
            int updatedCount = 0;

            for (Event event : eventsResponse.getValue()) {
                // 기존 일정 확인
                Optional<Schedule> existingSchedule = scheduleRepository
                        .findByOutlookEventIdAndUserId(event.getId(), user.getId());

                if (existingSchedule.isPresent()) {
                    // 기존 일정 업데이트 (실제 변경이 있을 때만 카운트)
                    Schedule schedule = existingSchedule.get();
                    boolean hasChanges = updateScheduleFromEvent(schedule, event, categoryCache, projectCache);
                    if (hasChanges) {
                        scheduleRepository.save(schedule);
                        updatedCount++;
                    }
                } else {
                    // 새 일정 생성
                    Schedule newSchedule = convertEventToSchedule(event, user, categoryCache, projectCache);
                    scheduleRepository.save(newSchedule);
                    syncedCount++;
                }
            }

            log.info("Synced {} new events, updated {} events, deleted {} events for user: {}",
                    syncedCount, updatedCount, deletedCount, user.getId());

            return Map.of("syncedCount", syncedCount, "updatedCount", updatedCount, "deletedCount", deletedCount);

        } catch (Exception e) {
            log.error("Failed to sync events for user: {}", user.getId(), e);
            throw e;
        }
    }

    /**
     * Outlook에서 삭제된 일정을 DB에서도 제거
     * @return 삭제된 일정 개수
     */
    private int detectAndDeleteRemovedEvents(UUID userId, Set<String> outlookEventIds) {
        try {
            List<String> dbEventIds = scheduleRepository.findOutlookEventIdsByUserId(userId);

            List<String> eventIdsToDelete = dbEventIds.stream()
                    .filter(id -> id != null && !outlookEventIds.contains(id))
                    .collect(Collectors.toList());

            if (!eventIdsToDelete.isEmpty()) {
                log.info("Deleting {} events removed from Outlook for user: {}",
                        eventIdsToDelete.size(), userId);
                scheduleRepository.deleteByOutlookEventIdsAndUserId(eventIdsToDelete, userId);
                return eventIdsToDelete.size();
            }

            return 0;
        } catch (Exception e) {
            log.error("Failed to detect and delete removed events for user: {}", userId, e);
            return 0;
        }
    }

    /**
     * Outlook Event를 Schedule 엔티티로 변환
     */
    private Schedule convertEventToSchedule(Event event, User user,
                                            Map<String, ScheduleCategory> categoryCache,
                                            Map<String, Project> projectCache) {
        Schedule schedule = Schedule.builder()
                .user(user)
                .outlookEventId(event.getId())
                .isFromOutlook(true)
                .title(event.getSubject() != null ? event.getSubject() : "(제목 없음)")
                .allDay(event.getIsAllDay() != null ? event.getIsAllDay() : false)
                .build();

        updateScheduleFromEvent(schedule, event, categoryCache, projectCache);

        return schedule;
    }

    /**
     * Event 정보로 Schedule 업데이트
     * @return 실제 변경이 있었으면 true, 없으면 false
     */
    private boolean updateScheduleFromEvent(Schedule schedule, Event event,
                                         Map<String, ScheduleCategory> categoryCache,
                                         Map<String, Project> projectCache) {
        boolean hasChanges = false;

        // 제목 비교 및 업데이트
        String newTitle = event.getSubject() != null ? event.getSubject() : "(제목 없음)";
        if (!Objects.equals(schedule.getTitle(), newTitle)) {
            schedule.setTitle(newTitle);
            hasChanges = true;
        }

        // 본문 (HTML -> 순수 텍스트로 변환)
        if (event.getBody() != null && event.getBody().getContent() != null) {
            String content = event.getBody().getContent();
            String plainText = stripHtmlTags(content);
            if (!Objects.equals(schedule.getDescription(), plainText)) {
                schedule.setDescription(plainText);
                hasChanges = true;
            }
        }

        // 시작 시간
        if (event.getStart() != null && event.getStart().getDateTime() != null) {
            String timeZone = event.getStart().getTimeZone();
            ZoneId zoneId = timeZone != null ? ZoneId.of(timeZone) : ZoneId.of("UTC");
            OffsetDateTime startOdt = OffsetDateTime.parse(event.getStart().getDateTime() + "Z")
                    .atZoneSameInstant(zoneId)
                    .toOffsetDateTime();
            java.time.Instant newStartTime = startOdt.toInstant();
            if (!Objects.equals(schedule.getStartTime(), newStartTime)) {
                schedule.setStartTime(newStartTime);
                hasChanges = true;
            }
        }

        // 종료 시간
        if (event.getEnd() != null && event.getEnd().getDateTime() != null) {
            String timeZone = event.getEnd().getTimeZone();
            ZoneId zoneId = timeZone != null ? ZoneId.of(timeZone) : ZoneId.of("UTC");
            OffsetDateTime endOdt = OffsetDateTime.parse(event.getEnd().getDateTime() + "Z")
                    .atZoneSameInstant(zoneId)
                    .toOffsetDateTime();
            java.time.Instant newEndTime = endOdt.toInstant();
            if (!Objects.equals(schedule.getEndTime(), newEndTime)) {
                schedule.setEndTime(newEndTime);
                hasChanges = true;
            }
        }

        // 종일 여부
        Boolean newAllDay = event.getIsAllDay() != null ? event.getIsAllDay() : false;
        if (!Objects.equals(schedule.getAllDay(), newAllDay)) {
            schedule.setAllDay(newAllDay);
            hasChanges = true;
        }

        // 장소
        if (event.getLocation() != null) {
            String newLocation = event.getLocation().getDisplayName();
            if (!Objects.equals(schedule.getLocation(), newLocation)) {
                schedule.setLocation(newLocation);
                hasChanges = true;
            }
        }

        // 주최자
        if (event.getOrganizer() != null && event.getOrganizer().getEmailAddress() != null) {
            String organizer = event.getOrganizer().getEmailAddress().getName();
            if (organizer == null) {
                organizer = event.getOrganizer().getEmailAddress().getAddress();
            }
            if (!Objects.equals(schedule.getOrganizer(), organizer)) {
                schedule.setOrganizer(organizer);
                hasChanges = true;
            }
        }

        // 참석자
        if (event.getAttendees() != null && !event.getAttendees().isEmpty()) {
            String attendees = event.getAttendees().stream()
                    .filter(a -> a.getEmailAddress() != null)
                    .map(a -> {
                        String name = a.getEmailAddress().getName();
                        return name != null ? name : a.getEmailAddress().getAddress();
                    })
                    .collect(Collectors.joining(", "));
            if (!Objects.equals(schedule.getAttendees(), attendees)) {
                schedule.setAttendees(attendees);
                hasChanges = true;
            }
        }

        // 범주 연결 및 프로젝트 자동 매칭
        if (event.getCategories() != null && !event.getCategories().isEmpty()) {
            List<ScheduleCategory> categories = new ArrayList<>();
            String firstCategoryColor = null;
            Project matchedProject = null;

            for (String categoryName : event.getCategories()) {
                // 1. 범주 연결
                ScheduleCategory category = categoryCache.get(categoryName);
                if (category != null) {
                    categories.add(category);
                    if (firstCategoryColor == null) {
                        firstCategoryColor = category.getColor();
                    }
                }

                // 2. 프로젝트 자동 매칭 (범주 이름과 프로젝트 이름이 일치하면 연결)
                if (matchedProject == null) {
                    Project project = projectCache.get(categoryName);
                    if (project != null) {
                        matchedProject = project;
                        log.debug("Auto-matched category '{}' to project '{}'", categoryName, project.getName());
                    }
                }
            }

            // 카테고리 변경 감지 (ID 목록 비교)
            Set<UUID> oldCategoryIds = schedule.getCategories() != null
                    ? schedule.getCategories().stream().map(ScheduleCategory::getId).collect(Collectors.toSet())
                    : new HashSet<>();
            Set<UUID> newCategoryIds = categories.stream().map(ScheduleCategory::getId).collect(Collectors.toSet());
            if (!oldCategoryIds.equals(newCategoryIds)) {
                schedule.setCategories(categories);
                hasChanges = true;
            }

            // 프로젝트 변경 감지
            UUID oldProjectId = schedule.getProject() != null ? schedule.getProject().getId() : null;
            UUID newProjectId = matchedProject != null ? matchedProject.getId() : null;
            if (!Objects.equals(oldProjectId, newProjectId)) {
                schedule.setProject(matchedProject);
                hasChanges = true;
            }

            // 색상 변경 감지
            if (firstCategoryColor != null && !Objects.equals(schedule.getColor(), firstCategoryColor)) {
                schedule.setColor(firstCategoryColor);
                hasChanges = true;
            }
        } else {
            // 카테고리가 없는 경우
            if (schedule.getCategories() != null && !schedule.getCategories().isEmpty()) {
                schedule.setCategories(new ArrayList<>());
                hasChanges = true;
            }
            if (schedule.getProject() != null) {
                schedule.setProject(null);
                hasChanges = true;
            }
        }

        return hasChanges;
    }

    /**
     * Outlook 색상 코드를 HEX 색상으로 변환
     */
    private String mapOutlookColor(com.microsoft.graph.models.CategoryColor color) {
        if (color == null) {
            return "#3498DB"; // 기본 파란색
        }

        String colorKey = color.getValue();
        return OUTLOOK_COLOR_MAP.getOrDefault(colorKey, "#3498DB");
    }

    /**
     * HTML 태그를 제거하고 순수 텍스트로 변환
     */
    private String stripHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        // 1. <br>, <p>, <div> 태그를 줄바꿈으로 변환
        String result = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("(?i)</li>", "\n");

        // 2. 모든 HTML 태그 제거
        result = result.replaceAll("<[^>]+>", "");

        // 3. HTML 엔티티 디코딩
        result = result
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");

        // 4. 연속 줄바꿈 정리 (3개 이상 -> 2개로)
        result = result.replaceAll("\n{3,}", "\n\n");

        // 5. 앞뒤 공백 제거
        result = result.trim();

        // 6. 너무 긴 경우 잘라내기 (최대 2000자)
        if (result.length() > 2000) {
            result = result.substring(0, 2000) + "...";
        }

        return result;
    }

    /**
     * 동기화된 Outlook 일정 수 조회
     */
    public int getOutlookScheduleCount(UUID userId) {
        return scheduleRepository.findByUserIdAndIsFromOutlookTrue(userId).size();
    }
}
