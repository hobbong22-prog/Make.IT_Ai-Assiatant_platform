#!/bin/bash

# MaKIT Docker 이미지 빌드 및 ECR 푸시

set -e

# 환경 변수 로드
source infrastructure-outputs.env

echo "🐳 Docker 이미지 빌드 및 ECR 푸시 시작..."

# ECR 로그인
echo "🔐 ECR 로그인..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# 백엔드 이미지 빌드
echo "🏗️ 백엔드 이미지 빌드 중..."
docker build -t ${PROJECT_NAME}-backend ./backend

# 프론트엔드 이미지 빌드
echo "🎨 프론트엔드 이미지 빌드 중..."
docker build -t ${PROJECT_NAME}-frontend .

# 이미지 태깅
echo "🏷️ 이미지 태깅..."
docker tag ${PROJECT_NAME}-backend:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/${PROJECT_NAME}-backend:latest
docker tag ${PROJECT_NAME}-frontend:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/${PROJECT_NAME}-frontend:latest

# ECR에 푸시
echo "📤 ECR에 이미지 푸시 중..."
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/${PROJECT_NAME}-backend:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/${PROJECT_NAME}-frontend:latest

# 이미지 URI 저장
BACKEND_IMAGE_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/${PROJECT_NAME}-backend:latest"
FRONTEND_IMAGE_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/${PROJECT_NAME}-frontend:latest"

echo "BACKEND_IMAGE_URI=$BACKEND_IMAGE_URI" >> infrastructure-outputs.env
echo "FRONTEND_IMAGE_URI=$FRONTEND_IMAGE_URI" >> infrastructure-outputs.env

echo "✅ Docker 이미지 빌드 및 푸시 완료!"
echo "백엔드 이미지: $BACKEND_IMAGE_URI"
echo "프론트엔드 이미지: $FRONTEND_IMAGE_URI"