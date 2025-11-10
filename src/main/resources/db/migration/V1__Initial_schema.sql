-- V1__Initial_schema.sql
-- NEXUS Translation Platform - Initial Database Schema
-- This migration creates the complete initial database structure based on current entities

-- ============================================================
-- 1. Users Table
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(30) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

COMMENT ON TABLE users IS 'User accounts for the NEXUS platform';
COMMENT ON COLUMN users.password_hash IS 'Bcrypt hashed password';
COMMENT ON COLUMN users.avatar_url IS 'URL to user profile picture';

-- ============================================================
-- 2. Projects Table
-- ============================================================
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_projects_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);

COMMENT ON TABLE projects IS 'Translation projects containing multiple documents';
COMMENT ON COLUMN projects.status IS 'Project status: ACTIVE, ARCHIVED, or DELETED';

-- ============================================================
-- 3. Documents Table
-- ============================================================
CREATE TYPE document_status AS ENUM ('UPLOADED', 'PROCESSING', 'PROCESSED', 'FAILED');

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) UNIQUE NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    upload_date TIMESTAMP NOT NULL,
    status document_status NOT NULL,
    is_analyzed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_documents_file_size CHECK (file_size > 0)
);

CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_upload_date ON documents(upload_date DESC);
CREATE INDEX idx_documents_stored_filename ON documents(stored_filename);

COMMENT ON TABLE documents IS 'Uploaded documents for translation and analysis';
COMMENT ON COLUMN documents.stored_filename IS 'Internal storage filename (UUID-based)';
COMMENT ON COLUMN documents.is_analyzed IS 'Whether document has been analyzed for glossary terms';

-- ============================================================
-- 4. Project-Document Junction Table (Many-to-Many)
-- ============================================================
CREATE TABLE IF NOT EXISTS project_documents (
    project_id UUID NOT NULL,
    document_id UUID NOT NULL,
    PRIMARY KEY (project_id, document_id),
    CONSTRAINT fk_project_documents_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_documents_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_project_documents_project_id ON project_documents(project_id);
CREATE INDEX idx_project_documents_document_id ON project_documents(document_id);

COMMENT ON TABLE project_documents IS 'Many-to-many relationship between projects and documents';

-- ============================================================
-- 5. Document Metadata Table
-- ============================================================
CREATE TABLE IF NOT EXISTS document_metadata (
    id UUID PRIMARY KEY,
    document_id UUID UNIQUE NOT NULL,
    language VARCHAR(10),
    page_count INTEGER,
    word_count INTEGER,
    character_count INTEGER,
    encoding VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_metadata_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT chk_metadata_counts CHECK (
        (page_count IS NULL OR page_count >= 0) AND
        (word_count IS NULL OR word_count >= 0) AND
        (character_count IS NULL OR character_count >= 0)
    )
);

CREATE INDEX idx_document_metadata_document_id ON document_metadata(document_id);
CREATE INDEX idx_document_metadata_language ON document_metadata(language);

COMMENT ON TABLE document_metadata IS 'Extracted metadata from documents';
COMMENT ON COLUMN document_metadata.language IS 'Detected document language (ISO 639-1 code)';

-- ============================================================
-- 6. Document Content Table
-- ============================================================
CREATE TABLE IF NOT EXISTS document_content (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    page_number INTEGER,
    content_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_content_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT chk_content_page_number CHECK (page_number IS NULL OR page_number > 0)
);

CREATE INDEX idx_document_content_document_id ON document_content(document_id);
CREATE INDEX idx_document_content_page_number ON document_content(document_id, page_number);

COMMENT ON TABLE document_content IS 'Extracted text content from documents, stored per page';
COMMENT ON COLUMN document_content.page_number IS 'Page number in the original document (null for non-paginated content)';

-- ============================================================
-- 7. Glossary Terms Table
-- ============================================================
CREATE TABLE IF NOT EXISTS glossary_terms (
    id UUID PRIMARY KEY,
    project_id UUID,
    user_id UUID NOT NULL,
    korean_term VARCHAR(255) NOT NULL,
    english_term VARCHAR(255),
    abbreviation VARCHAR(100),
    definition TEXT NOT NULL,
    context TEXT,
    domain VARCHAR(100),
    confidence_score DECIMAL(3,2),
    status VARCHAR(20) NOT NULL DEFAULT 'AUTO_EXTRACTED',
    is_verified BOOLEAN DEFAULT FALSE,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_glossary_terms_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_glossary_terms_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT glossary_terms_user_korean_unique UNIQUE (user_id, korean_term),
    CONSTRAINT chk_glossary_status CHECK (status IN ('AUTO_EXTRACTED', 'USER_ADDED', 'USER_EDITED')),
    CONSTRAINT chk_confidence_score CHECK (confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 1)),
    CONSTRAINT chk_usage_count CHECK (usage_count >= 0)
);

CREATE INDEX idx_glossary_terms_project_id ON glossary_terms(project_id);
CREATE INDEX idx_glossary_terms_user_id ON glossary_terms(user_id);
CREATE INDEX idx_glossary_terms_korean ON glossary_terms(korean_term);
CREATE INDEX idx_glossary_terms_english ON glossary_terms(english_term);
CREATE INDEX idx_glossary_terms_status ON glossary_terms(status);
CREATE INDEX idx_glossary_terms_domain ON glossary_terms(domain);

COMMENT ON TABLE glossary_terms IS 'Translation glossary terms extracted from documents or added by users';
COMMENT ON COLUMN glossary_terms.status IS 'Source of term: AUTO_EXTRACTED, USER_ADDED, or USER_EDITED';
COMMENT ON COLUMN glossary_terms.is_verified IS 'Whether term has been verified by user (independent from status)';
COMMENT ON COLUMN glossary_terms.confidence_score IS 'AI confidence score for auto-extracted terms (0.00-1.00)';

-- ============================================================
-- 8. Glossary Term-Document Junction Table (Many-to-Many)
-- ============================================================
CREATE TABLE IF NOT EXISTS glossary_term_documents (
    term_id UUID NOT NULL,
    document_id UUID NOT NULL,
    PRIMARY KEY (term_id, document_id),
    CONSTRAINT fk_glossary_term_documents_term FOREIGN KEY (term_id) REFERENCES glossary_terms(id) ON DELETE CASCADE,
    CONSTRAINT fk_glossary_term_documents_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_glossary_term_documents_term_id ON glossary_term_documents(term_id);
CREATE INDEX idx_glossary_term_documents_document_id ON glossary_term_documents(document_id);

COMMENT ON TABLE glossary_term_documents IS 'Many-to-many relationship tracking which documents contain which glossary terms';

-- ============================================================
-- 9. Glossary Extraction Jobs Table
-- ============================================================
CREATE TABLE IF NOT EXISTS glossary_extraction_jobs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    document_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress INTEGER DEFAULT 0,
    terms_extracted INTEGER DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_glossary_jobs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_glossary_jobs_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT glossary_extraction_jobs_document_unique UNIQUE (document_id),
    CONSTRAINT chk_extraction_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_extraction_progress CHECK (progress >= 0 AND progress <= 100),
    CONSTRAINT chk_terms_extracted CHECK (terms_extracted >= 0)
);

CREATE INDEX idx_glossary_jobs_user_id ON glossary_extraction_jobs(user_id);
CREATE INDEX idx_glossary_jobs_document_id ON glossary_extraction_jobs(document_id);
CREATE INDEX idx_glossary_jobs_status ON glossary_extraction_jobs(status);
CREATE INDEX idx_glossary_jobs_created_at ON glossary_extraction_jobs(created_at DESC);

COMMENT ON TABLE glossary_extraction_jobs IS 'Tracks background jobs for extracting glossary terms from documents';
COMMENT ON COLUMN glossary_extraction_jobs.progress IS 'Job completion percentage (0-100)';

-- ============================================================
-- 10. Schedules Table
-- ============================================================
CREATE TABLE IF NOT EXISTS schedules (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    all_day BOOLEAN DEFAULT FALSE,
    color VARCHAR(20),
    location VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedules_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_schedules_time CHECK (end_time IS NULL OR end_time >= start_time)
);

CREATE INDEX idx_schedules_user_id ON schedules(user_id);
CREATE INDEX idx_schedules_start_time ON schedules(start_time);
CREATE INDEX idx_schedules_end_time ON schedules(end_time);

COMMENT ON TABLE schedules IS 'User calendar events and schedules';
COMMENT ON COLUMN schedules.all_day IS 'Whether this is an all-day event';
COMMENT ON COLUMN schedules.color IS 'Color code for calendar display (e.g., #FF5733)';

-- ============================================================
-- Audit and Monitoring
-- ============================================================

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply auto-update trigger to relevant tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_projects_updated_at BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_glossary_terms_updated_at BEFORE UPDATE ON glossary_terms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_schedules_updated_at BEFORE UPDATE ON schedules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- Initial Data / Seed Data (Optional)
-- ============================================================

-- This section can be used to insert any required initial data
-- For now, we keep it empty as initial data should be handled separately

-- ============================================================
-- Schema Version Info
-- ============================================================

COMMENT ON DATABASE nexus IS 'NEXUS Translation Platform Database - Version 1.0';
