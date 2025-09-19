package com.Human.Ai.D.makit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.repository.AuditLogRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import com.Human.Ai.D.makit.service.AuditLoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 감사 로깅 시스템에 대한 통합 테스트
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AuditLoggingIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private AuditLoggingService auditLoggingService;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User testUser;
    private User adminUser;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 정리
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setCompanyName("Test Company");
        testUser.setUserRole(UserRole.CONTENT_CREATOR);
        testUser = userRepository.save(testUser);
        
        // 관리자 사용자 생성
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("password");
        adminUser.setCompanyName("Test Company");
        adminUser.setUserRole(UserRole.ADMIN);
        adminUser = userRepository.save(adminUser);
    }
    
    @Test
    void testAuditLogCreation_Success() {
        // Given
        String description = "테스트 콘텐츠 생성";
        Map<String, String> details = new HashMap<>();
        details.put("contentType", "BLOG_POST");
        details.put("wordCount", "500");
        
        // When
        auditLoggingService.logForUser(testUser, AuditAction.CONTENT_CREATE, "CONTENT", "123", description);
        
        // Then
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertEquals(1, auditLogs.size());
        
        AuditLog auditLog = auditLogs.get(0);
        assertEquals(testUser.getId(), auditLog.getUser().getId());
        assertEquals(testUser.getEmail(), auditLog.getUserEmail());
        assertEquals(AuditAction.CONTENT_CREATE, auditLog.getAction());
        assertEquals("CONTENT", auditLog.getEntityType());
        assertEquals("123", auditLog.getEntityId());
        assertEquals(description, auditLog.getDescription());
        assertEquals(AuditStatus.SUCCESS, auditLog.getStatus());
        assertEquals("BLOG_POST", auditLog.getDetails().get("contentType"));
        assertEquals("500", auditLog.getDetails().get("wordCount"));
        assertNotNull(auditLog.getTimestamp());
    }
    
    @Test
    void testAuditLogFailure_Success() {
        // Given
        String description = "콘텐츠 생성 실패";
        String errorMessage = "AI 서비스 연결 오류";
        
        // When
        auditLoggingService.logFailure(AuditAction.CONTENT_CREATE, "CONTENT", "123", description, errorMessage);
        
        // Then
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertEquals(1, auditLogs.size());
        
        AuditLog auditLog = auditLogs.get(0);
        assertEquals(AuditStatus.FAILURE, auditLog.getStatus());
        assertEquals(errorMessage, auditLog.getErrorMessage());
    }
    
    @Test
    void testSystemEventLogging_Success() {
        // Given
        String description = "시스템 설정 변경";
        
        // When
        auditLoggingService.logSystemEvent(AuditAction.SYSTEM_CONFIG_CHANGE, "SYSTEM", "config-1", description);
        
        // Then
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertEquals(1, auditLogs.size());
        
        AuditLog auditLog = auditLogs.get(0);
        assertNull(auditLog.getUser());
        assertEquals("SYSTEM", auditLog.getUserEmail());
        assertEquals(AuditAction.SYSTEM_CONFIG_CHANGE, auditLog.getAction());
    }
    
    @Test
    void testSecurityEventLogging_Success() {
        // Given
        String description = "의심스러운 로그인 시도";
        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0";
        
        // When
        auditLoggingService.logSecurityEvent(AuditAction.SECURITY_VIOLATION, description, ipAddress, userAgent);
        
        // Then
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertEquals(1, auditLogs.size());
        
        AuditLog auditLog = auditLogs.get(0);
        assertEquals("SECURITY", auditLog.getEntityType());
        assertEquals(ipAddress, auditLog.getIpAddress());
        assertEquals(userAgent, auditLog.getUserAgent());
    }
    
    @Test
    void testAuditLogQueries_Success() {
        // Given - 여러 감사 로그 생성
        auditLoggingService.logForUser(testUser, AuditAction.CONTENT_CREATE, "CONTENT", "1", "콘텐츠 1 생성");
        auditLoggingService.logForUser(testUser, AuditAction.CONTENT_UPDATE, "CONTENT", "1", "콘텐츠 1 수정");
        auditLoggingService.logForUser(adminUser, AuditAction.USER_LOGIN, "USER", adminUser.getId().toString(), "관리자 로그인");
        auditLoggingService.logFailure(AuditAction.USER_LOGIN, "USER", testUser.getId().toString(), "로그인 실패", "잘못된 비밀번호");
        
        // When & Then - 사용자별 조회
        Page<AuditLog> userLogs = auditLoggingService.getAuditLogsForUser(testUser, PageRequest.of(0, 10));
        assertEquals(3, userLogs.getContent().size()); // 2개 성공 + 1개 실패
        
        // When & Then - 작업별 조회
        Page<AuditLog> contentCreateLogs = auditLoggingService.getAuditLogsByAction(AuditAction.CONTENT_CREATE, PageRequest.of(0, 10));
        assertEquals(1, contentCreateLogs.getContent().size());
        
        // When & Then - 엔티티별 조회
        Page<AuditLog> contentLogs = auditLoggingService.getAuditLogsForEntity("CONTENT", "1", PageRequest.of(0, 10));
        assertEquals(2, contentLogs.getContent().size());
        
        // When & Then - 날짜 범위 조회
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        Page<AuditLog> dateRangeLogs = auditLoggingService.getAuditLogsByDateRange(startDate, endDate, PageRequest.of(0, 10));
        assertEquals(4, dateRangeLogs.getContent().size());
    }
    
    @Test
    void testFailedLoginAttemptsQuery_Success() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        auditLoggingService.logFailure(AuditAction.USER_LOGIN, "USER", "1", "로그인 실패 1", "잘못된 비밀번호");
        auditLoggingService.logFailure(AuditAction.USER_LOGIN, "USER", "2", "로그인 실패 2", "계정 잠김");
        auditLoggingService.logForUser(testUser, AuditAction.USER_LOGIN, "USER", testUser.getId().toString(), "성공적인 로그인");
        
        // When
        List<AuditLog> failedLogins = auditLoggingService.getFailedLoginAttempts(since);
        
        // Then
        assertEquals(2, failedLogins.size());
        failedLogins.forEach(log -> {
            assertEquals(AuditAction.USER_LOGIN, log.getAction());
            assertEquals(AuditStatus.FAILURE, log.getStatus());
        });
    }
    
    @Test
    void testSecurityEventsQuery_Success() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        auditLoggingService.logSecurityEvent(AuditAction.SECURITY_VIOLATION, "보안 위반", "192.168.1.1", "Browser");
        auditLoggingService.logSecurityEvent(AuditAction.ACCESS_DENIED, "접근 거부", "192.168.1.2", "Browser");
        auditLoggingService.logForUser(testUser, AuditAction.CONTENT_CREATE, "CONTENT", "1", "일반 작업");
        
        // When
        List<AuditLog> securityEvents = auditLoggingService.getSecurityEvents(since);
        
        // Then
        assertTrue(securityEvents.size() >= 2);
        // 보안 관련 액션들이 포함되어 있는지 확인
        boolean hasSecurityViolation = securityEvents.stream()
            .anyMatch(log -> log.getAction() == AuditAction.SECURITY_VIOLATION);
        boolean hasAccessDenied = securityEvents.stream()
            .anyMatch(log -> log.getAction() == AuditAction.ACCESS_DENIED);
        
        assertTrue(hasSecurityViolation || hasAccessDenied);
    }
    
    @Test
    void testStatisticsQueries_Success() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        auditLoggingService.logForUser(testUser, AuditAction.CONTENT_CREATE, "CONTENT", "1", "콘텐츠 1");
        auditLoggingService.logForUser(testUser, AuditAction.CONTENT_CREATE, "CONTENT", "2", "콘텐츠 2");
        auditLoggingService.logForUser(adminUser, AuditAction.USER_LOGIN, "USER", adminUser.getId().toString(), "로그인");
        
        // When - 작업별 통계
        List<Object[]> actionStats = auditLoggingService.getActionStatistics(since);
        
        // Then
        assertFalse(actionStats.isEmpty());
        // CONTENT_CREATE가 2번, USER_LOGIN이 1번 있어야 함
        boolean foundContentCreate = false;
        boolean foundUserLogin = false;
        
        for (Object[] stat : actionStats) {
            AuditAction action = (AuditAction) stat[0];
            Long count = (Long) stat[1];
            
            if (action == AuditAction.CONTENT_CREATE && count == 2L) {
                foundContentCreate = true;
            }
            if (action == AuditAction.USER_LOGIN && count == 1L) {
                foundUserLogin = true;
            }
        }
        
        assertTrue(foundContentCreate);
        assertTrue(foundUserLogin);
        
        // When - 사용자별 통계
        List<Object[]> userStats = auditLoggingService.getUserActivityStatistics(since);
        
        // Then
        assertFalse(userStats.isEmpty());
        boolean foundTestUser = false;
        boolean foundAdminUser = false;
        
        for (Object[] stat : userStats) {
            String userEmail = (String) stat[0];
            Long count = (Long) stat[1];
            
            if (testUser.getEmail().equals(userEmail) && count == 2L) {
                foundTestUser = true;
            }
            if (adminUser.getEmail().equals(userEmail) && count == 1L) {
                foundAdminUser = true;
            }
        }
        
        assertTrue(foundTestUser);
        assertTrue(foundAdminUser);
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testAuditLogController_GetAllLogs_Success() throws Exception {
        // Given
        auditLoggingService.logForUser(testUser, AuditAction.CONTENT_CREATE, "CONTENT", "1", "테스트 로그");
        
        // When & Then
        mockMvc.perform(get("/api/audit/logs")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testAuditLogController_GetUserLogs_Success() throws Exception {
        // Given
        auditLoggingService.logForUser(testUser, AuditAction.CONTENT_CREATE, "CONTENT", "1", "사용자 로그");
        
        // When & Then
        mockMvc.perform(get("/api/audit/logs/user/{userId}", testUser.getId())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testAuditLogController_GetActionStatistics_Success() throws Exception {
        // Given
        auditLoggingService.logForUser(testUser, AuditAction.CONTENT_CREATE, "CONTENT", "1", "통계 테스트");
        
        // When & Then
        mockMvc.perform(get("/api/audit/statistics/actions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.actionCounts").exists())
                .andExpect(jsonPath("$.totalActions").exists());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testAuditLogController_GetDashboard_Success() throws Exception {
        // Given
        auditLoggingService.logForUser(testUser, AuditAction.CONTENT_CREATE, "CONTENT", "1", "대시보드 테스트");
        auditLoggingService.logFailure(AuditAction.USER_LOGIN, "USER", "1", "로그인 실패", "오류");
        
        // When & Then
        mockMvc.perform(get("/api/audit/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.failedLoginsLast24h").exists())
                .andExpect(jsonPath("$.securityEventsLast24h").exists())
                .andExpect(jsonPath("$.lastUpdated").exists());
    }
    
    @Test
    @WithMockUser(roles = "USER")
    void testAuditLogController_AccessDenied_ForNonAdmin() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void testAuditLogController_Unauthorized_WithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isUnauthorized());
    }
}