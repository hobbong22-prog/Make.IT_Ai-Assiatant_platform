#!/bin/bash

# MaKIT AWS 리소스 정리 스크립트

set -e

# 환경 변수 로드
if [ -f infrastructure-outputs.env ]; then
    source infrastructure-outputs.env
else
    echo "❌ infrastructure-outputs.env 파일을 찾을 수 없습니다."
    exit 1
fi

echo "🧹 MaKIT AWS 리소스 정리를 시작합니다..."
echo "⚠️ 이 작업은 되돌릴 수 없습니다!"
read -p "계속하시겠습니까? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "정리 작업이 취소되었습니다."
    exit 1
fi

# 1. ECS 서비스 삭제
echo "🐳 ECS 서비스 정리..."
aws ecs update-service \
    --cluster ${PROJECT_NAME}-cluster \
    --service ${PROJECT_NAME}-service \
    --desired-count 0 || echo "Service not found"

aws ecs wait services-stable \
    --cluster ${PROJECT_NAME}-cluster \
    --services ${PROJECT_NAME}-service || echo "Service wait failed"

aws ecs delete-service \
    --cluster ${PROJECT_NAME}-cluster \
    --service ${PROJECT_NAME}-service || echo "Service deletion failed"

# 2. ECS 클러스터 삭제
aws ecs delete-cluster --cluster ${PROJECT_NAME}-cluster || echo "Cluster deletion failed"

# 3. ALB 삭제
echo "⚖️ Load Balancer 정리..."
aws elbv2 delete-load-balancer --load-balancer-arn $ALB_ARN || echo "ALB deletion failed"

# 4. 타겟 그룹 삭제
aws elbv2 delete-target-group --target-group-arn $TG_BACKEND_ARN || echo "Backend TG deletion failed"
aws elbv2 delete-target-group --target-group-arn $TG_FRONTEND_ARN || echo "Frontend TG deletion failed"

# 5. RDS 삭제
echo "🗄️ RDS 인스턴스 정리..."
aws rds delete-db-instance \
    --db-instance-identifier ${PROJECT_NAME}-postgres \
    --skip-final-snapshot || echo "RDS deletion failed"

# 6. ElastiCache 삭제
echo "⚡ ElastiCache 클러스터 정리..."
aws elasticache delete-cache-cluster \
    --cache-cluster-id ${PROJECT_NAME}-redis || echo "Cache deletion failed"

# 7. 보안 그룹 삭제 (의존성 때문에 나중에)
echo "🔒 보안 그룹 정리..."
sleep 60  # 리소스 삭제 대기

aws ec2 delete-security-group --group-id $SG_RDS || echo "RDS SG deletion failed"
aws ec2 delete-security-group --group-id $SG_CACHE || echo "Cache SG deletion failed"
aws ec2 delete-security-group --group-id $SG_ECS || echo "ECS SG deletion failed"
aws ec2 delete-security-group --group-id $SG_ALB || echo "ALB SG deletion failed"

# 8. 서브넷 및 VPC 정리
echo "🌐 네트워크 리소스 정리..."
aws ec2 delete-subnet --subnet-id $SUBNET_PRIVATE_1 || echo "Private subnet 1 deletion failed"
aws ec2 delete-subnet --subnet-id $SUBNET_PRIVATE_2 || echo "Private subnet 2 deletion failed"
aws ec2 delete-subnet --subnet-id $SUBNET_PUBLIC_1 || echo "Public subnet 1 deletion failed"
aws ec2 delete-subnet --subnet-id $SUBNET_PUBLIC_2 || echo "Public subnet 2 deletion failed"

aws ec2 detach-internet-gateway --vpc-id $VPC_ID --internet-gateway-id $IGW_ID || echo "IGW detach failed"
aws ec2 delete-internet-gateway --internet-gateway-id $IGW_ID || echo "IGW deletion failed"
aws ec2 delete-vpc --vpc-id $VPC_ID || echo "VPC deletion failed"

# 9. CloudWatch 로그 그룹 삭제
echo "📊 CloudWatch 로그 그룹 정리..."
aws logs delete-log-group --log-group-name /ecs/makit-backend || echo "Backend log group deletion failed"
aws logs delete-log-group --log-group-name /ecs/makit-frontend || echo "Frontend log group deletion failed"

# 10. ECR 리포지토리 정리 (선택사항)
read -p "ECR 리포지토리도 삭제하시겠습니까? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    aws ecr delete-repository --repository-name ${PROJECT_NAME}-backend --force || echo "Backend repo deletion failed"
    aws ecr delete-repository --repository-name ${PROJECT_NAME}-frontend --force || echo "Frontend repo deletion failed"
fi

echo "✅ AWS 리소스 정리 완료!"
echo "💰 비용 발생이 중단되었습니다."

# 정리 파일 삭제
rm -f infrastructure-outputs.env
rm -f ecs-task-definition-final.json