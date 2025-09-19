package com.Human.Ai.D.makit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "content_approvals")
public class ContentApproval {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_reviewer")
    private User currentReviewer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalWorkflowStep currentStep;
    
    @Column(name = "submission_notes", columnDefinition = "TEXT")
    private String submissionNotes;
    
    @Column(name = "reviewer_comments", columnDefinition = "TEXT")
    private String reviewerComments;
    
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Enumerated(EnumType.STRING)
    private Priority priority;
    
    @OneToMany(mappedBy = "contentApproval", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ApprovalHistory> approvalHistory = new ArrayList<>();
    
    // Constructors
    public ContentApproval() {}
    
    public ContentApproval(Content content, User submittedBy, String submissionNotes, Priority priority) {
        this.content = content;
        this.submittedBy = submittedBy;
        this.submissionNotes = submissionNotes;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.status = ApprovalStatus.PENDING;
        this.currentStep = ApprovalWorkflowStep.INITIAL_REVIEW;
        this.submittedAt = LocalDateTime.now();
        this.dueDate = calculateDueDate(this.priority);
    }
    
    // Business methods
    public boolean canTransitionTo(ApprovalStatus newStatus) {
        return switch (this.status) {
            case PENDING -> newStatus == ApprovalStatus.IN_REVIEW || 
                          newStatus == ApprovalStatus.REJECTED ||
                          newStatus == ApprovalStatus.CANCELLED;
            case IN_REVIEW -> newStatus == ApprovalStatus.APPROVED || 
                             newStatus == ApprovalStatus.REJECTED ||
                             newStatus == ApprovalStatus.NEEDS_REVISION ||
                             newStatus == ApprovalStatus.CANCELLED;
            case NEEDS_REVISION -> newStatus == ApprovalStatus.PENDING ||
                                  newStatus == ApprovalStatus.CANCELLED;
            case APPROVED, REJECTED, CANCELLED -> false; // Terminal states
        };
    }
    
    public void transitionTo(ApprovalStatus newStatus, User reviewer, String comments) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", this.status, newStatus)
            );
        }
        
        ApprovalStatus oldStatus = this.status;
        this.status = newStatus;
        this.currentReviewer = reviewer;
        this.reviewerComments = comments;
        this.reviewedAt = LocalDateTime.now();
        
        // Update workflow step based on new status
        updateWorkflowStep(newStatus);
        
        // Add to history
        addToHistory(oldStatus, newStatus, reviewer, comments);
    }
    
    private void updateWorkflowStep(ApprovalStatus status) {
        this.currentStep = switch (status) {
            case PENDING -> ApprovalWorkflowStep.INITIAL_REVIEW;
            case IN_REVIEW -> ApprovalWorkflowStep.CONTENT_REVIEW;
            case NEEDS_REVISION -> ApprovalWorkflowStep.REVISION_REQUIRED;
            case APPROVED -> ApprovalWorkflowStep.APPROVED;
            case REJECTED -> ApprovalWorkflowStep.REJECTED;
            case CANCELLED -> ApprovalWorkflowStep.CANCELLED;
        };
    }
    
    private void addToHistory(ApprovalStatus fromStatus, ApprovalStatus toStatus, User reviewer, String comments) {
        ApprovalHistory history = new ApprovalHistory(
            this, fromStatus, toStatus, reviewer, comments, LocalDateTime.now()
        );
        this.approvalHistory.add(history);
    }
    
    private LocalDateTime calculateDueDate(Priority priority) {
        LocalDateTime now = LocalDateTime.now();
        return switch (priority) {
            case HIGH -> now.plusHours(24);
            case MEDIUM -> now.plusDays(3);
            case LOW -> now.plusDays(7);
        };
    }
    
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && 
               (status == ApprovalStatus.PENDING || status == ApprovalStatus.IN_REVIEW);
    }
    
    public boolean requiresAttention() {
        return isOverdue() || 
               (priority == Priority.HIGH && status == ApprovalStatus.PENDING);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Content getContent() { return content; }
    public void setContent(Content content) { this.content = content; }
    
    public User getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(User submittedBy) { this.submittedBy = submittedBy; }
    
    public User getCurrentReviewer() { return currentReviewer; }
    public void setCurrentReviewer(User currentReviewer) { this.currentReviewer = currentReviewer; }
    
    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }
    
    public ApprovalWorkflowStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(ApprovalWorkflowStep currentStep) { this.currentStep = currentStep; }
    
    public String getSubmissionNotes() { return submissionNotes; }
    public void setSubmissionNotes(String submissionNotes) { this.submissionNotes = submissionNotes; }
    
    public String getReviewerComments() { return reviewerComments; }
    public void setReviewerComments(String reviewerComments) { this.reviewerComments = reviewerComments; }
    
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    
    public List<ApprovalHistory> getApprovalHistory() { return approvalHistory; }
    public void setApprovalHistory(List<ApprovalHistory> approvalHistory) { this.approvalHistory = approvalHistory; }
}