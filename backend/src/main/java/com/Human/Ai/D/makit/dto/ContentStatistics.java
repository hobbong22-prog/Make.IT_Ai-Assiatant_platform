package com.Human.Ai.D.makit.dto;

import com.Human.Ai.D.makit.domain.Content;
import com.Human.Ai.D.makit.service.DashboardService.DateRange;

import java.util.List;
import java.util.Map;

/**
 * DTO for content generation statistics
 */
public class ContentStatistics {
    private Long userId;
    private DateRange dateRange;
    private Map<String, Long> contentByType;
    private Map<String, Long> contentByStatus;
    private double averageQualityScore;
    private List<Content> recentContent;

    // Constructors
    public ContentStatistics() {}

    // Getters and setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public void setDateRange(DateRange dateRange) {
        this.dateRange = dateRange;
    }

    public Map<String, Long> getContentByType() {
        return contentByType;
    }

    public void setContentByType(Map<String, Long> contentByType) {
        this.contentByType = contentByType;
    }

    public Map<String, Long> getContentByStatus() {
        return contentByStatus;
    }

    public void setContentByStatus(Map<String, Long> contentByStatus) {
        this.contentByStatus = contentByStatus;
    }

    public double getAverageQualityScore() {
        return averageQualityScore;
    }

    public void setAverageQualityScore(double averageQualityScore) {
        this.averageQualityScore = averageQualityScore;
    }

    public List<Content> getRecentContent() {
        return recentContent;
    }

    public void setRecentContent(List<Content> recentContent) {
        this.recentContent = recentContent;
    }

    /**
     * Get total content count
     */
    public long getTotalContentCount() {
        return contentByType != null ? contentByType.values().stream().mapToLong(Long::longValue).sum() : 0;
    }

    /**
     * Get published content count
     */
    public long getPublishedContentCount() {
        return contentByStatus != null ? contentByStatus.getOrDefault("PUBLISHED", 0L) : 0;
    }

    /**
     * Get draft content count
     */
    public long getDraftContentCount() {
        return contentByStatus != null ? contentByStatus.getOrDefault("DRAFT", 0L) : 0;
    }

    /**
     * Calculate publish rate
     */
    public double getPublishRate() {
        long total = getTotalContentCount();
        return total > 0 ? (getPublishedContentCount() / (double) total) * 100 : 0.0;
    }

    /**
     * Get quality grade based on average score
     */
    public String getQualityGrade() {
        if (averageQualityScore >= 90) return "A";
        if (averageQualityScore >= 80) return "B";
        if (averageQualityScore >= 70) return "C";
        if (averageQualityScore >= 60) return "D";
        return "F";
    }
}