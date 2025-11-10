-- V3__Add_outlook_and_email_features.sql
-- Add Outlook integration and email management features
-- Remove unused language columns from projects table

-- ============================================================
-- 1. Add Outlook columns to users table
-- ============================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS outlook_email VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS outlook_access_token TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS outlook_refresh_token TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS outlook_token_expires_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS outlook_delta_link TEXT;

CREATE INDEX IF NOT EXISTS idx_users_outlook_email ON users(outlook_email);

COMMENT ON COLUMN users.outlook_email IS 'Connected Outlook/Microsoft account email';
COMMENT ON COLUMN users.outlook_access_token IS 'OAuth access token for Microsoft Graph API';
COMMENT ON COLUMN users.outlook_refresh_token IS 'OAuth refresh token for token renewal';
COMMENT ON COLUMN users.outlook_token_expires_at IS 'Token expiration timestamp';
COMMENT ON COLUMN users.outlook_delta_link IS 'Delta sync link for incremental email sync';

-- ============================================================
-- 2. Create emails table
-- ============================================================
CREATE TABLE IF NOT EXISTS emails (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    project_id UUID,
    message_id VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    from_address VARCHAR(255) NOT NULL,
    from_name VARCHAR(255),
    to_recipients TEXT,
    cc_recipients TEXT,
    bcc_recipients TEXT,
    body TEXT,
    body_preview VARCHAR(500),
    body_type VARCHAR(20),
    has_attachments BOOLEAN NOT NULL DEFAULT FALSE,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    conversation_id VARCHAR(255),
    folder VARCHAR(50),
    received_date_time TIMESTAMP,
    sent_date_time TIMESTAMP,
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_emails_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_emails_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT uk_emails_message_user UNIQUE (message_id, user_id)
);

CREATE INDEX idx_emails_user_id ON emails(user_id);
CREATE INDEX idx_emails_project_id ON emails(project_id);
CREATE INDEX idx_emails_folder ON emails(folder);
CREATE INDEX idx_emails_is_read ON emails(is_read);
CREATE INDEX idx_emails_received_date_time ON emails(received_date_time DESC);

COMMENT ON TABLE emails IS 'Synchronized emails from Outlook/Microsoft 365';
COMMENT ON COLUMN emails.message_id IS 'Unique Microsoft Graph API message ID';
COMMENT ON COLUMN emails.body_type IS 'Email body format: HTML or Text';
COMMENT ON COLUMN emails.folder IS 'Outlook folder name (Inbox, SentItems, etc.)';

-- ============================================================
-- 3. Create email_attachments table
-- ============================================================
CREATE TABLE IF NOT EXISTS email_attachments (
    id UUID PRIMARY KEY,
    email_id UUID NOT NULL,
    attachment_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    size BIGINT,
    is_inline BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_attachments_email FOREIGN KEY (email_id) REFERENCES emails(id) ON DELETE CASCADE,
    CONSTRAINT chk_attachment_size CHECK (size IS NULL OR size >= 0)
);

CREATE INDEX idx_email_attachments_email_id ON email_attachments(email_id);

COMMENT ON TABLE email_attachments IS 'Email attachments metadata';
COMMENT ON COLUMN email_attachments.attachment_id IS 'Microsoft Graph API attachment ID';
COMMENT ON COLUMN email_attachments.is_inline IS 'Whether attachment is inline (embedded in body)';

-- ============================================================
-- 4. Remove unused language columns from projects table
-- ============================================================
ALTER TABLE projects DROP COLUMN IF EXISTS source_language;
ALTER TABLE projects DROP COLUMN IF EXISTS target_language;

COMMENT ON TABLE projects IS 'Translation projects containing multiple documents';
