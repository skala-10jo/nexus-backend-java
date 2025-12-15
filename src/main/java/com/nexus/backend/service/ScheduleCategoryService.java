package com.nexus.backend.service;

import com.nexus.backend.dto.request.CategoryOrderRequest;
import com.nexus.backend.dto.request.CategoryRequest;
import com.nexus.backend.dto.response.ScheduleCategoryResponse;
import com.nexus.backend.entity.ScheduleCategory;
import com.nexus.backend.entity.User;
import com.nexus.backend.exception.ResourceNotFoundException;
import com.nexus.backend.repository.ScheduleCategoryRepository;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleCategoryService {

    private final ScheduleCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProjectCategorySyncService syncService;

    /**
     * Get all categories for a user
     */
    public List<ScheduleCategoryResponse> getUserCategories(UUID userId) {
        return categoryRepository.findByUserIdOrderByDisplayOrder(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new category
     */
    @Transactional
    public ScheduleCategoryResponse createCategory(UUID userId, CategoryRequest request) {
        // Check for duplicate category name
        if (categoryRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get next display order
        int nextOrder = getNextDisplayOrder(userId);

        ScheduleCategory category = ScheduleCategory.builder()
                .user(user)
                .name(request.getName())
                .color(request.getColor())
                .icon(request.getIcon())
                .description(request.getDescription())
                .isDefault(false)
                .displayOrder(nextOrder)
                .build();

        ScheduleCategory saved = categoryRepository.save(category);

        // 프로젝트 동기화: 카테고리 생성 시 동일 이름의 프로젝트 자동 생성
        syncService.onCategoryCreated(userId, request.getName());

        return toResponse(saved);
    }

    /**
     * Update an existing category
     */
    @Transactional
    public ScheduleCategoryResponse updateCategory(UUID userId, UUID categoryId, CategoryRequest request) {
        ScheduleCategory category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        // 이름 변경 시 프로젝트 동기화를 위해 이전 이름 저장
        String oldName = category.getName();

        // Check for duplicate name (excluding current category)
        if (!category.getName().equals(request.getName()) &&
            categoryRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
        }

        // Default categories can be updated but not renamed
        if (category.getIsDefault() && !category.getName().equals(request.getName())) {
            throw new IllegalArgumentException("Cannot rename default category");
        }

        category.setName(request.getName());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        category.setDescription(request.getDescription());

        ScheduleCategory updated = categoryRepository.save(category);

        // 프로젝트 동기화: 카테고리 이름 변경 시 동일 이름의 프로젝트 이름 변경
        syncService.onCategoryUpdated(userId, oldName, request.getName());

        return toResponse(updated);
    }

    /**
     * Delete a category
     */
    @Transactional
    public void deleteCategory(UUID userId, UUID categoryId) {
        ScheduleCategory category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        // Cannot delete default categories
        if (category.getIsDefault()) {
            throw new IllegalArgumentException("Cannot delete default category");
        }

        // 프로젝트 동기화를 위해 카테고리 정보 저장
        String categoryName = category.getName();
        boolean isFromOutlook = Boolean.TRUE.equals(category.getIsFromOutlook());

        // Note: Associated schedules will be handled via cascade in junction table
        // Schedules themselves won't be deleted, just the category association
        categoryRepository.delete(category);

        // 프로젝트 동기화: 카테고리 삭제 시 동일 이름의 프로젝트 삭제
        syncService.onCategoryDeleted(userId, categoryName, isFromOutlook);
    }

    /**
     * Reorder categories
     */
    @Transactional
    public void reorderCategories(UUID userId, List<CategoryOrderRequest> orders) {
        for (CategoryOrderRequest orderRequest : orders) {
            categoryRepository.findByIdAndUserId(orderRequest.getCategoryId(), userId)
                    .ifPresent(category -> {
                        category.setDisplayOrder(orderRequest.getOrder());
                        categoryRepository.save(category);
                    });
        }
    }

    /**
     * Get next display order for new category
     */
    private int getNextDisplayOrder(UUID userId) {
        List<ScheduleCategory> categories = categoryRepository.findByUserIdOrderByDisplayOrder(userId);
        if (categories.isEmpty()) {
            return 0;
        }
        return categories.get(categories.size() - 1).getDisplayOrder() + 1;
    }

    /**
     * Convert entity to response DTO
     */
    private ScheduleCategoryResponse toResponse(ScheduleCategory category) {
        return ScheduleCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .icon(category.getIcon())
                .description(category.getDescription())
                .isDefault(category.getIsDefault())
                .displayOrder(category.getDisplayOrder())
                .isFromOutlook(category.getIsFromOutlook())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .scheduleCount((long) category.getSchedules().size())
                .build();
    }
}
