-- V34: Add daily delta columns to user_expression_quiz_results
--
-- Purpose: 일별 정확한 통계를 위한 증분 필드 추가
-- 기존 correct_count/incorrect_count는 누적값, 새 필드는 일별 증분
--


-- 일별 증분 추적 필드 추가
ALTER TABLE user_expression_quiz_results
ADD COLUMN daily_correct_delta INTEGER NOT NULL DEFAULT 0,
ADD COLUMN daily_incorrect_delta INTEGER NOT NULL DEFAULT 0,
ADD COLUMN delta_date DATE;

-- 기존 데이터 마이그레이션: last_attempted_at 기준으로 delta 값 설정
UPDATE user_expression_quiz_results
SET delta_date = DATE(last_attempted_at),
    daily_correct_delta = correct_count,
    daily_incorrect_delta = incorrect_count
WHERE last_attempted_at IS NOT NULL;

-- 인덱스 추가 (일별 통계 쿼리 성능)
CREATE INDEX idx_quiz_results_delta_date ON user_expression_quiz_results(delta_date);
CREATE INDEX idx_quiz_results_user_delta_date ON user_expression_quiz_results(user_id, delta_date);

-- Comments
COMMENT ON COLUMN user_expression_quiz_results.daily_correct_delta IS '해당 날짜의 정답 횟수 증분';
COMMENT ON COLUMN user_expression_quiz_results.daily_incorrect_delta IS '해당 날짜의 오답 횟수 증분';
COMMENT ON COLUMN user_expression_quiz_results.delta_date IS '증분 계산 기준 날짜';
