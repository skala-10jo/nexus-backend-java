package com.nexus.backend.service;

import com.nexus.backend.entity.Project;
import com.nexus.backend.entity.ScheduleCategory;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.ProjectRepository;
import com.nexus.backend.repository.ScheduleCategoryRepository;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Project와 ScheduleCategory 간의 양방향 동기화 서비스
 *
 * <p>이 서비스는 Project와 ScheduleCategory를 동일한 개념으로 취급하여
 * 한쪽에서 CRUD 작업이 발생하면 다른 쪽도 자동으로 동기화합니다.</p>
 *
 * <h3>동기화 규칙:</h3>
 * <ul>
 *   <li>Project 생성 → 동일 이름의 Category 자동 생성 (없는 경우)</li>
 *   <li>Category 생성 → 동일 이름의 Project 자동 생성 (없는 경우)</li>
 *   <li>Project 이름 변경 → 동일 이름의 Category 이름 변경</li>
 *   <li>Category 이름 변경 → 동일 이름의 Project 이름 변경</li>
 *   <li>Project 삭제 → 동일 이름의 Category 삭제 (Outlook 카테고리 제외)</li>
 *   <li>Category 삭제 → 동일 이름의 Project 삭제 (동기화로 생성된 경우만)</li>
 * </ul>
 *
 * <h3>무한 루프 방지:</h3>
 * <p>ThreadLocal을 사용하여 동기화 중인 상태를 추적하고,
 * 이미 동기화 중인 경우 추가 동기화를 스킵합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectCategorySyncService {

    private final ProjectRepository projectRepository;
    private final ScheduleCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * 무한 루프 방지를 위한 ThreadLocal 플래그
     * Project → Category 동기화가 다시 Category → Project 동기화를 트리거하는 것을 방지
     */
    private static final ThreadLocal<Boolean> syncing = ThreadLocal.withInitial(() -> false);

    /**
     * 카테고리 생성 시 사용할 색상 팔레트
     */
    private static final String[] COLOR_PALETTE = {
        "#3B82F6", // blue
        "#10B981", // emerald
        "#8B5CF6", // violet
        "#F59E0B", // amber
        "#EF4444", // red
        "#EC4899", // pink
        "#06B6D4", // cyan
        "#84CC16", // lime
        "#F97316", // orange
        "#6366F1"  // indigo
    };

    // ============================================
    // Project → Category 동기화
    // ============================================

    /**
     * 프로젝트 생성 시 카테고리 동기화
     *
     * @param userId 사용자 ID
     * @param projectName 프로젝트 이름
     */
    @Transactional
    public void onProjectCreated(UUID userId, String projectName) {
        if (syncing.get()) {
            log.debug("Skipping onProjectCreated - already syncing");
            return;
        }

        try {
            syncing.set(true);

            // 이미 동일한 이름의 카테고리가 있는지 확인
            if (categoryRepository.existsByUserIdAndName(userId, projectName)) {
                log.debug("Category '{}' already exists, skipping creation", projectName);
                return;
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // 다음 표시 순서 계산
            int nextOrder = getNextCategoryDisplayOrder(userId);

            // 사용 가능한 색상 선택
            String color = getAvailableColor(userId);

            // 카테고리 생성
            ScheduleCategory category = ScheduleCategory.builder()
                    .user(user)
                    .name(projectName)
                    .color(color)
                    .description("프로젝트에서 동기화: " + projectName)
                    .isDefault(false)
                    .displayOrder(nextOrder)
                    .isFromOutlook(false)
                    .build();

            categoryRepository.save(category);
            log.info("Synced: Created category '{}' from project (userId={})", projectName, userId);

        } finally {
            syncing.set(false);
        }
    }

    /**
     * 프로젝트 이름 변경 시 카테고리 동기화
     *
     * @param userId 사용자 ID
     * @param oldName 이전 이름
     * @param newName 새 이름
     */
    @Transactional
    public void onProjectUpdated(UUID userId, String oldName, String newName) {
        if (syncing.get()) {
            log.debug("Skipping onProjectUpdated - already syncing");
            return;
        }

        if (oldName.equals(newName)) {
            return; // 이름 변경 없음
        }

        try {
            syncing.set(true);

            // 기존 이름의 카테고리 찾기
            Optional<ScheduleCategory> categoryOpt = categoryRepository.findByUserIdAndName(userId, oldName);

            if (categoryOpt.isEmpty()) {
                log.debug("Category '{}' not found, cannot update", oldName);
                return;
            }

            ScheduleCategory category = categoryOpt.get();

            // 새 이름으로 이미 다른 카테고리가 있는지 확인
            if (categoryRepository.existsByUserIdAndName(userId, newName)) {
                log.warn("Category '{}' already exists, cannot rename from '{}'", newName, oldName);
                return;
            }

            category.setName(newName);
            categoryRepository.save(category);
            log.info("Synced: Renamed category '{}' → '{}' (userId={})", oldName, newName, userId);

        } finally {
            syncing.set(false);
        }
    }

    /**
     * 프로젝트 삭제 시 카테고리 동기화
     *
     * @param userId 사용자 ID
     * @param projectName 프로젝트 이름
     */
    @Transactional
    public void onProjectDeleted(UUID userId, String projectName) {
        if (syncing.get()) {
            log.debug("Skipping onProjectDeleted - already syncing");
            return;
        }

        try {
            syncing.set(true);

            Optional<ScheduleCategory> categoryOpt = categoryRepository.findByUserIdAndName(userId, projectName);

            if (categoryOpt.isEmpty()) {
                log.debug("Category '{}' not found, nothing to delete", projectName);
                return;
            }

            ScheduleCategory category = categoryOpt.get();

            // Outlook에서 가져온 카테고리는 삭제하지 않음
            if (Boolean.TRUE.equals(category.getIsFromOutlook())) {
                log.info("Category '{}' is from Outlook, skipping delete", projectName);
                return;
            }

            // 기본 카테고리는 삭제하지 않음
            if (Boolean.TRUE.equals(category.getIsDefault())) {
                log.info("Category '{}' is default, skipping delete", projectName);
                return;
            }

            categoryRepository.delete(category);
            log.info("Synced: Deleted category '{}' (userId={})", projectName, userId);

        } finally {
            syncing.set(false);
        }
    }

    // ============================================
    // Category → Project 동기화
    // ============================================

    /**
     * 카테고리 생성 시 프로젝트 동기화
     *
     * @param userId 사용자 ID
     * @param categoryName 카테고리 이름
     */
    @Transactional
    public void onCategoryCreated(UUID userId, String categoryName) {
        if (syncing.get()) {
            log.debug("Skipping onCategoryCreated - already syncing");
            return;
        }

        try {
            syncing.set(true);

            // 이미 동일한 이름의 프로젝트가 있는지 확인
            if (projectRepository.existsByUserIdAndName(userId, categoryName)) {
                log.debug("Project '{}' already exists, skipping creation", categoryName);
                return;
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // 프로젝트 생성
            Project project = Project.builder()
                    .user(user)
                    .name(categoryName)
                    .description("카테고리에서 동기화: " + categoryName)
                    .status("ACTIVE")
                    .build();

            projectRepository.save(project);
            log.info("Synced: Created project '{}' from category (userId={})", categoryName, userId);

        } finally {
            syncing.set(false);
        }
    }

    /**
     * 카테고리 이름 변경 시 프로젝트 동기화
     *
     * @param userId 사용자 ID
     * @param oldName 이전 이름
     * @param newName 새 이름
     */
    @Transactional
    public void onCategoryUpdated(UUID userId, String oldName, String newName) {
        if (syncing.get()) {
            log.debug("Skipping onCategoryUpdated - already syncing");
            return;
        }

        if (oldName.equals(newName)) {
            return; // 이름 변경 없음
        }

        try {
            syncing.set(true);

            // 기존 이름의 프로젝트 찾기
            Optional<Project> projectOpt = projectRepository.findByUserIdAndName(userId, oldName);

            if (projectOpt.isEmpty()) {
                log.debug("Project '{}' not found, cannot update", oldName);
                return;
            }

            Project project = projectOpt.get();

            // 새 이름으로 이미 다른 프로젝트가 있는지 확인
            if (projectRepository.existsByUserIdAndName(userId, newName)) {
                log.warn("Project '{}' already exists, cannot rename from '{}'", newName, oldName);
                return;
            }

            project.setName(newName);
            projectRepository.save(project);
            log.info("Synced: Renamed project '{}' → '{}' (userId={})", oldName, newName, userId);

        } finally {
            syncing.set(false);
        }
    }

    /**
     * 카테고리 삭제 시 프로젝트 동기화
     *
     * @param userId 사용자 ID
     * @param categoryName 카테고리 이름
     * @param isFromOutlook Outlook에서 가져온 카테고리인지 여부
     */
    @Transactional
    public void onCategoryDeleted(UUID userId, String categoryName, boolean isFromOutlook) {
        if (syncing.get()) {
            log.debug("Skipping onCategoryDeleted - already syncing");
            return;
        }

        try {
            syncing.set(true);

            Optional<Project> projectOpt = projectRepository.findByUserIdAndName(userId, categoryName);

            if (projectOpt.isEmpty()) {
                log.debug("Project '{}' not found, nothing to delete", categoryName);
                return;
            }

            Project project = projectOpt.get();

            // 동기화로 생성된 프로젝트만 삭제 (description에 "동기화" 포함)
            // 또는 Outlook 카테고리에서 생성된 프로젝트 (description에 "Outlook" 포함)
            String description = project.getDescription();
            boolean isSyncedProject = description != null &&
                (description.contains("동기화") || description.contains("Outlook"));

            if (!isSyncedProject) {
                log.info("Project '{}' was not synced, skipping delete", categoryName);
                return;
            }

            // 문서나 용어집이 연결된 프로젝트는 삭제하지 않음
            if (project.getFiles() != null && !project.getFiles().isEmpty()) {
                log.info("Project '{}' has documents attached, skipping delete", categoryName);
                return;
            }

            // Soft delete (상태를 DELETED로 변경)
            project.setStatus("DELETED");
            projectRepository.save(project);
            log.info("Synced: Deleted project '{}' (userId={})", categoryName, userId);

        } finally {
            syncing.set(false);
        }
    }

    // ============================================
    // 유틸리티 메서드
    // ============================================

    /**
     * 다음 카테고리 표시 순서 계산
     */
    private int getNextCategoryDisplayOrder(UUID userId) {
        List<ScheduleCategory> categories = categoryRepository.findByUserIdOrderByDisplayOrder(userId);
        if (categories.isEmpty()) {
            return 0;
        }
        return categories.get(categories.size() - 1).getDisplayOrder() + 1;
    }

    /**
     * 사용 가능한 색상 선택
     * 이미 사용 중인 색상을 피해서 선택
     */
    private String getAvailableColor(UUID userId) {
        List<ScheduleCategory> categories = categoryRepository.findByUserIdOrderByDisplayOrder(userId);
        List<String> usedColors = categories.stream()
                .map(ScheduleCategory::getColor)
                .toList();

        // 사용되지 않은 색상 찾기
        for (String color : COLOR_PALETTE) {
            if (!usedColors.contains(color)) {
                return color;
            }
        }

        // 모든 색상이 사용 중이면 첫 번째 색상 반환
        return COLOR_PALETTE[0];
    }
}
