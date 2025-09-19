package com.Human.Ai.D.makit.dto;

import com.Human.Ai.D.makit.domain.CampaignAnalytics;
import com.Human.Ai.D.makit.service.DashboardService.DateRange;

import java.util.List;

/**
 * DTO for campaign performance summary
 */
public class CampaignPerformanceSummary {
    private Long userId;
    private DateRange dateRange;
    private double totalImpressions;
    private double totalClicks;
    private double totalConversions;
    private double totalCost;
    private double totalRevenue;
    private double averageClickThroughRate;
    private double averageConversionRate;
    private double averageReturnOnAdSpend;
    private List<CampaignAnalytics> topPerformingCampaigns;

    // Constructors
    public CampaignPerformanceSummary() {}

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

    public double getTotalImpressions() {
        return totalImpressions;
    }

    public void setTotalImpressions(double totalImpressions) {
        this.totalImpressions = totalImpressions;
    }

    public double getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(double totalClicks) {
        this.totalClicks = totalClicks;
    }

    public double getTotalConversions() {
        return totalConversions;
    }

    public void setTotalConversions(double totalConversions) {
        this.totalConversions = totalConversions;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public double getAverageClickThroughRate() {
        return averageClickThroughRate;
    }

    public void setAverageClickThroughRate(double averageClickThroughRate) {
        this.averageClickThroughRate = averageClickThroughRate;
    }

    public double getAverageConversionRate() {
        return averageConversionRate;
    }

    public void setAverageConversionRate(double averageConversionRate) {
        this.averageConversionRate = averageConversionRate;
    }

    public double getAverageReturnOnAdSpend() {
        return averageReturnOnAdSpend;
    }

    public void setAverageReturnOnAdSpend(double averageReturnOnAdSpend) {
        this.averageReturnOnAdSpend = averageReturnOnAdSpend;
    }

    public List<CampaignAnalytics> getTopPerformingCampaigns() {
        return topPerformingCampaigns;
    }

    public void setTopPerformingCampaigns(List<CampaignAnalytics> topPerformingCampaigns) {
        this.topPerformingCampaigns = topPerformingCampaigns;
    }

    /**
     * Calculate profit margin
     */
    public double getProfitMargin() {
        return totalRevenue > 0 ? ((totalRevenue - totalCost) / totalRevenue) * 100 : 0.0;
    }

    /**
     * Get performance grade based on ROAS
     */
    public String getPerformanceGrade() {
        if (averageReturnOnAdSpend >= 4.0) return "A";
        if (averageReturnOnAdSpend >= 3.0) return "B";
        if (averageReturnOnAdSpend >= 2.0) return "C";
        if (averageReturnOnAdSpend >= 1.0) return "D";
        return "F";
    }
}