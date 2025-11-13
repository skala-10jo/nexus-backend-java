-- Change document status column from ENUM to VARCHAR
ALTER TABLE documents
ALTER COLUMN status TYPE VARCHAR(20);

-- Drop the old ENUM type
DROP TYPE IF EXISTS document_status CASCADE;

-- Add check constraint to ensure valid values
ALTER TABLE documents
ADD CONSTRAINT chk_document_status
CHECK (status IN ('UPLOADED', 'PROCESSING', 'PROCESSED', 'FAILED'));
