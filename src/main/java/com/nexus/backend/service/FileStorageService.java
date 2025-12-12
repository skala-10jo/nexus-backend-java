package com.nexus.backend.service;

import com.nexus.backend.exception.ResourceNotFoundException;
import com.nexus.backend.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadPath;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException ex) {
            throw new ServiceException("Could not create upload directory", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        try {
            // Generate UUID-based filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String storedFilename = UUID.randomUUID().toString() + fileExtension;

            // Create date-based directory structure (yyyy/MM/dd)
            LocalDate today = LocalDate.now();
            String dateFolder = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path datePath = this.uploadPath.resolve(dateFolder);
            Files.createDirectories(datePath);

            // Store file
            Path targetLocation = datePath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path
            return dateFolder + "/" + storedFilename;
        } catch (IOException ex) {
            throw new ServiceException("Could not store file. Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String filePath) {
        try {
            Path file = this.uploadPath.resolve(filePath).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + filePath);
            }
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResourceNotFoundException("File not found: " + filePath);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Path file = this.uploadPath.resolve(filePath).normalize();
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new ServiceException("Could not delete file: " + filePath, ex);
        }
    }

    public Path getFilePath(String filePath) {
        return this.uploadPath.resolve(filePath).normalize();
    }
}
