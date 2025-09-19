package com.Human.Ai.D.makit.domain;

public enum Priority {
    LOW("Low", "Standard processing time", 1),
    MEDIUM("Medium", "Normal priority", 2),
    HIGH("High", "Urgent - requires immediate attention", 3);
    
    private final String displayName;
    private final String description;
    private final int level;
    
    Priority(String displayName, String description, int level) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean isHigherThan(Priority other) {
        return this.level > other.level;
    }
}