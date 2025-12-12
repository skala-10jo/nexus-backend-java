-- V30: Add japanese_term and chinese_term to glossary_terms
--
-- Purpose: 전문용어 사전에 일본어/중국어 번역 필드 추가
--
-- Add new columns
ALTER TABLE glossary_terms 
ADD COLUMN japanese_term VARCHAR(255),
ADD COLUMN chinese_term VARCHAR(255);

-- Add comments for documentation
COMMENT ON COLUMN glossary_terms.japanese_term IS '일본어 번역 (日本語)';
COMMENT ON COLUMN glossary_terms.chinese_term IS '중국어 번역 (简体中文)';
