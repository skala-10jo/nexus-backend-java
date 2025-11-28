-- V17: Create project_files table (M:N relationship between projects and files)
--
-- This table replaces 'project_documents' join table.
-- It maintains the M:N relationship between projects and files (documents/videos).

CREATE TABLE project_files (
    project_id UUID NOT NULL,
    file_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (project_id, file_id),

    CONSTRAINT fk_project_files_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_files_file FOREIGN KEY (file_id)
        REFERENCES files(id) ON DELETE CASCADE
);

-- Indexes for efficient lookups from both directions
CREATE INDEX idx_project_files_file ON project_files(file_id);
CREATE INDEX idx_project_files_project ON project_files(project_id);

-- Comments
COMMENT ON TABLE project_files IS 'M:N join table between projects and files (documents/videos)';
COMMENT ON COLUMN project_files.created_at IS 'When the file was added to the project';
