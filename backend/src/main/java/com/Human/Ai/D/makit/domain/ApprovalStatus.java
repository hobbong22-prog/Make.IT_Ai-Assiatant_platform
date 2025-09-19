package com.Human.Ai.D.makit.domain;

public enum ApprovalStatus {
    PENDING("Pending Review", "Content is waiting for initial review"),
    IN_REVIEW("In Review", "Content is currently being reviewed"),
    NEEDS_REVISION("Needs Revision", "Content requires changes before approval"),
    APPROVED("Approved", "Content has been approved for publication"),
    REJECTED("Rejected", "Content has been rejected"),
    CANCELLED("Cancelled", "Approval process has been cancelled");
    
    private final String displayName;
    private final String description;
    
    ApprovalStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }
    
    public boolean isActive() {
        return this == PENDING || this == IN_REVIEW || this == NEEDS_REVISION;
    }
}