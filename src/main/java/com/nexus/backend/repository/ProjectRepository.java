package com.nexus.backend.repository;

import com.nexus.backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByUserId(UUID userId);

    List<Project> findByUserIdAndStatus(UUID userId, String status);

    Optional<Project> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    /**
     * Find project by user ID and name (for sync with ScheduleCategory)
     */
    Optional<Project> findByUserIdAndName(UUID userId, String name);

    /**
     * Check if project with name exists for user (for sync with ScheduleCategory)
     */
    boolean existsByUserIdAndName(UUID userId, String name);

    /**
     * Check if ACTIVE project with name exists for user (excludes DELETED projects)
     */
    boolean existsByUserIdAndNameAndStatusNot(UUID userId, String name, String status);

    /**
     * Find ACTIVE project by user ID and name (excludes DELETED projects)
     */
    Optional<Project> findByUserIdAndNameAndStatusNot(UUID userId, String name, String status);
}
