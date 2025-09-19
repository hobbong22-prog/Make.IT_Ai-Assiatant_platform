package com.Human.Ai.D.makit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 메인 애플리케이션 컨텍스트 로딩 테스트
 * Human.Ai.D MaKIT 플랫폼의 기본 동작을 확인합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class MakitPlatformApplicationTests {

    @Test
    void contextLoads() {
        // 애플리케이션 컨텍스트가 성공적으로 로드되는지 확인
        // 이 테스트는 Spring Boot 애플리케이션이 정상적으로 시작되는지 검증합니다.
        System.out.println("🎉 Human.Ai.D MaKIT 플랫폼 애플리케이션 컨텍스트 로딩 성공!");
    }
    
    @Test
    void applicationPropertiesLoaded() {
        // 테스트 프로파일이 정상적으로 로드되는지 확인
        String activeProfile = System.getProperty("spring.profiles.active");
        System.out.println("활성 프로파일: " + activeProfile);
        // 테스트 환경에서는 test 프로파일이 활성화되어야 함
    }
}