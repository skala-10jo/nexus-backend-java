-- V32: Create Speaking Tutor tables
-- Manages speaking analysis sessions and utterances with speaker diarization
-- This migration is idempotent (safe to run multiple times)

-- ============================================
-- speaking_analysis_sessions table
-- ============================================
-- Stores uploaded audio analysis sessions

CREATE TABLE IF NOT EXISTS speaking_analysis_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,

    -- File information
    original_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    duration_seconds DOUBLE PRECISION,

    -- Analysis status: PENDING, PROCESSING, COMPLETED, FAILED
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,

    -- Metadata
    speaker_count INTEGER NOT NULL DEFAULT 0,
    utterance_count INTEGER NOT NULL DEFAULT 0,
    language VARCHAR(10) NOT NULL DEFAULT 'en-US',

    -- Speaker labels: {"1": "나", "2": "상대방1", ...}
    speaker_labels JSONB NOT NULL DEFAULT '{}',

    -- AI-generated summary of meeting content
    summary TEXT,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Foreign key constraint
    CONSTRAINT fk_speaking_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Create indexes if not exist
CREATE INDEX IF NOT EXISTS ix_speaking_analysis_sessions_user_id
    ON speaking_analysis_sessions(user_id);

CREATE INDEX IF NOT EXISTS ix_speaking_analysis_sessions_status
    ON speaking_analysis_sessions(status);

CREATE INDEX IF NOT EXISTS ix_speaking_analysis_sessions_created_at
    ON speaking_analysis_sessions(created_at DESC);


-- ============================================
-- speaking_utterances table
-- ============================================
-- Stores speaker-separated utterances with timing and feedback

CREATE TABLE IF NOT EXISTS speaking_utterances (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,

    -- Utterance information
    speaker_id INTEGER NOT NULL,
    text TEXT NOT NULL,

    -- Timestamps (milliseconds)
    start_time_ms BIGINT NOT NULL,
    end_time_ms BIGINT NOT NULL,

    -- Confidence score from Azure STT (0.0 - 1.0)
    confidence DOUBLE PRECISION,

    -- Feedback (JSONB for flexibility)
    -- Structure: {
    --   "grammar_corrections": [...],
    --   "suggestions": [...],
    --   "improved_sentence": "...",
    --   "score": 7,
    --   "score_breakdown": {"grammar": 6, "vocabulary": 8, "fluency": 7, "clarity": 7}
    -- }
    feedback JSONB,

    -- Sequence number for ordering
    sequence_number INTEGER NOT NULL,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Foreign key constraint
    CONSTRAINT fk_speaking_utterances_session
        FOREIGN KEY (session_id)
        REFERENCES speaking_analysis_sessions(id)
        ON DELETE CASCADE
);

-- Create indexes if not exist
CREATE INDEX IF NOT EXISTS ix_speaking_utterances_session_id
    ON speaking_utterances(session_id);

CREATE INDEX IF NOT EXISTS ix_speaking_utterances_sequence
    ON speaking_utterances(session_id, sequence_number);


-- ============================================
-- Add comments for documentation
-- ============================================

COMMENT ON TABLE speaking_analysis_sessions IS 'Speaking analysis sessions for audio file uploads with speaker diarization';
COMMENT ON COLUMN speaking_analysis_sessions.id IS 'Primary key (UUID)';
COMMENT ON COLUMN speaking_analysis_sessions.user_id IS 'Reference to users table';
COMMENT ON COLUMN speaking_analysis_sessions.original_filename IS 'Original name of uploaded audio file';
COMMENT ON COLUMN speaking_analysis_sessions.file_path IS 'Server path to stored audio file';
COMMENT ON COLUMN speaking_analysis_sessions.file_size IS 'File size in bytes';
COMMENT ON COLUMN speaking_analysis_sessions.duration_seconds IS 'Audio duration in seconds (after analysis)';
COMMENT ON COLUMN speaking_analysis_sessions.status IS 'Analysis status: PENDING, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN speaking_analysis_sessions.progress IS 'Analysis progress percentage (0-100)';
COMMENT ON COLUMN speaking_analysis_sessions.error_message IS 'Error message if analysis failed';
COMMENT ON COLUMN speaking_analysis_sessions.speaker_count IS 'Number of speakers detected';
COMMENT ON COLUMN speaking_analysis_sessions.utterance_count IS 'Total number of utterances';
COMMENT ON COLUMN speaking_analysis_sessions.language IS 'Language code (e.g., en-US, ko-KR)';
COMMENT ON COLUMN speaking_analysis_sessions.speaker_labels IS 'JSONB mapping speaker IDs to labels';
COMMENT ON COLUMN speaking_analysis_sessions.summary IS 'AI-generated meeting/conversation summary';
COMMENT ON COLUMN speaking_analysis_sessions.created_at IS 'Session creation timestamp';
COMMENT ON COLUMN speaking_analysis_sessions.completed_at IS 'Analysis completion timestamp';
COMMENT ON COLUMN speaking_analysis_sessions.updated_at IS 'Last update timestamp';

COMMENT ON TABLE speaking_utterances IS 'Individual utterances from speaker diarization analysis';
COMMENT ON COLUMN speaking_utterances.id IS 'Primary key (UUID)';
COMMENT ON COLUMN speaking_utterances.session_id IS 'Reference to speaking_analysis_sessions';
COMMENT ON COLUMN speaking_utterances.speaker_id IS 'Speaker identifier (1, 2, 3, ...)';
COMMENT ON COLUMN speaking_utterances.text IS 'Transcribed text content';
COMMENT ON COLUMN speaking_utterances.start_time_ms IS 'Start time in milliseconds';
COMMENT ON COLUMN speaking_utterances.end_time_ms IS 'End time in milliseconds';
COMMENT ON COLUMN speaking_utterances.confidence IS 'STT confidence score (0.0-1.0)';
COMMENT ON COLUMN speaking_utterances.feedback IS 'AI feedback with grammar corrections, suggestions, and scores';
COMMENT ON COLUMN speaking_utterances.sequence_number IS 'Ordering sequence within session';
COMMENT ON COLUMN speaking_utterances.created_at IS 'Utterance creation timestamp';
