package com.nexus.backend.repository;

import com.nexus.backend.entity.VideoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VideoFile entity.
 * Provides queries for video-specific metadata and processing status.
 */
@Repository
public interface VideoFileRepository extends JpaRepository<VideoFile, UUID> {

    /**
     * Find video file by file ID (same as video file ID)
     */
    Optional<VideoFile> findById(UUID fileId);

    /**
     * Find video file with File entity eagerly loaded
     */
    @Query("SELECT vf FROM VideoFile vf JOIN FETCH vf.file WHERE vf.id = :fileId")
    Optional<VideoFile> findByIdWithFile(@Param("fileId") UUID fileId);

    /**
     * Find videos by STT status
     */
    @Query("SELECT vf FROM VideoFile vf JOIN FETCH vf.file f WHERE f.user.id = :userId AND vf.sttStatus = :status")
    List<VideoFile> findByUserIdAndSttStatus(@Param("userId") UUID userId, @Param("status") String status);

    /**
     * Find videos by translation status
     */
    @Query("SELECT vf FROM VideoFile vf JOIN FETCH vf.file f WHERE f.user.id = :userId AND vf.translationStatus = :status")
    List<VideoFile> findByUserIdAndTranslationStatus(@Param("userId") UUID userId, @Param("status") String status);

    /**
     * Find videos ready for processing (STT or translation pending)
     */
    @Query("SELECT vf FROM VideoFile vf WHERE vf.sttStatus = 'pending' OR vf.translationStatus = 'pending'")
    List<VideoFile> findPendingProcessing();
}
