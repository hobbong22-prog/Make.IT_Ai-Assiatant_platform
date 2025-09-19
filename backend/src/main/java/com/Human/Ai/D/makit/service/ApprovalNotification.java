package com.Human.Ai.D.makit.service;

import java.time.LocalDateTime;

public class ApprovalNotification {
    
    private String type;
    private Long approvalId;
    private String contentTitle;
    private String actorUsername;
    private String priority;
    private String message;
    private LocalDateTime timestamp;
    
    // Constructors
    public ApprovalNotification() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ApprovalNotification(String type, Long approvalId, String contentTitle, 
                              String actorUsername, String priority, String message) {
        this.type = type;
        this.approvalId = approvalId;
        this.contentTitle = contentTitle;
        this.actorUsername = actorUsername;
        this.priority = priority;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Long getApprovalId() { return approvalId; }
    public void setApprovalId(Long approvalId) { this.approvalId = approvalId; }
    
    public String getContentTitle() { return contentTitle; }
    public void setContentTitle(String contentTitle) { this.contentTitle = contentTitle; }
    
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return "ApprovalNotification{" +
                "type='" + type + '\'' +
                ", approvalId=" + approvalId +
                ", contentTitle='" + contentTitle + '\'' +
                ", actorUsername='" + actorUsername + '\'' +
                ", priority='" + priority + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}