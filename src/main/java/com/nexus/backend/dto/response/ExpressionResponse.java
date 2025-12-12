package com.nexus.backend.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.backend.entity.Expression;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressionResponse {

    private UUID id;
    private String expression;
    private String meaning;
    private List<ExampleSentence> examples;
    private String unit;
    private String chapter;
    private Boolean isLearned;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExampleSentence {
        private String text;
        private String translation;
    }

    /**
     * Entity -> DTO 변환
     */
    public static ExpressionResponse from(Expression expression, Boolean isLearned) {
        ObjectMapper mapper = new ObjectMapper();
        List<ExampleSentence> examples = new ArrayList<>();

        try {
            JsonNode jsonNode = mapper.readTree(expression.getExamples());
            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    examples.add(ExampleSentence.builder()
                            .text(node.get("text").asText())
                            .translation(node.get("translation").asText())
                            .build());
                }
            }
        } catch (Exception e) {
            // JSON 파싱 실패 시 빈 리스트
        }

        return ExpressionResponse.builder()
                .id(expression.getId())
                .expression(expression.getExpression())
                .meaning(expression.getMeaning())
                .examples(examples)
                .unit(expression.getUnit())
                .chapter(expression.getChapter())
                .isLearned(isLearned)
                .build();
    }
}
