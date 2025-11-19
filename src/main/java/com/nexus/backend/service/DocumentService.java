package com.nexus.backend.service;

import com.nexus.backend.dto.response.*;
import com.nexus.backend.entity.*;
import com.nexus.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final Tika tika = new Tika();

    @Value("${file.max-size}")
    private long maxFileSize;

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            // Video formats
            "video/mp4",
            "video/x-msvideo",      // AVI
            "video/quicktime",       // MOV
            "video/x-matroska"       // MKV
    );

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, User user) {
        // Validate file
        validateFile(file);

        try {
            // Detect MIME type
            String mimeType = tika.detect(file.getInputStream());
            if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
                throw new RuntimeException("Unsupported file type: " + mimeType);
            }

            // Store file
            String filePath = fileStorageService.storeFile(file);

            // Extract file type
            String fileType = extractFileType(file.getOriginalFilename());

            // Create document entity
            Document document = Document.builder()
                    .user(user)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(filePath.substring(filePath.lastIndexOf("/") + 1))
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .fileType(fileType)
                    .mimeType(mimeType)
                    .uploadDate(LocalDateTime.now())
                    .status(DocumentStatus.PROCESSED)  // Document is ready to use
                    .isAnalyzed(false)
                    .build();

            document = documentRepository.save(document);

            return mapToResponse(document);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload document", ex);
        }
    }

    public Page<DocumentResponse> getDocuments(UUID userId, Pageable pageable) {
        Page<Document> documents = documentRepository.findByUserId(userId, pageable);
        return documents.map(this::mapToResponse);
    }

    public DocumentDetailResponse getDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        return mapToDetailResponse(document);
    }

    public Resource downloadDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        return fileStorageService.loadFileAsResource(document.getFilePath());
    }

    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        String filePath = document.getFilePath();

        // Delete from database (CASCADE will handle project_documents relationships)
        documentRepository.delete(document);

        // Then try to delete file from storage
        // If this fails, the database record is already deleted
        try {
            fileStorageService.deleteFile(filePath);
        } catch (Exception ex) {
            // Log the error but don't fail the operation
            System.err.println("Warning: Failed to delete file from storage: " + filePath);
        }
    }

    public Page<DocumentResponse> searchDocuments(UUID userId, String query, Pageable pageable) {
        Page<Document> documents = documentRepository.searchDocuments(userId, query, pageable);
        return documents.map(this::mapToResponse);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum limit");
        }
    }

    private String extractFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private DocumentResponse mapToResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .originalFilename(document.getOriginalFilename())
                .fileSize(document.getFileSize())
                .fileType(document.getFileType())
                .uploadDate(document.getUploadDate())
                .status(document.getStatus().name())
                .isAnalyzed(document.getIsAnalyzed())
                .build();
    }

    private DocumentDetailResponse mapToDetailResponse(Document document) {
        DocumentDetailResponse.DocumentDetailResponseBuilder builder = DocumentDetailResponse.builder()
                .id(document.getId())
                .originalFilename(document.getOriginalFilename())
                .fileSize(document.getFileSize())
                .fileType(document.getFileType())
                .mimeType(document.getMimeType())
                .uploadDate(document.getUploadDate())
                .status(document.getStatus().name())
                .isAnalyzed(document.getIsAnalyzed());

        if (document.getMetadata() != null) {
            builder.metadata(mapToMetadataDto(document.getMetadata()));
        }

        return builder.build();
    }

    private DocumentMetadataDto mapToMetadataDto(DocumentMetadata metadata) {
        return DocumentMetadataDto.builder()
                .language(metadata.getLanguage())
                .pageCount(metadata.getPageCount())
                .wordCount(metadata.getWordCount())
                .characterCount(metadata.getCharacterCount())
                .build();
    }
}
