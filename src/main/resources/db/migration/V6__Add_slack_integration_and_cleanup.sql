-- V6__Add_slack_integration_and_cleanup.sql
-- Add Slack integration to users table (consistent with Outlook pattern)
-- Migrate existing slack_integrations data and cleanup duplicate tables

-- ============================================================
-- 1. Add Slack columns to users table (same pattern as Outlook)
-- ============================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS slack_workspace_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS slack_workspace_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS slack_access_token TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS slack_bot_user_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS slack_bot_access_token TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS slack_user_access_token TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS slack_scope TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS slack_is_active BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS slack_token_expires_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS slack_connected_at TIMESTAMP;

-- Add index for Slack workspace lookup
CREATE INDEX IF NOT EXISTS idx_users_slack_workspace_id ON users(slack_workspace_id);
CREATE INDEX IF NOT EXISTS idx_users_slack_is_active ON users(slack_is_active);

-- Add column comments
COMMENT ON COLUMN users.slack_workspace_id IS 'Connected Slack workspace/team ID (e.g., T09RUC0MLMQ)';
COMMENT ON COLUMN users.slack_workspace_name IS 'Human-readable workspace name';
COMMENT ON COLUMN users.slack_access_token IS 'Slack OAuth access token (bot token, should be encrypted in application)';
COMMENT ON COLUMN users.slack_bot_user_id IS 'Slack bot user ID (e.g., U09RN5MC42F)';
COMMENT ON COLUMN users.slack_bot_access_token IS 'Bot OAuth access token (may be same as access_token)';
COMMENT ON COLUMN users.slack_user_access_token IS 'User OAuth access token for user-scoped operations';
COMMENT ON COLUMN users.slack_scope IS 'Granted OAuth scopes (comma-separated)';
COMMENT ON COLUMN users.slack_is_active IS 'Whether Slack integration is currently active';
COMMENT ON COLUMN users.slack_token_expires_at IS 'Slack token expiration timestamp (if provided by Slack)';
COMMENT ON COLUMN users.slack_connected_at IS 'Timestamp when Slack was first connected';

-- ============================================================
-- 2. Migrate existing slack_integrations data to users table
--    (Only if slack_integrations table exists)
-- ============================================================
DO $$
BEGIN
    -- Check if slack_integrations table exists
    IF EXISTS (
        SELECT FROM information_schema.tables
        WHERE table_schema = 'public'
        AND table_name = 'slack_integrations'
    ) THEN
        -- Migrate data from slack_integrations to users
        UPDATE users u
        SET
            slack_workspace_id = s.workspace_id,
            slack_workspace_name = s.workspace_name,
            slack_access_token = s.access_token,
            slack_bot_user_id = s.bot_user_id,
            slack_bot_access_token = s.bot_access_token,
            slack_user_access_token = s.user_access_token,
            slack_scope = s.scope,
            slack_is_active = s.is_active,
            slack_connected_at = s.created_at
        FROM slack_integrations s
        WHERE u.id = s.user_id;

        RAISE NOTICE 'Migrated data from slack_integrations to users table';
    ELSE
        RAISE NOTICE 'slack_integrations table does not exist, skipping data migration';
    END IF;
END $$;

-- ============================================================
-- 3. Create slack_messages table (no project_id - communication only)
-- ============================================================
CREATE TABLE IF NOT EXISTS slack_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,

    -- Slack message identifiers
    channel_id VARCHAR(255) NOT NULL,
    channel_name VARCHAR(255),
    channel_type VARCHAR(50),  -- public_channel, private_channel, im, mpim
    message_ts VARCHAR(50) NOT NULL,  -- Slack timestamp (unique message ID)
    thread_ts VARCHAR(50),  -- Parent message timestamp (for threaded replies)

    -- Message sender info
    user_slack_id VARCHAR(255) NOT NULL,  -- Slack user ID who sent the message
    username VARCHAR(255),
    user_real_name VARCHAR(255),

    -- Message content
    text TEXT,
    message_type VARCHAR(50),  -- message, file_share, channel_join, etc.
    subtype VARCHAR(50),  -- bot_message, file_comment, etc.

    -- Message metadata
    has_attachments BOOLEAN DEFAULT FALSE,
    has_files BOOLEAN DEFAULT FALSE,
    is_read BOOLEAN DEFAULT FALSE,
    is_starred BOOLEAN DEFAULT FALSE,

    -- Timestamps
    posted_at TIMESTAMP NOT NULL,
    edited_at TIMESTAMP,
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_slack_messages_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Unique constraint (prevent duplicate message sync)
    CONSTRAINT uk_slack_message
        UNIQUE (user_id, channel_id, message_ts)
);

-- Indexes for performance
CREATE INDEX idx_slack_messages_user_id ON slack_messages(user_id);
CREATE INDEX idx_slack_messages_channel_id ON slack_messages(channel_id);
CREATE INDEX idx_slack_messages_thread_ts ON slack_messages(thread_ts) WHERE thread_ts IS NOT NULL;
CREATE INDEX idx_slack_messages_is_read ON slack_messages(is_read);
CREATE INDEX idx_slack_messages_posted_at ON slack_messages(posted_at DESC);
CREATE INDEX idx_slack_messages_user_channel ON slack_messages(user_id, channel_id, posted_at DESC);

-- Table comments
COMMENT ON TABLE slack_messages IS 'Synchronized Slack messages for communication tracking';
COMMENT ON COLUMN slack_messages.message_ts IS 'Slack message timestamp (unique ID in format like 1234567890.123456)';
COMMENT ON COLUMN slack_messages.thread_ts IS 'Parent message timestamp for threaded replies (NULL for top-level messages)';
COMMENT ON COLUMN slack_messages.channel_type IS 'Channel type: public_channel, private_channel, im (direct message), mpim (group DM)';

-- ============================================================
-- 4. Create slack_message_attachments table
-- ============================================================
CREATE TABLE IF NOT EXISTS slack_message_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL,

    -- Slack file info
    file_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    title VARCHAR(500),
    mimetype VARCHAR(100),
    filetype VARCHAR(50),  -- pdf, png, docx, etc.
    size BIGINT,

    -- URLs (Slack provides multiple URL types)
    url_private VARCHAR(1000),  -- Private download URL
    url_private_download VARCHAR(1000),
    permalink VARCHAR(1000),  -- Public permalink

    -- Metadata
    is_external BOOLEAN DEFAULT FALSE,  -- External file vs Slack-hosted
    is_public BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_slack_attachments_message
        FOREIGN KEY (message_id) REFERENCES slack_messages(id) ON DELETE CASCADE,

    -- Check constraints
    CONSTRAINT chk_slack_attachment_size
        CHECK (size IS NULL OR size >= 0)
);

-- Indexes
CREATE INDEX idx_slack_message_attachments_message_id ON slack_message_attachments(message_id);
CREATE INDEX idx_slack_message_attachments_file_id ON slack_message_attachments(file_id);

-- Comments
COMMENT ON TABLE slack_message_attachments IS 'Slack message file attachments';
COMMENT ON COLUMN slack_message_attachments.file_id IS 'Slack file ID (e.g., F1234567890)';
COMMENT ON COLUMN slack_message_attachments.url_private IS 'Private download URL (requires authentication)';

-- ============================================================
-- 5. Drop old slack_integrations table
-- ============================================================
DROP TABLE IF EXISTS slack_integrations CASCADE;

-- ============================================================
-- 6. Drop duplicate outlook_mails table
-- ============================================================
DROP TABLE IF EXISTS outlook_mails CASCADE;

-- ============================================================
-- 7. Update users table comment
-- ============================================================
COMMENT ON TABLE users IS 'User accounts with integrated Outlook and Slack connections (1:1 pattern)';
