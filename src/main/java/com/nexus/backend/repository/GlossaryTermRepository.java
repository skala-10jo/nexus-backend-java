package com.nexus.backend.repository;

import com.nexus.backend.entity.GlossaryTerm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    // Update project_id for terms associated with a document
    @Modifying
    @Query("UPDATE GlossaryTerm t SET t.project.id = :projectId " +
           "WHERE EXISTS (SELECT 1 FROM t.documents d WHERE d.id = :documentId)")
    int updateProjectIdForDocumentTerms(@Param("projectId") UUID projectId, @Param("documentId") UUID documentId);

    // Clear project_id for terms associated with a document
    @Modifying
    @Query("UPDATE GlossaryTerm t SET t.project.id = NULL " +
           "WHERE EXISTS (SELECT 1 FROM t.documents d WHERE d.id = :documentId)")
    int clearProjectIdForDocumentTerms(@Param("documentId") UUID documentId);
}
