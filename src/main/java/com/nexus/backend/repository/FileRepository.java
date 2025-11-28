package com.nexus.backend.repository;

import com.nexus.backend.entity.File;
import com.nexus.backend.entity.FileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for File entity.
 * Provides queries for all file types with explicit type filtering.
 *
 * @author NEXUS Team
 * @version 1.0
 * @since 2025-01-18
 */
@Repository
public interface FileRepository extends JpaRepository<File, UUID> {

    /**
     * Find files by user ID and file type (paginated)
     */
    Page<File> findByUserIdAndFileType(UUID userId, FileType fileType, Pageable pageable);

    /**
     * Find files by user ID (all types, paginated)
     */
    Page<File> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find files by user ID (all types, not paginated)
     */
    List<File> findByUserId(UUID userId);

    /**
     * Find a specific file by ID and user ID (security check)
     */
    Optional<File> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Check if a file exists for a user
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);

    /**
     * Search files by filename or type
     */
    @Query("SELECT f FROM File f WHERE f.user.id = :userId AND f.fileType = :fileType AND " +
           "(LOWER(f.originalFilename) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<File> searchFiles(@Param("userId") UUID userId,
                           @Param("fileType") FileType fileType,
                           @Param("query") String query,
                           Pageable pageable);
}
