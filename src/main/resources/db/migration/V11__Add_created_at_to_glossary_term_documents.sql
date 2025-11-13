-- Add created_at column to glossary_term_documents table
ALTER TABLE glossary_term_documents
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
