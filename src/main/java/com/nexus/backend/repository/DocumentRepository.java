package com.nexus.backend.repository;

import com.nexus.backend.entity.Document;
import com.nexus.backend.entity.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUserId(UUID userId);

    Page<Document> findByUserId(UUID userId, Pageable pageable);

    List<Document> findByUserIdAndStatus(UUID userId, DocumentStatus status);

    List<Document> findByUserIdAndFileType(UUID userId, String fileType);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT d FROM Document d WHERE d.user.id = :userId AND " +
           "(LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.fileType) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Document> searchDocuments(@Param("userId") UUID userId,
                                   @Param("query") String query,
                                   Pageable pageable);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
