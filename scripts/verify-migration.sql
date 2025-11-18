-- ================================================
-- Migration 검증 SQL 스크립트
-- 목적: V14-V18 Flyway migrations 실행 확인
-- 사용법: psql -U kihoon -d nexus -f verify-migration.sql
-- ================================================

\echo '=== 1. Flyway Migration 히스토리 확인 ==='
SELECT version, description, installed_on, success
FROM flyway_schema_history
WHERE version IN ('14', '15', '16', '17', '18')
ORDER BY version;

\echo ''
\echo '=== 2. 새로 생성된 테이블 확인 ==='
SELECT table_name,
       (SELECT COUNT(*) FROM information_schema.columns WHERE table_name = t.table_name) as column_count
FROM information_schema.tables t
WHERE table_schema = 'public'
  AND table_name IN ('files', 'document_files', 'video_files', 'project_files', 'file_id_mapping')
ORDER BY table_name;

\echo ''
\echo '=== 3. files 테이블 구조 확인 ==='
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'files'
ORDER BY ordinal_position;

\echo ''
\echo '=== 4. document_files 테이블 구조 확인 ==='
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'document_files'
ORDER BY ordinal_position;

\echo ''
\echo '=== 5. video_files 테이블 구조 확인 ==='
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'video_files'
ORDER BY ordinal_position;

\echo ''
\echo '=== 6. 인덱스 확인 ==='
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('files', 'document_files', 'video_files', 'project_files')
ORDER BY tablename, indexname;

\echo ''
\echo '=== 7. 외래 키 제약 조건 확인 ==='
SELECT
    tc.table_name,
    tc.constraint_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_name IN ('files', 'document_files', 'video_files', 'project_files', 'file_id_mapping')
ORDER BY tc.table_name, tc.constraint_name;

\echo ''
\echo '=== 8. 데이터 카운트 확인 ==='
SELECT 'files' as table_name, COUNT(*) as row_count FROM files
UNION ALL
SELECT 'document_files', COUNT(*) FROM document_files
UNION ALL
SELECT 'video_files', COUNT(*) FROM video_files
UNION ALL
SELECT 'project_files', COUNT(*) FROM project_files
UNION ALL
SELECT 'file_id_mapping', COUNT(*) FROM file_id_mapping
UNION ALL
SELECT 'documents (legacy)', COUNT(*) FROM documents;

\echo ''
\echo '=== 검증 완료 ==='
\echo '모든 테이블이 정상적으로 생성되었는지 확인하세요.'
