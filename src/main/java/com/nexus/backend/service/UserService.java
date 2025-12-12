package com.nexus.backend.service;

import com.nexus.backend.dto.request.UpdateUserRequest;
import com.nexus.backend.dto.response.UserResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.exception.BadRequestException;
import com.nexus.backend.exception.ResourceNotFoundException;
import com.nexus.backend.exception.UnauthorizedException;
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
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request, User currentUser) {
        // Check if user can only update their own profile
        if (!currentUser.getId().equals(id)) {
            throw new UnauthorizedException("You can only update your own profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

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

        if (request.getPreferredLanguage() != null) {
            user.setPreferredLanguage(request.getPreferredLanguage().toLowerCase());
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
            throw new UnauthorizedException("You can only update your own avatar");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Allowed types: PNG, JPG, JPEG, GIF, WEBP");
        }

        // Validate file size
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BadRequestException("File size exceeds maximum limit of 5MB");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

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

    /**
     * Update user's preferred language for Slack translation.
     *
     * @param id          user ID
     * @param language    preferred language code (ko, en, ja, vi, zh)
     * @param currentUser authenticated user
     * @return updated user response
     */
    @Transactional
    public UserResponse updatePreferredLanguage(UUID id, String language, User currentUser) {
        // Check if user can only update their own preference
        if (!currentUser.getId().equals(id)) {
            throw new UnauthorizedException("You can only update your own preferences");
        }

        // Validate language code
        List<String> validLanguages = Arrays.asList("ko", "en", "ja", "vi", "zh");
        if (language == null || !validLanguages.contains(language.toLowerCase())) {
            throw new BadRequestException("Invalid language code. Allowed: ko, en, ja, vi, zh");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setPreferredLanguage(language.toLowerCase());
        user = userRepository.save(user);

        return UserResponse.from(user);
    }
}
