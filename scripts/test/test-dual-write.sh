#!/bin/bash

# Dual Write 기능 테스트 스크립트
# 새로운 파일 업로드가 files + documents 양쪽에 저장되는지 검증

set -e

BASE_URL="http://localhost:3000"
TEST_USER="testuser_$(date +%s)"
TEST_EMAIL="test_$(date +%s)@example.com"
TEST_PASSWORD="Test1234!"

echo "======================================"
echo "🧪 Dual Write 기능 테스트 시작"
echo "======================================"
echo ""

# Step 1: 사용자 등록
echo "📝 Step 1: 테스트 사용자 생성..."
REGISTER_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"${TEST_USER}\",
    \"email\": \"${TEST_EMAIL}\",
    \"password\": \"${TEST_PASSWORD}\",
    \"fullName\": \"Test User\"
  }")

echo "Response: $REGISTER_RESPONSE"

# Step 2: 로그인하여 JWT 토큰 받기
echo ""
echo "🔑 Step 2: 로그인 및 JWT 토큰 획득..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"${TEST_USER}\",
    \"password\": \"${TEST_PASSWORD}\"
  }")

echo "Login Response: $LOGIN_RESPONSE"

# JWT 토큰 추출 (jq 사용)
if command -v jq &> /dev/null; then
    TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.token // .token // empty')
else
    # jq가 없으면 간단한 문자열 파싱
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
fi

if [ -z "$TOKEN" ]; then
    echo "❌ JWT 토큰을 가져오지 못했습니다."
    echo "Login response: $LOGIN_RESPONSE"
    exit 1
fi

echo "✅ JWT Token: ${TOKEN:0:50}..."

# Step 3: 테스트 파일 생성
echo ""
echo "📄 Step 3: 테스트 파일 생성..."
TEST_FILE="/tmp/test-document-$(date +%s).txt"
echo "This is a test document for Dual Write validation." > "$TEST_FILE"
echo "Created at: $(date)" >> "$TEST_FILE"
echo "File size: $(wc -c < "$TEST_FILE") bytes" >> "$TEST_FILE"

echo "✅ 테스트 파일 생성 완료: $TEST_FILE"

# Step 4: 파일 업로드 전 DB 상태 확인
echo ""
echo "🗄️  Step 4: 업로드 전 DB 상태 확인..."
echo "--- 기존 documents 테이블 ---"
psql nexus -c "SELECT COUNT(*) as old_documents_count FROM documents;" -t

echo "--- 새로운 files 테이블 ---"
psql nexus -c "SELECT COUNT(*) as new_files_count FROM files;" -t

OLD_DOC_COUNT=$(psql nexus -t -c "SELECT COUNT(*) FROM documents;")
OLD_FILE_COUNT=$(psql nexus -t -c "SELECT COUNT(*) FROM files;")

echo "Before: documents=$OLD_DOC_COUNT, files=$OLD_FILE_COUNT"

# Step 5: 파일 업로드 (Dual Write 테스트)
echo ""
echo "📤 Step 5: 파일 업로드 (Dual Write)..."
UPLOAD_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/files/documents" \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "file=@${TEST_FILE}")

echo "Upload Response: $UPLOAD_RESPONSE"

# Step 6: 업로드 후 DB 상태 확인
echo ""
echo "🗄️  Step 6: 업로드 후 DB 상태 확인..."
sleep 2  # DB에 반영될 시간 대기

NEW_DOC_COUNT=$(psql nexus -t -c "SELECT COUNT(*) FROM documents;")
NEW_FILE_COUNT=$(psql nexus -t -c "SELECT COUNT(*) FROM files;")
MAPPING_COUNT=$(psql nexus -t -c "SELECT COUNT(*) FROM file_id_mapping;")

echo "After: documents=$NEW_DOC_COUNT, files=$NEW_FILE_COUNT, mappings=$MAPPING_COUNT"

# Step 7: Dual Write 검증
echo ""
echo "======================================"
echo "✅ Dual Write 검증 결과"
echo "======================================"

DOC_DIFF=$((NEW_DOC_COUNT - OLD_DOC_COUNT))
FILE_DIFF=$((NEW_FILE_COUNT - OLD_FILE_COUNT))

echo "📊 변화량:"
echo "  - documents 테이블: +$DOC_DIFF"
echo "  - files 테이블: +$FILE_DIFF"
echo "  - file_id_mapping: $MAPPING_COUNT"

if [ "$DOC_DIFF" -eq 1 ] && [ "$FILE_DIFF" -eq 1 ]; then
    echo ""
    echo "🎉 Dual Write 성공!"
    echo "   ✅ documents 테이블에 저장됨 (+1)"
    echo "   ✅ files 테이블에 저장됨 (+1)"

    if [ "$MAPPING_COUNT" -gt 0 ]; then
        echo "   ✅ file_id_mapping에 매핑 저장됨"
    fi

    # 매핑 상세 정보 출력
    echo ""
    echo "📋 최신 매핑 정보:"
    psql nexus -c "SELECT new_file_id, old_document_id, created_at FROM file_id_mapping ORDER BY created_at DESC LIMIT 1;"

    echo ""
    echo "🔍 새로 생성된 files 레코드:"
    psql nexus -c "SELECT id, file_type, original_filename, file_size, created_at FROM files ORDER BY created_at DESC LIMIT 1;"

    echo ""
    echo "🔍 새로 생성된 documents 레코드:"
    psql nexus -c "SELECT id, original_filename, file_size, created_at FROM documents ORDER BY created_at DESC LIMIT 1;"

    exit 0
else
    echo ""
    echo "❌ Dual Write 실패!"
    echo "   Expected: documents +1, files +1"
    echo "   Got: documents +$DOC_DIFF, files +$FILE_DIFF"

    echo ""
    echo "서버 로그 확인:"
    tail -50 /tmp/latest-server.log | grep -i error || echo "No errors found in log"

    exit 1
fi

# Cleanup
rm -f "$TEST_FILE"
