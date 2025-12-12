-- V13: Add video translation tables
--
-- 영상 번역 시스템을 위한 테이블 추가
-- - video_documents: 영상 파일 메타데이터 (documents 테이블 확장)
-- - video_subtitles: 자막 세그먼트 (원본/번역 텍스트, 타이밍)

-- video_documents 테이블: 영상 메타데이터
CREATE TABLE video_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID UNIQUE NOT NULL REFERENCES documents(id) ON DELETE CASCADE,

    -- 영상 메타데이터
    duration_seconds INTEGER,
    video_codec VARCHAR(50),
    audio_codec VARCHAR(50),
    resolution VARCHAR(20),
    frame_rate DECIMAL(5, 2),
    has_audio BOOLEAN DEFAULT TRUE,

    -- 처리 상태
    stt_status VARCHAR(20) DEFAULT 'pending',
    -- pending, processing, completed, failed

    translation_status VARCHAR(20) DEFAULT 'pending',
    -- pending, processing, completed, failed

    -- 언어 설정
    source_language VARCHAR(10),
    target_language VARCHAR(10),

    -- 결과 파일 경로
    original_subtitle_path VARCHAR(500),
    translated_subtitle_path VARCHAR(500),

    -- 에러 정보
    error_message TEXT,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- video_subtitles 테이블: 자막 세그먼트
CREATE TABLE video_subtitles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_document_id UUID NOT NULL REFERENCES video_documents(id) ON DELETE CASCADE,

    -- 시퀀스 및 타이밍
    sequence_number INTEGER NOT NULL,
    start_time_ms BIGINT NOT NULL,  -- 밀리초 단위
    end_time_ms BIGINT NOT NULL,

    -- 텍스트
    original_text TEXT NOT NULL,
    translated_text TEXT,

    -- 화자 정보 (선택사항)
    speaker_id INTEGER,

    -- 신뢰도 점수 (STT 결과)
    confidence_score DECIMAL(3, 2),

    -- 탐지된 전문용어 (JSON 배열)
    detected_terms JSONB,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (video_document_id, sequence_number)
);

-- video_translation_glossaries 테이블: 영상-용어사전 매핑 (M:N)
CREATE TABLE video_translation_glossaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_document_id UUID NOT NULL REFERENCES video_documents(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (video_document_id, document_id)
);

-- 인덱스 생성
CREATE INDEX idx_video_documents_document_id ON video_documents(document_id);
CREATE INDEX idx_video_documents_stt_status ON video_documents(stt_status);
CREATE INDEX idx_video_documents_translation_status ON video_documents(translation_status);

CREATE INDEX idx_video_subtitles_video_document_id ON video_subtitles(video_document_id);
CREATE INDEX idx_video_subtitles_sequence ON video_subtitles(video_document_id, sequence_number);
CREATE INDEX idx_video_subtitles_time_range ON video_subtitles(video_document_id, start_time_ms, end_time_ms);

CREATE INDEX idx_video_translation_glossaries_video ON video_translation_glossaries(video_document_id);
CREATE INDEX idx_video_translation_glossaries_document ON video_translation_glossaries(document_id);

-- 참고:
-- - documents 테이블의 기존 파일 관리 기능 재사용
-- - video_documents는 documents를 1:1로 확장하는 형태
-- - 자막은 video_subtitles 테이블에 세그먼트별로 저장
-- - 전문용어사전은 documents 테이블의 문서들과 M:N 관계
