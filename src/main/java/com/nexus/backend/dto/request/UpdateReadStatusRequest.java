package com.nexus.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReadStatusRequest {

    @NotNull(message = "읽음 상태는 필수입니다")
    private Boolean isRead;
}
