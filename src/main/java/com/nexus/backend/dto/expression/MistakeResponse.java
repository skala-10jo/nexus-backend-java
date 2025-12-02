package com.nexus.backend.dto.expression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.backend.entity.Expression;
import com.nexus.backend.entity.UserExpressionQuizResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Mistake Response DTO
 * 오답노트 응답
 *
 * @author NEXUS Team
 * @since 2025-12-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeResponse {

    private UUID id;  // quiz result id
    private UUID expressionId;
    private String expression;
    private String meaning;
    private Integer exampleIndex;
    private String exampleText;
    private String exampleTranslation;
    private String unit;
    private String chapter;
    private Integer correctCount;
    private Integer incorrectCount;
    private Integer totalAttempts;
    private ZonedDateTime lastAttemptedAt;

    /**
     * Entity -> DTO 변환
     */
    public static MistakeResponse from(UserExpressionQuizResult result) {
        Expression expr = result.getExpression();

        // examples JSON에서 해당 인덱스의 예문 추출
        String exampleText = "";
        String exampleTranslation = "";

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(expr.getExamples());
            if (jsonNode.isArray() && jsonNode.size() > result.getExampleIndex()) {
                JsonNode exampleNode = jsonNode.get(result.getExampleIndex());
                exampleText = exampleNode.has("text") ? exampleNode.get("text").asText() : "";
                exampleTranslation = exampleNode.has("translation") ? exampleNode.get("translation").asText() : "";
            }
        } catch (Exception e) {
            // JSON 파싱 실패 시 빈 문자열
        }

        return MistakeResponse.builder()
                .id(result.getId())
                .expressionId(expr.getId())
                .expression(expr.getExpression())
                .meaning(expr.getMeaning())
                .exampleIndex(result.getExampleIndex())
                .exampleText(exampleText)
                .exampleTranslation(exampleTranslation)
                .unit(expr.getUnit())
                .chapter(expr.getChapter())
                .correctCount(result.getCorrectCount())
                .incorrectCount(result.getIncorrectCount())
                .totalAttempts(result.getTotalAttempts())
                .lastAttemptedAt(result.getLastAttemptedAt())
                .build();
    }
}
