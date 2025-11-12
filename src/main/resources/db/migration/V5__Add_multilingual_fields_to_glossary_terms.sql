-- V5__Add_multilingual_fields_to_glossary_terms.sql
-- Add Vietnamese term, example sentence, and note fields to glossary_terms table

ALTER TABLE glossary_terms
ADD COLUMN vietnamese_term VARCHAR(255),
ADD COLUMN example_sentence TEXT,
ADD COLUMN note TEXT;

-- Add index for Vietnamese term search optimization
CREATE INDEX idx_glossary_terms_vietnamese ON glossary_terms(vietnamese_term);

-- Add comment for documentation
COMMENT ON COLUMN glossary_terms.vietnamese_term IS 'Vietnamese translation of the term';
COMMENT ON COLUMN glossary_terms.example_sentence IS 'Example sentence showing term usage';
COMMENT ON COLUMN glossary_terms.note IS 'Additional notes and references';
