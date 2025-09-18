# MaKIT AWS 배포 가이드

## 🚀 배포 준비 완료 상태

### ✅ 준비된 구성 요소
- **백엔드**: Spring Boot 3.2.0 (Java 21)
- **프론트엔드**: HTML/CSS/JavaScript
- **데이터베이스**: PostgreSQL (Docker Compose)
- **캐시**: Redis (Docker Compose)
- **컨테이너**: Docker & Docker Compose

## 📋 AWS 배포 옵션

### Option 1: ECS Fargate (권장)
```bash
# 1. ECR 리포지토리 생성
aws ecr create-repository --repository-name makit-backend
aws ecr create-repository --repository-name makit-frontend

# 2. Docker 이미지 빌드 및 푸시
docker build -t makit-backend ./backend
docker build -t makit-frontend .

# 3. ECR에 푸시
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin [ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com

docker tag makit-backend:latest [ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com/makit-backend:latest
docker tag makit-frontend:latest [ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com/makit-frontend:latest

docker push [ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com/makit-backend:latest
docker push [ACCOUNT_ID].dkr.ecr.ap-northeast-2.amazonaws.com/makit-frontend:latest
```

### Option 2: EC2 + Docker Compose
```bash
# 1. EC2 인스턴스 생성 (t3.medium 이상 권장)
# 2. Docker 및 Docker Compose 설치
# 3. 프로젝트 파일 업로드
# 4. 환경 변수 설정
# 5. Docker Compose 실행
docker-compose up -d
```

### Option 3: AWS App Runner
```bash
# 1. GitHub 연동
# 2. 자동 빌드 및 배포 설정
# 3. 환경 변수 구성
```

## 🔧 필수 환경 변수

```bash
# AWS 설정
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key

# 데이터베이스 (RDS)
SPRING_DATASOURCE_URL=jdbc:postgresql://your-rds-endpoint:5432/makit
SPRING_DATASOURCE_USERNAME=your_db_user
SPRING_DATASOURCE_PASSWORD=your_db_password

# Redis (ElastiCache)
SPRING_DATA_REDIS_HOST=your-redis-endpoint
SPRING_DATA_REDIS_PORT=6379

# JWT
JWT_SECRET=your_jwt_secret_key

# AWS 서비스
AWS_S3_BUCKET=your-s3-bucket
AWS_COGNITO_USER_POOL_ID=your-user-pool-id
AWS_COGNITO_CLIENT_ID=your-client-id
```

## 🏗️ AWS 인프라 구성

### 필수 AWS 서비스
1. **ECS/EC2**: 애플리케이션 실행
2. **RDS PostgreSQL**: 데이터베이스
3. **ElastiCache Redis**: 캐싱
4. **S3**: 정적 파일 저장
5. **CloudFront**: CDN
6. **Application Load Balancer**: 로드 밸런싱
7. **Route 53**: DNS 관리
8. **Certificate Manager**: SSL 인증서

### 선택적 AWS 서비스
1. **AWS Bedrock**: AI 기능
2. **AWS Cognito**: 사용자 인증
3. **CloudWatch**: 모니터링
4. **AWS Secrets Manager**: 보안 정보 관리

## 📊 예상 비용 (월간)

### 최소 구성 (개발/테스트)
- **ECS Fargate**: $30-50
- **RDS t3.micro**: $15-20
- **ElastiCache t3.micro**: $15-20
- **S3 + CloudFront**: $5-10
- **총 예상 비용**: $65-100/월

### 프로덕션 구성
- **ECS Fargate (고가용성)**: $100-200
- **RDS t3.medium (Multi-AZ)**: $60-80
- **ElastiCache t3.small**: $30-40
- **S3 + CloudFront**: $20-30
- **총 예상 비용**: $210-350/월

## 🚀 빠른 배포 스크립트

```bash
#!/bin/bash
# quick-deploy.sh

# 환경 변수 설정
export AWS_REGION=ap-northeast-2
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# ECR 로그인
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# 이미지 빌드 및 푸시
docker build -t makit-backend ./backend
docker build -t makit-frontend .

docker tag makit-backend:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/makit-backend:latest
docker tag makit-frontend:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/makit-frontend:latest

docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/makit-backend:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/makit-frontend:latest

echo "✅ 이미지 푸시 완료!"
echo "다음 단계: ECS 서비스 생성 및 배포"
```

## 📝 배포 체크리스트

### 사전 준비
- [ ] AWS 계정 및 IAM 권한 설정
- [ ] AWS CLI 설치 및 구성
- [ ] Docker 설치 및 실행
- [ ] 도메인 준비 (선택사항)

### 인프라 구성
- [ ] VPC 및 서브넷 생성
- [ ] RDS PostgreSQL 인스턴스 생성
- [ ] ElastiCache Redis 클러스터 생성
- [ ] S3 버킷 생성
- [ ] ECR 리포지토리 생성

### 애플리케이션 배포
- [ ] Docker 이미지 빌드 및 푸시
- [ ] ECS 클러스터 및 서비스 생성
- [ ] 로드 밸런서 구성
- [ ] 도메인 및 SSL 설정

### 테스트 및 모니터링
- [ ] 애플리케이션 동작 확인
- [ ] API 엔드포인트 테스트
- [ ] 모니터링 및 로깅 설정
- [ ] 백업 및 복구 계획 수립

## 🔗 유용한 링크

- [AWS ECS 가이드](https://docs.aws.amazon.com/ecs/)
- [AWS RDS 가이드](https://docs.aws.amazon.com/rds/)
- [Spring Boot on AWS](https://spring.io/guides/gs/spring-boot-docker/)
- [Docker Compose to ECS](https://docs.docker.com/cloud/ecs-integration/)