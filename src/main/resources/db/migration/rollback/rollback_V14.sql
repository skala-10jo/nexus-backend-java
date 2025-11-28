-- Rollback V14: Drop files table
--
-- WARNING: This will delete all file metadata in the NEW structure
-- The old documents table will remain intact
-- IMPORTANT: Must drop dependent tables first (V15-V18)

DROP TABLE IF EXISTS files CASCADE;
