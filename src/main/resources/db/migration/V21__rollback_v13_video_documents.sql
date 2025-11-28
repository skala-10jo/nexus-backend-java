-- V21: Rollback V13 (Remove video_documents system)
-- Author: NEXUS Team
-- Date: 2025-01-19
--
-- Reason: Consolidating to files/video_files architecture
-- The video_documents system (V13) is incompatible with the new files-based system (V14-V18)
-- This migration removes the old video_documents, video_subtitles, and video_translation_glossaries tables

-- Drop tables in reverse dependency order (CASCADE handles foreign keys automatically)
DROP TABLE IF EXISTS video_translation_glossaries CASCADE;
DROP TABLE IF EXISTS video_subtitles CASCADE;
DROP TABLE IF EXISTS video_documents CASCADE;

-- Note: This rollback is safe because:
-- 1. Java backend uses files/video_files tables (V14-V16)
-- 2. Python backend will be updated to use video_files instead of video_documents
-- 3. Existing data in video_documents was test data only (2 records, pending status)
