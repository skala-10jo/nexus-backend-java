package com.nexus.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDocumentResponse {

    /**
     * VideoDocument ID
     */
    private UUID id;

    /**
     * 연관된 Document ID
     */
    private UUID documentId;

    /**
     * 영상 제목 (원본 파일명)
     */
    private String title;

    /**
     * 영상 길이 (초)
     */
    private Integer durationSeconds;

    /**
     * 해상도 (예: 1920x1080)
     */
    private String resolution;

    /**
     * 프레임율 (예: 30.00)
     */
    private String frameRate;

    /**
     * STT 처리 상태 (pending/processing/completed/failed)
     */
    private String sttStatus;

    /**
     * 번역 상태 (pending/processing/completed/failed)
     */
    private String translationStatus;

    /**
     * 원본 언어
     */
    private String sourceLanguage;

    /**
     * 목표 언어
     */
    private String targetLanguage;

    /**
     * 영상 파일 경로
     */
    private String videoFilePath;

    /**
     * 원본 자막 파일 경로
     */
    private String originalSubtitlePath;

    /**
     * 번역된 자막 파일 경로
     */
    private String translatedSubtitlePath;

    /**
     * 자막 세그먼트 개수
     */
    private Integer subtitleCount;

    /**
     * 선택된 용어집 문서 목록
     */
    private List<DocumentResponse> selectedDocuments;

    /**
     * 에러 메시지 (있는 경우)
     */
    private String errorMessage;

    /**
     * 생성 시각
     */
    private LocalDateTime createdAt;

    /**
     * 수정 시각
     */
    private LocalDateTime updatedAt;
}
