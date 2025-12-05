package com.nexus.backend.repository;

import com.nexus.backend.entity.GlossaryTerm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, UUID> {

    // User-level queries (all terms for a user)
    Page<GlossaryTerm> findByUserId(UUID userId, Pageable pageable);

    Page<GlossaryTerm> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);

    @Query("SELECT t FROM GlossaryTerm t WHERE t.user.id = :userId " +
           "AND (LOWER(t.koreanTerm) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.englishTerm) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.definition) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<GlossaryTerm> searchByUserIdAndQuery(
        @Param("userId") UUID userId,
        @Param("query") String query,
        Pageable pageable
    );

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, String status);

    long countByUserIdAndIsVerified(UUID userId, boolean isVerified);

    boolean existsByUserIdAndKoreanTerm(UUID userId, String koreanTerm);

    // Project-level queries (filtered by project)
    Page<GlossaryTerm> findByProjectId(UUID projectId, Pageable pageable);

    Page<GlossaryTerm> findByProjectIdAndStatus(UUID projectId, String status, Pageable pageable);

    Optional<GlossaryTerm> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT t FROM GlossaryTerm t WHERE t.project.id = :projectId " +
           "AND (LOWER(t.koreanTerm) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.englishTerm) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(t.definition) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<GlossaryTerm> searchByProjectIdAndQuery(
        @Param("projectId") UUID projectId,
        @Param("query") String query,
        Pageable pageable
    );

    long countByProjectId(UUID projectId);

    long countByProjectIdAndStatus(UUID projectId, String status);

    long countByProjectIdAndIsVerified(UUID projectId, boolean isVerified);

    // NEW: Query terms by project's linked files using native SQL
    // This works even when project_id is NULL (terms extracted before project assignment)
    @Query(value = "SELECT DISTINCT t.* FROM glossary_terms t " +
                   "INNER JOIN glossary_term_documents gtd ON t.id = gtd.term_id " +
                   "INNER JOIN project_files pf ON gtd.file_id = pf.file_id " +
                   "WHERE pf.project_id = :projectId",
           countQuery = "SELECT COUNT(DISTINCT t.id) FROM glossary_terms t " +
                        "INNER JOIN glossary_term_documents gtd ON t.id = gtd.term_id " +
                        "INNER JOIN project_files pf ON gtd.file_id = pf.file_id " +
                        "WHERE pf.project_id = :projectId",
           nativeQuery = true)
    Page<GlossaryTerm> findTermsByProjectFiles(
        @Param("projectId") UUID projectId,
        Pageable pageable
    );

    @Query(value = "SELECT DISTINCT t.* FROM glossary_terms t " +
                   "INNER JOIN glossary_term_documents gtd ON t.id = gtd.term_id " +
                   "INNER JOIN project_files pf ON gtd.file_id = pf.file_id " +
                   "WHERE pf.project_id = :projectId " +
                   "AND (LOWER(t.korean_term) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(t.english_term) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(t.definition) LIKE LOWER(CONCAT('%', :query, '%')))",
           countQuery = "SELECT COUNT(DISTINCT t.id) FROM glossary_terms t " +
                        "INNER JOIN glossary_term_documents gtd ON t.id = gtd.term_id " +
                        "INNER JOIN project_files pf ON gtd.file_id = pf.file_id " +
                        "WHERE pf.project_id = :projectId " +
                        "AND (LOWER(t.korean_term) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(t.english_term) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(t.definition) LIKE LOWER(CONCAT('%', :query, '%')))",
           nativeQuery = true)
    Page<GlossaryTerm> searchTermsByProjectFiles(
        @Param("projectId") UUID projectId,
        @Param("query") String query,
        Pageable pageable
    );

    @Query(value = "SELECT COUNT(DISTINCT t.id) FROM glossary_terms t " +
                   "INNER JOIN glossary_term_documents gtd ON t.id = gtd.term_id " +
                   "INNER JOIN project_files pf ON gtd.file_id = pf.file_id " +
                   "WHERE pf.project_id = :projectId",
           nativeQuery = true)
    long countTermsByProjectFiles(@Param("projectId") UUID projectId);

    @Query(value = "SELECT COUNT(DISTINCT t.id) FROM glossary_terms t " +
                   "INNER JOIN glossary_term_documents gtd ON t.id = gtd.term_id " +
                   "INNER JOIN project_files pf ON gtd.file_id = pf.file_id " +
                   "WHERE pf.project_id = :projectId AND t.is_verified = :isVerified",
           nativeQuery = true)
    long countTermsByProjectFilesAndIsVerified(
        @Param("projectId") UUID projectId,
        @Param("isVerified") boolean isVerified
    );

    @Query(value = "SELECT COUNT(DISTINCT t.id) FROM glossary_terms t " +
                   "INNER JOIN glossary_term_documents gtd ON t.id = gtd.term_id " +
                   "INNER JOIN project_files pf ON gtd.file_id = pf.file_id " +
                   "WHERE pf.project_id = :projectId AND t.status = :status",
           nativeQuery = true)
    long countTermsByProjectFilesAndStatus(
        @Param("projectId") UUID projectId,
        @Param("status") String status
    );

    // Document-level queries (filtered by source file)
    @Query(value = "SELECT DISTINCT t.* FROM glossary_terms t " +
                   "INNER JOIN glossary_term_documents gtd ON t.id = gtd.term_id " +
                   "WHERE gtd.file_id = :fileId",
           countQuery = "SELECT COUNT(DISTINCT t.id) FROM glossary_terms t " +
                        "INNER JOIN glossary_term_documents gtd ON t.id = gtd.term_id " +
                        "WHERE gtd.file_id = :fileId",
           nativeQuery = true)
    Page<GlossaryTerm> findBySourceFileId(
        @Param("fileId") UUID fileId,
        Pageable pageable
    );

    @Query(value = "SELECT DISTINCT t.* FROM glossary_terms t " +
                   "INNER JOIN glossary_term_documents gtd ON t.id = gtd.term_id " +
                   "WHERE gtd.file_id = :fileId " +
                   "AND (LOWER(t.korean_term) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(t.english_term) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(t.definition) LIKE LOWER(CONCAT('%', :query, '%')))",
           countQuery = "SELECT COUNT(DISTINCT t.id) FROM glossary_terms t " +
                        "INNER JOIN glossary_term_documents gtd ON t.id = gtd.term_id " +
                        "WHERE gtd.file_id = :fileId " +
                        "AND (LOWER(t.korean_term) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(t.english_term) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(t.definition) LIKE LOWER(CONCAT('%', :query, '%')))",
           nativeQuery = true)
    Page<GlossaryTerm> searchBySourceFileIdAndQuery(
        @Param("fileId") UUID fileId,
        @Param("query") String query,
        Pageable pageable
    );
}
