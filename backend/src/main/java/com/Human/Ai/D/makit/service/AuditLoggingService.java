package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.*;
import com.Human.Ai.D.makit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 시스템 내 사용자 작업 및 시스템 이벤트를 추적하는 감사 로깅 서비스
 * 
 * 이 서비스는 보안, 규정 준수 및 시스템 모니터링 목적으로 
 * 모든 중요한 사용자 작업과 시스템 이벤트를 기록합니다.
 */
@Service
@Transactional
public class AuditLoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingService.class);
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private com.Human.Ai.D.makit.repository.UserRepository userRepository;
    
    /**
     * 감사 로그를 비동기적으로 기록합니다.
     */
    @Async
    public CompletableFuture<Void> logAsync(AuditAction action, String entityType, String entityId, String description) {
        try {
            User currentUser = getCurrentUser();
            AuditLog auditLog = createAuditLog(currentUser, action, entityType, entityId, description);
            auditLogRepository.save(auditLog);
            
            logger.debug("감사 로그 기록됨: {}", auditLog);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("감사 로그 기록 실패: action={}, entityType={}, entityId={}", 
                        action, entityType, entityId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 감사 로그를 동기적으로 기록합니다.
     */
    public void log(AuditAction action, String entityType, String entityId, String description) {
        try {
            User currentUser = getCurrentUser();
            AuditLog auditLog = createAuditLog(currentUser, action, entityType, entityId, description);
            auditLogRepository.save(auditLog);
            
            logger.debug("감사 로그 기록됨: {}", auditLog);
        } catch (Exception e) {
            logger.error("감사 로그 기록 실패: action={}, entityType={}, entityId={}", 
                        action, entityType, entityId, e);
        }
    }
    
    /**
     * 상세 정보와 함께 감사 로그를 기록합니다.
     */
    public void log(AuditAction action, String entityType, String entityId, 
                   String description, Map<String, String> details) {
        try {
            User currentUser = getCurrentUser();
            AuditLog auditLog = createAuditLog(currentUser, action, entityType, entityId, description);
            
            if (details != null && !details.isEmpty()) {
                auditLog.setDetails(details);
            }
            
            auditLogRepository.save(auditLog);
            logger.debug("상세 정보와 함께 감사 로그 기록됨: {}", auditLog);
        } catch (Exception e) {
            logger.error("상세 감사 로그 기록 실패: action={}, entityType={}, entityId={}", 
                        action, entityType, entityId, e);
        }
    }
    
    /**
     * 실패한 작업에 대한 감사 로그를 기록합니다.
     */
    public void logFailure(AuditAction action, String entityType, String entityId, 
                          String description, String errorMessage) {
        try {
            User currentUser = getCurrentUser();
            AuditLog auditLog = createAuditLog(currentUser, action, entityType, entityId, description);
            auditLog.setStatus(AuditStatus.FAILURE);
            auditLog.setErrorMessage(errorMessage);
            
            auditLogRepository.save(auditLog);
            logger.warn("실패 감사 로그 기록됨: {}", auditLog);
        } catch (Exception e) {
            logger.error("실패 감사 로그 기록 실패: action={}, entityType={}, entityId={}", 
                        action, entityType, entityId, e);
        }
    }
    
    /**
     * 특정 사용자에 대한 감사 로그를 기록합니다.
     */
    public void logForUser(User user, AuditAction action, String entityType, String entityId, String description) {
        try {
            AuditLog auditLog = createAuditLog(user, action, entityType, entityId, description);
            auditLogRepository.save(auditLog);
            
            logger.debug("사용자별 감사 로그 기록됨: {}", auditLog);
        } catch (Exception e) {
            logger.error("사용자별 감사 로그 기록 실패: user={}, action={}, entityType={}, entityId={}", 
                        user != null ? user.getEmail() : "null", action, entityType, entityId, e);
        }
    }
    
    /**
     * 보안 관련 이벤트를 기록합니다.
     */
    public void logSecurityEvent(AuditAction action, String description, String ipAddress, String userAgent) {
        try {
            User currentUser = getCurrentUser();
            AuditLog auditLog = createAuditLog(currentUser, action, "SECURITY", null, description);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            
            auditLogRepository.save(auditLog);
            logger.warn("보안 이벤트 기록됨: {}", auditLog);
        } catch (Exception e) {
            logger.error("보안 이벤트 기록 실패: action={}, description={}", action, description, e);
        }
    }
    
    /**
     * 시스템 이벤트를 기록합니다 (사용자 컨텍스트 없이).
     */
    public void logSystemEvent(AuditAction action, String entityType, String entityId, String description) {
        try {
            AuditLog auditLog = createAuditLog(null, action, entityType, entityId, description);
            auditLog.setUserEmail("SYSTEM");
            
            auditLogRepository.save(auditLog);
            logger.info("시스템 이벤트 기록됨: {}", auditLog);
        } catch (Exception e) {
            logger.error("시스템 이벤트 기록 실패: action={}, entityType={}, entityId={}", 
                        action, entityType, entityId, e);
        }
    }
    
    // 조회 메서드들
    
    /**
     * 특정 사용자의 감사 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsForUser(User user, Pageable pageable) {
        return auditLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }
    
    /**
     * 특정 작업 유형의 감사 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByAction(AuditAction action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action, pageable);
    }
    
    /**
     * 날짜 범위 내의 감사 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable);
    }
    
    /**
     * 특정 엔티티의 감사 로그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsForEntity(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId, pageable);
    }
    
    /**
     * 실패한 로그인 시도를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getFailedLoginAttempts(LocalDateTime since) {
        return auditLogRepository.findFailedLoginAttempts(AuditAction.USER_LOGIN, AuditStatus.FAILURE, since);
    }
    
    /**
     * 보안 관련 이벤트를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getSecurityEvents(LocalDateTime since) {
        List<AuditAction> securityActions = List.of(
            AuditAction.SECURITY_VIOLATION,
            AuditAction.ACCESS_DENIED,
            AuditAction.USER_LOGIN,
            AuditAction.USER_LOGOUT,
            AuditAction.PERMISSION_GRANT,
            AuditAction.PERMISSION_REVOKE
        );
        return auditLogRepository.findSecurityEvents(securityActions, since);
    }
    
    /**
     * 작업별 통계를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getActionStatistics(LocalDateTime since) {
        return auditLogRepository.getActionStatistics(since);
    }
    
    /**
     * 사용자별 활동 통계를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUserActivityStatistics(LocalDateTime since) {
        return auditLogRepository.getUserActivityStatistics(since);
    }
    
    // 유틸리티 메서드들
    
    /**
     * AuditLog 객체를 생성합니다.
     */
    private AuditLog createAuditLog(User user, AuditAction action, String entityType, String entityId, String description) {
        AuditLog auditLog = new AuditLog(user, action, entityType, entityId);
        auditLog.setDescription(description);
        
        // HTTP 요청 정보 추가
        try {
            HttpServletRequest request = getCurrentHttpRequest();
            if (request != null) {
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setSessionId(request.getSession(false) != null ? 
                                     request.getSession(false).getId() : null);
            }
        } catch (Exception e) {
            logger.debug("HTTP 요청 정보 추가 실패", e);
        }
        
        return auditLog;
    }
    
    /**
     * 현재 인증된 사용자를 가져옵니다.
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                // 토큰이 있는 경우 사용, 없으면 사용자명으로 조회
                Object credentials = authentication.getCredentials();
                if (credentials instanceof String) {
                    return authService.getCurrentUser((String) credentials);
                } else {
                    // 토큰이 없는 경우 사용자명으로 직접 조회 (UserRepository 사용)
                    String username = authentication.getName();
                    return userRepository.findByEmail(username).orElse(null);
                }
            }
        } catch (Exception e) {
            logger.debug("현재 사용자 조회 실패", e);
        }
        return null;
    }
    
    /**
     * 현재 HTTP 요청을 가져옵니다.
     */
    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 클라이언트 IP 주소를 가져옵니다.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 여러 IP가 있는 경우 첫 번째 IP 사용
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
}