package com.nexus.backend.repository;

import com.nexus.backend.entity.EmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailAttachmentRepository extends JpaRepository<EmailAttachment, UUID> {

    List<EmailAttachment> findByEmailId(UUID emailId);

    void deleteByEmailId(UUID emailId);
}
