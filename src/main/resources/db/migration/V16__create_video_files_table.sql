-- V16: Create video_files table (video-specific metadata extension)
--
-- This table extends the 'files' table with video-specific metadata.
-- It has a 1:1 relationship with files where file_type = 'VIDEO'.

CREATE TABLE video_files (
    id UUID PRIMARY KEY,
    duration_seconds INTEGER CHECK (duration_seconds >= 0),
    video_codec VARCHAR(50),
    audio_codec VARCHAR(50),
    resolution VARCHAR(20),
    frame_rate DECIMAL(5,2) CHECK (frame_rate > 0),
    has_audio BOOLEAN DEFAULT TRUE,
    stt_status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (stt_status IN ('pending', 'processing', 'completed', 'failed')),
    translation_status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (translation_status IN ('pending', 'processing', 'completed', 'failed')),
    source_language VARCHAR(10),
    target_language VARCHAR(10),
    original_subtitle_path VARCHAR(500),
    translated_subtitle_path VARCHAR(500),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_video_files_file FOREIGN KEY (id)
        REFERENCES files(id) ON DELETE CASCADE
);

-- Indexes for video processing queries
CREATE INDEX idx_video_files_stt_status ON video_files(stt_status);
CREATE INDEX idx_video_files_translation_status ON video_files(translation_status);
CREATE INDEX idx_video_files_languages ON video_files(source_language, target_language);

-- Comments
COMMENT ON TABLE video_files IS 'Video-specific metadata (extends files table)';
COMMENT ON COLUMN video_files.id IS 'Same as file_id (1:1 relationship with files)';
COMMENT ON COLUMN video_files.stt_status IS 'Speech-to-Text processing status';
COMMENT ON COLUMN video_files.translation_status IS 'Translation processing status';
