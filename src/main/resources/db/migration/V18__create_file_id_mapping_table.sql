-- V18: Create file_id_mapping table (for dual write validation)
--
-- This temporary table tracks the mapping between new files table and old documents table
-- during the dual write phase. Used for data consistency validation.
-- This table will be dropped in Phase 5 (cleanup) after migration is complete.

CREATE TABLE file_id_mapping (
    new_file_id UUID PRIMARY KEY,
    old_document_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_file_id_mapping_new FOREIGN KEY (new_file_id)
        REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_id_mapping_old FOREIGN KEY (old_document_id)
        REFERENCES documents(id) ON DELETE CASCADE
);

-- Index for reverse lookups
CREATE INDEX idx_file_id_mapping_old ON file_id_mapping(old_document_id);

-- Comments
COMMENT ON TABLE file_id_mapping IS 'Temporary: Maps new file IDs to old document IDs during migration';
COMMENT ON COLUMN file_id_mapping.new_file_id IS 'ID from new files table';
COMMENT ON COLUMN file_id_mapping.old_document_id IS 'ID from old documents table';
