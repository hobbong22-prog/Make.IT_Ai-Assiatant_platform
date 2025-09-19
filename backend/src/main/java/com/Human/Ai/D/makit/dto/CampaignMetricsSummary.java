package com.Human.Ai.D.makit.dto;

/**
 * DTO for campaign metrics summary
 */
public class CampaignMetricsSummary {
    private int totalCampaigns;
    private int activeCampaigns;
    private double totalSpend;
    private double totalRevenue;
    private double averageRoas;

    // Constructors
    public CampaignMetricsSummary() {}

    public CampaignMetricsSummary(int totalCampaigns, int activeCampaigns, double totalSpend, double totalRevenue) {
        this.totalCampaigns = totalCampaigns;
        this.activeCampaigns = activeCampaigns;
        this.totalSpend = totalSpend;
        this.totalRevenue = totalRevenue;
        this.averageRoas = totalSpend > 0 ? totalRevenue / totalSpend : 0;
    }

    // Getters and setters
    public int getTotalCampaigns() {
        return totalCampaigns;
    }

    public void setTotalCampaigns(int totalCampaigns) {
        this.totalCampaigns = totalCampaigns;
    }

    public int getActiveCampaigns() {
        return activeCampaigns;
    }

    public void setActiveCampaigns(int activeCampaigns) {
        this.activeCampaigns = activeCampaigns;
    }

    public double getTotalSpend() {
        return totalSpend;
    }

    public void setTotalSpend(double totalSpend) {
        this.totalSpend = totalSpend;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public double getAverageRoas() {
        return averageRoas;
    }

    public void setAverageRoas(double averageRoas) {
        this.averageRoas = averageRoas;
    }

    /**
     * Calculate profit
     */
    public double getProfit() {
        return totalRevenue - totalSpend;
    }

    /**
     * Calculate profit margin percentage
     */
    public double getProfitMargin() {
        return totalRevenue > 0 ? (getProfit() / totalRevenue) * 100 : 0.0;
    }

    /**
     * Get campaign utilization rate
     */
    public double getCampaignUtilizationRate() {
        return totalCampaigns > 0 ? (activeCampaigns / (double) totalCampaigns) * 100 : 0.0;
    }
}