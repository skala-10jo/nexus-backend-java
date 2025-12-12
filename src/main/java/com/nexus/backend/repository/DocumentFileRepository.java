package com.nexus.backend.repository;

import com.nexus.backend.entity.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DocumentFile entity.
 * Provides queries for document-specific metadata.
 *
 */
@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, UUID> {

    /**
     * Find document file by file ID (same as document file ID)
     */
    Optional<DocumentFile> findById(UUID fileId);

    /**
     * Find document file with File entity eagerly loaded
     */
    @Query("SELECT df FROM DocumentFile df JOIN FETCH df.file WHERE df.id = :fileId")
    Optional<DocumentFile> findByIdWithFile(@Param("fileId") UUID fileId);

    /**
     * Check if document has been analyzed
     */
    @Query("SELECT df.isAnalyzed FROM DocumentFile df WHERE df.id = :fileId")
    Optional<Boolean> isAnalyzed(@Param("fileId") UUID fileId);
}
