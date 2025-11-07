package com.nexus.backend.controller;

import com.nexus.backend.dto.request.ProjectRequest;
import com.nexus.backend.dto.response.ProjectResponse;
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
    public ResponseEntity<List<ProjectResponse>> getUserProjects(
            @AuthenticationPrincipal User user) {
        log.info("Getting projects for user: {}", user.getId());
        List<ProjectResponse> projects = projectService.getUserProjects(user);
        return ResponseEntity.ok(projects);
    }

    /**
     * Get project detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Getting project: {} for user: {}", id, user.getId());
        ProjectResponse project = projectService.getProject(id, user);
        return ResponseEntity.ok(project);
    }

    /**
     * Create new project
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestBody ProjectRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Creating project for user: {}", user.getId());
        ProjectResponse project = projectService.createProject(request, user);
        return ResponseEntity.ok(project);
    }

    /**
     * Update project
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID id,
            @RequestBody ProjectRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Updating project: {} for user: {}", id, user.getId());
        ProjectResponse project = projectService.updateProject(id, request, user);
        return ResponseEntity.ok(project);
    }

    /**
     * Delete project (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        log.info("Deleting project: {} for user: {}", id, user.getId());
        projectService.deleteProject(id, user);
        return ResponseEntity.noContent().build();
    }
}
