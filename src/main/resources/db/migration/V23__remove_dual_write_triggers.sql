-- V23: Remove dual write triggers (cleanup after video_documents removal)
--
-- Background:
-- - V13 created video_documents table with dual write trigger
-- - V21 removed video_documents table (migrated to files/video_files system)
-- - V22 created new video_subtitles referencing video_files
--
-- This migration removes the leftover trigger and function that were attempting
-- to insert into the now-deleted video_documents table, causing errors.
--
-- Reason for removal:
-- - Dual write phase is complete (files/video_files is the single source of truth)
-- - FileService explicitly handles both files and video_files tables
-- - Hidden trigger logic makes debugging difficult
-- - Architectural consistency: video_documents system fully removed

-- Drop the trigger first (depends on the function)
DROP TRIGGER IF EXISTS trigger_create_video_document ON files;

-- Drop the function
DROP FUNCTION IF EXISTS create_video_document_on_video_file();

-- Comments
COMMENT ON TABLE files IS 'Common file metadata for all file types (documents, videos, audio). Extended by type-specific tables (video_files, document_files).';
COMMENT ON TABLE video_files IS 'Video-specific metadata (extends files table with 1:1 relationship). Managed explicitly by FileService.';
