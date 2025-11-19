package com.nexus.backend.service;

import com.nexus.backend.dto.request.VideoUploadRequest;
import com.nexus.backend.dto.response.DocumentResponse;
import com.nexus.backend.dto.response.VideoDocumentResponse;
import com.nexus.backend.dto.response.VideoSubtitleResponse;
import com.nexus.backend.entity.*;
import com.nexus.backend.repository.DocumentRepository;
import com.nexus.backend.repository.VideoDocumentRepository;
import com.nexus.backend.repository.VideoSubtitleRepository;
import com.nexus.backend.repository.VideoTranslationGlossaryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoDocumentService {

    private final VideoDocumentRepository videoDocumentRepository;
    private final VideoSubtitleRepository videoSubtitleRepository;
    private final VideoTranslationGlossaryRepository videoTranslationGlossaryRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final FileStorageService fileStorageService;
    private final Tika tika = new Tika();

    @Value("${file.max-size}")
    private long maxFileSize;

    private static final List<String> ALLOWED_VIDEO_MIME_TYPES = Arrays.asList(
            "video/mp4",
            "video/x-msvideo", // AVI
            "video/quicktime", // MOV
            "video/x-matroska" // MKV
    );

    /**
     * 영상 업로드 및 VideoDocument 생성
     */
    @Transactional
    public VideoDocumentResponse uploadVideo(
            MultipartFile file,
            VideoUploadRequest request,
            User user
    ) {
        // 파일 검증
        validateVideoFile(file);

        try {
            // MIME 타입 검증
            String mimeType = tika.detect(file.getInputStream());
            if (!ALLOWED_VIDEO_MIME_TYPES.contains(mimeType)) {
                throw new RuntimeException("지원하지 않는 영상 형식입니다: " + mimeType);
            }

            // Document 생성 (DocumentService 재사용)
            DocumentResponse documentResponse = documentService.uploadDocument(file, user);

            // Document 엔티티 조회
            Document document = documentRepository.findById(documentResponse.getId())
                    .orElseThrow(() -> new RuntimeException("생성된 Document를 찾을 수 없습니다"));

            // VideoDocument 생성
            VideoDocument videoDocument = VideoDocument.builder()
                    .document(document)
                    .sourceLanguage(request.getSourceLanguage())
                    .targetLanguage(request.getTargetLanguage())
                    .sttStatus("pending")
                    .translationStatus("pending")
                    .hasAudio(true)
                    .subtitles(new ArrayList<>())
                    .glossaries(new ArrayList<>())
                    .build();

            VideoDocument savedVideoDocument = videoDocumentRepository.save(videoDocument);

            // 용어집 매핑 저장
            if (request.getDocumentIds() != null && !request.getDocumentIds().isEmpty()) {
                final VideoDocument finalVideoDocument = savedVideoDocument;
                List<VideoTranslationGlossary> glossaries = request.getDocumentIds().stream()
                        .map(docId -> {
                            Document glossaryDoc = documentRepository.findById(docId)
                                    .orElseThrow(() -> new RuntimeException("용어집 문서를 찾을 수 없습니다: " + docId));

                            return VideoTranslationGlossary.builder()
                                    .videoDocument(finalVideoDocument)
                                    .document(glossaryDoc)
                                    .build();
                        })
                        .collect(Collectors.toList());

                videoTranslationGlossaryRepository.saveAll(glossaries);
                savedVideoDocument.setGlossaries(glossaries);
            }

            return mapToResponse(savedVideoDocument);

        } catch (Exception ex) {
            throw new RuntimeException("영상 업로드 실패", ex);
        }
    }

    /**
     * VideoDocument 조회 (권한 확인)
     */
    public VideoDocumentResponse getVideoDocument(UUID id, User user) {
        VideoDocument videoDocument = videoDocumentRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("영상을 찾을 수 없습니다"));

        return mapToResponse(videoDocument);
    }

    /**
     * 자막 목록 조회
     */
    public List<VideoSubtitleResponse> getSubtitles(UUID videoDocumentId, User user) {
        // 권한 확인
        VideoDocument videoDocument = videoDocumentRepository.findByIdAndUserId(videoDocumentId, user.getId())
                .orElseThrow(() -> new RuntimeException("영상을 찾을 수 없습니다"));

        // 자막 조회
        List<VideoSubtitle> subtitles = videoSubtitleRepository
                .findByVideoDocumentIdOrderBySequenceNumber(videoDocumentId);

        return subtitles.stream()
                .map(this::mapToSubtitleResponse)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 영상 목록 조회 (페이징)
     */
    public Page<VideoDocumentResponse> getUserVideos(UUID userId, Pageable pageable) {
        Page<VideoDocument> videoDocuments = videoDocumentRepository.findByUserId(userId, pageable);
        return videoDocuments.map(this::mapToResponse);
    }

    /**
     * 영상 파일 검증
     */
    private void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("파일이 비어있습니다");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("파일 크기가 최대 허용 크기를 초과했습니다");
        }
    }

    /**
     * VideoDocument → VideoDocumentResponse 매핑
     */
    private VideoDocumentResponse mapToResponse(VideoDocument videoDocument) {
        Document document = videoDocument.getDocument();

        // 선택된 용어집 문서 목록 생성
        List<DocumentResponse> selectedDocuments = videoDocument.getGlossaries().stream()
                .map(glossary -> DocumentResponse.builder()
                        .id(glossary.getDocument().getId())
                        .originalFilename(glossary.getDocument().getOriginalFilename())
                        .fileSize(glossary.getDocument().getFileSize())
                        .fileType(glossary.getDocument().getFileType())
                        .uploadDate(glossary.getDocument().getUploadDate())
                        .status(glossary.getDocument().getStatus().name())
                        .isAnalyzed(glossary.getDocument().getIsAnalyzed())
                        .build())
                .collect(Collectors.toList());

        return VideoDocumentResponse.builder()
                .id(videoDocument.getId())
                .documentId(document.getId())
                .title(document.getOriginalFilename())
                .durationSeconds(videoDocument.getDurationSeconds())
                .resolution(videoDocument.getResolution())
                .frameRate(videoDocument.getFrameRate() != null ? videoDocument.getFrameRate().toString() : null)
                .sttStatus(videoDocument.getSttStatus())
                .translationStatus(videoDocument.getTranslationStatus())
                .sourceLanguage(videoDocument.getSourceLanguage())
                .targetLanguage(videoDocument.getTargetLanguage())
                .videoFilePath(document.getFilePath())
                .originalSubtitlePath(videoDocument.getOriginalSubtitlePath())
                .translatedSubtitlePath(videoDocument.getTranslatedSubtitlePath())
                .subtitleCount(videoDocument.getSubtitles().size())
                .selectedDocuments(selectedDocuments)
                .errorMessage(videoDocument.getErrorMessage())
                .createdAt(videoDocument.getCreatedAt())
                .updatedAt(videoDocument.getUpdatedAt())
                .build();
    }

    /**
     * VideoSubtitle → VideoSubtitleResponse 매핑
     */
    private VideoSubtitleResponse mapToSubtitleResponse(VideoSubtitle subtitle) {
        return VideoSubtitleResponse.builder()
                .id(subtitle.getId())
                .sequenceNumber(subtitle.getSequenceNumber())
                .startTimeMs(subtitle.getStartTimeMs())
                .endTimeMs(subtitle.getEndTimeMs())
                .originalText(subtitle.getOriginalText())
                .translatedText(subtitle.getTranslatedText())
                .speakerId(subtitle.getSpeakerId())
                .confidenceScore(subtitle.getConfidenceScore())
                .detectedTerms(subtitle.getDetectedTerms())
                .build();
    }
}
