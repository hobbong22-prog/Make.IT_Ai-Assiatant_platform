#!/bin/bash

echo "🔍 MaKIT API 테스트 시작..."

# 헬스체크
echo "1. 헬스체크 테스트"
curl -s -o /dev/null -w "Status: %{http_code}\n" http://localhost:8083/actuator/health

# 사용자 목록 조회
echo "2. 사용자 목록 조회"
curl -s -o /dev/null -w "Status: %{http_code}\n" http://localhost:8083/api/auth/users

# 캠페인 목록 조회
echo "3. 캠페인 목록 조회"
curl -s -o /dev/null -w "Status: %{http_code}\n" http://localhost:8083/api/campaigns

# 콘텐츠 목록 조회
echo "4. 콘텐츠 목록 조회"
curl -s -o /dev/null -w "Status: %{http_code}\n" http://localhost:8083/api/content

echo "✅ API 테스트 완료"