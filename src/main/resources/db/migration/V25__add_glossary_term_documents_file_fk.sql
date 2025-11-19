-- V25: Add missing foreign key constraint on glossary_term_documents.file_id
--
-- Background:
-- - glossary_term_documents table has file_id column referencing files(id)
-- - However, the FK constraint was missing, causing data integrity risks
-- - Without FK: invalid file_id can be inserted, orphaned records possible
-- - Without FK index: JOIN performance degradation
--
-- This migration adds the missing FK constraint with CASCADE delete
-- to maintain referential integrity.
--
-- Data validation performed before migration:
-- - 35 records in glossary_term_documents
-- - All file_id values are valid (exist in files table)
-- - Safe to add FK constraint

-- Add foreign key constraint
ALTER TABLE glossary_term_documents
ADD CONSTRAINT fk_glossary_term_documents_file
FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE;

-- Verify constraint
COMMENT ON CONSTRAINT fk_glossary_term_documents_file ON glossary_term_documents
IS 'Ensures file_id references valid files. Cascade delete when file is removed.';
