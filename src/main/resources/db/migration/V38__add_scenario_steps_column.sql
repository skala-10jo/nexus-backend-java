-- V38: Add steps column to scenarios table
--
-- Stores structured conversation flow steps for scenario practice.
-- Each step contains: name, title, guide, terminology

-- Add steps column (stores structured step data as JSONB)
ALTER TABLE scenarios
ADD COLUMN steps JSONB DEFAULT '[]'::jsonb;

-- Add comment for documentation
COMMENT ON COLUMN scenarios.steps IS 'Structured conversation steps with guide and terminology for each step';
