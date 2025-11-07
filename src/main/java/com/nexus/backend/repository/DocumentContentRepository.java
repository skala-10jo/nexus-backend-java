package com.nexus.backend.repository;

import com.nexus.backend.entity.DocumentContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentContentRepository extends JpaRepository<DocumentContent, UUID> {

    List<DocumentContent> findByDocumentIdOrderByPageNumber(UUID documentId);

    Optional<DocumentContent> findByDocumentIdAndPageNumber(UUID documentId, Integer pageNumber);

    void deleteByDocumentId(UUID documentId);
}
