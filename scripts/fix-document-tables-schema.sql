-- Fix document_content and document_metadata tables
-- Remove old document_id columns to match entity definitions

-- ============================================================
-- 1. Fix document_content table
-- ============================================================

-- Drop old indexes
DROP INDEX IF EXISTS idx_document_content_document_id;
DROP INDEX IF EXISTS idx_document_content_page_number;

-- Recreate page_number index with new column
CREATE INDEX idx_document_content_page_number ON document_content(file_id, page_number);

-- Drop old column
ALTER TABLE document_content
    DROP COLUMN IF EXISTS document_id;

-- Verify document_content
\d document_content;

-- ============================================================
-- 2. Fix document_metadata table
-- ============================================================

-- Drop old constraints
ALTER TABLE document_metadata
    DROP CONSTRAINT IF EXISTS document_metadata_document_id_key;

-- Drop old indexes
DROP INDEX IF EXISTS idx_document_metadata_document_id;

-- Drop old column
ALTER TABLE document_metadata
    DROP COLUMN IF EXISTS document_id;

-- Verify document_metadata
\d document_metadata;

-- Summary
SELECT 'All schema migrations completed successfully!' AS status;
