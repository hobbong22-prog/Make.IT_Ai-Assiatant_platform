package com.Human.Ai.D.makit.dto;

import java.time.LocalDate;

/**
 * DTO for performance trend data
 */
public class PerformanceTrend {
    private LocalDate date;
    private double impressions;
    private double clicks;
    private double conversions;
    private double cost;
    private double revenue;

    // Constructors
    public PerformanceTrend() {}

    public PerformanceTrend(LocalDate date, double impressions, double clicks, double conversions) {
        this.date = date;
        this.impressions = impressions;
        this.clicks = clicks;
        this.conversions = conversions;
    }

    public PerformanceTrend(LocalDate date, double impressions, double clicks, double conversions, double cost, double revenue) {
        this.date = date;
        this.impressions = impressions;
        this.clicks = clicks;
        this.conversions = conversions;
        this.cost = cost;
        this.revenue = revenue;
    }

    // Getters and setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getImpressions() {
        return impressions;
    }

    public void setImpressions(double impressions) {
        this.impressions = impressions;
    }

    public double getClicks() {
        return clicks;
    }

    public void setClicks(double clicks) {
        this.clicks = clicks;
    }

    public double getConversions() {
        return conversions;
    }

    public void setConversions(double conversions) {
        this.conversions = conversions;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getRevenue() {
        return revenue;
    }

    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }

    /**
     * Calculate click-through rate
     */
    public double getClickThroughRate() {
        return impressions > 0 ? (clicks / impressions) * 100 : 0.0;
    }

    /**
     * Calculate conversion rate
     */
    public double getConversionRate() {
        return clicks > 0 ? (conversions / clicks) * 100 : 0.0;
    }

    /**
     * Calculate return on ad spend
     */
    public double getReturnOnAdSpend() {
        return cost > 0 ? revenue / cost : 0.0;
    }

    /**
     * Calculate cost per click
     */
    public double getCostPerClick() {
        return clicks > 0 ? cost / clicks : 0.0;
    }

    /**
     * Calculate cost per conversion
     */
    public double getCostPerConversion() {
        return conversions > 0 ? cost / conversions : 0.0;
    }
}