package com.langapp.backend.controller;

import com.langapp.backend.dto.request.LoginRequest;
import com.langapp.backend.dto.request.RegisterRequest;
import com.langapp.backend.dto.response.ApiResponse;
import com.langapp.backend.dto.response.AuthResponse;
import com.langapp.backend.dto.response.UserResponse;
import com.langapp.backend.entity.User;
import com.langapp.backend.service.AuthService;
import com.langapp.backend.service.UserService;
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
}
