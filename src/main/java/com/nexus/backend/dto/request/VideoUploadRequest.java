package com.nexus.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadRequest {

    /**
     * 영상 제목
     */
    private String title;

    /**
     * 원본 언어 (STT 처리용)
     */
    private String sourceLanguage;

    /**
     * 목표 언어 (번역용)
     */
    private String targetLanguage;

    /**
     * 번역 시 적용할 용어집 문서 ID 목록 (선택사항)
     */
    private List<UUID> documentIds;
}
