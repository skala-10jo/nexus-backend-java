-- Fix glossary_extraction_jobs table schema to match File entity
-- Rename document_id to file_id

-- Drop existing constraints and indexes that reference document_id
ALTER TABLE glossary_extraction_jobs
    DROP CONSTRAINT IF EXISTS glossary_extraction_jobs_document_unique;

DROP INDEX IF EXISTS idx_glossary_jobs_document_id;

-- Rename the column
ALTER TABLE glossary_extraction_jobs
    RENAME COLUMN document_id TO file_id;

-- Recreate unique constraint with new name
ALTER TABLE glossary_extraction_jobs
    ADD CONSTRAINT glossary_extraction_jobs_file_unique
    UNIQUE (file_id);

-- Recreate index with new column name
CREATE INDEX idx_glossary_jobs_file_id ON glossary_extraction_jobs(file_id);

-- Verify the changes
\d glossary_extraction_jobs;
