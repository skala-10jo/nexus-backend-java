-- V5: Add email embeddings table for RAG-based email search
-- Author: NEXUS Team
-- Date: 2025-01-12

-- pgvector 확장 설치 (이미 설치되어 있으면 무시)
-- CREATE EXTENSION IF NOT EXISTS vector;

-- email_embeddings 테이블 생성
CREATE TABLE email_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email_id UUID NOT NULL REFERENCES emails(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(1536) NOT NULL,  -- OpenAI text-embedding-ada-002
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(email_id, chunk_index)  -- 같은 메일의 같은 청크는 중복 방지
);

-- HNSW 인덱스 생성 (빠른 유사도 검색)
CREATE INDEX email_embeddings_embedding_idx
ON email_embeddings
USING hnsw (embedding vector_cosine_ops);

-- email_id로 빠른 조회를 위한 인덱스
CREATE INDEX email_embeddings_email_id_idx
ON email_embeddings(email_id);

-- 메타데이터 필터링을 위한 GIN 인덱스
CREATE INDEX email_embeddings_metadata_idx
ON email_embeddings
USING gin (metadata);

-- 코멘트 추가
COMMENT ON TABLE email_embeddings IS 'Email content embeddings for semantic search (RAG)';
COMMENT ON COLUMN email_embeddings.email_id IS 'Reference to emails table';
COMMENT ON COLUMN email_embeddings.chunk_index IS 'Chunk order within the email (0, 1, 2, ...)';
COMMENT ON COLUMN email_embeddings.chunk_text IS 'Chunked text with metadata (subject, from/to, date)';
COMMENT ON COLUMN email_embeddings.embedding IS 'OpenAI embedding vector (1536 dimensions)';
COMMENT ON COLUMN email_embeddings.metadata IS 'JSON metadata: {subject, from_name, to_recipients, date, folder, has_attachments}';
