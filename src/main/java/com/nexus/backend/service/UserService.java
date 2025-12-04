package com.nexus.backend.service;

import com.nexus.backend.dto.request.UpdateUserRequest;
import com.nexus.backend.dto.response.UserResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/png", "image/jpg", "image/jpeg", "image/gif", "image/webp"
    );
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request, User currentUser) {
        // Check if user can only update their own profile
        if (!currentUser.getId().equals(id)) {
            throw new RuntimeException("You can only update your own profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update fields if provided
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        user = userRepository.save(user);

        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(User user) {
        return UserResponse.from(user);
    }

    /**
     * Upload avatar image for user.
     *
     * @param id          user ID
     * @param file        avatar image file
     * @param currentUser authenticated user
     * @return updated user response
     */
    @Transactional
    public UserResponse uploadAvatar(UUID id, MultipartFile file, User currentUser) {
        // Check if user can only update their own avatar
        if (!currentUser.getId().equals(id)) {
            throw new RuntimeException("You can only update your own avatar");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Invalid file type. Allowed types: PNG, JPG, JPEG, GIF, WEBP");
        }

        // Validate file size
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new RuntimeException("File size exceeds maximum limit of 5MB");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete old avatar if exists and is a stored file (not external URL)
        String oldAvatarUrl = user.getAvatarUrl();
        if (oldAvatarUrl != null && !oldAvatarUrl.startsWith("http")) {
            try {
                fileStorageService.deleteFile(oldAvatarUrl);
            } catch (Exception e) {
                // Log but don't fail if old file deletion fails
            }
        }

        // Store new avatar
        String storedPath = fileStorageService.storeFile(file);

        // Update user avatar URL
        user.setAvatarUrl(storedPath);
        user = userRepository.save(user);

        return UserResponse.from(user);
    }
}
