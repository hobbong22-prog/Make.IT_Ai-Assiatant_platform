package com.Human.Ai.D.makit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_history")
public class ApprovalHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_approval_id", nullable = false)
    private ContentApproval contentApproval;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private ApprovalStatus fromStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private ApprovalStatus toStatus;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;
    
    @Column(columnDefinition = "TEXT")
    private String comments;
    
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
    
    // Constructors
    public ApprovalHistory() {}
    
    public ApprovalHistory(ContentApproval contentApproval, ApprovalStatus fromStatus, 
                          ApprovalStatus toStatus, User changedBy, String comments, LocalDateTime changedAt) {
        this.contentApproval = contentApproval;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.comments = comments;
        this.changedAt = changedAt;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ContentApproval getContentApproval() { return contentApproval; }
    public void setContentApproval(ContentApproval contentApproval) { this.contentApproval = contentApproval; }
    
    public ApprovalStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(ApprovalStatus fromStatus) { this.fromStatus = fromStatus; }
    
    public ApprovalStatus getToStatus() { return toStatus; }
    public void setToStatus(ApprovalStatus toStatus) { this.toStatus = toStatus; }
    
    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}