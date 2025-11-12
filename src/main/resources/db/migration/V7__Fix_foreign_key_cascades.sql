-- ============================================================
-- V7: Fix Foreign Key Constraints to Support CASCADE DELETE
-- ============================================================
-- 목적: Hibernate가 생성한 외래키 제약조건에 CASCADE DELETE를 추가하여
--       문서 삭제 및 용어 삭제 시 참조 무결성 오류 방지

-- ============================================================
-- 1. project_documents 테이블 수정 (문서 삭제 지원)
-- ============================================================
-- Hibernate가 생성한 제약조건 제거
ALTER TABLE project_documents DROP CONSTRAINT IF EXISTS fk845kd34e1hofxb67dkr4nowqe;

-- CASCADE DELETE를 포함한 새 제약조건 추가
-- 문서 삭제 시 project_documents의 관련 레코드도 자동 삭제됨
ALTER TABLE project_documents
ADD CONSTRAINT fk_project_documents_document
FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

-- ============================================================
-- 2. translation_terms 테이블 수정 (용어 삭제 지원)
-- ============================================================
-- Hibernate가 생성한 제약조건 제거
ALTER TABLE translation_terms DROP CONSTRAINT IF EXISTS fkndm6cb3512wdvikt1ily3r249;
ALTER TABLE translation_terms DROP CONSTRAINT IF EXISTS fkbkr5b4vv93ek9jf664ltntpa5;

-- CASCADE DELETE를 포함한 새 제약조건 추가
-- 용어 삭제 시 translation_terms의 관련 레코드도 자동 삭제됨
ALTER TABLE translation_terms
ADD CONSTRAINT fk_translation_terms_glossary_term
FOREIGN KEY (glossary_term_id) REFERENCES glossary_terms(id) ON DELETE CASCADE;

-- 번역 삭제 시 translation_terms의 관련 레코드도 자동 삭제됨
ALTER TABLE translation_terms
ADD CONSTRAINT fk_translation_terms_translation
FOREIGN KEY (translation_id) REFERENCES translations(id) ON DELETE CASCADE;

-- ============================================================
-- 변경사항 요약
-- ============================================================
-- 1. documents 삭제 시 project_documents 자동 삭제
-- 2. glossary_terms 삭제 시 translation_terms 자동 삭제
-- 3. translations 삭제 시 translation_terms 자동 삭제
