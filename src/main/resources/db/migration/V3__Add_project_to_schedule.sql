-- Add project_id column to schedules table
ALTER TABLE schedules
ADD COLUMN IF NOT EXISTS project_id UUID;

-- Add foreign key constraint (drop if exists first to handle re-runs)
ALTER TABLE schedules
DROP CONSTRAINT IF EXISTS fk_schedules_project;

ALTER TABLE schedules
ADD CONSTRAINT fk_schedules_project
FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_schedules_project_id ON schedules(project_id);
