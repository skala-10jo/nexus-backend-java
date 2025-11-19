package com.nexus.backend.repository;

import com.nexus.backend.entity.VideoDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoDocumentRepository extends JpaRepository<VideoDocument, UUID> {

    /**
     * Document ID로 VideoDocument 조회
     */
    Optional<VideoDocument> findByDocumentId(UUID documentId);

    /**
     * 사용자의 모든 영상 문서 조회 (페이징)
     */
    @Query("SELECT vd FROM VideoDocument vd " +
           "WHERE vd.document.user.id = :userId " +
           "ORDER BY vd.createdAt DESC")
    Page<VideoDocument> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * 사용자의 영상 문서 조회 (STT 상태 필터링)
     */
    @Query("SELECT vd FROM VideoDocument vd " +
           "WHERE vd.document.user.id = :userId " +
           "AND vd.sttStatus = :sttStatus " +
           "ORDER BY vd.createdAt DESC")
    Page<VideoDocument> findByUserIdAndSttStatus(
        @Param("userId") UUID userId,
        @Param("sttStatus") String sttStatus,
        Pageable pageable
    );

    /**
     * 사용자의 영상 문서 조회 (번역 상태 필터링)
     */
    @Query("SELECT vd FROM VideoDocument vd " +
           "WHERE vd.document.user.id = :userId " +
           "AND vd.translationStatus = :translationStatus " +
           "ORDER BY vd.createdAt DESC")
    Page<VideoDocument> findByUserIdAndTranslationStatus(
        @Param("userId") UUID userId,
        @Param("translationStatus") String translationStatus,
        Pageable pageable
    );

    /**
     * 특정 사용자의 VideoDocument 조회 (권한 확인용)
     */
    @Query("SELECT vd FROM VideoDocument vd " +
           "WHERE vd.id = :id AND vd.document.user.id = :userId")
    Optional<VideoDocument> findByIdAndUserId(
        @Param("id") UUID id,
        @Param("userId") UUID userId
    );
}
