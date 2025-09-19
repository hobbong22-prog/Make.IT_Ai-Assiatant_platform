package com.Human.Ai.D.makit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_metrics")
public class CampaignMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer impressions;
    private Integer clicks;
    private Integer conversions;
    private Double ctr; // Click-through rate
    private Double cpc; // Cost per click
    private Double cpa; // Cost per acquisition
    private Double spend;
    private Double revenue;
    private Double roi; // Return on investment
    
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    // Constructors
    public CampaignMetrics() {}

    public CampaignMetrics(Campaign campaign) {
        this.campaign = campaign;
        this.recordedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getImpressions() { return impressions; }
    public void setImpressions(Integer impressions) { this.impressions = impressions; }

    public Integer getClicks() { return clicks; }
    public void setClicks(Integer clicks) { this.clicks = clicks; }

    public Integer getConversions() { return conversions; }
    public void setConversions(Integer conversions) { this.conversions = conversions; }

    public Double getCtr() { return ctr; }
    public void setCtr(Double ctr) { this.ctr = ctr; }

    public Double getCpc() { return cpc; }
    public void setCpc(Double cpc) { this.cpc = cpc; }

    public Double getCpa() { return cpa; }
    public void setCpa(Double cpa) { this.cpa = cpa; }

    public Double getSpend() { return spend; }
    public void setSpend(Double spend) { this.spend = spend; }

    public Double getRevenue() { return revenue; }
    public void setRevenue(Double revenue) { this.revenue = revenue; }

    public Double getRoi() { return roi; }
    public void setRoi(Double roi) { this.roi = roi; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }

    // Helper methods
    public void calculateMetrics() {
        if (impressions != null && clicks != null && impressions > 0) {
            this.ctr = (double) clicks / impressions * 100;
        }
        
        if (spend != null && clicks != null && clicks > 0) {
            this.cpc = spend / clicks;
        }
        
        if (spend != null && conversions != null && conversions > 0) {
            this.cpa = spend / conversions;
        }
        
        if (revenue != null && spend != null && spend > 0) {
            this.roi = (revenue - spend) / spend * 100;
        }
    }
}