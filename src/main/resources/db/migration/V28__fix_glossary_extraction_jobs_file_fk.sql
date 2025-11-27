-- V28: Migrate glossary_extraction_jobs from document_id to file_id
--
-- Background:
-- - V1 created glossary_extraction_jobs with document_id referencing documents table
-- - V14-V24 migrated documents → files (unified file system)
-- - V24 dropped documents table (FK was CASCADE deleted)
-- - Need to add file_id column and remove legacy document_id column
--
-- This migration:
-- 1. Adds file_id column (if not exists)
-- 2. Removes document_id column and its constraints
-- 3. Adds proper FK and index for file_id

-- Step 1: Add file_id column if not exists
ALTER TABLE glossary_extraction_jobs
    ADD COLUMN IF NOT EXISTS file_id UUID;

-- Step 2: Drop document_id unique constraint
ALTER TABLE glossary_extraction_jobs
    DROP CONSTRAINT IF EXISTS glossary_extraction_jobs_document_unique CASCADE;

-- Step 3: Drop document_id index
DROP INDEX IF EXISTS idx_glossary_jobs_document_id;

-- Step 4: Drop document_id column
ALTER TABLE glossary_extraction_jobs
    DROP COLUMN IF EXISTS document_id;

-- Step 5: Add file_id unique constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'glossary_extraction_jobs_file_unique'
        AND table_name = 'glossary_extraction_jobs'
    ) THEN
        ALTER TABLE glossary_extraction_jobs
            ADD CONSTRAINT glossary_extraction_jobs_file_unique UNIQUE (file_id);
    END IF;
END $$;

-- Step 6: Add file_id index
CREATE INDEX IF NOT EXISTS idx_glossary_jobs_file_id ON glossary_extraction_jobs(file_id);

-- Step 7: Add FK constraint (drop Hibernate-generated one if exists, then add with proper name)
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

-- Step 8: Update comments
COMMENT ON TABLE glossary_extraction_jobs IS 'Tracks background jobs for extracting glossary terms from files';
COMMENT ON COLUMN glossary_extraction_jobs.file_id IS 'Reference to the file being processed for glossary extraction';
