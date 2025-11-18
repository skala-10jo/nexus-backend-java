-- ============================================================================
-- File ID Mapping Cleanup Script
-- ============================================================================
-- Purpose: Remove the temporary file_id_mapping table after migration completion
--
-- EXECUTION TIMELINE:
-- - Phase 7: After complete migration to new file structure
-- - Prerequisites:
--   1. All reads migrated to new structure (read-percentage = 100)
--   2. All legacy data migrated or archived
--   3. Dual Write disabled (feature.new-file-structure.enabled = false)
--
-- VALIDATION BEFORE EXECUTION:
-- 1. Verify no active Dual Write operations:
--    SELECT COUNT(*) FROM file_id_mapping WHERE created_at > NOW() - INTERVAL '1 hour';
--    (Should be 0 if Dual Write is disabled)
--
-- 2. Archive mapping data for audit:
--    CREATE TABLE file_id_mapping_archive AS SELECT * FROM file_id_mapping;
--
-- EXECUTION:
-- psql -U postgres -d nexus -f cleanup_file_id_mapping.sql
-- ============================================================================

-- Step 1: Create archive table for audit trail
CREATE TABLE IF NOT EXISTS file_id_mapping_archive (
    new_file_id UUID NOT NULL,
    old_document_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Step 2: Archive all mapping data
INSERT INTO file_id_mapping_archive (new_file_id, old_document_id, created_at)
SELECT new_file_id, old_document_id, created_at
FROM file_id_mapping;

-- Step 3: Drop the temporary table
DROP TABLE IF EXISTS file_id_mapping;

-- Step 4: Verification
DO $$
BEGIN
    RAISE NOTICE 'Cleanup completed successfully';
    RAISE NOTICE 'Archived records: %', (SELECT COUNT(*) FROM file_id_mapping_archive);
    RAISE NOTICE 'Archive table: file_id_mapping_archive (retained for audit)';
END $$;
