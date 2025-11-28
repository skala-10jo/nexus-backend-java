-- Rollback V15: Drop document_files table
--
-- WARNING: This will delete all document-specific metadata in the NEW structure
-- The old document_metadata table will remain intact

DROP TABLE IF EXISTS document_files CASCADE;
