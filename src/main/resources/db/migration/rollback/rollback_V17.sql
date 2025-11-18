-- Rollback V17: Drop project_files table
--
-- WARNING: This will delete all project-file relationships in the NEW structure
-- The old project_documents table will remain intact

DROP TABLE IF EXISTS project_files CASCADE;
