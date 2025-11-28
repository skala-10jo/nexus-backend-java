package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * UserExpression Entity
 * 사용자별 표현 학습 여부
 *
 * @author NEXUS Team
 * @since 2025-01-21
 */
@Entity
@Table(
    name = "user_expressions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_expressions",
        columnNames = {"user_id", "expression_id"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExpression {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expression_id", nullable = false)
    private Expression expression;

    @Column(name = "is_learned", nullable = false)
    @Builder.Default
    private Boolean isLearned = false;
}
