package com.nexus.backend.repository;

import com.nexus.backend.entity.TranslationTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 번역-용어 매핑 Repository
 *
 * 번역에서 탐지된 전문용어 매핑 정보에 대한 데이터 접근 인터페이스를 제공합니다.
 */
@Repository
public interface TranslationTermRepository extends JpaRepository<TranslationTerm, UUID> {

    /**
     * 특정 번역의 모든 탐지된 용어 조회
     *
     * @param translationId 번역 ID
     * @return 탐지된 용어 목록
     */
    List<TranslationTerm> findByTranslationId(UUID translationId);

    /**
     * 특정 용어집 용어가 탐지된 모든 번역 조회
     *
     * @param glossaryTermId 용어집 용어 ID
     * @return 탐지된 용어 목록
     */
    List<TranslationTerm> findByGlossaryTermId(UUID glossaryTermId);

    /**
     * 특정 번역의 탐지된 용어 개수
     *
     * @param translationId 번역 ID
     * @return 탐지된 용어 개수
     */
    long countByTranslationId(UUID translationId);

    /**
     * 특정 용어집 용어가 탐지된 횟수
     *
     * @param glossaryTermId 용어집 용어 ID
     * @return 탐지 횟수
     */
    long countByGlossaryTermId(UUID glossaryTermId);

    /**
     * 번역과 용어집 용어로 매칭 존재 여부 확인
     *
     * @param translationId 번역 ID
     * @param glossaryTermId 용어집 용어 ID
     * @return 존재 여부
     */
    boolean existsByTranslationIdAndGlossaryTermId(UUID translationId, UUID glossaryTermId);

    /**
     * 특정 번역에서 매칭된 텍스트로 조회
     *
     * @param translationId 번역 ID
     * @param matchedText 매칭된 텍스트
     * @return 탐지된 용어 목록
     */
    List<TranslationTerm> findByTranslationIdAndMatchedText(UUID translationId, String matchedText);

    /**
     * 번역에서 위치 순서대로 탐지된 용어 조회
     *
     * @param translationId 번역 ID
     * @return 위치 순서대로 정렬된 탐지된 용어 목록
     */
    @Query("SELECT tt FROM TranslationTerm tt WHERE tt.translation.id = :translationId " +
           "ORDER BY tt.positionStart ASC")
    List<TranslationTerm> findByTranslationIdOrderByPosition(@Param("translationId") UUID translationId);

    /**
     * 특정 번역의 모든 탐지된 용어 삭제
     *
     * @param translationId 번역 ID
     */
    void deleteByTranslationId(UUID translationId);

    /**
     * 특정 용어집 용어와 관련된 모든 매핑 삭제
     *
     * @param glossaryTermId 용어집 용어 ID
     */
    void deleteByGlossaryTermId(UUID glossaryTermId);
}
