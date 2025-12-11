-- V36__Remove_default_schedule_categories.sql
-- Remove default schedule categories (업무, 개인, 회의, 휴가, 기타)
-- Categories will now come from Outlook calendar sync instead

-- ============================================================
-- 1. Delete category mappings that reference default categories
-- ============================================================
DELETE FROM schedule_category_mappings
WHERE category_id IN (
    SELECT id FROM schedule_categories WHERE is_default = true
);

-- ============================================================
-- 2. Delete all default categories
-- ============================================================
DELETE FROM schedule_categories WHERE is_default = true;

-- ============================================================
-- 3. Drop the trigger that creates default categories for new users
-- ============================================================
DROP TRIGGER IF EXISTS create_default_categories_on_user_insert ON users;

-- ============================================================
-- 4. Replace the function with an empty one (to maintain compatibility)
-- ============================================================
CREATE OR REPLACE FUNCTION create_default_categories(p_user_id UUID)
RETURNS VOID AS $$
BEGIN
    -- No longer creates default categories
    -- Categories are now imported from Outlook calendar sync
    NULL;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION create_default_categories(UUID) IS 'Deprecated: No longer creates default categories. Categories come from Outlook sync.';

-- ============================================================
-- 5. Also replace the trigger function
-- ============================================================
CREATE OR REPLACE FUNCTION trigger_create_default_categories()
RETURNS TRIGGER AS $$
BEGIN
    -- No longer creates default categories
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
