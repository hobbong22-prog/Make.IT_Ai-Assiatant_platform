package com.Human.Ai.D.makit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ApprovalActionRequest {
    
    @NotBlank(message = "Comments are required")
    @Size(max = 2000, message = "Comments cannot exceed 2000 characters")
    private String comments;
    
    // Constructors
    public ApprovalActionRequest() {}
    
    public ApprovalActionRequest(String comments) {
        this.comments = comments;
    }
    
    // Getters and Setters
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}