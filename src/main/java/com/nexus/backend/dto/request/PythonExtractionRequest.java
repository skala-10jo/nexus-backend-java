package com.nexus.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PythonExtractionRequest {

    @JsonProperty("job_id")
    private UUID jobId;

    @JsonProperty("file_id")
    private UUID fileId;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("project_id")
    private UUID projectId;
}
