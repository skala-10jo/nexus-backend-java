package com.nexus.backend.service;

import com.nexus.backend.dto.request.ProjectRequest;
import com.nexus.backend.dto.response.ProjectResponse;
import com.nexus.backend.dto.response.ScheduleResponse;
import com.nexus.backend.entity.File;
import com.nexus.backend.entity.Project;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.FileRepository;
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
    private final FileRepository fileRepository;
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
                .files(new ArrayList<>())
                .build();

        // Add files if documentIds provided (keeping parameter name for backwards compatibility)
        if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
            List<File> files = fileRepository.findAllById(request.getDocumentIds());
            // Verify all files belong to the user
            files.forEach(file -> {
                if (!file.getUser().getId().equals(user.getId())) {
                    throw new RuntimeException("File does not belong to user");
                }
            });
            project.getFiles().addAll(files);
        }

        project = projectRepository.save(project);
        log.info("Created project: {} for user: {} with {} files",
                project.getId(), user.getId(), project.getFiles().size());

        // Update project_id for all terms associated with these files
        for (File file : project.getFiles()) {
            int updatedTerms = glossaryTermRepository.updateProjectIdForDocumentTerms(project.getId(), file.getId());
            log.info("Updated {} terms for file: {} with project_id: {}",
                    updatedTerms, file.getId(), project.getId());
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

        // Update files if documentIds provided (keeping parameter name for backwards compatibility)
        if (request.getDocumentIds() != null) {
            // Store old file IDs to detect changes
            List<UUID> oldFileIds = project.getFiles().stream()
                    .map(File::getId)
                    .collect(Collectors.toList());

            project.getFiles().clear();

            if (!request.getDocumentIds().isEmpty()) {
                List<File> files = fileRepository.findAllById(request.getDocumentIds());
                // Verify all files belong to the user
                files.forEach(file -> {
                    if (!file.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("File does not belong to user");
                    }
                });
                project.getFiles().addAll(files);
            }

            // Find removed files (old - new)
            List<UUID> removedFileIds = oldFileIds.stream()
                    .filter(id -> !request.getDocumentIds().contains(id))
                    .collect(Collectors.toList());

            // Find added files (new - old)
            List<UUID> addedFileIds = request.getDocumentIds().stream()
                    .filter(id -> !oldFileIds.contains(id))
                    .collect(Collectors.toList());

            // Clear project_id for terms of removed files
            for (UUID fileId : removedFileIds) {
                int clearedTerms = glossaryTermRepository.clearProjectIdForDocumentTerms(fileId);
                log.info("Cleared project_id for {} terms from removed file: {}", clearedTerms, fileId);
            }

            // Set project_id for terms of added files
            for (UUID fileId : addedFileIds) {
                int updatedTerms = glossaryTermRepository.updateProjectIdForDocumentTerms(projectId, fileId);
                log.info("Updated {} terms for added file: {} with project_id: {}",
                        updatedTerms, fileId, projectId);
            }
        }

        project = projectRepository.save(project);
        log.info("Updated project: {} with {} files",
                project.getId(), project.getFiles().size());

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
