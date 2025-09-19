#!/bin/bash

# MaKIT 전체 AWS 배포 자동화 스크립트

set -e

echo "🚀 MaKIT 전체 AWS 배포를 시작합니다..."
echo "예상 소요 시간: 15-20분"
echo ""

# 사전 확인
echo "🔍 사전 확인..."
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI가 설치되지 않았습니다."
    exit 1
fi

if ! command -v docker &> /dev/null; then
    echo "❌ Docker가 설치되지 않았습니다."
    exit 1
fi

if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS 자격 증명이 설정되지 않았습니다."
    echo "aws configure를 실행하여 자격 증명을 설정해주세요."
    exit 1
fi

echo "✅ 사전 확인 완료"
echo ""

# 단계별 실행
echo "1️⃣ 인프라 설정 (5분)"
chmod +x scripts/aws-infrastructure.sh
./scripts/aws-infrastructure.sh

echo ""
echo "2️⃣ RDS 생성 (5-10분)"
chmod +x scripts/create-rds.sh
./scripts/create-rds.sh &
RDS_PID=$!

echo ""
echo "3️⃣ ElastiCache 생성 (3-5분)"
chmod +x scripts/create-elasticache.sh
./scripts/create-elasticache.sh &
CACHE_PID=$!

echo ""
echo "4️⃣ Docker 이미지 빌드 (3-5분)"
chmod +x scripts/build-and-push.sh
./scripts/build-and-push.sh

echo ""
echo "⏳ RDS 및 ElastiCache 생성 완료 대기..."
wait $RDS_PID
wait $CACHE_PID

echo ""
echo "5️⃣ ECS 서비스 배포 (2-3분)"
chmod +x scripts/deploy-ecs.sh
./scripts/deploy-ecs.sh

echo ""
echo "🎉 MaKIT 플랫폼 AWS 배포 완료!"
echo ""
echo "📋 배포 결과:"
source infrastructure-outputs.env
echo "🌐 애플리케이션 URL: http://$ALB_DNS"
echo "🔗 API 엔드포인트: http://$ALB_DNS/api"
echo "📊 ECS 클러스터: ${PROJECT_NAME}-cluster"
echo "🗄️ RDS 엔드포인트: $RDS_ENDPOINT"
echo "⚡ Redis 엔드포인트: $REDIS_ENDPOINT"
echo ""
echo "📝 다음 단계:"
echo "1. 도메인 연결 (Route 53)"
echo "2. SSL 인증서 설정 (Certificate Manager)"
echo "3. CloudFront CDN 설정"
echo "4. 모니터링 및 알람 설정"
echo ""
echo "⚠️ 주의사항:"
echo "- 서비스가 완전히 시작되기까지 5-10분 소요"
echo "- 비용 발생: 월 $100-200 예상"
echo "- 사용하지 않을 때는 리소스 정리 필요"