package com.nexus.backend.repository;

import com.nexus.backend.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, UUID> {

    Optional<DocumentMetadata> findByDocumentId(UUID documentId);

    void deleteByDocumentId(UUID documentId);
}
