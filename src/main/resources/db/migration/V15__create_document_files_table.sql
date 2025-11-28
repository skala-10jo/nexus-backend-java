-- V15: Create document_files table (document-specific metadata extension)
--
-- This table extends the 'files' table with document-specific metadata.
-- It has a 1:1 relationship with files where file_type = 'DOCUMENT'.

CREATE TABLE document_files (
    id UUID PRIMARY KEY,
    language VARCHAR(10),
    page_count INTEGER CHECK (page_count >= 0),
    word_count INTEGER CHECK (word_count >= 0),
    character_count INTEGER CHECK (character_count >= 0),
    is_analyzed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_files_file FOREIGN KEY (id)
        REFERENCES files(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_document_files_language ON document_files(language);
CREATE INDEX idx_document_files_analyzed ON document_files(is_analyzed);

-- Comments
COMMENT ON TABLE document_files IS 'Document-specific metadata (extends files table)';
COMMENT ON COLUMN document_files.id IS 'Same as file_id (1:1 relationship with files)';
COMMENT ON COLUMN document_files.is_analyzed IS 'Whether the document has been analyzed for glossary extraction';
