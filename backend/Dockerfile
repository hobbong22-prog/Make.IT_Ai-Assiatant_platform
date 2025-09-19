# 멀티 스테이지 빌드를 사용하여 최적화된 이미지 생성
FROM maven:latest as builder

# 작업 디렉토리 설정
WORKDIR /app

# pom.xml 복사
COPY pom.xml .

# 의존성 다운로드 (캐시 최적화)
RUN mvn dependency:go-offline -B

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# 실행 스테이지
FROM amazoncorretto:21-alpine

# curl 설치 (헬스체크용)
RUN apk add --no-cache curl

# 애플리케이션 사용자 생성
RUN addgroup --system spring && adduser --system spring --ingroup spring

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/target/*.jar app.jar

# 파일 소유권 변경
RUN chown spring:spring app.jar

# 사용자 변경
USER spring:spring

# 포트 노출
EXPOSE 8080

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]