package com.Human.Ai.D.makit.dto;

import com.Human.Ai.D.makit.domain.OptimizationRecommendation;
import com.Human.Ai.D.makit.service.DashboardService.DateRange;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for comprehensive dashboard overview
 */
public class DashboardOverview {
    private Long userId;
    private DateRange dateRange;
    private LocalDateTime generatedAt;
    private CampaignMetricsSummary campaignMetrics;
    private ContentMetricsSummary contentMetrics;
    private List<ActivitySummary> recentActivities;
    private List<PerformanceTrend> performanceTrends;
    private List<OptimizationRecommendation> topRecommendations;
    private double overallPerformanceScore;

    // Constructors
    public DashboardOverview() {}

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

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public CampaignMetricsSummary getCampaignMetrics() {
        return campaignMetrics;
    }

    public void setCampaignMetrics(CampaignMetricsSummary campaignMetrics) {
        this.campaignMetrics = campaignMetrics;
    }

    public ContentMetricsSummary getContentMetrics() {
        return contentMetrics;
    }

    public void setContentMetrics(ContentMetricsSummary contentMetrics) {
        this.contentMetrics = contentMetrics;
    }

    public List<ActivitySummary> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<ActivitySummary> recentActivities) {
        this.recentActivities = recentActivities;
    }

    public List<PerformanceTrend> getPerformanceTrends() {
        return performanceTrends;
    }

    public void setPerformanceTrends(List<PerformanceTrend> performanceTrends) {
        this.performanceTrends = performanceTrends;
    }

    public List<OptimizationRecommendation> getTopRecommendations() {
        return topRecommendations;
    }

    public void setTopRecommendations(List<OptimizationRecommendation> topRecommendations) {
        this.topRecommendations = topRecommendations;
    }

    public double getOverallPerformanceScore() {
        return overallPerformanceScore;
    }

    public void setOverallPerformanceScore(double overallPerformanceScore) {
        this.overallPerformanceScore = overallPerformanceScore;
    }
}