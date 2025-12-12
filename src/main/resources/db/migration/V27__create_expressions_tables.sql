-- V27: Create expressions and user_expressions tables
--
-- Purpose: 비즈니스 영어 표현 학습 시스템
--

-- ============================================================================
-- 1. expressions 테이블 (표현 마스터 데이터)
-- ============================================================================
CREATE TABLE expressions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 표현 정보
    expression VARCHAR(500) NOT NULL UNIQUE,  -- 영어 표현
    meaning VARCHAR(500) NOT NULL,            -- 한국어 의미
    examples JSONB NOT NULL,                  -- 예문 배열

    -- 분류 정보
    unit VARCHAR(200) NOT NULL,               -- 단원
    chapter VARCHAR(200) NOT NULL,            -- 챕터
    source_section VARCHAR(200)               -- 출처 섹션
);

-- 인덱스
CREATE INDEX idx_expressions_expression ON expressions(expression);
CREATE INDEX idx_expressions_unit ON expressions(unit);
CREATE INDEX idx_expressions_chapter ON expressions(chapter);


-- ============================================================================
-- 2. user_expressions 테이블 (사용자별 학습 여부)
-- ============================================================================
CREATE TABLE user_expressions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expression_id UUID NOT NULL REFERENCES expressions(id) ON DELETE CASCADE,

    is_learned BOOLEAN NOT NULL DEFAULT FALSE,

    -- 제약 조건 (사용자당 표현 중복 방지)
    CONSTRAINT uk_user_expressions UNIQUE(user_id, expression_id)
);

-- 인덱스
CREATE INDEX idx_user_expressions_user_id ON user_expressions(user_id);
CREATE INDEX idx_user_expressions_expression_id ON user_expressions(expression_id);
