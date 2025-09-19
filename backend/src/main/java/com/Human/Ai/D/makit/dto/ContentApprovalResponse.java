package com.Human.Ai.D.makit.dto;

import com.Human.Ai.D.makit.domain.ApprovalStatus;
import com.Human.Ai.D.makit.domain.ApprovalWorkflowStep;
import com.Human.Ai.D.makit.domain.Priority;

import java.time.LocalDateTime;
import java.util.List;

public class ContentApprovalResponse {
    
    private Long id;
    private Long contentId;
    private String contentTitle;
    private String submittedByUsername;
    private String currentReviewerUsername;
    private ApprovalStatus status;
    private ApprovalWorkflowStep currentStep;
    private String submissionNotes;
    private String reviewerComments;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime dueDate;
    private Priority priority;
    private boolean overdue;
    private boolean requiresAttention;
    private List<ApprovalHistoryResponse> history;
    
    // Constructors
    public ContentApprovalResponse() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    
    public String getContentTitle() { return contentTitle; }
    public void setContentTitle(String contentTitle) { this.contentTitle = contentTitle; }
    
    public String getSubmittedByUsername() { return submittedByUsername; }
    public void setSubmittedByUsername(String submittedByUsername) { this.submittedByUsername = submittedByUsername; }
    
    public String getCurrentReviewerUsername() { return currentReviewerUsername; }
    public void setCurrentReviewerUsername(String currentReviewerUsername) { this.currentReviewerUsername = currentReviewerUsername; }
    
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
    
    public boolean isOverdue() { return overdue; }
    public void setOverdue(boolean overdue) { this.overdue = overdue; }
    
    public boolean isRequiresAttention() { return requiresAttention; }
    public void setRequiresAttention(boolean requiresAttention) { this.requiresAttention = requiresAttention; }
    
    public List<ApprovalHistoryResponse> getHistory() { return history; }
    public void setHistory(List<ApprovalHistoryResponse> history) { this.history = history; }
}