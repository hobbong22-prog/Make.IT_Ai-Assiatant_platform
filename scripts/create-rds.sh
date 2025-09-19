#!/bin/bash

# MaKIT RDS PostgreSQL 인스턴스 생성

set -e

# 환경 변수 로드
source infrastructure-outputs.env

echo "🗄️ RDS PostgreSQL 인스턴스 생성 중..."

# RDS 인스턴스 생성
aws rds create-db-instance \
    --db-instance-identifier ${PROJECT_NAME}-postgres \
    --db-instance-class db.t3.micro \
    --engine postgres \
    --engine-version 15.4 \
    --master-username makituser \
    --master-user-password MakitPassword123! \
    --allocated-storage 20 \
    --storage-type gp2 \
    --vpc-security-group-ids $SG_RDS \
    --db-subnet-group-name ${PROJECT_NAME}-db-subnet-group \
    --backup-retention-period 7 \
    --storage-encrypted \
    --multi-az false \
    --publicly-accessible false \
    --tags Key=Name,Value=${PROJECT_NAME}-postgres \
    --deletion-protection false

echo "⏳ RDS 인스턴스 생성 중... (약 5-10분 소요)"
echo "상태 확인: aws rds describe-db-instances --db-instance-identifier ${PROJECT_NAME}-postgres"

# RDS 엔드포인트 대기 및 저장
echo "RDS 엔드포인트 대기 중..."
aws rds wait db-instance-available --db-instance-identifier ${PROJECT_NAME}-postgres

RDS_ENDPOINT=$(aws rds describe-db-instances \
    --db-instance-identifier ${PROJECT_NAME}-postgres \
    --query 'DBInstances[0].Endpoint.Address' \
    --output text)

echo "RDS_ENDPOINT=$RDS_ENDPOINT" >> infrastructure-outputs.env
echo "✅ RDS 인스턴스 생성 완료: $RDS_ENDPOINT"