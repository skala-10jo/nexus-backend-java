package com.nexus.backend.repository;

import com.nexus.backend.entity.Translation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 번역 Repository
 *
 * 번역 기록에 대한 데이터 접근 인터페이스를 제공합니다.
 */
@Repository
public interface TranslationRepository extends JpaRepository<Translation, UUID> {

    /**
     * 사용자별 번역 기록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 번역 기록 페이지
     */
    Page<Translation> findByUserId(UUID userId, Pageable pageable);

    /**
     * 프로젝트별 번역 기록 조회 (페이징)
     *
     * @param projectId 프로젝트 ID
     * @param pageable 페이징 정보
     * @return 번역 기록 페이지
     */
    Page<Translation> findByProjectId(UUID projectId, Pageable pageable);

    /**
     * 사용자 및 프로젝트별 번역 기록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param projectId 프로젝트 ID
     * @param pageable 페이징 정보
     * @return 번역 기록 페이지
     */
    Page<Translation> findByUserIdAndProjectId(UUID userId, UUID projectId, Pageable pageable);

    /**
     * 사용자의 프로젝트 없는 번역 기록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 번역 기록 페이지
     */
    Page<Translation> findByUserIdAndProjectIdIsNull(UUID userId, Pageable pageable);

    /**
     * 컨텍스트 사용 여부로 필터링하여 조회
     *
     * @param userId 사용자 ID
     * @param contextUsed 컨텍스트 사용 여부
     * @param pageable 페이징 정보
     * @return 번역 기록 페이지
     */
    Page<Translation> findByUserIdAndContextUsed(UUID userId, Boolean contextUsed, Pageable pageable);

    /**
     * 특정 번역 조회 (사용자 검증 포함)
     *
     * @param id 번역 ID
     * @param userId 사용자 ID
     * @return 번역 Optional
     */
    Optional<Translation> findByIdAndUserId(UUID id, UUID userId);

    /**
     * 사용자별 번역 개수
     *
     * @param userId 사용자 ID
     * @return 번역 개수
     */
    long countByUserId(UUID userId);

    /**
     * 프로젝트별 번역 개수
     *
     * @param projectId 프로젝트 ID
     * @return 번역 개수
     */
    long countByProjectId(UUID projectId);

    /**
     * 컨텍스트를 사용한 번역 개수
     *
     * @param userId 사용자 ID
     * @param contextUsed 컨텍스트 사용 여부
     * @return 번역 개수
     */
    long countByUserIdAndContextUsed(UUID userId, Boolean contextUsed);

    /**
     * 원문 또는 번역문에서 검색 (사용자별)
     *
     * @param userId 사용자 ID
     * @param query 검색 쿼리
     * @param pageable 페이징 정보
     * @return 번역 기록 페이지
     */
    @Query("SELECT t FROM Translation t WHERE t.user.id = :userId " +
           "AND (LOWER(t.originalText) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.translatedText) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Translation> searchByUserIdAndQuery(
        @Param("userId") UUID userId,
        @Param("query") String query,
        Pageable pageable
    );

    /**
     * 원문 또는 번역문에서 검색 (프로젝트별)
     *
     * @param projectId 프로젝트 ID
     * @param query 검색 쿼리
     * @param pageable 페이징 정보
     * @return 번역 기록 페이지
     */
    @Query("SELECT t FROM Translation t WHERE t.project.id = :projectId " +
           "AND (LOWER(t.originalText) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.translatedText) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Translation> searchByProjectIdAndQuery(
        @Param("projectId") UUID projectId,
        @Param("query") String query,
        Pageable pageable
    );

    /**
     * 언어 쌍으로 번역 기록 조회
     *
     * @param userId 사용자 ID
     * @param sourceLanguage 원본 언어
     * @param targetLanguage 목표 언어
     * @param pageable 페이징 정보
     * @return 번역 기록 페이지
     */
    Page<Translation> findByUserIdAndSourceLanguageAndTargetLanguage(
        UUID userId,
        String sourceLanguage,
        String targetLanguage,
        Pageable pageable
    );

    /**
     * 사용자와 번역 ID로 존재 여부 확인
     *
     * @param id 번역 ID
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
