package com.nexus.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultRequest {

    @NotNull(message = "표현 ID는 필수입니다")
    private UUID expressionId;

    @NotNull(message = "예문 인덱스는 필수입니다")
    private Integer exampleIndex;

    @NotNull(message = "정답 여부는 필수입니다")
    private Boolean isCorrect;
}
