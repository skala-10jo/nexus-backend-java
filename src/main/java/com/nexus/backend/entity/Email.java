package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "emails")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "from_address", nullable = false)
    private String fromAddress;

    @Column(name = "from_name")
    private String fromName;

    @Column(name = "to_recipients", columnDefinition = "TEXT")
    private String toRecipients; // JSON array as string

    @Column(name = "cc_recipients", columnDefinition = "TEXT")
    private String ccRecipients; // JSON array as string

    @Column(name = "bcc_recipients", columnDefinition = "TEXT")
    private String bccRecipients; // JSON array as string

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "body_preview", length = 500)
    private String bodyPreview;

    @Column(name = "body_type", length = 20)
    private String bodyType; // HTML or Text

    @Column(name = "has_attachments", nullable = false)
    @Builder.Default
    private Boolean hasAttachments = false;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(length = 50)
    private String folder; // Inbox, SentItems, Drafts, etc.

    @Column(name = "received_date_time")
    private LocalDateTime receivedDateTime;

    @Column(name = "sent_date_time")
    private LocalDateTime sentDateTime;

    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EmailAttachment> attachments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "synced_at", nullable = false, updatable = false)
    private LocalDateTime syncedAt;
}
