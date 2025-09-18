#!/bin/bash

# MaKIT 플랫폼 설정 스크립트

echo "🚀 MaKIT 플랫폼 설정을 시작합니다..."

# 환경 변수 파일 생성
if [ ! -f .env ]; then
    echo "📝 환경 변수 파일(.env)을 생성합니다..."
    cat > .env << EOF
# AWS 설정
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=your_access_key_here
AWS_SECRET_ACCESS_KEY=your_secret_key_here

# 데이터베이스 설정
POSTGRES_DB=makit
POSTGRES_USER=makit_user
POSTGRES_PASSWORD=makit_password

# JWT 설정
JWT_SECRET=mySecretKeyForJWTTokenGeneration

# 애플리케이션 설정
SPRING_PROFILES_ACTIVE=docker
EOF
    echo "✅ .env 파일이 생성되었습니다. AWS 자격 증명을 설정해주세요."
else
    echo "ℹ️  .env 파일이 이미 존재합니다."
fi

# Docker 이미지 빌드
echo "🐳 Docker 이미지를 빌드합니다..."
docker-compose build

# 데이터베이스 초기화 확인
echo "🗄️  데이터베이스 설정을 확인합니다..."
if [ ! -f backend/src/main/resources/db/migration/V1__Initial_schema.sql ]; then
    echo "⚠️  데이터베이스 마이그레이션 파일이 없습니다."
    echo "   JPA가 자동으로 스키마를 생성합니다."
fi

echo "✅ 설정이 완료되었습니다!"
echo ""
echo "🎯 다음 명령어로 애플리케이션을 실행하세요:"
echo "   docker-compose up -d"
echo ""
echo "📱 접속 URL:"
echo "   프론트엔드: http://localhost"
echo "   백엔드 API: http://localhost:8080"
echo "   데이터베이스: localhost:5432"
echo ""
echo "🔧 개발 모드로 실행하려면:"
echo "   cd backend && mvn spring-boot:run"