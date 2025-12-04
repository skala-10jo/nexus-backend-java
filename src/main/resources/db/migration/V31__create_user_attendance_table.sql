-- V31: Create user_attendance table for daily attendance tracking
-- This migration is idempotent (safe to run multiple times)

-- Create table if not exists
CREATE TABLE IF NOT EXISTS user_attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    attendance_date DATE NOT NULL,
    CONSTRAINT fk_user_attendance_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Add unique constraint for one attendance per user per day (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_user_attendance_user_date'
    ) THEN
        ALTER TABLE user_attendance
        ADD CONSTRAINT uk_user_attendance_user_date
        UNIQUE (user_id, attendance_date);
    END IF;
END $$;

-- Create indexes if not exist
CREATE INDEX IF NOT EXISTS idx_user_attendance_user_id
    ON user_attendance(user_id);

CREATE INDEX IF NOT EXISTS idx_user_attendance_date
    ON user_attendance(attendance_date DESC);

-- Add comment
COMMENT ON TABLE user_attendance IS 'Daily attendance tracking for users';
COMMENT ON COLUMN user_attendance.id IS 'Primary key (UUID)';
COMMENT ON COLUMN user_attendance.user_id IS 'Reference to users table';
COMMENT ON COLUMN user_attendance.attendance_date IS 'Date of attendance (one per user per day)';
