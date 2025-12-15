-- V37: Add document summary column to document_files table
--
-- Stores extracted text and AI-generated summary for faster
-- glossary extraction and scenario generation.

-- Add extracted_text column (stores full document text)
ALTER TABLE document_files
ADD COLUMN extracted_text TEXT;

-- Add summary column (stores AI-generated summary)
ALTER TABLE document_files
ADD COLUMN summary TEXT;

-- Add processed_at column (tracks when document was processed)
ALTER TABLE document_files
ADD COLUMN processed_at TIMESTAMP WITH TIME ZONE;
