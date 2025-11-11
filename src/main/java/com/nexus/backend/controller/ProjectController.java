package com.nexus.backend.controller;

import com.nexus.backend.dto.request.ProjectRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.ProjectResponse;
import com.nexus.backend.dto.response.ScheduleResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Get all projects for current user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getUserProjects(
            @AuthenticationPrincipal User user) {
        log.info("Getting projects for user: {}", user.getId());
        List<ProjectResponse> projects = projectService.getUserProjects(user);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    /**
     * Get project detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Getting project: {} for user: {}", id, user.getId());
        ProjectResponse project = projectService.getProject(id, user);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    /**
     * Create new project
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @RequestBody ProjectRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Creating project for user: {}", user.getId());
        ProjectResponse project = projectService.createProject(request, user);
        return ResponseEntity.ok(ApiResponse.success("프로젝트가 생성되었습니다.", project));
    }

    /**
     * Update project
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable UUID id,
            @RequestBody ProjectRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Updating project: {} for user: {}", id, user.getId());
        ProjectResponse project = projectService.updateProject(id, request, user);
        return ResponseEntity.ok(ApiResponse.success("프로젝트가 수정되었습니다.", project));
    }

    /**
     * Delete project (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Deleting project: {} for user: {}", id, user.getId());
        projectService.deleteProject(id, user);
        return ResponseEntity.ok(ApiResponse.success("프로젝트가 삭제되었습니다.", null));
    }

    /**
     * Get all schedules for a project
     */
    @GetMapping("/{id}/schedules")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getProjectSchedules(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Getting schedules for project: {} for user: {}", id, user.getId());
        List<ScheduleResponse> schedules = projectService.getProjectSchedules(id, user);
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }
}
