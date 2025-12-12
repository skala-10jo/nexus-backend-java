package com.nexus.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkLearnedRequest {

    @NotEmpty(message = "expressionIds는 필수입니다")
    private List<UUID> expressionIds;
}
