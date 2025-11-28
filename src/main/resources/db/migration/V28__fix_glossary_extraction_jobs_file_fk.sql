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
-- - file_id column: MAY or MAY NOT exist (depends on Hibernate state)
-- - document_id column: MAY exist with constraints
--
-- This migration handles both cases: existing DB and fresh DB

-- Step 1: Add file_id column if it doesn't exist (for fresh DB)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'glossary_extraction_jobs'
        AND column_name = 'file_id'
    ) THEN
        ALTER TABLE glossary_extraction_jobs ADD COLUMN file_id UUID;
    END IF;
END $$;

-- Step 2: Drop document_id unique constraint (if exists)
ALTER TABLE glossary_extraction_jobs
    DROP CONSTRAINT IF EXISTS glossary_extraction_jobs_document_unique CASCADE;

-- Step 3: Drop document_id index (if exists)
DROP INDEX IF EXISTS idx_glossary_jobs_document_id;

-- Step 4: Drop document_id column (if exists)
ALTER TABLE glossary_extraction_jobs
    DROP COLUMN IF EXISTS document_id;

-- Step 5: Add file_id index
CREATE INDEX IF NOT EXISTS idx_glossary_jobs_file_id ON glossary_extraction_jobs(file_id);

-- Step 6: Add FK if not exists (for fresh DB) or rename Hibernate-generated FK (for existing DB)
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

    -- Add FK with proper name if not exists (handles both fresh and existing DB)
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

-- Step 7: Update comments
COMMENT ON TABLE glossary_extraction_jobs IS 'Tracks background jobs for extracting glossary terms from files';
COMMENT ON COLUMN glossary_extraction_jobs.file_id IS 'Reference to the file being processed for glossary extraction';
