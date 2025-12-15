package com.nexus.backend.service;

import com.nexus.backend.dto.request.ProjectRequest;
import com.nexus.backend.dto.response.ProjectResponse;
import com.nexus.backend.dto.response.ScheduleResponse;
import com.nexus.backend.entity.File;
import com.nexus.backend.entity.Project;
import com.nexus.backend.entity.User;
import com.nexus.backend.exception.ResourceNotFoundException;
import com.nexus.backend.exception.UnauthorizedException;
import com.nexus.backend.repository.FileRepository;
import com.nexus.backend.repository.GlossaryTermRepository;
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
    private final GlossaryTermRepository glossaryTermRepository;
    private final ProjectCategorySyncService syncService;

    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects(User user) {
        return projectRepository.findByUserId(user.getId())
                .stream()
                .filter(project -> !"DELETED".equals(project.getStatus())) // Exclude deleted projects only
                .map(project -> {
                    ProjectResponse response = ProjectResponse.from(project);
                    // Use project files approach to count terms (handles project_id = NULL case)
                    long actualTermCount = glossaryTermRepository.countTermsByProjectFiles(project.getId());
                    response.setTermCount((int) actualTermCount);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, User user) {
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        ProjectResponse response = ProjectResponse.from(project);
        // Use project files approach to count terms (handles project_id = NULL case)
        long actualTermCount = glossaryTermRepository.countTermsByProjectFiles(project.getId());
        response.setTermCount((int) actualTermCount);
        return response;
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

        // Save project first to get the ID
        project = projectRepository.save(project);

        // Add files if documentIds provided (keeping parameter name for backwards compatibility)
        // IMPORTANT: File is the owner of the Many-to-Many relationship, so we must add the project to each file
        if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
            List<File> files = fileRepository.findAllById(request.getDocumentIds());
            // Verify all files belong to the user
            for (File file : files) {
                if (!file.getUser().getId().equals(user.getId())) {
                    throw new UnauthorizedException("File does not belong to user");
                }
                // Add project to file (File is the owner)
                if (!file.getProjects().contains(project)) {
                    file.getProjects().add(project);
                }
            }
            // Save files to persist the relationship
            fileRepository.saveAll(files);
        }

        // Refresh project to get updated files list
        UUID projectId = project.getId();
        project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        log.info("Created project: {} for user: {} with {} files",
                project.getId(), user.getId(), project.getFiles().size());

        // 카테고리 동기화: 프로젝트 생성 시 동일 이름의 카테고리 자동 생성
        syncService.onProjectCreated(user.getId(), request.getName());

        ProjectResponse response = ProjectResponse.from(project);
        // Use project files approach to count terms (handles project_id = NULL case)
        long actualTermCount = glossaryTermRepository.countTermsByProjectFiles(project.getId());
        response.setTermCount((int) actualTermCount);
        return response;
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, ProjectRequest request, User user) {
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // 이름 변경 시 카테고리 동기화를 위해 이전 이름 저장
        String oldName = project.getName();

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        // Update status if provided
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        projectRepository.save(project);

        // Update files if documentIds provided (keeping parameter name for backwards compatibility)
        // IMPORTANT: File is the owner of the Many-to-Many relationship
        if (request.getDocumentIds() != null) {
            // Get current file IDs
            List<UUID> oldFileIds = project.getFiles().stream()
                    .map(File::getId)
                    .collect(Collectors.toList());

            // Find removed files (old - new)
            List<UUID> removedFileIds = oldFileIds.stream()
                    .filter(id -> !request.getDocumentIds().contains(id))
                    .collect(Collectors.toList());

            // Find added files (new - old)
            List<UUID> addedFileIds = request.getDocumentIds().stream()
                    .filter(id -> !oldFileIds.contains(id))
                    .collect(Collectors.toList());

            log.info("Updating project files - Removed: {}, Added: {}",
                    removedFileIds.size(), addedFileIds.size());

            // Remove project from files that are being removed
            if (!removedFileIds.isEmpty()) {
                List<File> removedFiles = fileRepository.findAllById(removedFileIds);
                for (File file : removedFiles) {
                    file.getProjects().remove(project);
                }
                fileRepository.saveAll(removedFiles);
            }

            // Add project to files that are being added
            if (!addedFileIds.isEmpty()) {
                List<File> addedFiles = fileRepository.findAllById(addedFileIds);
                for (File file : addedFiles) {
                    // Verify file belongs to user
                    if (!file.getUser().getId().equals(user.getId())) {
                        throw new UnauthorizedException("File does not belong to user");
                    }
                    // Add project to file (File is the owner)
                    if (!file.getProjects().contains(project)) {
                        file.getProjects().add(project);
                    }
                }
                fileRepository.saveAll(addedFiles);
            }
        }

        // Refresh project to get updated files list
        project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        log.info("Updated project: {} with {} files",
                project.getId(), project.getFiles().size());

        // 카테고리 동기화: 프로젝트 이름 변경 시 동일 이름의 카테고리 이름 변경
        syncService.onProjectUpdated(user.getId(), oldName, request.getName());

        ProjectResponse response = ProjectResponse.from(project);
        // Use project files approach to count terms (handles project_id = NULL case)
        long actualTermCount = glossaryTermRepository.countTermsByProjectFiles(project.getId());
        response.setTermCount((int) actualTermCount);
        return response;
    }

    @Transactional
    public void deleteProject(UUID projectId, User user) {
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // 카테고리 동기화를 위해 프로젝트 이름 저장
        String projectName = project.getName();

        project.setStatus("DELETED");
        projectRepository.save(project);
        log.info("Deleted project: {}", projectId);

        // 카테고리 동기화: 프로젝트 삭제 시 동일 이름의 카테고리 삭제
        syncService.onProjectDeleted(user.getId(), projectName);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getProjectSchedules(UUID projectId, User user) {
        // Verify project belongs to user
        projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Use scheduleRepository to fetch schedules with project information
        return scheduleRepository.findByProjectIdOrderByStartTimeAsc(projectId)
                .stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
}
