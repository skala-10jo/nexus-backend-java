package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    private Email email;

    @Column(name = "attachment_id", nullable = false)
    private String attachmentId; // Microsoft Graph attachment ID

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long size; // in bytes

    @Column(name = "is_inline")
    private Boolean isInline = false;

    @Column(name = "content_id")
    private String contentId;

    @Column(name = "file_path")
    private String filePath; // Local storage path if downloaded

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
