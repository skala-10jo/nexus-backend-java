package com.nexus.backend.repository;

import com.nexus.backend.entity.SlackIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlackIntegrationRepository extends JpaRepository<SlackIntegration, UUID> {

    List<SlackIntegration> findByUserId(UUID userId);

    Optional<SlackIntegration> findByIdAndUserId(UUID id, UUID userId);

    Optional<SlackIntegration> findByUserIdAndWorkspaceId(UUID userId, String workspaceId);

    List<SlackIntegration> findByUserIdAndIsActive(UUID userId, Boolean isActive);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
