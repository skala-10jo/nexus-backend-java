package com.nexus.backend.dto.response;

import com.nexus.backend.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID id;
    private String name;
    private String description;
    private String status;
    private Integer documentCount;
    private Integer termCount;
    private List<UUID> documentIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .documentCount(project.getDocuments() != null ? project.getDocuments().size() : 0)
                .termCount(project.getGlossaryTerms() != null ? project.getGlossaryTerms().size() : 0)
                .documentIds(project.getDocuments() != null ?
                        project.getDocuments().stream()
                                .map(doc -> doc.getId())
                                .collect(Collectors.toList()) :
                        List.of())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
