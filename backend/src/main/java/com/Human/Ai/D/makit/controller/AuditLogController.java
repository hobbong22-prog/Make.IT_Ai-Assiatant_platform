package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.domain.AuditAction;
import com.Human.Ai.D.makit.domain.AuditLog;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.service.AuditLoggingService;
import com.Human.Ai.D.makit.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 감사 로그 조회 및 관리를 위한 REST API 컨트롤러
 * 
 * 관리자 권한을 가진 사용자만 감사 로그에 접근할 수 있습니다.
 */
@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit Log", description = "감사 로그 관리 API")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {
    
    @Autowired
    private AuditLoggingService auditLoggingService;
    
    @Autowired
    private AuthService authService;
    
    /**
     * 모든 감사 로그를 페이지네이션으로 조회합니다.
     */
    @GetMapping("/logs")
    @Operation(summary = "감사 로그 목록 조회", description = "모든 감사 로그를 페이지네이션으로 조회합니다.")
    public ResponseEntity<Page<AuditLog>> getAllAuditLogs(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditLoggingService.getAuditLogsByDateRange(
            LocalDateTime.now().minusMonths(1), LocalDateTime.now(), pageable);
        
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * 특정 사용자의 감사 로그를 조회합니다.
     */
    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "사용자별 감사 로그 조회", description = "특정 사용자의 감사 로그를 조회합니다.")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        User user = authService.findById(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditLoggingService.getAuditLogsForUser(user, pageable);
        
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * 특정 작업 유형의 감사 로그를 조회합니다.
     */
    @GetMapping("/logs/action/{action}")
    @Operation(summary = "작업별 감사 로그 조회", description = "특정 작업 유형의 감사 로그를 조회합니다.")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByAction(
            @Parameter(description = "작업 유형") @PathVariable AuditAction action,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditLoggingService.getAuditLogsByAction(action, pageable);
        
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * 날짜 범위로 감사 로그를 조회합니다.
     */
    @GetMapping("/logs/date-range")
    @Operation(summary = "날짜 범위별 감사 로그 조회", description = "지정된 날짜 범위의 감사 로그를 조회합니다.")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByDateRange(
            @Parameter(description = "시작 날짜") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료 날짜") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditLoggingService.getAuditLogsByDateRange(startDate, endDate, pageable);
        
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * 특정 엔티티의 감사 로그를 조회합니다.
     */
    @GetMapping("/logs/entity/{entityType}/{entityId}")
    @Operation(summary = "엔티티별 감사 로그 조회", description = "특정 엔티티의 감사 로그를 조회합니다.")
    public ResponseEntity<Page<AuditLog>> getAuditLogsForEntity(
            @Parameter(description = "엔티티 타입") @PathVariable String entityType,
            @Parameter(description = "엔티티 ID") @PathVariable String entityId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditLoggingService.getAuditLogsForEntity(entityType, entityId, pageable);
        
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * 실패한 로그인 시도를 조회합니다.
     */
    @GetMapping("/security/failed-logins")
    @Operation(summary = "실패한 로그인 시도 조회", description = "최근 실패한 로그인 시도를 조회합니다.")
    public ResponseEntity<List<AuditLog>> getFailedLoginAttempts(
            @Parameter(description = "조회 시작 시간 (기본: 24시간 전)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        
        if (since == null) {
            since = LocalDateTime.now().minusDays(1);
        }
        
        List<AuditLog> failedLogins = auditLoggingService.getFailedLoginAttempts(since);
        return ResponseEntity.ok(failedLogins);
    }
    
    /**
     * 보안 관련 이벤트를 조회합니다.
     */
    @GetMapping("/security/events")
    @Operation(summary = "보안 이벤트 조회", description = "보안 관련 감사 로그를 조회합니다.")
    public ResponseEntity<List<AuditLog>> getSecurityEvents(
            @Parameter(description = "조회 시작 시간 (기본: 7일 전)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        
        if (since == null) {
            since = LocalDateTime.now().minusDays(7);
        }
        
        List<AuditLog> securityEvents = auditLoggingService.getSecurityEvents(since);
        return ResponseEntity.ok(securityEvents);
    }
    
    /**
     * 작업별 통계를 조회합니다.
     */
    @GetMapping("/statistics/actions")
    @Operation(summary = "작업별 통계 조회", description = "작업 유형별 감사 로그 통계를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getActionStatistics(
            @Parameter(description = "조회 시작 시간 (기본: 30일 전)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        
        if (since == null) {
            since = LocalDateTime.now().minusDays(30);
        }
        
        List<Object[]> statistics = auditLoggingService.getActionStatistics(since);
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Long> actionCounts = new HashMap<>();
        
        for (Object[] stat : statistics) {
            AuditAction action = (AuditAction) stat[0];
            Long count = (Long) stat[1];
            actionCounts.put(action.name(), count);
        }
        
        result.put("actionCounts", actionCounts);
        result.put("totalActions", actionCounts.values().stream().mapToLong(Long::longValue).sum());
        result.put("period", Map.of("since", since, "until", LocalDateTime.now()));
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 사용자별 활동 통계를 조회합니다.
     */
    @GetMapping("/statistics/users")
    @Operation(summary = "사용자별 활동 통계 조회", description = "사용자별 감사 로그 통계를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getUserActivityStatistics(
            @Parameter(description = "조회 시작 시간 (기본: 30일 전)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        
        if (since == null) {
            since = LocalDateTime.now().minusDays(30);
        }
        
        List<Object[]> statistics = auditLoggingService.getUserActivityStatistics(since);
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Long> userCounts = new HashMap<>();
        
        for (Object[] stat : statistics) {
            String userEmail = (String) stat[0];
            Long count = (Long) stat[1];
            userCounts.put(userEmail != null ? userEmail : "SYSTEM", count);
        }
        
        result.put("userCounts", userCounts);
        result.put("totalUsers", userCounts.size());
        result.put("totalActivities", userCounts.values().stream().mapToLong(Long::longValue).sum());
        result.put("period", Map.of("since", since, "until", LocalDateTime.now()));
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 감사 로그 대시보드 요약 정보를 조회합니다.
     */
    @GetMapping("/dashboard")
    @Operation(summary = "감사 로그 대시보드", description = "감사 로그 요약 정보를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getAuditDashboard() {
        LocalDateTime last24Hours = LocalDateTime.now().minusDays(1);
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        
        Map<String, Object> dashboard = new HashMap<>();
        
        // 최근 24시간 통계
        List<AuditLog> failedLogins = auditLoggingService.getFailedLoginAttempts(last24Hours);
        List<AuditLog> securityEvents = auditLoggingService.getSecurityEvents(last24Hours);
        
        dashboard.put("failedLoginsLast24h", failedLogins.size());
        dashboard.put("securityEventsLast24h", securityEvents.size());
        
        // 작업별 통계 (지난 7일)
        List<Object[]> actionStats = auditLoggingService.getActionStatistics(last7Days);
        Map<String, Long> topActions = new HashMap<>();
        actionStats.stream().limit(10).forEach(stat -> {
            AuditAction action = (AuditAction) stat[0];
            Long count = (Long) stat[1];
            topActions.put(action.getDescription(), count);
        });
        dashboard.put("topActionsLast7Days", topActions);
        
        // 사용자별 통계 (지난 30일)
        List<Object[]> userStats = auditLoggingService.getUserActivityStatistics(last30Days);
        Map<String, Long> topUsers = new HashMap<>();
        userStats.stream().limit(10).forEach(stat -> {
            String userEmail = (String) stat[0];
            Long count = (Long) stat[1];
            topUsers.put(userEmail != null ? userEmail : "SYSTEM", count);
        });
        dashboard.put("topUsersLast30Days", topUsers);
        
        dashboard.put("lastUpdated", LocalDateTime.now());
        
        return ResponseEntity.ok(dashboard);
    }
}