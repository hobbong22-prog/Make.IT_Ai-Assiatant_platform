#!/bin/bash

# MaKIT ECS 서비스 배포

set -e

# 환경 변수 로드
source infrastructure-outputs.env

echo "🚀 ECS 서비스 배포 시작..."

# 1. CloudWatch 로그 그룹 생성
echo "📊 CloudWatch 로그 그룹 생성..."
aws logs create-log-group --log-group-name /ecs/makit-backend --region $AWS_REGION || echo "Backend log group already exists"
aws logs create-log-group --log-group-name /ecs/makit-frontend --region $AWS_REGION || echo "Frontend log group already exists"

# 2. IAM 역할 생성 (존재하지 않는 경우)
echo "🔐 IAM 역할 확인 및 생성..."

# ECS Task Execution Role
aws iam create-role \
    --role-name ecsTaskExecutionRole \
    --assume-role-policy-document '{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Principal": {
                    "Service": "ecs-tasks.amazonaws.com"
                },
                "Action": "sts:AssumeRole"
            }
        ]
    }' || echo "ecsTaskExecutionRole already exists"

aws iam attach-role-policy \
    --role-name ecsTaskExecutionRole \
    --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy || echo "Policy already attached"

# ECS Task Role
aws iam create-role \
    --role-name ecsTaskRole \
    --assume-role-policy-document '{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Principal": {
                    "Service": "ecs-tasks.amazonaws.com"
                },
                "Action": "sts:AssumeRole"
            }
        ]
    }' || echo "ecsTaskRole already exists"

# Bedrock 및 S3 접근 정책 생성 및 연결
aws iam put-role-policy \
    --role-name ecsTaskRole \
    --policy-name MakitBedrockS3Policy \
    --policy-document '{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Action": [
                    "bedrock:InvokeModel",
                    "bedrock:InvokeModelWithResponseStream",
                    "s3:GetObject",
                    "s3:PutObject",
                    "s3:DeleteObject",
                    "s3:ListBucket"
                ],
                "Resource": "*"
            }
        ]
    }' || echo "Policy already exists"

# 3. Application Load Balancer 생성
echo "⚖️ Application Load Balancer 생성..."

ALB_ARN=$(aws elbv2 create-load-balancer \
    --name ${PROJECT_NAME}-alb \
    --subnets $SUBNET_PUBLIC_1 $SUBNET_PUBLIC_2 \
    --security-groups $SG_ALB \
    --scheme internet-facing \
    --type application \
    --ip-address-type ipv4 \
    --tags Key=Name,Value=${PROJECT_NAME}-alb \
    --query 'LoadBalancers[0].LoadBalancerArn' \
    --output text)

# ALB DNS 이름 가져오기
ALB_DNS=$(aws elbv2 describe-load-balancers \
    --load-balancer-arns $ALB_ARN \
    --query 'LoadBalancers[0].DNSName' \
    --output text)

# 4. 타겟 그룹 생성
echo "🎯 타겟 그룹 생성..."

# 백엔드 타겟 그룹
TG_BACKEND_ARN=$(aws elbv2 create-target-group \
    --name ${PROJECT_NAME}-backend-tg \
    --protocol HTTP \
    --port 8080 \
    --vpc-id $VPC_ID \
    --target-type ip \
    --health-check-path /actuator/health \
    --health-check-interval-seconds 30 \
    --health-check-timeout-seconds 5 \
    --healthy-threshold-count 2 \
    --unhealthy-threshold-count 3 \
    --query 'TargetGroups[0].TargetGroupArn' \
    --output text)

# 프론트엔드 타겟 그룹
TG_FRONTEND_ARN=$(aws elbv2 create-target-group \
    --name ${PROJECT_NAME}-frontend-tg \
    --protocol HTTP \
    --port 80 \
    --vpc-id $VPC_ID \
    --target-type ip \
    --health-check-path / \
    --health-check-interval-seconds 30 \
    --health-check-timeout-seconds 5 \
    --healthy-threshold-count 2 \
    --unhealthy-threshold-count 3 \
    --query 'TargetGroups[0].TargetGroupArn' \
    --output text)

# 5. ALB 리스너 생성
echo "👂 ALB 리스너 생성..."

# 기본 리스너 (프론트엔드로 라우팅)
aws elbv2 create-listener \
    --load-balancer-arn $ALB_ARN \
    --protocol HTTP \
    --port 80 \
    --default-actions Type=forward,TargetGroupArn=$TG_FRONTEND_ARN

# API 경로 리스너 규칙 (백엔드로 라우팅)
LISTENER_ARN=$(aws elbv2 describe-listeners \
    --load-balancer-arn $ALB_ARN \
    --query 'Listeners[0].ListenerArn' \
    --output text)

aws elbv2 create-rule \
    --listener-arn $LISTENER_ARN \
    --priority 100 \
    --conditions Field=path-pattern,Values="/api/*" \
    --actions Type=forward,TargetGroupArn=$TG_BACKEND_ARN

# 6. 태스크 정의 등록
echo "📋 태스크 정의 등록..."

# 환경 변수 치환
envsubst < ecs-task-definition.json > ecs-task-definition-final.json

TASK_DEFINITION_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://ecs-task-definition-final.json \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

# 7. ECS 서비스 생성
echo "🐳 ECS 서비스 생성..."

aws ecs create-service \
    --cluster ${PROJECT_NAME}-cluster \
    --service-name ${PROJECT_NAME}-service \
    --task-definition $TASK_DEFINITION_ARN \
    --desired-count 2 \
    --launch-type FARGATE \
    --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_PRIVATE_1,$SUBNET_PRIVATE_2],securityGroups=[$SG_ECS],assignPublicIp=DISABLED}" \
    --load-balancers targetGroupArn=$TG_BACKEND_ARN,containerName=makit-backend,containerPort=8080 targetGroupArn=$TG_FRONTEND_ARN,containerName=makit-frontend,containerPort=80 \
    --health-check-grace-period-seconds 300

# 환경 변수 저장
echo "ALB_ARN=$ALB_ARN" >> infrastructure-outputs.env
echo "ALB_DNS=$ALB_DNS" >> infrastructure-outputs.env
echo "TG_BACKEND_ARN=$TG_BACKEND_ARN" >> infrastructure-outputs.env
echo "TG_FRONTEND_ARN=$TG_FRONTEND_ARN" >> infrastructure-outputs.env

echo "✅ ECS 서비스 배포 완료!"
echo ""
echo "🌐 애플리케이션 접속 정보:"
echo "URL: http://$ALB_DNS"
echo "API: http://$ALB_DNS/api"
echo ""
echo "📊 모니터링:"
echo "ECS 콘솔: https://console.aws.amazon.com/ecs/home?region=$AWS_REGION#/clusters/${PROJECT_NAME}-cluster"
echo "CloudWatch 로그: https://console.aws.amazon.com/cloudwatch/home?region=$AWS_REGION#logsV2:log-groups"
echo ""
echo "⏳ 서비스가 완전히 시작되기까지 5-10분 정도 소요될 수 있습니다."