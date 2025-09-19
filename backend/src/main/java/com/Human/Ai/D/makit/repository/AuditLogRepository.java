package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.AuditAction;
import com.Human.Ai.D.makit.domain.AuditLog;
import com.Human.Ai.D.makit.domain.AuditStatus;
import com.Human.Ai.D.makit.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 감사 로그 데이터 액세스를 위한 리포지토리 인터페이스
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    /**
     * 특정 사용자의 감사 로그를 조회합니다.
     */
    Page<AuditLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);
    
    /**
     * 특정 사용자 이메일의 감사 로그를 조회합니다.
     */
    Page<AuditLog> findByUserEmailOrderByTimestampDesc(String userEmail, Pageable pageable);
    
    /**
     * 특정 작업 유형의 감사 로그를 조회합니다.
     */
    Page<AuditLog> findByActionOrderByTimestampDesc(AuditAction action, Pageable pageable);
    
    /**
     * 특정 상태의 감사 로그를 조회합니다.
     */
    Page<AuditLog> findByStatusOrderByTimestampDesc(AuditStatus status, Pageable pageable);
    
    /**
     * 특정 엔티티 유형의 감사 로그를 조회합니다.
     */
    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);
    
    /**
     * 특정 엔티티의 감사 로그를 조회합니다.
     */
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType, String entityId, Pageable pageable);
    
    /**
     * 날짜 범위 내의 감사 로그를 조회합니다.
     */
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * 특정 사용자의 날짜 범위 내 감사 로그를 조회합니다.
     */
    Page<AuditLog> findByUserAndTimestampBetweenOrderByTimestampDesc(
            User user, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * 특정 작업과 상태의 감사 로그를 조회합니다.
     */
    Page<AuditLog> findByActionAndStatusOrderByTimestampDesc(
            AuditAction action, AuditStatus status, Pageable pageable);
    
    /**
     * 특정 IP 주소의 감사 로그를 조회합니다.
     */
    List<AuditLog> findByIpAddressOrderByTimestampDesc(String ipAddress);
    
    /**
     * 특정 세션 ID의 감사 로그를 조회합니다.
     */
    List<AuditLog> findBySessionIdOrderByTimestampDesc(String sessionId);
    
    /**
     * 실패한 로그인 시도를 조회합니다.
     */
    @Query("SELECT al FROM AuditLog al WHERE al.action = :action AND al.status = :status " +
           "AND al.timestamp >= :since ORDER BY al.timestamp DESC")
    List<AuditLog> findFailedLoginAttempts(
            @Param("action") AuditAction action, 
            @Param("status") AuditStatus status, 
            @Param("since") LocalDateTime since);
    
    /**
     * 특정 사용자의 최근 활동을 조회합니다.
     */
    @Query("SELECT al FROM AuditLog al WHERE al.user = :user " +
           "AND al.timestamp >= :since ORDER BY al.timestamp DESC")
    List<AuditLog> findRecentUserActivity(@Param("user") User user, @Param("since") LocalDateTime since);
    
    /**
     * 작업별 통계를 조회합니다.
     */
    @Query("SELECT al.action, COUNT(al) FROM AuditLog al " +
           "WHERE al.timestamp >= :since GROUP BY al.action ORDER BY COUNT(al) DESC")
    List<Object[]> getActionStatistics(@Param("since") LocalDateTime since);
    
    /**
     * 사용자별 활동 통계를 조회합니다.
     */
    @Query("SELECT al.userEmail, COUNT(al) FROM AuditLog al " +
           "WHERE al.timestamp >= :since GROUP BY al.userEmail ORDER BY COUNT(al) DESC")
    List<Object[]> getUserActivityStatistics(@Param("since") LocalDateTime since);
    
    /**
     * 시간별 활동 통계를 조회합니다.
     */
    @Query("SELECT FUNCTION('DATE_TRUNC', 'hour', al.timestamp) as hour, COUNT(al) FROM AuditLog al " +
           "WHERE al.timestamp >= :since GROUP BY FUNCTION('DATE_TRUNC', 'hour', al.timestamp) " +
           "ORDER BY hour DESC")
    List<Object[]> getHourlyActivityStatistics(@Param("since") LocalDateTime since);
    
    /**
     * 보안 관련 이벤트를 조회합니다.
     */
    @Query("SELECT al FROM AuditLog al WHERE al.action IN :securityActions " +
           "AND al.timestamp >= :since ORDER BY al.timestamp DESC")
    List<AuditLog> findSecurityEvents(
            @Param("securityActions") List<AuditAction> securityActions, 
            @Param("since") LocalDateTime since);
    
    /**
     * 특정 기간 동안의 오류 로그를 조회합니다.
     */
    @Query("SELECT al FROM AuditLog al WHERE al.status = 'FAILURE' " +
           "AND al.timestamp >= :since ORDER BY al.timestamp DESC")
    List<AuditLog> findErrorLogs(@Param("since") LocalDateTime since);
    
    /**
     * 오래된 감사 로그를 삭제하기 위한 쿼리 (데이터 보존 정책용)
     */
    @Query("DELETE FROM AuditLog al WHERE al.timestamp < :cutoffDate")
    void deleteOldAuditLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
}