#!/bin/bash

# MaKIT Platform 테스트 실행 스크립트
# Human.Ai.D - 2025

echo "🚀 MaKIT Platform 테스트 시작"
echo "================================"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 함수 정의
print_step() {
    echo -e "${BLUE}📋 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 테스트 타입 확인
TEST_TYPE=${1:-"all"}

case $TEST_TYPE in
    "unit")
        print_step "단위 테스트 실행 중..."
        mvn clean test -Dtest="*Test" -DfailIfNoTests=false
        ;;
    "integration")
        print_step "통합 테스트 실행 중..."
        mvn clean verify -Dtest="*IntegrationTest" -DfailIfNoTests=false
        ;;
    "performance")
        print_step "성능 테스트 실행 중..."
        mvn clean test -Dtest="*PerformanceTest" -DfailIfNoTests=false
        ;;
    "coverage")
        print_step "코드 커버리지 테스트 실행 중..."
        mvn clean test jacoco:report
        print_success "커버리지 리포트가 target/site/jacoco/index.html에 생성되었습니다."
        ;;
    "all")
        print_step "전체 테스트 실행 중..."
        
        # 1. 단위 테스트
        print_step "1/4 단위 테스트 실행..."
        mvn clean test -Dtest="*Test" -DfailIfNoTests=false
        if [ $? -eq 0 ]; then
            print_success "단위 테스트 완료"
        else
            print_error "단위 테스트 실패"
            exit 1
        fi
        
        # 2. 통합 테스트
        print_step "2/4 통합 테스트 실행..."
        mvn verify -Dtest="*IntegrationTest" -DfailIfNoTests=false
        if [ $? -eq 0 ]; then
            print_success "통합 테스트 완료"
        else
            print_error "통합 테스트 실패"
            exit 1
        fi
        
        # 3. 코드 커버리지
        print_step "3/4 코드 커버리지 분석..."
        mvn jacoco:report
        if [ $? -eq 0 ]; then
            print_success "코드 커버리지 분석 완료"
        else
            print_warning "코드 커버리지 분석 실패 (테스트는 계속 진행)"
        fi
        
        # 4. 성능 테스트 (선택적)
        print_step "4/4 성능 테스트 실행 (선택적)..."
        read -p "성능 테스트를 실행하시겠습니까? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            mvn test -Dtest="*PerformanceTest" -DfailIfNoTests=false
            if [ $? -eq 0 ]; then
                print_success "성능 테스트 완료"
            else
                print_warning "성능 테스트 실패 (전체 테스트는 성공)"
            fi
        else
            print_warning "성능 테스트 건너뜀"
        fi
        ;;
    *)
        echo "사용법: $0 [unit|integration|performance|coverage|all]"
        echo ""
        echo "테스트 타입:"
        echo "  unit        - 단위 테스트만 실행"
        echo "  integration - 통합 테스트만 실행"
        echo "  performance - 성능 테스트만 실행"
        echo "  coverage    - 코드 커버리지 포함 테스트"
        echo "  all         - 모든 테스트 실행 (기본값)"
        exit 1
        ;;
esac

# 테스트 결과 확인
if [ $? -eq 0 ]; then
    echo ""
    echo "================================"
    print_success "🎉 모든 테스트가 성공적으로 완료되었습니다!"
    echo ""
    echo "📊 테스트 결과 파일:"
    echo "  - 단위 테스트: target/surefire-reports/"
    echo "  - 통합 테스트: target/failsafe-reports/"
    echo "  - 커버리지: target/site/jacoco/index.html"
    echo ""
    echo "Human.Ai.D MaKIT Platform - 테스트 완료 ✨"
else
    echo ""
    echo "================================"
    print_error "❌ 테스트 실행 중 오류가 발생했습니다."
    echo ""
    echo "로그를 확인하여 문제를 해결해주세요."
    exit 1
fi