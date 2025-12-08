package com.nexus.backend.controller;

import com.nexus.backend.dto.request.UpdateUserRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.UserResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, UserResponse>>> getUserById(
            @PathVariable UUID id) {

        UserResponse userResponse = userService.getUserById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        Map.of("user", userResponse)
                ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, UserResponse>>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal User currentUser) {

        UserResponse userResponse = userService.updateUser(id, request, currentUser);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Profile updated successfully",
                        Map.of("user", userResponse)
                ));
    }

    /**
     * Upload user avatar image.
     * POST /api/users/{id}/avatar
     *
     * @param id          user ID
     * @param file        avatar image file (PNG, JPG, JPEG, GIF, WEBP)
     * @param currentUser authenticated user
     * @return updated user with new avatar URL
     */
    @PostMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<Map<String, UserResponse>>> uploadAvatar(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {

        log.info("Avatar upload request: userId={}, filename={}", id, file.getOriginalFilename());

        UserResponse userResponse = userService.uploadAvatar(id, file, currentUser);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Avatar uploaded successfully",
                        Map.of("user", userResponse)
                ));
    }

    /**
     * Update user's preferred language for Slack translation.
     * PATCH /api/users/{id}/preferred-language
     *
     * @param id          user ID
     * @param request     Map containing "language" key (ko, en, ja, vi, zh)
     * @param currentUser authenticated user
     * @return updated user response
     */
    @PatchMapping("/{id}/preferred-language")
    public ResponseEntity<ApiResponse<Map<String, UserResponse>>> updatePreferredLanguage(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User currentUser) {

        String language = request.get("language");
        log.info("Preferred language update request: userId={}, language={}", id, language);

        UserResponse userResponse = userService.updatePreferredLanguage(id, language, currentUser);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Preferred language updated successfully",
                        Map.of("user", userResponse)
                ));
    }
}
