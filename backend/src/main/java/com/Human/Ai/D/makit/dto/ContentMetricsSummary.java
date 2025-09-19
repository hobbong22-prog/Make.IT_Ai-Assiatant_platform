package com.Human.Ai.D.makit.dto;

/**
 * DTO for content metrics summary
 */
public class ContentMetricsSummary {
    private int totalContent;
    private int publishedContent;
    private double averageQualityScore;

    // Constructors
    public ContentMetricsSummary() {}

    public ContentMetricsSummary(int totalContent, int publishedContent, double averageQualityScore) {
        this.totalContent = totalContent;
        this.publishedContent = publishedContent;
        this.averageQualityScore = averageQualityScore;
    }

    // Getters and setters
    public int getTotalContent() {
        return totalContent;
    }

    public void setTotalContent(int totalContent) {
        this.totalContent = totalContent;
    }

    public int getPublishedContent() {
        return publishedContent;
    }

    public void setPublishedContent(int publishedContent) {
        this.publishedContent = publishedContent;
    }

    public double getAverageQualityScore() {
        return averageQualityScore;
    }

    public void setAverageQualityScore(double averageQualityScore) {
        this.averageQualityScore = averageQualityScore;
    }

    /**
     * Calculate publish rate
     */
    public double getPublishRate() {
        return totalContent > 0 ? (publishedContent / (double) totalContent) * 100 : 0.0;
    }

    /**
     * Get draft content count
     */
    public int getDraftContent() {
        return totalContent - publishedContent;
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

    /**
     * Check if content production is healthy
     */
    public boolean isHealthyProduction() {
        return totalContent > 0 && getPublishRate() >= 70 && averageQualityScore >= 75;
    }
}