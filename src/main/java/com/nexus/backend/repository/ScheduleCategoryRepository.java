package com.nexus.backend.repository;

import com.nexus.backend.entity.ScheduleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleCategoryRepository extends JpaRepository<ScheduleCategory, UUID> {

    /**
     * Find all categories for a user, ordered by display order
     */
    List<ScheduleCategory> findByUserIdOrderByDisplayOrder(UUID userId);

    /**
     * Check if a category with the same name exists for this user
     */
    boolean existsByUserIdAndName(UUID userId, String name);

    /**
     * Find category by ID and user ID (for authorization)
     */
    Optional<ScheduleCategory> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find first default category for a user
     */
    Optional<ScheduleCategory> findFirstByUserIdAndIsDefaultTrueOrderByDisplayOrder(UUID userId);

    /**
     * Find all default categories for a user
     */
    List<ScheduleCategory> findByUserIdAndIsDefaultTrue(UUID userId);

    /**
     * Count categories for a user
     */
    long countByUserId(UUID userId);

    /**
     * Delete all categories for a user
     */
    void deleteByUserId(UUID userId);
}
