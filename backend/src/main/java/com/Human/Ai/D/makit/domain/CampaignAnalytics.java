package com.Human.Ai.D.makit.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_analytics")
public class CampaignAnalytics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;
    
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;
    
    // Raw metrics
    private Double impressions;
    private Double clicks;
    private Double conversions;
    private Double cost;
    private Double revenue;
    
    // Calculated metrics
    @Column(name = "click_through_rate")
    private Double clickThroughRate;
    
    @Column(name = "conversion_rate")
    private Double conversionRate;
    
    @Column(name = "return_on_ad_spend")
    private Double returnOnAdSpend;
    
    @Column(name = "cost_per_click")
    private Double costPerClick;
    
    @Column(name = "cost_per_conversion")
    private Double costPerConversion;
    
    // AI-generated insights
    @Column(name = "performance_score")
    private Double performanceScore;
    
    @Column(name = "ai_insights", columnDefinition = "TEXT")
    private String aiInsights;
    
    @Column(name = "trend_analysis", columnDefinition = "TEXT")
    private String trendAnalysis;
    
    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public CampaignAnalytics() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public CampaignAnalytics(Campaign campaign, LocalDate reportDate) {
        this();
        this.campaign = campaign;
        this.reportDate = reportDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }

    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }

    public Double getImpressions() { return impressions; }
    public void setImpressions(Double impressions) { this.impressions = impressions; }

    public Double getClicks() { return clicks; }
    public void setClicks(Double clicks) { this.clicks = clicks; }

    public Double getConversions() { return conversions; }
    public void setConversions(Double conversions) { this.conversions = conversions; }

    public Double getCost() { return cost; }
    public void setCost(Double cost) { this.cost = cost; }

    public Double getRevenue() { return revenue; }
    public void setRevenue(Double revenue) { this.revenue = revenue; }

    public Double getClickThroughRate() { return clickThroughRate; }
    public void setClickThroughRate(Double clickThroughRate) { this.clickThroughRate = clickThroughRate; }

    public Double getConversionRate() { return conversionRate; }
    public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }

    public Double getReturnOnAdSpend() { return returnOnAdSpend; }
    public void setReturnOnAdSpend(Double returnOnAdSpend) { this.returnOnAdSpend = returnOnAdSpend; }

    public Double getCostPerClick() { return costPerClick; }
    public void setCostPerClick(Double costPerClick) { this.costPerClick = costPerClick; }

    public Double getCostPerConversion() { return costPerConversion; }
    public void setCostPerConversion(Double costPerConversion) { this.costPerConversion = costPerConversion; }

    public Double getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(Double performanceScore) { this.performanceScore = performanceScore; }

    public String getAiInsights() { return aiInsights; }
    public void setAiInsights(String aiInsights) { this.aiInsights = aiInsights; }

    public String getTrendAnalysis() { return trendAnalysis; }
    public void setTrendAnalysis(String trendAnalysis) { this.trendAnalysis = trendAnalysis; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Business methods
    public void calculateMetrics() {
        // Calculate CTR
        if (impressions != null && impressions > 0 && clicks != null) {
            this.clickThroughRate = (clicks / impressions) * 100;
        }
        
        // Calculate conversion rate
        if (clicks != null && clicks > 0 && conversions != null) {
            this.conversionRate = (conversions / clicks) * 100;
        }
        
        // Calculate ROAS
        if (cost != null && cost > 0 && revenue != null) {
            this.returnOnAdSpend = revenue / cost;
        }
        
        // Calculate CPC
        if (clicks != null && clicks > 0 && cost != null) {
            this.costPerClick = cost / clicks;
        }
        
        // Calculate cost per conversion
        if (conversions != null && conversions > 0 && cost != null) {
            this.costPerConversion = cost / conversions;
        }
        
        this.calculatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}