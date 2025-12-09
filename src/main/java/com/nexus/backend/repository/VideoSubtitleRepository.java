package com.nexus.backend.repository;

import com.nexus.backend.entity.VideoSubtitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoSubtitleRepository extends JpaRepository<VideoSubtitle, UUID> {

    /**
     * VideoFile ID로 모든 자막 조회 (시퀀스 순서대로)
     */
    @Query("SELECT vs FROM VideoSubtitle vs " +
           "WHERE vs.videoFile.id = :videoFileId " +
           "ORDER BY vs.sequenceNumber ASC")
    List<VideoSubtitle> findByVideoFileIdOrderBySequenceNumber(
        @Param("videoFileId") UUID videoFileId
    );

    /**
     * VideoFile ID로 번역된 자막만 조회
     */
    @Query("SELECT vs FROM VideoSubtitle vs " +
           "WHERE vs.videoFile.id = :videoFileId " +
           "AND vs.translatedText IS NOT NULL " +
           "ORDER BY vs.sequenceNumber ASC")
    List<VideoSubtitle> findTranslatedSubtitles(
        @Param("videoFileId") UUID videoFileId
    );

    /**
     * 특정 시간 범위의 자막 조회
     */
    @Query("SELECT vs FROM VideoSubtitle vs " +
           "WHERE vs.videoFile.id = :videoFileId " +
           "AND vs.startTimeMs <= :endTimeMs " +
           "AND vs.endTimeMs >= :startTimeMs " +
           "ORDER BY vs.sequenceNumber ASC")
    List<VideoSubtitle> findByTimeRange(
        @Param("videoFileId") UUID videoFileId,
        @Param("startTimeMs") Long startTimeMs,
        @Param("endTimeMs") Long endTimeMs
    );

    /**
     * VideoFile ID로 자막 전체 삭제
     *
     * @param videoFileId 영상 파일 ID
     */
    @Modifying
    @Query("DELETE FROM VideoSubtitle vs WHERE vs.videoFile.id = :videoFileId")
    void deleteByVideoFileId(@Param("videoFileId") UUID videoFileId);

    /**
     * VideoFile ID로 자막 존재 여부 확인
     *
     * @param videoFileId 영상 파일 ID
     * @return 자막이 하나라도 있으면 true
     */
    boolean existsByVideoFileId(UUID videoFileId);

    /**
     * VideoFile ID로 자막 개수 조회
     *
     * @param videoFileId 영상 파일 ID
     * @return 자막 개수
     */
    long countByVideoFileId(UUID videoFileId);

    /**
     * VideoFile ID로 첫 번째 자막 조회 (원본 언어 확인용)
     *
     * @param videoFileId 영상 파일 ID
     * @return 첫 번째 자막 (Optional)
     */
    @Query("SELECT vs FROM VideoSubtitle vs " +
           "WHERE vs.videoFile.id = :videoFileId " +
           "ORDER BY vs.sequenceNumber ASC " +
           "LIMIT 1")
    Optional<VideoSubtitle> findFirstByVideoFileId(
        @Param("videoFileId") UUID videoFileId
    );
}
