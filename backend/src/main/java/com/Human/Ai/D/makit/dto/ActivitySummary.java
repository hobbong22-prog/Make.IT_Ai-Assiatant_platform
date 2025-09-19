package com.Human.Ai.D.makit.dto;

import java.time.LocalDateTime;

/**
 * DTO for activity summary in dashboard
 */
public class ActivitySummary {
    private String activityType;
    private String description;
    private LocalDateTime timestamp;
    private String entityId;
    private String entityType;

    // Constructors
    public ActivitySummary() {}

    public ActivitySummary(String activityType, String description, LocalDateTime timestamp) {
        this.activityType = activityType;
        this.description = description;
        this.timestamp = timestamp;
    }

    public ActivitySummary(String activityType, String description, LocalDateTime timestamp, String entityId, String entityType) {
        this.activityType = activityType;
        this.description = description;
        this.timestamp = timestamp;
        this.entityId = entityId;
        this.entityType = entityType;
    }

    // Getters and setters
    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * Get activity icon based on type
     */
    public String getIcon() {
        switch (activityType) {
            case "CAMPAIGN_CREATED": return "📊";
            case "CONTENT_GENERATED": return "📝";
            case "CONTENT_PUBLISHED": return "🚀";
            case "RECOMMENDATION_GENERATED": return "💡";
            case "ANALYTICS_UPDATED": return "📈";
            default: return "ℹ️";
        }
    }

    /**
     * Get relative time description
     */
    public String getRelativeTime() {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(timestamp, now).toMinutes();
        
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";
        
        long hours = minutes / 60;
        if (hours < 24) return hours + " hours ago";
        
        long days = hours / 24;
        if (days < 7) return days + " days ago";
        
        return timestamp.toLocalDate().toString();
    }
}