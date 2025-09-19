#!/bin/bash

# MaKIT ElastiCache Redis 클러스터 생성

set -e

# 환경 변수 로드
source infrastructure-outputs.env

echo "⚡ ElastiCache Redis 클러스터 생성 중..."

# ElastiCache 보안 그룹 생성
SG_CACHE=$(aws ec2 create-security-group \
    --group-name ${PROJECT_NAME}-cache-sg \
    --description "Security group for ElastiCache" \
    --vpc-id $VPC_ID \
    --tag-specifications "ResourceType=security-group,Tags=[{Key=Name,Value=${PROJECT_NAME}-cache-sg}]" \
    --query 'GroupId' --output text)

aws ec2 authorize-security-group-ingress --group-id $SG_CACHE --protocol tcp --port 6379 --source-group $SG_ECS

# ElastiCache 클러스터 생성
aws elasticache create-cache-cluster \
    --cache-cluster-id ${PROJECT_NAME}-redis \
    --cache-node-type cache.t3.micro \
    --engine redis \
    --num-cache-nodes 1 \
    --cache-subnet-group-name ${PROJECT_NAME}-cache-subnet-group \
    --security-group-ids $SG_CACHE \
    --tags Key=Name,Value=${PROJECT_NAME}-redis

echo "⏳ ElastiCache 클러스터 생성 중... (약 3-5분 소요)"

# ElastiCache 엔드포인트 대기 및 저장
echo "ElastiCache 엔드포인트 대기 중..."
aws elasticache wait cache-cluster-available --cache-cluster-id ${PROJECT_NAME}-redis

REDIS_ENDPOINT=$(aws elasticache describe-cache-clusters \
    --cache-cluster-id ${PROJECT_NAME}-redis \
    --show-cache-node-info \
    --query 'CacheClusters[0].CacheNodes[0].Endpoint.Address' \
    --output text)

echo "SG_CACHE=$SG_CACHE" >> infrastructure-outputs.env
echo "REDIS_ENDPOINT=$REDIS_ENDPOINT" >> infrastructure-outputs.env
echo "✅ ElastiCache 클러스터 생성 완료: $REDIS_ENDPOINT"