-- Rollback ALL (V14-V18): Complete rollback of new file structure
--
-- This script rolls back all changes made in Phase 1
-- Run this ONLY if you need to completely abandon the migration
--
-- IMPORTANT: This must be run in a transaction for safety

BEGIN;

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS file_id_mapping CASCADE;
DROP TABLE IF EXISTS project_files CASCADE;
DROP TABLE IF EXISTS video_files CASCADE;
DROP TABLE IF EXISTS document_files CASCADE;
DROP TABLE IF EXISTS files CASCADE;

-- Verify rollback
DO $$
BEGIN
    IF EXISTS (
        SELECT FROM information_schema.tables
        WHERE table_name IN ('files', 'document_files', 'video_files', 'project_files', 'file_id_mapping')
    ) THEN
        RAISE EXCEPTION 'Rollback failed: Some tables still exist';
    ELSE
        RAISE NOTICE 'Rollback successful: All new tables dropped';
    END IF;
END $$;

COMMIT;
