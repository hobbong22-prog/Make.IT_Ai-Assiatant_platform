package com.Human.Ai.D.makit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 시스템 내 사용자 작업 및 시스템 이벤트를 추적하기 위한 감사 로그 엔티티
 * 
 * 이 엔티티는 보안 및 규정 준수 목적으로 모든 중요한 사용자 작업을 기록합니다.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_entity_type", columnList = "entity_type")
})
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "user_email")
    private String userEmail;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;
    
    @Column(name = "entity_type")
    private String entityType;
    
    @Column(name = "entity_id")
    private String entityId;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AuditStatus status;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @ElementCollection
    @CollectionTable(name = "audit_log_details", joinColumns = @JoinColumn(name = "audit_log_id"))
    @MapKeyColumn(name = "detail_key")
    @Column(name = "detail_value", length = 1000)
    private Map<String, String> details = new HashMap<>();
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    // 생성자
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
        this.status = AuditStatus.SUCCESS;
    }
    
    public AuditLog(User user, AuditAction action, String entityType, String entityId) {
        this();
        this.user = user;
        this.userEmail = user != null ? user.getEmail() : null;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { 
        this.user = user;
        this.userEmail = user != null ? user.getEmail() : null;
    }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) { this.status = status; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Map<String, String> getDetails() { return details; }
    public void setDetails(Map<String, String> details) { this.details = details; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    // 편의 메서드
    public void addDetail(String key, String value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
    }
    
    public void addDetail(String key, Object value) {
        addDetail(key, value != null ? value.toString() : null);
    }
    
    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", userEmail='" + userEmail + '\'' +
                ", action=" + action +
                ", entityType='" + entityType + '\'' +
                ", entityId='" + entityId + '\'' +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }
}