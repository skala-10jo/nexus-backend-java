-- Add preferred_language column to users table
-- Used for Slack message translation feature

ALTER TABLE users
ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(10) DEFAULT 'ko';

-- Add comment for documentation
COMMENT ON COLUMN users.preferred_language IS 'User preferred language for Slack translation (ko, en, ja, vi, zh)';
