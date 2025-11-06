package com.nexus.backend.controller;

import com.nexus.backend.dto.request.UpdateUserRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.UserResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
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
}
