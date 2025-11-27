package com.nexus.backend.repository;

import com.nexus.backend.entity.Email;
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
public interface EmailRepository extends JpaRepository<Email, UUID> {

    // Find by message ID (unique Microsoft Graph ID)
    Optional<Email> findByMessageId(String messageId);

    boolean existsByMessageId(String messageId);

    // Check if email exists for specific user
    boolean existsByMessageIdAndUserId(String messageId, UUID userId);

    // Find by user
    Page<Email> findByUserId(UUID userId, Pageable pageable);

    List<Email> findByUserId(UUID userId);

    Optional<Email> findByIdAndUserId(UUID id, UUID userId);

    // Delete all emails by user
    void deleteByUserId(UUID userId);

    // Find by project
    Page<Email> findByProjectId(UUID projectId, Pageable pageable);

    Page<Email> findByUserIdAndProjectId(UUID userId, UUID projectId, Pageable pageable);

    // Find by folder
    Page<Email> findByUserIdAndFolder(UUID userId, String folder, Pageable pageable);

    // Find unread emails
    Page<Email> findByUserIdAndIsRead(UUID userId, Boolean isRead, Pageable pageable);

    // Count unread emails
    long countByUserIdAndIsRead(UUID userId, Boolean isRead);

    // Search emails
    @Query("SELECT e FROM Email e WHERE e.user.id = :userId AND " +
           "(LOWER(e.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.fromAddress) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.fromName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.body) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Email> searchEmails(@Param("userId") UUID userId,
                             @Param("query") String query,
                             Pageable pageable);

    // Advanced search with folder and project filters
    @Query("SELECT e FROM Email e WHERE e.user.id = :userId " +
           "AND (:folder IS NULL OR e.folder = :folder) " +
           "AND (:projectId IS NULL OR e.project.id = :projectId) " +
           "AND (LOWER(e.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.fromAddress) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.fromName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Email> advancedSearch(@Param("userId") UUID userId,
                               @Param("query") String query,
                               @Param("folder") String folder,
                               @Param("projectId") UUID projectId,
                               Pageable pageable);

    // Get messageIds for specific user and folder (for sync deletion detection)
    @Query("SELECT e.messageId FROM Email e WHERE e.user.id = :userId AND e.folder = :folder")
    List<String> findMessageIdsByUserIdAndFolder(@Param("userId") UUID userId,
                                                   @Param("folder") String folder);

    // Bulk delete by messageIds and userId (한 번에 여러 개 삭제)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Email e WHERE e.messageId IN :messageIds AND e.user.id = :userId")
    void deleteByMessageIdsAndUserId(@Param("messageIds") List<String> messageIds,
                                       @Param("userId") UUID userId);
}
