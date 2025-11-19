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
     * VideoDocument ID로 선택된 용어집 문서 목록 조회
     */
    @Query("SELECT vtg FROM VideoTranslationGlossary vtg " +
           "WHERE vtg.videoDocument.id = :videoDocumentId")
    List<VideoTranslationGlossary> findByVideoDocumentId(
        @Param("videoDocumentId") UUID videoDocumentId
    );

    /**
     * VideoDocument ID로 선택된 Document ID 목록 조회
     */
    @Query("SELECT vtg.document.id FROM VideoTranslationGlossary vtg " +
           "WHERE vtg.videoDocument.id = :videoDocumentId")
    List<UUID> findDocumentIdsByVideoDocumentId(
        @Param("videoDocumentId") UUID videoDocumentId
    );

    /**
     * VideoDocument ID로 모든 용어집 매핑 삭제
     */
    void deleteByVideoDocumentId(UUID videoDocumentId);
}
