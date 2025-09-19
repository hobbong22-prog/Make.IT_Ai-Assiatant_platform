# MaKIT Platform Backend

MaKIT은 Human.Ai.D에서 개발한 AI 기반 마케팅 자동화 플랫폼입니다.

## 🚀 주요 기능

### 1. AI 콘텐츠 생성
- **Amazon Bedrock 통합**: Claude, Titan, Stable Diffusion 모델 활용
- **다양한 콘텐츠 타입**: 블로그 포스트, 광고 문구, 이메일 템플릿, 소셜 미디어 포스트
- **비동기 처리**: 대용량 콘텐츠 생성을 위한 비동기 처리

### 2. 캠페인 관리
- **캠페인 생성 및 관리**: 이메일, 소셜 미디어, 검색 광고 등 다양한 캠페인 타입
- **성과 추적**: 실시간 캠페인 메트릭스 수집 및 분석
- **상태 관리**: 초안, 활성, 일시정지, 완료, 취소 상태 관리

### 3. AI 챗봇
- **실시간 채팅**: WebSocket 기반 실시간 고객 지원
- **RAG 시스템**: 기업 데이터 기반 맞춤형 답변
- **마케팅 어시스턴트**: 전문적인 마케팅 조언 제공

## 🛠 기술 스택

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: H2 (개발), PostgreSQL (운영)
- **AI Platform**: Amazon Bedrock
- **Cloud**: AWS (S3, Lambda, ECS)
- **Build Tool**: Maven

## 📁 프로젝트 구조

```
backend/
├── src/main/java/com/Human/Ai/D/makit/
│   ├── MakitApplication.java           # 메인 애플리케이션
│   ├── config/                         # 설정 클래스
│   │   ├── DataLoader.java            # 샘플 데이터 로더
│   │   └── WebSocketConfig.java       # WebSocket 설정
│   ├── controller/                     # REST API 컨트롤러
│   │   ├── CampaignController.java    # 캠페인 API
│   │   ├── ChatbotController.java     # 챗봇 API
│   │   └── ContentController.java     # 콘텐츠 API
│   ├── domain/                         # 도메인 모델
│   │   ├── Campaign.java              # 캠페인 엔티티
│   │   ├── CampaignMetrics.java       # 캠페인 메트릭스
│   │   ├── Content.java               # 콘텐츠 엔티티
│   │   └── User.java                  # 사용자 엔티티
│   ├── dto/                           # 데이터 전송 객체
│   │   ├── CampaignCreateRequest.java
│   │   ├── ChatMessage.java
│   │   └── ContentGenerationRequest.java
│   ├── repository/                     # 데이터 접근 계층
│   │   ├── CampaignRepository.java
│   │   ├── ContentRepository.java
│   │   └── UserRepository.java
│   └── service/                        # 비즈니스 로직
│       ├── ai/
│       │   └── BedrockService.java    # AWS Bedrock 서비스
│       ├── CampaignService.java       # 캠페인 서비스
│       └── ContentGenerationService.java # 콘텐츠 생성 서비스
└── src/main/resources/
    └── application.yml                 # 애플리케이션 설정
```

## 🚀 실행 방법

### 1. 사전 요구사항
- Java 17 이상
- Maven 3.6 이상
- AWS 계정 및 Bedrock 액세스 권한

### 2. AWS 설정
```bash
# AWS CLI 설정
aws configure
# 또는 환경변수 설정
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_DEFAULT_REGION=us-east-1
```

### 3. 애플리케이션 실행
```bash
cd backend
mvn spring-boot:run
```

### 4. 접속 정보
- **API 서버**: http://localhost:8080
- **H2 콘솔**: http://localhost:8080/h2-console
- **WebSocket**: ws://localhost:8080/ws

## 📚 API 문서

### 콘텐츠 생성 API

#### 블로그 포스트 생성
```http
POST /api/content/generate/blog
Content-Type: application/json

{
  "userId": 1,
  "topic": "AI 마케팅의 미래",
  "targetAudience": "마케팅 전문가"
}
```

#### 광고 문구 생성
```http
POST /api/content/generate/ad-copy
Content-Type: application/json

{
  "userId": 1,
  "product": "MaKIT 플랫폼",
  "targetAudience": "중소기업 마케터",
  "platform": "Google Ads"
}
```

### 캠페인 관리 API

#### 캠페인 생성
```http
POST /api/campaigns
Content-Type: application/json

{
  "userId": 1,
  "name": "신제품 출시 캠페인",
  "description": "새로운 AI 기능 출시",
  "type": "EMAIL",
  "targetAudience": "기존 고객",
  "budget": 100000
}
```

#### 캠페인 목록 조회
```http
GET /api/campaigns/user/1
```

### 챗봇 API

#### REST 채팅
```http
POST /api/chat
Content-Type: application/json

{
  "sender": "사용자",
  "content": "마케팅 캠페인 최적화 방법을 알려주세요",
  "type": "CHAT"
}
```

## 🔧 개발 환경 설정

### 1. IDE 설정
- IntelliJ IDEA 또는 Eclipse 사용 권장
- Lombok 플러그인 설치 필요

### 2. 데이터베이스 설정
개발 환경에서는 H2 인메모리 데이터베이스를 사용합니다.
- URL: `jdbc:h2:mem:makitdb`
- Username: `sa`
- Password: (없음)

### 3. 테스트 데이터
애플리케이션 시작 시 자동으로 샘플 데이터가 로드됩니다:
- 데모 사용자: `demo@Human.Ai.D.com` / `password123`
- 샘플 캠페인 및 콘텐츠

## 🚀 배포

### Docker 배포
```bash
# Dockerfile 생성 후
docker build -t makit-backend .
docker run -p 8080:8080 makit-backend
```

### AWS ECS 배포
1. ECR에 이미지 푸시
2. ECS 태스크 정의 생성
3. ECS 서비스 배포

## 🤝 기여하기

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 Human.Ai.D의 소유입니다.

## 📞 문의

- 이메일: contact@Human.Ai.D.com
- 웹사이트: https://makit.Human.Ai.D.com