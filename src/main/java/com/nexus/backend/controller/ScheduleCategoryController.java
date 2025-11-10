package com.nexus.backend.controller;

import com.nexus.backend.dto.request.CategoryOrderRequest;
import com.nexus.backend.dto.request.CategoryRequest;
import com.nexus.backend.dto.response.ScheduleCategoryResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.ScheduleCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedule-categories")
@RequiredArgsConstructor
public class ScheduleCategoryController {

    private final ScheduleCategoryService categoryService;

    /**
     * Get all categories for current user
     */
    @GetMapping
    public ResponseEntity<List<ScheduleCategoryResponse>> getCategories(
            @AuthenticationPrincipal User user
    ) {
        List<ScheduleCategoryResponse> categories = categoryService.getUserCategories(user.getId());
        return ResponseEntity.ok(categories);
    }

    /**
     * Create a new category
     */
    @PostMapping
    public ResponseEntity<ScheduleCategoryResponse> createCategory(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CategoryRequest request
    ) {
        ScheduleCategoryResponse created = categoryService.createCategory(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing category
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<ScheduleCategoryResponse> updateCategory(
            @AuthenticationPrincipal User user,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryRequest request
    ) {
        ScheduleCategoryResponse updated = categoryService.updateCategory(user.getId(), categoryId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a category
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal User user,
            @PathVariable UUID categoryId
    ) {
        categoryService.deleteCategory(user.getId(), categoryId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reorder categories
     */
    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorderCategories(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody List<CategoryOrderRequest> orders
    ) {
        categoryService.reorderCategories(user.getId(), orders);
        return ResponseEntity.ok().build();
    }
}
