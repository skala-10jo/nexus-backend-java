package com.nexus.backend.service;

import com.nexus.backend.dto.request.ProjectRequest;
import com.nexus.backend.dto.response.ProjectResponse;
import com.nexus.backend.dto.response.ScheduleResponse;
import com.nexus.backend.entity.Document;
import com.nexus.backend.entity.Project;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.DocumentRepository;
import com.nexus.backend.repository.ProjectRepository;
import com.nexus.backend.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final ScheduleRepository scheduleRepository;
    private final com.nexus.backend.repository.GlossaryTermRepository glossaryTermRepository;

    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects(User user) {
        return projectRepository.findByUserId(user.getId())
                .stream()
                .filter(project -> !"DELETED".equals(project.getStatus())) // Exclude deleted projects only
                .map(ProjectResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, User user) {
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request, User user) {
        // Build project
        Project project = Project.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .status("ACTIVE")
                .documents(new ArrayList<>())
                .build();

        // Add documents if documentIds provided
        if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
            List<Document> documents = documentRepository.findAllById(request.getDocumentIds());
            // Verify all documents belong to the user
            documents.forEach(doc -> {
                if (!doc.getUser().getId().equals(user.getId())) {
                    throw new RuntimeException("Document does not belong to user");
                }
            });
            project.getDocuments().addAll(documents);
        }

        project = projectRepository.save(project);
        log.info("Created project: {} for user: {} with {} documents",
                project.getId(), user.getId(), project.getDocuments().size());

        // Update project_id for all terms associated with these documents
        for (Document doc : project.getDocuments()) {
            int updatedTerms = glossaryTermRepository.updateProjectIdForDocumentTerms(project.getId(), doc.getId());
            log.info("Updated {} terms for document: {} with project_id: {}",
                    updatedTerms, doc.getId(), project.getId());
        }

        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, ProjectRequest request, User user) {
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        // Update status if provided
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        // Update documents if documentIds provided
        if (request.getDocumentIds() != null) {
            // Store old document IDs to detect changes
            List<UUID> oldDocumentIds = project.getDocuments().stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());

            project.getDocuments().clear();

            if (!request.getDocumentIds().isEmpty()) {
                List<Document> documents = documentRepository.findAllById(request.getDocumentIds());
                // Verify all documents belong to the user
                documents.forEach(doc -> {
                    if (!doc.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("Document does not belong to user");
                    }
                });
                project.getDocuments().addAll(documents);
            }

            // Find removed documents (old - new)
            List<UUID> removedDocumentIds = oldDocumentIds.stream()
                    .filter(id -> !request.getDocumentIds().contains(id))
                    .collect(Collectors.toList());

            // Find added documents (new - old)
            List<UUID> addedDocumentIds = request.getDocumentIds().stream()
                    .filter(id -> !oldDocumentIds.contains(id))
                    .collect(Collectors.toList());

            // Clear project_id for terms of removed documents
            for (UUID docId : removedDocumentIds) {
                int clearedTerms = glossaryTermRepository.clearProjectIdForDocumentTerms(docId);
                log.info("Cleared project_id for {} terms from removed document: {}", clearedTerms, docId);
            }

            // Set project_id for terms of added documents
            for (UUID docId : addedDocumentIds) {
                int updatedTerms = glossaryTermRepository.updateProjectIdForDocumentTerms(projectId, docId);
                log.info("Updated {} terms for added document: {} with project_id: {}",
                        updatedTerms, docId, projectId);
            }
        }

        project = projectRepository.save(project);
        log.info("Updated project: {} with {} documents",
                project.getId(), project.getDocuments().size());

        return ProjectResponse.from(project);
    }

    @Transactional
    public void deleteProject(UUID projectId, User user) {
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        project.setStatus("DELETED");
        projectRepository.save(project);
        log.info("Deleted project: {}", projectId);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getProjectSchedules(UUID projectId, User user) {
        // Verify project belongs to user
        projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Use scheduleRepository to fetch schedules with project information
        return scheduleRepository.findByProjectIdOrderByStartTimeAsc(projectId)
                .stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
}
