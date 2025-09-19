package com.Human.Ai.D.makit.dto;

import com.Human.Ai.D.makit.domain.Priority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ContentApprovalRequest {
    
    @NotNull(message = "Content ID is required")
    private Long contentId;
    
    @Size(max = 1000, message = "Submission notes cannot exceed 1000 characters")
    private String submissionNotes;
    
    private Priority priority = Priority.MEDIUM;
    
    // Constructors
    public ContentApprovalRequest() {}
    
    public ContentApprovalRequest(Long contentId, String submissionNotes, Priority priority) {
        this.contentId = contentId;
        this.submissionNotes = submissionNotes;
        this.priority = priority != null ? priority : Priority.MEDIUM;
    }
    
    // Getters and Setters
    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    
    public String getSubmissionNotes() { return submissionNotes; }
    public void setSubmissionNotes(String submissionNotes) { this.submissionNotes = submissionNotes; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
}