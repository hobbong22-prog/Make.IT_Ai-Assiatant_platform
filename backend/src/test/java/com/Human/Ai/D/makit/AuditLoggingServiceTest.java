package com.Human.Ai.D.makit;

import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.repository.AuditLogRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import com.Human.Ai.D.makit.service.AuthService;
import com.Human.Ai.D.makit.service.AuditLoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AuditLoggingService에 대한 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class AuditLoggingServiceTest {
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @Mock
    private AuthService authService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private AuditLoggingService auditLoggingService;
    
    private User testUser;
    private AuditLog testAuditLog;
    
    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setUserRole(UserRole.ADMIN);
        
        // 테스트 감사 로그 생성
        testAuditLog = new AuditLog();
        testAuditLog.setId(1L);
        testAuditLog.setUser(testUser);
        testAuditLog.setUserEmail(testUser.getEmail());
        testAuditLog.setAction(AuditAction.CONTENT_CREATE);
        testAuditLog.setEntityType("CONTENT");
        testAuditLog.setEntityId("123");
        testAuditLog.setDescription("테스트 콘텐츠 생성");
        testAuditLog.setStatus(AuditStatus.SUCCESS);
        testAuditLog.setTimestamp(LocalDateTime.now());
        
        // Security Context 설정
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }
    
    @Test
    void testLog_Success() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // When
        auditLoggingService.log(AuditAction.CONTENT_CREATE, "CONTENT", "123", "테스트 콘텐츠 생성");
        
        // Then
        verify(auditLogRepository).save(any(AuditLog.class));
    }
    
    @Test
    void testLogWithDetails_Success() {
        // Given
        Map<String, String> details = new HashMap<>();
        details.put("contentType", "BLOG_POST");
        details.put("wordCount", "500");
        
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // When
        auditLoggingService.log(AuditAction.CONTENT_CREATE, "CONTENT", "123", "테스트 콘텐츠 생성", details);
        
        // Then
        verify(auditLogRepository).save(argThat(auditLog -> 
            auditLog.getDetails().containsKey("contentType") &&
            auditLog.getDetails().get("contentType").equals("BLOG_POST")
        ));
    }
    
    @Test
    void testLogFailure_Success() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // When
        auditLoggingService.logFailure(AuditAction.CONTENT_CREATE, "CONTENT", "123", 
                                     "콘텐츠 생성 실패", "AI 서비스 오류");
        
        // Then
        verify(auditLogRepository).save(argThat(auditLog -> 
            auditLog.getStatus() == AuditStatus.FAILURE &&
            auditLog.getErrorMessage().equals("AI 서비스 오류")
        ));
    }
    
    @Test
    void testLogForUser_Success() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // When
        auditLoggingService.logForUser(testUser, AuditAction.USER_LOGIN, "USER", "1", "사용자 로그인");
        
        // Then
        verify(auditLogRepository).save(argThat(auditLog -> 
            auditLog.getUser().equals(testUser) &&
            auditLog.getAction() == AuditAction.USER_LOGIN
        ));
    }
    
    @Test
    void testLogSecurityEvent_Success() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // When
        auditLoggingService.logSecurityEvent(AuditAction.SECURITY_VIOLATION, 
                                           "의심스러운 로그인 시도", 
                                           "192.168.1.100", 
                                           "Mozilla/5.0");
        
        // Then
        verify(auditLogRepository).save(argThat(auditLog -> 
            auditLog.getEntityType().equals("SECURITY") &&
            auditLog.getIpAddress().equals("192.168.1.100") &&
            auditLog.getUserAgent().equals("Mozilla/5.0")
        ));
    }
    
    @Test
    void testLogSystemEvent_Success() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // When
        auditLoggingService.logSystemEvent(AuditAction.SYSTEM_CONFIG_CHANGE, 
                                         "SYSTEM", 
                                         "config-1", 
                                         "시스템 설정 변경");
        
        // Then
        verify(auditLogRepository).save(argThat(auditLog -> 
            auditLog.getUserEmail().equals("SYSTEM") &&
            auditLog.getUser() == null
        ));
    }
    
    @Test
    void testGetAuditLogsForUser_Success() {
        // Given
        List<AuditLog> auditLogs = Arrays.asList(testAuditLog);
        Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(auditLogRepository.findByUserOrderByTimestampDesc(testUser, pageable))
            .thenReturn(auditLogPage);
        
        // When
        Page<AuditLog> result = auditLoggingService.getAuditLogsForUser(testUser, pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testAuditLog, result.getContent().get(0));
        verify(auditLogRepository).findByUserOrderByTimestampDesc(testUser, pageable);
    }
    
    @Test
    void testGetAuditLogsByAction_Success() {
        // Given
        List<AuditLog> auditLogs = Arrays.asList(testAuditLog);
        Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(auditLogRepository.findByActionOrderByTimestampDesc(AuditAction.CONTENT_CREATE, pageable))
            .thenReturn(auditLogPage);
        
        // When
        Page<AuditLog> result = auditLoggingService.getAuditLogsByAction(AuditAction.CONTENT_CREATE, pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(auditLogRepository).findByActionOrderByTimestampDesc(AuditAction.CONTENT_CREATE, pageable);
    }
    
    @Test
    void testGetAuditLogsByDateRange_Success() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        List<AuditLog> auditLogs = Arrays.asList(testAuditLog);
        Page<AuditLog> auditLogPage = new PageImpl<>(auditLogs);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable))
            .thenReturn(auditLogPage);
        
        // When
        Page<AuditLog> result = auditLoggingService.getAuditLogsByDateRange(startDate, endDate, pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(auditLogRepository).findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable);
    }
    
    @Test
    void testGetFailedLoginAttempts_Success() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        List<AuditLog> failedLogins = Arrays.asList(testAuditLog);
        
        when(auditLogRepository.findFailedLoginAttempts(AuditAction.USER_LOGIN, AuditStatus.FAILURE, since))
            .thenReturn(failedLogins);
        
        // When
        List<AuditLog> result = auditLoggingService.getFailedLoginAttempts(since);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(auditLogRepository).findFailedLoginAttempts(AuditAction.USER_LOGIN, AuditStatus.FAILURE, since);
    }
    
    @Test
    void testGetSecurityEvents_Success() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<AuditLog> securityEvents = Arrays.asList(testAuditLog);
        
        when(auditLogRepository.findSecurityEvents(any(List.class), eq(since)))
            .thenReturn(securityEvents);
        
        // When
        List<AuditLog> result = auditLoggingService.getSecurityEvents(since);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(auditLogRepository).findSecurityEvents(any(List.class), eq(since));
    }
    
    @Test
    void testGetActionStatistics_Success() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> statistics = Arrays.asList(
            new Object[]{AuditAction.CONTENT_CREATE, 10L},
            new Object[]{AuditAction.USER_LOGIN, 25L}
        );
        
        when(auditLogRepository.getActionStatistics(since)).thenReturn(statistics);
        
        // When
        List<Object[]> result = auditLoggingService.getActionStatistics(since);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(auditLogRepository).getActionStatistics(since);
    }
    
    @Test
    void testGetUserActivityStatistics_Success() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> statistics = Arrays.asList(
            new Object[]{"test@example.com", 15L},
            new Object[]{"admin@example.com", 30L}
        );
        
        when(auditLogRepository.getUserActivityStatistics(since)).thenReturn(statistics);
        
        // When
        List<Object[]> result = auditLoggingService.getUserActivityStatistics(since);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(auditLogRepository).getUserActivityStatistics(since);
    }
    
    @Test
    void testLogAsync_Success() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // When
        auditLoggingService.logAsync(AuditAction.CONTENT_CREATE, "CONTENT", "123", "비동기 로그 테스트");
        
        // Then
        // 비동기 메서드이므로 호출 여부만 확인
        // 실제 저장은 별도 스레드에서 실행됨
        verify(auditLogRepository, timeout(1000)).save(any(AuditLog.class));
    }
    
    @Test
    void testLog_WithNullUser_Success() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);
        
        // When
        auditLoggingService.log(AuditAction.CONTENT_CREATE, "CONTENT", "123", "익명 사용자 로그");
        
        // Then
        verify(auditLogRepository).save(argThat(auditLog -> 
            auditLog.getUser() == null
        ));
    }
    
    @Test
    void testLog_WithException_HandlesGracefully() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB 오류"));
        
        // When & Then
        assertDoesNotThrow(() -> 
            auditLoggingService.log(AuditAction.CONTENT_CREATE, "CONTENT", "123", "오류 테스트")
        );
        
        verify(auditLogRepository).save(any(AuditLog.class));
    }
}