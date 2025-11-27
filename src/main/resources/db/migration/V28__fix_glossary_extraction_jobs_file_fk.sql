-- V28: Remove legacy document_id column from glossary_extraction_jobs
--
-- Background:
-- - V1 created glossary_extraction_jobs with document_id referencing documents table
-- - V14-V24 migrated documents → files (unified file system)
-- - V24 dropped documents table (FK was CASCADE deleted)
-- - Hibernate added file_id column with FK and unique constraint
-- - But document_id column still exists with NOT NULL constraint
-- - This causes "null value in document_id" error on INSERT
--
-- Current DB state (verified):
-- - file_id column: EXISTS, NOT NULL, FK exists, unique constraint exists ✓
-- - document_id column: EXISTS, NOT NULL, FK deleted, unique constraint exists, index exists
--
-- This migration only needs to remove document_id and its constraints

-- Step 1: Drop document_id unique constraint
ALTER TABLE glossary_extraction_jobs
    DROP CONSTRAINT IF EXISTS glossary_extraction_jobs_document_unique CASCADE;

-- Step 2: Drop document_id index
DROP INDEX IF EXISTS idx_glossary_jobs_document_id;

-- Step 3: Drop document_id column
ALTER TABLE glossary_extraction_jobs
    DROP COLUMN IF EXISTS document_id;

-- Step 4: Add file_id index (currently missing)
CREATE INDEX IF NOT EXISTS idx_glossary_jobs_file_id ON glossary_extraction_jobs(file_id);

-- Step 5: Rename Hibernate-generated FK to standard naming (optional, for consistency)
-- First drop existing FK, then add with proper name
DO $$
BEGIN
    -- Drop Hibernate-generated FK if exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fktk9xm99w018ja0q91t6bt08qn'
        AND table_name = 'glossary_extraction_jobs'
    ) THEN
        ALTER TABLE glossary_extraction_jobs DROP CONSTRAINT fktk9xm99w018ja0q91t6bt08qn;
    END IF;

    -- Add FK with proper name if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_glossary_jobs_file'
        AND table_name = 'glossary_extraction_jobs'
    ) THEN
        ALTER TABLE glossary_extraction_jobs
            ADD CONSTRAINT fk_glossary_jobs_file
            FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Step 6: Update comments
COMMENT ON TABLE glossary_extraction_jobs IS 'Tracks background jobs for extracting glossary terms from files';
COMMENT ON COLUMN glossary_extraction_jobs.file_id IS 'Reference to the file being processed for glossary extraction';
