-- V12: Drop email_embeddings table (migrated to Qdrant)
-- Author: NEXUS Team
-- Date: 2025-01-17
--
-- Email embeddings는 이제 Qdrant vector database에서 관리됩니다.
-- PostgreSQL의 email_embeddings 테이블과 관련 인덱스를 제거합니다.

-- 인덱스 제거 (테이블 삭제 전 명시적으로 제거)
DROP INDEX IF EXISTS email_embeddings_metadata_idx;
DROP INDEX IF EXISTS email_embeddings_email_id_idx;
DROP INDEX IF EXISTS email_embeddings_embedding_idx;

-- 테이블 제거
DROP TABLE IF EXISTS email_embeddings;

-- 참고:
-- - pgvector 확장은 다른 테이블에서 사용할 수 있으므로 유지합니다
-- - emails 테이블은 그대로 유지됩니다 (메일 메타데이터용)
-- - 임베딩 벡터는 Qdrant의 "email_embeddings" collection에서 관리됩니다
