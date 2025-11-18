-- V14: Create files table (common metadata for all file types)
--
-- This table stores common metadata for documents, videos, and future file types.
-- It replaces the 'documents' table with a clearer domain separation using file_type enum.

CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    file_type VARCHAR(20) NOT NULL CHECK (file_type IN ('DOCUMENT', 'VIDEO', 'AUDIO')),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    mime_type VARCHAR(100) NOT NULL,
    upload_date TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_files_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for performance optimization
CREATE INDEX idx_files_user_type ON files(user_id, file_type);
CREATE INDEX idx_files_created_at ON files(created_at DESC);
CREATE INDEX idx_files_status ON files(status);
CREATE INDEX idx_files_user_id ON files(user_id);

-- Comments for documentation
COMMENT ON TABLE files IS 'Common metadata for all file types (documents, videos, audio)';
COMMENT ON COLUMN files.file_type IS 'Enum: DOCUMENT, VIDEO, AUDIO - explicit type identification';
COMMENT ON COLUMN files.stored_filename IS 'Unique filename on disk (prevents collisions)';
COMMENT ON COLUMN files.upload_date IS 'When the file was uploaded (user perspective)';
COMMENT ON COLUMN files.created_at IS 'When the record was created (system perspective)';
