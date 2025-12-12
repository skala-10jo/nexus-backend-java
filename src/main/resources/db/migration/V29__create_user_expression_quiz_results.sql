-- V29: Create user_expression_quiz_results table
--
-- Purpose: 사용자별 표현 퀴즈 결과 관리 (예문별 정답/오답 횟수 추적)
--

CREATE TABLE user_expression_quiz_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    expression_id UUID NOT NULL,
    example_index INTEGER NOT NULL,
    correct_count INTEGER NOT NULL DEFAULT 0,
    incorrect_count INTEGER NOT NULL DEFAULT 0,
    last_attempted_at TIMESTAMP WITH TIME ZONE,

    -- Foreign keys
    CONSTRAINT fk_user_expression_quiz_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_expression_quiz_expression
        FOREIGN KEY (expression_id) REFERENCES expressions(id) ON DELETE CASCADE,

    -- Unique constraint: 사용자당 표현의 예문별 한 행
    CONSTRAINT uk_user_expression_quiz UNIQUE (user_id, expression_id, example_index),

    -- Check constraints
    CONSTRAINT chk_example_index CHECK (example_index >= 0),
    CONSTRAINT chk_correct_count CHECK (correct_count >= 0),
    CONSTRAINT chk_incorrect_count CHECK (incorrect_count >= 0)
);

-- Indexes for performance
CREATE INDEX idx_user_expression_quiz_user_id ON user_expression_quiz_results(user_id);
CREATE INDEX idx_user_expression_quiz_expression_id ON user_expression_quiz_results(expression_id);
CREATE INDEX idx_user_expression_quiz_last_attempted ON user_expression_quiz_results(last_attempted_at DESC);

-- Comments
COMMENT ON TABLE user_expression_quiz_results IS '사용자별 표현 퀴즈 결과 (예문별 정답/오답 횟수 추적)';
COMMENT ON COLUMN user_expression_quiz_results.example_index IS 'expressions.examples JSONB 배열의 인덱스 (0, 1, 2...)';
COMMENT ON COLUMN user_expression_quiz_results.correct_count IS '해당 예문의 정답 횟수';
COMMENT ON COLUMN user_expression_quiz_results.incorrect_count IS '해당 예문의 오답 횟수';
COMMENT ON COLUMN user_expression_quiz_results.last_attempted_at IS '마지막 퀴즈 시도 시간';
