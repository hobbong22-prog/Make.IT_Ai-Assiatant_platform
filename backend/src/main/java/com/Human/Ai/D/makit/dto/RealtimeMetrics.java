package com.Human.Ai.D.makit.dto;

import java.time.LocalDateTime;

/**
 * DTO for real-time dashboard metrics
 */
public class RealtimeMetrics {
    private Long userId;
    private LocalDateTime timestamp;
    private int activeCampaignsCount;
    private int contentGeneratedToday;
    private double weeklyImpressions;
    private double weeklyClicks;
    private int pendingRecommendations;

    // Constructors
    public RealtimeMetrics() {}

    public RealtimeMetrics(Long userId) {
        this.userId = userId;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getActiveCampaignsCount() {
        return activeCampaignsCount;
    }

    public void setActiveCampaignsCount(int activeCampaignsCount) {
        this.activeCampaignsCount = activeCampaignsCount;
    }

    public int getContentGeneratedToday() {
        return contentGeneratedToday;
    }

    public void setContentGeneratedToday(int contentGeneratedToday) {
        this.contentGeneratedToday = contentGeneratedToday;
    }

    public double getWeeklyImpressions() {
        return weeklyImpressions;
    }

    public void setWeeklyImpressions(double weeklyImpressions) {
        this.weeklyImpressions = weeklyImpressions;
    }

    public double getWeeklyClicks() {
        return weeklyClicks;
    }

    public void setWeeklyClicks(double weeklyClicks) {
        this.weeklyClicks = weeklyClicks;
    }

    public int getPendingRecommendations() {
        return pendingRecommendations;
    }

    public void setPendingRecommendations(int pendingRecommendations) {
        this.pendingRecommendations = pendingRecommendations;
    }

    /**
     * Calculate weekly CTR
     */
    public double getWeeklyClickThroughRate() {
        return weeklyImpressions > 0 ? (weeklyClicks / weeklyImpressions) * 100 : 0.0;
    }
}