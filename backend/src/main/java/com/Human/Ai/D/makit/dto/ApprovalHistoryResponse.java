package com.Human.Ai.D.makit.dto;

import com.Human.Ai.D.makit.domain.ApprovalStatus;

import java.time.LocalDateTime;

public class ApprovalHistoryResponse {
    
    private Long id;
    private ApprovalStatus fromStatus;
    private ApprovalStatus toStatus;
    private String changedByUsername;
    private String comments;
    private LocalDateTime changedAt;
    
    // Constructors
    public ApprovalHistoryResponse() {}
    
    public ApprovalHistoryResponse(Long id, ApprovalStatus fromStatus, ApprovalStatus toStatus,
                                 String changedByUsername, String comments, LocalDateTime changedAt) {
        this.id = id;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUsername = changedByUsername;
        this.comments = comments;
        this.changedAt = changedAt;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ApprovalStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(ApprovalStatus fromStatus) { this.fromStatus = fromStatus; }
    
    public ApprovalStatus getToStatus() { return toStatus; }
    public void setToStatus(ApprovalStatus toStatus) { this.toStatus = toStatus; }
    
    public String getChangedByUsername() { return changedByUsername; }
    public void setChangedByUsername(String changedByUsername) { this.changedByUsername = changedByUsername; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}