#!/bin/bash

# AWS 배포 스크립트

set -e

echo "🚀 MaKIT 플랫폼 AWS 배포를 시작합니다..."

# 환경 변수 확인
if [ -z "$AWS_REGION" ] || [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo "❌ AWS 환경 변수가 설정되지 않았습니다."
    echo "   다음 환경 변수를 설정해주세요:"
    echo "   - AWS_REGION"
    echo "   - AWS_ACCESS_KEY_ID"
    echo "   - AWS_SECRET_ACCESS_KEY"
    exit 1
fi

# ECR 로그인
echo "🔐 ECR에 로그인합니다..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Docker 이미지 빌드 및 태깅
echo "🐳 Docker 이미지를 빌드합니다..."
docker build -t makit-backend ./backend
docker build -t makit-frontend .

# ECR에 푸시
echo "📤 ECR에 이미지를 푸시합니다..."
docker tag makit-backend:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/makit-backend:latest
docker tag makit-frontend:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/makit-frontend:latest

docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/makit-backend:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/makit-frontend:latest

echo "✅ AWS 배포가 완료되었습니다!"
echo ""
echo "📝 다음 단계:"
echo "1. ECS 서비스 업데이트"
echo "2. 로드 밸런서 설정 확인"
echo "3. 도메인 및 SSL 인증서 설정"