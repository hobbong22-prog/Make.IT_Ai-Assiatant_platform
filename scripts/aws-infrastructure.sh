#!/bin/bash

# MaKIT ECS Fargate 인프라 설정 스크립트

set -e

echo "🚀 MaKIT ECS Fargate 인프라 설정을 시작합니다..."

# 환경 변수 설정
export AWS_REGION=${AWS_REGION:-ap-northeast-2}
export PROJECT_NAME="makit"
export ENVIRONMENT=${ENVIRONMENT:-prod}

# AWS 계정 ID 가져오기
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "AWS Account ID: $AWS_ACCOUNT_ID"

# 1. VPC 및 네트워킹 설정
echo "📡 VPC 및 네트워킹 설정..."

# VPC 생성
VPC_ID=$(aws ec2 create-vpc \
    --cidr-block 10.0.0.0/16 \
    --tag-specifications "ResourceType=vpc,Tags=[{Key=Name,Value=${PROJECT_NAME}-vpc}]" \
    --query 'Vpc.VpcId' --output text)
echo "VPC 생성 완료: $VPC_ID"

# 인터넷 게이트웨이 생성 및 연결
IGW_ID=$(aws ec2 create-internet-gateway \
    --tag-specifications "ResourceType=internet-gateway,Tags=[{Key=Name,Value=${PROJECT_NAME}-igw}]" \
    --query 'InternetGateway.InternetGatewayId' --output text)
aws ec2 attach-internet-gateway --vpc-id $VPC_ID --internet-gateway-id $IGW_ID
echo "인터넷 게이트웨이 생성 및 연결 완료: $IGW_ID"

# 퍼블릭 서브넷 생성 (2개 AZ)
SUBNET_PUBLIC_1=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block 10.0.1.0/24 \
    --availability-zone ${AWS_REGION}a \
    --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-public-1}]" \
    --query 'Subnet.SubnetId' --output text)

SUBNET_PUBLIC_2=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block 10.0.2.0/24 \
    --availability-zone ${AWS_REGION}c \
    --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-public-2}]" \
    --query 'Subnet.SubnetId' --output text)

echo "퍼블릭 서브넷 생성 완료: $SUBNET_PUBLIC_1, $SUBNET_PUBLIC_2"

# 프라이빗 서브넷 생성 (2개 AZ)
SUBNET_PRIVATE_1=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block 10.0.3.0/24 \
    --availability-zone ${AWS_REGION}a \
    --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-private-1}]" \
    --query 'Subnet.SubnetId' --output text)

SUBNET_PRIVATE_2=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block 10.0.4.0/24 \
    --availability-zone ${AWS_REGION}c \
    --tag-specifications "ResourceType=subnet,Tags=[{Key=Name,Value=${PROJECT_NAME}-private-2}]" \
    --query 'Subnet.SubnetId' --output text)

echo "프라이빗 서브넷 생성 완료: $SUBNET_PRIVATE_1, $SUBNET_PRIVATE_2"

# 라우팅 테이블 설정
ROUTE_TABLE_PUBLIC=$(aws ec2 create-route-table \
    --vpc-id $VPC_ID \
    --tag-specifications "ResourceType=route-table,Tags=[{Key=Name,Value=${PROJECT_NAME}-public-rt}]" \
    --query 'RouteTable.RouteTableId' --output text)

aws ec2 create-route --route-table-id $ROUTE_TABLE_PUBLIC --destination-cidr-block 0.0.0.0/0 --gateway-id $IGW_ID
aws ec2 associate-route-table --subnet-id $SUBNET_PUBLIC_1 --route-table-id $ROUTE_TABLE_PUBLIC
aws ec2 associate-route-table --subnet-id $SUBNET_PUBLIC_2 --route-table-id $ROUTE_TABLE_PUBLIC

echo "라우팅 테이블 설정 완료"

# 2. 보안 그룹 생성
echo "🔒 보안 그룹 생성..."

# ALB 보안 그룹
SG_ALB=$(aws ec2 create-security-group \
    --group-name ${PROJECT_NAME}-alb-sg \
    --description "Security group for ALB" \
    --vpc-id $VPC_ID \
    --tag-specifications "ResourceType=security-group,Tags=[{Key=Name,Value=${PROJECT_NAME}-alb-sg}]" \
    --query 'GroupId' --output text)

aws ec2 authorize-security-group-ingress --group-id $SG_ALB --protocol tcp --port 80 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-id $SG_ALB --protocol tcp --port 443 --cidr 0.0.0.0/0

# ECS 보안 그룹
SG_ECS=$(aws ec2 create-security-group \
    --group-name ${PROJECT_NAME}-ecs-sg \
    --description "Security group for ECS tasks" \
    --vpc-id $VPC_ID \
    --tag-specifications "ResourceType=security-group,Tags=[{Key=Name,Value=${PROJECT_NAME}-ecs-sg}]" \
    --query 'GroupId' --output text)

aws ec2 authorize-security-group-ingress --group-id $SG_ECS --protocol tcp --port 8080 --source-group $SG_ALB
aws ec2 authorize-security-group-ingress --group-id $SG_ECS --protocol tcp --port 80 --source-group $SG_ALB

# RDS 보안 그룹
SG_RDS=$(aws ec2 create-security-group \
    --group-name ${PROJECT_NAME}-rds-sg \
    --description "Security group for RDS" \
    --vpc-id $VPC_ID \
    --tag-specifications "ResourceType=security-group,Tags=[{Key=Name,Value=${PROJECT_NAME}-rds-sg}]" \
    --query 'GroupId' --output text)

aws ec2 authorize-security-group-ingress --group-id $SG_RDS --protocol tcp --port 5432 --source-group $SG_ECS

echo "보안 그룹 생성 완료"

# 3. ECR 리포지토리 생성
echo "📦 ECR 리포지토리 생성..."

aws ecr create-repository --repository-name ${PROJECT_NAME}-backend --region $AWS_REGION || echo "Backend repository already exists"
aws ecr create-repository --repository-name ${PROJECT_NAME}-frontend --region $AWS_REGION || echo "Frontend repository already exists"

echo "ECR 리포지토리 생성 완료"

# 4. RDS 서브넷 그룹 생성
echo "🗄️ RDS 서브넷 그룹 생성..."

aws rds create-db-subnet-group \
    --db-subnet-group-name ${PROJECT_NAME}-db-subnet-group \
    --db-subnet-group-description "Subnet group for MaKIT RDS" \
    --subnet-ids $SUBNET_PRIVATE_1 $SUBNET_PRIVATE_2 \
    --tags Key=Name,Value=${PROJECT_NAME}-db-subnet-group || echo "DB subnet group already exists"

# 5. ElastiCache 서브넷 그룹 생성
echo "⚡ ElastiCache 서브넷 그룹 생성..."

aws elasticache create-cache-subnet-group \
    --cache-subnet-group-name ${PROJECT_NAME}-cache-subnet-group \
    --cache-subnet-group-description "Subnet group for MaKIT ElastiCache" \
    --subnet-ids $SUBNET_PRIVATE_1 $SUBNET_PRIVATE_2 || echo "Cache subnet group already exists"

# 6. ECS 클러스터 생성
echo "🐳 ECS 클러스터 생성..."

aws ecs create-cluster \
    --cluster-name ${PROJECT_NAME}-cluster \
    --capacity-providers FARGATE \
    --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1 \
    --tags key=Name,value=${PROJECT_NAME}-cluster || echo "ECS cluster already exists"

# 환경 변수 파일 생성
cat > infrastructure-outputs.env << EOF
# MaKIT 인프라 출력 변수
AWS_ACCOUNT_ID=$AWS_ACCOUNT_ID
AWS_REGION=$AWS_REGION
VPC_ID=$VPC_ID
SUBNET_PUBLIC_1=$SUBNET_PUBLIC_1
SUBNET_PUBLIC_2=$SUBNET_PUBLIC_2
SUBNET_PRIVATE_1=$SUBNET_PRIVATE_1
SUBNET_PRIVATE_2=$SUBNET_PRIVATE_2
SG_ALB=$SG_ALB
SG_ECS=$SG_ECS
SG_RDS=$SG_RDS
PROJECT_NAME=$PROJECT_NAME
EOF

echo "✅ 인프라 설정 완료!"
echo "📄 설정 정보가 infrastructure-outputs.env 파일에 저장되었습니다."
echo ""
echo "다음 단계:"
echo "1. RDS 인스턴스 생성: ./create-rds.sh"
echo "2. ElastiCache 클러스터 생성: ./create-elasticache.sh"
echo "3. Docker 이미지 빌드 및 푸시: ./build-and-push.sh"
echo "4. ECS 서비스 배포: ./deploy-ecs.sh"