-- V22: Create video_subtitles table (for video_files system)
--
-- This table stores subtitle segments for video files
-- Replaces the old video_subtitles (V13) which referenced video_documents
-- Now references video_files (V16) for consistency with files-based architecture

CREATE TABLE video_subtitles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_file_id UUID NOT NULL,

    -- Sequence and timing
    sequence_number INTEGER NOT NULL,
    start_time_ms BIGINT NOT NULL CHECK (start_time_ms >= 0),
    end_time_ms BIGINT NOT NULL CHECK (end_time_ms > start_time_ms),

    -- Text content
    original_text TEXT NOT NULL,
    original_language VARCHAR(10),

    -- Multi-language translations (JSONB for flexibility)
    -- Example: {"ko": "한국어 번역", "en": "English translation", "ja": "日本語翻訳"}
    translations JSONB DEFAULT '{}'::jsonb,

    -- Legacy field for backward compatibility
    translated_text TEXT,

    -- STT metadata
    confidence_score DECIMAL(3, 2) CHECK (confidence_score >= 0 AND confidence_score <= 1),
    speaker_id INTEGER,

    -- Detected terminology (JSONB array)
    -- Example: [{"term": "API", "translation": "API"}, {"term": "backend", "translation": "백엔드"}]
    detected_terms JSONB DEFAULT '[]'::jsonb,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key to video_files
    CONSTRAINT fk_video_subtitles_video_file FOREIGN KEY (video_file_id)
        REFERENCES video_files(id) ON DELETE CASCADE,

    -- Unique constraint: one subtitle per sequence number per video
    UNIQUE (video_file_id, sequence_number)
);

-- Indexes for efficient querying
CREATE INDEX idx_video_subtitles_video_file_id ON video_subtitles(video_file_id);
CREATE INDEX idx_video_subtitles_sequence ON video_subtitles(video_file_id, sequence_number);
CREATE INDEX idx_video_subtitles_time_range ON video_subtitles(video_file_id, start_time_ms, end_time_ms);
CREATE INDEX idx_video_subtitles_language ON video_subtitles(original_language);

-- GIN index for JSONB fields (efficient translation and term lookups)
CREATE INDEX idx_video_subtitles_translations ON video_subtitles USING GIN (translations);
CREATE INDEX idx_video_subtitles_detected_terms ON video_subtitles USING GIN (detected_terms);

-- Comments
COMMENT ON TABLE video_subtitles IS 'Subtitle segments for video files (STT and translation results)';
COMMENT ON COLUMN video_subtitles.video_file_id IS 'Foreign key to video_files table';
COMMENT ON COLUMN video_subtitles.translations IS 'Multi-language translations in JSONB format: {"ko": "...", "en": "...", "ja": "..."}';
COMMENT ON COLUMN video_subtitles.detected_terms IS 'Terminology detected in subtitle (JSONB array)';
