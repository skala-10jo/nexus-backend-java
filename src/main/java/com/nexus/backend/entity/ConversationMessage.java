package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversation_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ConversationSession session;

    @Column(nullable = false, length = 10)
    private String sender;

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "translated_text", columnDefinition = "TEXT")
    private String translatedText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detected_terms", columnDefinition = "jsonb")
    private List<String> detectedTerms;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
