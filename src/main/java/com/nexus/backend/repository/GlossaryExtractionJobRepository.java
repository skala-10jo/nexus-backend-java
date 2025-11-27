package com.nexus.backend.repository;

import com.nexus.backend.entity.GlossaryExtractionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GlossaryExtractionJobRepository extends JpaRepository<GlossaryExtractionJob, UUID> {

    Optional<GlossaryExtractionJob> findByFileId(UUID fileId);

    List<GlossaryExtractionJob> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<GlossaryExtractionJob> findByStatus(String status);

    boolean existsByFileId(UUID fileId);

    /**
     * Delete all extraction jobs for a specific file.
     * Used when deleting a file to clean up related jobs.
     *
     * @param fileId the file ID
     */
    void deleteByFileId(UUID fileId);
}
