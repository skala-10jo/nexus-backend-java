-- Rollback V18: Drop file_id_mapping table
--
-- WARNING: This will delete all ID mapping data
-- Only run this if you need to completely rollback the migration

DROP TABLE IF EXISTS file_id_mapping CASCADE;
