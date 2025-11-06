package com.nexus.backend.controller;

import com.nexus.backend.dto.request.LoginRequest;
import com.nexus.backend.dto.request.RegisterRequest;
import com.nexus.backend.dto.response.ApiResponse;
import com.nexus.backend.dto.response.AuthResponse;
import com.nexus.backend.dto.response.UserResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.service.AuthService;
import com.nexus.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse authResponse = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "User registered successfully",
                        Map.of(
                                "user", authResponse.getUser(),
                                "token", authResponse.getToken()
                        )
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successful",
                        Map.of(
                                "user", authResponse.getUser(),
                                "token", authResponse.getToken()
                        )
                ));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, UserResponse>>> getCurrentUser(
            @AuthenticationPrincipal User user) {

        UserResponse userResponse = userService.getCurrentUser(user);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        Map.of("user", userResponse)
                ));
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkUsername(
            @RequestParam String username) {

        boolean exists = authService.existsByUsername(username);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        Map.of("exists", exists)
                ));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(
            @RequestParam String email) {

        boolean exists = authService.existsByEmail(email);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        Map.of("exists", exists)
                ));
    }
}
