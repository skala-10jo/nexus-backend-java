package com.nexus.backend.repository;

import com.nexus.backend.entity.VideoTranslationGlossary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoTranslationGlossaryRepository extends JpaRepository<VideoTranslationGlossary, UUID> {

    /**
     * VideoFile ID로 선택된 용어집 파일 목록 조회
     */
    @Query("SELECT vtg FROM VideoTranslationGlossary vtg " +
           "WHERE vtg.videoFile.id = :videoFileId")
    List<VideoTranslationGlossary> findByVideoFileId(
        @Param("videoFileId") UUID videoFileId
    );

    /**
     * VideoFile ID로 선택된 File ID 목록 조회
     */
    @Query("SELECT vtg.file.id FROM VideoTranslationGlossary vtg " +
           "WHERE vtg.videoFile.id = :videoFileId")
    List<UUID> findFileIdsByVideoFileId(
        @Param("videoFileId") UUID videoFileId
    );

    /**
     * VideoFile ID로 모든 용어집 매핑 삭제
     */
    void deleteByVideoFileId(UUID videoFileId);

    /**
     * File ID (용어집 파일)로 모든 매핑 삭제.
     * 용어집 파일 삭제 시 해당 파일과 연결된 비디오 매핑 정리.
     *
     * @param fileId the glossary file ID
     */
    void deleteByFileId(UUID fileId);
}
