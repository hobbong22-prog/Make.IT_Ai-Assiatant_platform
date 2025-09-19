package com.Human.Ai.D.makit.domain;

public enum ApprovalWorkflowStep {
    INITIAL_REVIEW("Initial Review", "Content submitted for initial review", 1),
    CONTENT_REVIEW("Content Review", "Detailed content review in progress", 2),
    REVISION_REQUIRED("Revision Required", "Content needs revision before proceeding", 3),
    APPROVED("Approved", "Content approved for publication", 4),
    REJECTED("Rejected", "Content rejected", 4),
    CANCELLED("Cancelled", "Workflow cancelled", 4);
    
    private final String displayName;
    private final String description;
    private final int order;
    
    ApprovalWorkflowStep(String displayName, String description, int order) {
        this.displayName = displayName;
        this.description = description;
        this.order = order;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getOrder() {
        return order;
    }
    
    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }
    
    public ApprovalWorkflowStep getNextStep() {
        return switch (this) {
            case INITIAL_REVIEW -> CONTENT_REVIEW;
            case CONTENT_REVIEW -> APPROVED; // Default next step
            case REVISION_REQUIRED -> INITIAL_REVIEW;
            default -> this; // Terminal states stay the same
        };
    }
}