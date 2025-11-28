-- Fix video-related tables to remove old document_id columns
-- Keep only file_id columns to match entity definitions

-- ============================================================
-- 1. Fix video_translation_glossaries table
-- ============================================================

-- Drop old constraints that reference document columns
ALTER TABLE video_translation_glossaries
    DROP CONSTRAINT IF EXISTS ukj66qfpd7y0r08odishe3tw7rm;

ALTER TABLE video_translation_glossaries
    DROP CONSTRAINT IF EXISTS video_translation_glossaries_video_document_id_document_id_key;

-- Drop old indexes
DROP INDEX IF EXISTS idx_video_translation_glossaries_document;
DROP INDEX IF EXISTS idx_video_translation_glossaries_video;

-- Drop old columns
ALTER TABLE video_translation_glossaries
    DROP COLUMN IF EXISTS video_document_id;

ALTER TABLE video_translation_glossaries
    DROP COLUMN IF EXISTS document_id;

-- Verify video_translation_glossaries
\d video_translation_glossaries;

-- ============================================================
-- 2. Fix video_subtitles table
-- ============================================================

-- Drop old constraints that reference video_document_id
ALTER TABLE video_subtitles
    DROP CONSTRAINT IF EXISTS ukhab1fp72jbwmroggv3at3gosq;

ALTER TABLE video_subtitles
    DROP CONSTRAINT IF EXISTS video_subtitles_video_document_id_sequence_number_key;

-- Drop old indexes
DROP INDEX IF EXISTS idx_video_subtitles_sequence;
DROP INDEX IF EXISTS idx_video_subtitles_time_range;
DROP INDEX IF EXISTS idx_video_subtitles_video_document_id;

-- Recreate indexes with new column name
CREATE INDEX idx_video_subtitles_sequence ON video_subtitles(video_file_id, sequence_number);
CREATE INDEX idx_video_subtitles_time_range ON video_subtitles(video_file_id, start_time_ms, end_time_ms);

-- Drop old column
ALTER TABLE video_subtitles
    DROP COLUMN IF EXISTS video_document_id;

-- Drop deprecated column that's not in entity
ALTER TABLE video_subtitles
    DROP COLUMN IF EXISTS original_language;

-- Verify video_subtitles
\d video_subtitles;

-- Summary
SELECT 'Schema migration completed successfully!' AS status;
