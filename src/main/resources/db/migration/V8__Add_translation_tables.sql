-- V6__Add_translation_tables.sql
-- NEXUS Translation Platform - Translation Feature Tables
-- This migration creates tables for translation history and detected term tracking

-- ============================================================
-- 1. Translations Table
-- ============================================================
CREATE TABLE IF NOT EXISTS translations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    project_id UUID,
    original_text TEXT NOT NULL,
    translated_text TEXT NOT NULL,
    source_language VARCHAR(10) NOT NULL,
    target_language VARCHAR(10) NOT NULL,
    context_used BOOLEAN NOT NULL DEFAULT FALSE,
    context_summary TEXT,
    terms_detected INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_translations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_translations_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT chk_translations_terms_detected CHECK (terms_detected >= 0)
);

CREATE INDEX idx_translations_user_id ON translations(user_id);
CREATE INDEX idx_translations_project_id ON translations(project_id);
CREATE INDEX idx_translations_created_at ON translations(created_at DESC);
CREATE INDEX idx_translations_source_target ON translations(source_language, target_language);
CREATE INDEX idx_translations_context_used ON translations(context_used);

COMMENT ON TABLE translations IS '번역 기록 - 사용자의 번역 요청과 결과를 저장';
COMMENT ON COLUMN translations.user_id IS '번역을 요청한 사용자 ID';
COMMENT ON COLUMN translations.project_id IS '연결된 프로젝트 ID (선택사항)';
COMMENT ON COLUMN translations.original_text IS '번역 전 원문';
COMMENT ON COLUMN translations.translated_text IS '번역 후 결과문';
COMMENT ON COLUMN translations.source_language IS '원본 언어 코드 (ko, en, ja, vi 등)';
COMMENT ON COLUMN translations.target_language IS '목표 언어 코드';
COMMENT ON COLUMN translations.context_used IS '프로젝트 컨텍스트(문서/용어집) 사용 여부';
COMMENT ON COLUMN translations.context_summary IS '사용된 문서 컨텍스트 요약';
COMMENT ON COLUMN translations.terms_detected IS '탐지된 전문용어 개수';

-- ============================================================
-- 2. Translation Terms Table
-- ============================================================
CREATE TABLE IF NOT EXISTS translation_terms (
    id UUID PRIMARY KEY,
    translation_id UUID NOT NULL,
    glossary_term_id UUID NOT NULL,
    position_start INTEGER NOT NULL,
    position_end INTEGER NOT NULL,
    matched_text VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_translation_terms_translation FOREIGN KEY (translation_id) REFERENCES translations(id) ON DELETE CASCADE,
    CONSTRAINT fk_translation_terms_glossary_term FOREIGN KEY (glossary_term_id) REFERENCES glossary_terms(id) ON DELETE CASCADE,
    CONSTRAINT chk_translation_terms_position CHECK (position_start >= 0 AND position_end > position_start)
);

CREATE INDEX idx_translation_terms_translation_id ON translation_terms(translation_id);
CREATE INDEX idx_translation_terms_glossary_term_id ON translation_terms(glossary_term_id);
CREATE INDEX idx_translation_terms_position ON translation_terms(translation_id, position_start);

COMMENT ON TABLE translation_terms IS '번역-용어 매핑 - 번역 시 탐지된 전문용어와 위치 정보';
COMMENT ON COLUMN translation_terms.translation_id IS '번역 기록 ID';
COMMENT ON COLUMN translation_terms.glossary_term_id IS '탐지된 용어집 용어 ID';
COMMENT ON COLUMN translation_terms.position_start IS '원문에서 용어의 시작 위치 (0-based index)';
COMMENT ON COLUMN translation_terms.position_end IS '원문에서 용어의 종료 위치 (exclusive)';
COMMENT ON COLUMN translation_terms.matched_text IS '실제로 탐지된 텍스트 (대소문자, 변형 형태 보존)';
