package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSubtitleResponse {

    /**
     * 자막 ID
     */
    private UUID id;

    /**
     * 시퀀스 번호 (순서)
     */
    private Integer sequenceNumber;

    /**
     * 시작 시간 (밀리초)
     */
    private Long startTimeMs;

    /**
     * 종료 시간 (밀리초)
     */
    private Long endTimeMs;

    /**
     * 원본 텍스트 (STT 결과)
     */
    private String originalText;

    /**
     * 번역된 텍스트
     */
    private String translatedText;

    /**
     * 화자 ID (선택사항)
     */
    private Integer speakerId;

    /**
     * STT 신뢰도 점수 (0.00 ~ 1.00)
     */
    private BigDecimal confidenceScore;

    /**
     * 탐지된 전문용어 (JSONB)
     * 예: {"term": "API", "translation": "에이피아이", "source": "glossary_doc_123"}
     */
    private Map<String, Object> detectedTerms;
}
