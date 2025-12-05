-- V33: Add missing FK constraint to speaking_analysis_sessions
-- Ensures user_id references users table with CASCADE delete
-- This migration is idempotent (safe to run multiple times)

-- First, clean up any orphan records (sessions with non-existent user_id)
-- This should not delete anything in a healthy database
DELETE FROM speaking_analysis_sessions
WHERE user_id NOT IN (SELECT id FROM users);

-- Add FK constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_speaking_sessions_user'
        AND conrelid = 'speaking_analysis_sessions'::regclass
    ) THEN
        ALTER TABLE speaking_analysis_sessions
        ADD CONSTRAINT fk_speaking_sessions_user
            FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE;

        RAISE NOTICE 'Added FK constraint fk_speaking_sessions_user';
    ELSE
        RAISE NOTICE 'FK constraint fk_speaking_sessions_user already exists';
    END IF;
END $$;

-- Add comment
COMMENT ON CONSTRAINT fk_speaking_sessions_user ON speaking_analysis_sessions
    IS 'Foreign key to users table with cascade delete';
