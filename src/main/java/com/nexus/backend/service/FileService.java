package com.nexus.backend.service;

import com.nexus.backend.dto.request.VideoUploadRequest;
import com.nexus.backend.dto.response.FileDetailResponse;
import com.nexus.backend.dto.response.FileResponse;
import com.nexus.backend.entity.*;
import com.nexus.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing files (documents and videos).
 *
 * Unified file management service supporting:
 * - Document uploads (PDF, DOCX, XLSX, TXT)
 * - Video uploads with metadata (source/target language)
 * - File retrieval and deletion
 *
 * @author NEXUS Team
 * @version 2.0
 * @since 2025-01-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    // Repositories
    private final FileRepository fileRepository;
    private final DocumentFileRepository documentFileRepository;
    private final VideoFileRepository videoFileRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final DocumentContentRepository documentContentRepository;
    private final GlossaryExtractionJobRepository glossaryExtractionJobRepository;
    private final VideoTranslationGlossaryRepository videoTranslationGlossaryRepository;

    // Services
    private final FileStorageService fileStorageService;

    // Utils
    private final Tika tika = new Tika();

    /**
     * Upload a document file.
     *
     * @param file the file to upload
     * @param user the uploading user
     * @return FileResponse
     */
    @Transactional
    public FileResponse uploadDocument(MultipartFile file, User user) {
        log.info("Uploading document: user={}, filename={}", user.getUsername(), file.getOriginalFilename());

        try {
            // Detect MIME type
            String mimeType = tika.detect(file.getInputStream());

            // Store file
            String filePath = fileStorageService.storeFile(file);
            String storedFilename = filePath.substring(filePath.lastIndexOf("/") + 1);

            // Create file entity
            File newFile = File.builder()
                    .user(user)
                    .fileType(FileType.DOCUMENT)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(storedFilename)
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .mimeType(mimeType)
                    .uploadDate(LocalDateTime.now())
                    .status("PROCESSED")
                    .build();

            newFile = fileRepository.save(newFile);

            // Create document-specific metadata
            DocumentFile documentFile = DocumentFile.builder()
                    .file(newFile)
                    .isAnalyzed(false)
                    .build();

            documentFileRepository.save(documentFile);

            return mapToResponse(newFile, documentFile);

        } catch (Exception ex) {
            log.error("Failed to upload document", ex);
            throw new RuntimeException("Failed to upload document", ex);
        }
    }

    /**
     * Upload a video file.
     *
     * @param file    the file to upload
     * @param request video upload metadata
     * @param user    the uploading user
     * @return FileResponse
     */
    @Transactional
    public FileResponse uploadVideo(MultipartFile file, VideoUploadRequest request, User user) {
        log.info("Uploading video: user={}, filename={}", user.getUsername(), file.getOriginalFilename());

        try {
            // Detect MIME type
            String mimeType = tika.detect(file.getInputStream());

            // Store file
            String filePath = fileStorageService.storeFile(file);
            String storedFilename = filePath.substring(filePath.lastIndexOf("/") + 1);

            // Create file entity
            File newFile = File.builder()
                    .user(user)
                    .fileType(FileType.VIDEO)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(storedFilename)
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .mimeType(mimeType)
                    .uploadDate(LocalDateTime.now())
                    .status("PROCESSED")
                    .build();

            newFile = fileRepository.save(newFile);

            // Create video-specific metadata
            VideoFile videoFile = VideoFile.builder()
                    .file(newFile)
                    .sourceLanguage(request.getSourceLanguage())
                    .targetLanguage(request.getTargetLanguage())
                    .sttStatus("pending")
                    .translationStatus("pending")
                    .build();

            videoFileRepository.save(videoFile);

            return mapToVideoResponse(newFile, videoFile);

        } catch (Exception ex) {
            log.error("Failed to upload video", ex);
            throw new RuntimeException("Failed to upload video", ex);
        }
    }

    /**
     * Get documents for a user.
     *
     * @param userId   user ID
     * @param pageable pagination
     * @return paginated documents
     */
    public Page<FileResponse> getDocuments(UUID userId, Pageable pageable) {
        log.debug("Reading documents: userId={}", userId);
        return fileRepository.findByUserIdAndFileType(userId, FileType.DOCUMENT, pageable)
                .map(this::mapFileToResponse);
    }

    /**
     * Get videos for a user.
     *
     * @param userId   user ID
     * @param pageable pagination
     * @return paginated videos
     */
    public Page<FileResponse> getVideos(UUID userId, Pageable pageable) {
        log.debug("Reading videos: userId={}", userId);
        return fileRepository.findByUserIdAndFileType(userId, FileType.VIDEO, pageable)
                .map(this::mapFileToResponse);
    }

    /**
     * Get file detail.
     *
     * @param fileId file ID
     * @param userId user ID
     * @return file details
     */
    public FileDetailResponse getFileDetail(UUID fileId, UUID userId) {
        log.debug("Reading file detail: fileId={}", fileId);
        File file = fileRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        return mapToDetailResponse(file);
    }

    /**
     * Delete a file and all related data.
     *
     * This method safely deletes a file by:
     * 1. Removing all FK-constrained related records first
     * 2. Clearing ManyToMany relationships (project_files)
     * 3. Deleting the physical file from storage
     * 4. Deleting the File entity (cascades to DocumentFile/VideoFile)
     *
     * @param fileId file ID
     * @param userId user ID
     * @throws RuntimeException if file not found or deletion fails
     */
    @Transactional
    public void deleteFile(UUID fileId, UUID userId) {
        File file = fileRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        log.info("Starting file deletion: fileId={}, filename={}", fileId, file.getOriginalFilename());

        try {
            // 1. Delete related records that have FK constraints (no cascade)
            // Order matters: delete dependent records first

            // Document-related cleanup
            documentMetadataRepository.deleteByFileId(fileId);
            log.debug("Deleted document_metadata for file: {}", fileId);

            documentContentRepository.deleteByFileId(fileId);
            log.debug("Deleted document_content for file: {}", fileId);

            // Glossary extraction jobs cleanup
            glossaryExtractionJobRepository.deleteByFileId(fileId);
            log.debug("Deleted glossary_extraction_jobs for file: {}", fileId);

            // Video translation glossary mappings cleanup (for glossary files)
            videoTranslationGlossaryRepository.deleteByFileId(fileId);
            log.debug("Deleted video_translation_glossaries for file: {}", fileId);

            // 2. Clear ManyToMany relationships (project_files join table)
            if (file.getProjects() != null && !file.getProjects().isEmpty()) {
                file.getProjects().clear();
                log.debug("Cleared project associations for file: {}", fileId);
            }

            // 3. Delete physical file from storage
            String filePath = file.getFilePath();
            if (filePath != null && !filePath.isEmpty()) {
                try {
                    fileStorageService.deleteFile(filePath);
                    log.debug("Deleted physical file: {}", filePath);
                } catch (Exception e) {
                    // Log warning but continue with DB deletion
                    // Physical file might already be deleted or moved
                    log.warn("Failed to delete physical file (continuing): path={}, error={}",
                            filePath, e.getMessage());
                }
            }

            // 4. Delete File entity (cascades to DocumentFile/VideoFile via CascadeType.ALL)
            // Note: glossary_term_documents is handled by DB-level CASCADE
            fileRepository.delete(file);

            log.info("Successfully deleted file: fileId={}, filename={}",
                    fileId, file.getOriginalFilename());

        } catch (Exception e) {
            log.error("Failed to delete file: fileId={}, error={}", fileId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    // Mapping methods
    private FileResponse mapToResponse(File file, DocumentFile documentFile) {
        return FileResponse.builder()
                .id(file.getId())
                .fileType(file.getFileType().name())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .uploadDate(file.getUploadDate())
                .status(file.getStatus())
                .createdAt(file.getCreatedAt())
                .isAnalyzed(documentFile.getIsAnalyzed())
                .pageCount(documentFile.getPageCount())
                .language(documentFile.getLanguage())
                .build();
    }

    private FileResponse mapToVideoResponse(File file, VideoFile videoFile) {
        return FileResponse.builder()
                .id(file.getId())
                .fileType(file.getFileType().name())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .uploadDate(file.getUploadDate())
                .status(file.getStatus())
                .createdAt(file.getCreatedAt())
                .durationSeconds(videoFile.getDurationSeconds())
                .resolution(videoFile.getResolution())
                .sttStatus(videoFile.getSttStatus())
                .translationStatus(videoFile.getTranslationStatus())
                .build();
    }

    private FileResponse mapFileToResponse(File file) {
        FileResponse.FileResponseBuilder builder = FileResponse.builder()
                .id(file.getId())
                .fileType(file.getFileType().name())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .uploadDate(file.getUploadDate())
                .status(file.getStatus())
                .createdAt(file.getCreatedAt());

        if (file.getFileType() == FileType.DOCUMENT && file.getDocumentFile() != null) {
            DocumentFile df = file.getDocumentFile();
            builder.isAnalyzed(df.getIsAnalyzed())
                    .pageCount(df.getPageCount())
                    .language(df.getLanguage());
        } else if (file.getFileType() == FileType.VIDEO && file.getVideoFile() != null) {
            VideoFile vf = file.getVideoFile();
            builder.durationSeconds(vf.getDurationSeconds())
                    .resolution(vf.getResolution())
                    .sttStatus(vf.getSttStatus())
                    .translationStatus(vf.getTranslationStatus());
        }

        return builder.build();
    }

    private FileDetailResponse mapToDetailResponse(File file) {
        FileDetailResponse response = FileDetailResponse.builder()
                .id(file.getId())
                .fileType(file.getFileType().name())
                .originalFilename(file.getOriginalFilename())
                .storedFilename(file.getStoredFilename())
                .filePath(file.getFilePath())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .uploadDate(file.getUploadDate())
                .status(file.getStatus())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();

        if (file.getFileType() == FileType.DOCUMENT && file.getDocumentFile() != null) {
            DocumentFile df = file.getDocumentFile();
            response.setDocumentDetail(FileDetailResponse.DocumentFileDetail.builder()
                    .language(df.getLanguage())
                    .pageCount(df.getPageCount())
                    .wordCount(df.getWordCount())
                    .characterCount(df.getCharacterCount())
                    .isAnalyzed(df.getIsAnalyzed())
                    .build());
        } else if (file.getFileType() == FileType.VIDEO && file.getVideoFile() != null) {
            VideoFile vf = file.getVideoFile();
            response.setVideoDetail(FileDetailResponse.VideoFileDetail.builder()
                    .durationSeconds(vf.getDurationSeconds())
                    .videoCodec(vf.getVideoCodec())
                    .audioCodec(vf.getAudioCodec())
                    .resolution(vf.getResolution())
                    .frameRate(vf.getFrameRate())
                    .hasAudio(vf.getHasAudio())
                    .sttStatus(vf.getSttStatus())
                    .translationStatus(vf.getTranslationStatus())
                    .sourceLanguage(vf.getSourceLanguage())
                    .targetLanguage(vf.getTargetLanguage())
                    .originalSubtitlePath(vf.getOriginalSubtitlePath())
                    .translatedSubtitlePath(vf.getTranslatedSubtitlePath())
                    .errorMessage(vf.getErrorMessage())
                    .build());
        }

        return response;
    }
}
