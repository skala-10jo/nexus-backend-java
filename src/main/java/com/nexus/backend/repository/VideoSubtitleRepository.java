package com.nexus.backend.repository;

import com.nexus.backend.entity.VideoSubtitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoSubtitleRepository extends JpaRepository<VideoSubtitle, UUID> {

    /**
     * VideoDocument ID로 모든 자막 조회 (시퀀스 순서대로)
     */
    @Query("SELECT vs FROM VideoSubtitle vs " +
           "WHERE vs.videoDocument.id = :videoDocumentId " +
           "ORDER BY vs.sequenceNumber ASC")
    List<VideoSubtitle> findByVideoDocumentIdOrderBySequenceNumber(
        @Param("videoDocumentId") UUID videoDocumentId
    );

    /**
     * VideoDocument ID로 번역된 자막만 조회
     */
    @Query("SELECT vs FROM VideoSubtitle vs " +
           "WHERE vs.videoDocument.id = :videoDocumentId " +
           "AND vs.translatedText IS NOT NULL " +
           "ORDER BY vs.sequenceNumber ASC")
    List<VideoSubtitle> findTranslatedSubtitles(
        @Param("videoDocumentId") UUID videoDocumentId
    );

    /**
     * 특정 시간 범위의 자막 조회
     */
    @Query("SELECT vs FROM VideoSubtitle vs " +
           "WHERE vs.videoDocument.id = :videoDocumentId " +
           "AND vs.startTimeMs <= :endTimeMs " +
           "AND vs.endTimeMs >= :startTimeMs " +
           "ORDER BY vs.sequenceNumber ASC")
    List<VideoSubtitle> findByTimeRange(
        @Param("videoDocumentId") UUID videoDocumentId,
        @Param("startTimeMs") Long startTimeMs,
        @Param("endTimeMs") Long endTimeMs
    );

    /**
     * VideoDocument ID로 자막 전체 삭제
     */
    void deleteByVideoDocumentId(UUID videoDocumentId);
}
