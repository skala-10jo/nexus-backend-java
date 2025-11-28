-- V25: Migrate glossary_term_documents from document_id to file_id
--
-- Background:
-- - glossary_term_documents originally used document_id (from legacy documents table)
-- - documents table was removed in V24 (migrated to files architecture)
-- - Need to migrate glossary_term_documents to use file_id instead
--
-- Current status:
-- - glossary_term_documents table is empty (verified: 0 rows)
-- - Safe to recreate table with new schema

-- Step 1: Drop existing table (it's empty, no data loss)
DROP TABLE IF EXISTS glossary_term_documents CASCADE;

-- Step 2: Recreate table with file_id instead of document_id
CREATE TABLE glossary_term_documents (
    term_id UUID NOT NULL,
    file_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (term_id, file_id),
    CONSTRAINT fk_glossary_term_documents_term
        FOREIGN KEY (term_id) REFERENCES glossary_terms(id) ON DELETE CASCADE,
    CONSTRAINT fk_glossary_term_documents_file
        FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE
);

-- Step 3: Create indexes for performance
CREATE INDEX idx_glossary_term_documents_term_id ON glossary_term_documents(term_id);
CREATE INDEX idx_glossary_term_documents_file_id ON glossary_term_documents(file_id);

-- Step 4: Add comments
COMMENT ON TABLE glossary_term_documents
IS 'Many-to-many relationship tracking which files contain which glossary terms (migrated from document_id to file_id in V25)';

COMMENT ON CONSTRAINT fk_glossary_term_documents_file ON glossary_term_documents
IS 'Ensures file_id references valid files. Cascade delete when file is removed.';

COMMENT ON CONSTRAINT fk_glossary_term_documents_term ON glossary_term_documents
IS 'Ensures term_id references valid glossary terms. Cascade delete when term is removed.';
