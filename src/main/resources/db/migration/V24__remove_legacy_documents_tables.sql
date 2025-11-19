-- V24: Remove legacy documents and project_documents tables
--
-- Background:
-- - V1 created documents and project_documents tables
-- - These tables were replaced by the unified files system (V14-V17)
-- - documents → files (with file_type='DOCUMENT')
-- - project_documents → project_files
-- - Hibernate ddl-auto already removed these tables based on entities
-- - However, in fresh environments, V1 creates these tables causing conflicts
--
-- This migration ensures clean state by explicitly removing legacy tables
-- even if they don't currently exist in production.

-- Drop project_documents first (has FK to documents)
DROP TABLE IF EXISTS project_documents CASCADE;

-- Drop documents table
DROP TABLE IF EXISTS documents CASCADE;

-- Drop document_status enum type if exists (created by V1)
DROP TYPE IF EXISTS document_status CASCADE;

-- Comments for clarity
COMMENT ON TABLE files IS 'Unified file metadata for all file types (documents, videos, audio). Replaced legacy documents table.';
COMMENT ON TABLE project_files IS 'M:N relationship between projects and files. Replaced legacy project_documents table.';
