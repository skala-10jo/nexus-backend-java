package com.nexus.backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Expression Entity
 * 비즈니스 영어 표현 마스터 데이터
 *
 * @author NEXUS Team
 * @since 2025-01-21
 */
@Entity
@Table(name = "expressions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expression {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "expression", nullable = false, unique = true, length = 500)
    private String expression;  // 영어 표현

    @Column(name = "meaning", nullable = false, length = 500)
    private String meaning;  // 한국어 의미

    @Column(name = "examples", nullable = false, columnDefinition = "jsonb")
    private String examples;  // 예문 배열 (JSON 형식)

    @Column(name = "unit", nullable = false, length = 200)
    private String unit;  // 단원

    @Column(name = "chapter", nullable = false, length = 200)
    private String chapter;  // 챕터

    @Column(name = "source_section", length = 200)
    private String sourceSection;  // 출처 섹션
}
