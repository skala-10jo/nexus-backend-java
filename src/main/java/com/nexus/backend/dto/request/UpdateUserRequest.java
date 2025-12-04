package com.nexus.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    private String avatarUrl;

    @Size(max = 100, message = "Role must be less than 100 characters")
    private String role;
}
