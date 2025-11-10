-- V2__Add_schedule_categories.sql
-- Add schedule categories feature with many-to-many relationship support

-- ============================================================
-- 1. Create schedule_categories table
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule_categories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20) NOT NULL,
    icon VARCHAR(50),
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_schedule_categories_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT schedule_categories_user_name_unique UNIQUE (user_id, name),
    CONSTRAINT chk_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_schedule_categories_user_id ON schedule_categories(user_id);
CREATE INDEX idx_schedule_categories_display_order ON schedule_categories(display_order);

COMMENT ON TABLE schedule_categories IS 'User-defined schedule categories for classification';
COMMENT ON COLUMN schedule_categories.is_default IS 'Whether this is a system default category (cannot be deleted)';
COMMENT ON COLUMN schedule_categories.display_order IS 'Display order in UI (lower = higher priority)';

-- ============================================================
-- 2. Create junction table for many-to-many relationship
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule_category_mappings (
    schedule_id UUID NOT NULL,
    category_id UUID NOT NULL,
    PRIMARY KEY (schedule_id, category_id),
    CONSTRAINT fk_schedule_category_mappings_schedule
        FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_category_mappings_category
        FOREIGN KEY (category_id) REFERENCES schedule_categories(id) ON DELETE CASCADE
);

CREATE INDEX idx_schedule_category_mappings_schedule ON schedule_category_mappings(schedule_id);
CREATE INDEX idx_schedule_category_mappings_category ON schedule_category_mappings(category_id);

COMMENT ON TABLE schedule_category_mappings IS 'Many-to-many mapping between schedules and categories';

-- ============================================================
-- 3. Function to create default categories for a user
-- ============================================================
CREATE OR REPLACE FUNCTION create_default_categories(p_user_id UUID)
RETURNS VOID AS $$
BEGIN
    INSERT INTO schedule_categories (id, user_id, name, color, icon, is_default, display_order)
    VALUES
        (gen_random_uuid(), p_user_id, '업무', '#3B82F6', 'briefcase', true, 1),
        (gen_random_uuid(), p_user_id, '개인', '#10B981', 'user', true, 2),
        (gen_random_uuid(), p_user_id, '회의', '#8B5CF6', 'users', true, 3),
        (gen_random_uuid(), p_user_id, '휴가', '#F59E0B', 'sun', true, 4),
        (gen_random_uuid(), p_user_id, '기타', '#6B7280', 'dots-horizontal', true, 5)
    ON CONFLICT DO NOTHING;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION create_default_categories(UUID) IS 'Creates default categories for a new user';

-- ============================================================
-- 4. Create default categories for existing users
-- ============================================================
DO $$
DECLARE
    user_record RECORD;
BEGIN
    FOR user_record IN SELECT id FROM users LOOP
        PERFORM create_default_categories(user_record.id);
    END LOOP;
END $$;

-- ============================================================
-- 5. Trigger to auto-create default categories for new users
-- ============================================================
CREATE OR REPLACE FUNCTION trigger_create_default_categories()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM create_default_categories(NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER create_default_categories_on_user_insert
AFTER INSERT ON users
FOR EACH ROW
EXECUTE FUNCTION trigger_create_default_categories();

-- ============================================================
-- 6. Migrate existing schedules to use categories
-- ============================================================
-- Map existing schedules to default "기타" category based on their color
DO $$
DECLARE
    schedule_record RECORD;
    default_category_id UUID;
BEGIN
    FOR schedule_record IN
        SELECT s.id as schedule_id, s.user_id, s.color
        FROM schedules s
    LOOP
        -- Find or use default "기타" category for this user
        SELECT id INTO default_category_id
        FROM schedule_categories
        WHERE user_id = schedule_record.user_id
        AND is_default = true
        ORDER BY display_order DESC
        LIMIT 1;

        -- Create mapping if default category exists
        IF default_category_id IS NOT NULL THEN
            INSERT INTO schedule_category_mappings (schedule_id, category_id)
            VALUES (schedule_record.schedule_id, default_category_id)
            ON CONFLICT DO NOTHING;
        END IF;
    END LOOP;
END $$;

-- ============================================================
-- 7. Apply auto-update trigger for schedule_categories
-- ============================================================
CREATE TRIGGER update_schedule_categories_updated_at BEFORE UPDATE ON schedule_categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- Note: Keep color column in schedules table for now
-- It will be removed in a future migration after data verification
-- ALTER TABLE schedules DROP COLUMN color;  -- Execute this later
-- ============================================================
