-- Rollback V16: Drop video_files table
--
-- WARNING: This will delete all video-specific metadata in the NEW structure
-- The old video_documents table will remain intact

DROP TABLE IF EXISTS video_files CASCADE;
