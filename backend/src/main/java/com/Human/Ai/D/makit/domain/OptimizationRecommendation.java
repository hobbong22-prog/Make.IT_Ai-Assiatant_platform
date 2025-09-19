package com.Human.Ai.D.makit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "optimization_recommendations")
public class OptimizationRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;
    
    @Column(name = "recommendation_type", nullable = false)
    private String recommendationType;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "action_required", columnDefinition = "TEXT")
    private String actionRequired;
    
    @Column(name = "expected_impact")
    private Double expectedImpact;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private RecommendationPriority priority;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RecommendationStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private RecommendationCategory category;
    
    @Column(name = "metric_target")
    private String metricTarget;
    
    @Column(name = "baseline_value")
    private Double baselineValue;
    
    @Column(name = "target_value")
    private Double targetValue;
    
    @Column(name = "implementation_effort")
    private String implementationEffort;
    
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "implemented_at")
    private LocalDateTime implementedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum RecommendationPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum RecommendationStatus {
        PENDING, REVIEWED, IMPLEMENTED, DISMISSED, EXPIRED
    }

    public enum RecommendationCategory {
        BUDGET_OPTIMIZATION, AUDIENCE_TARGETING, CONTENT_IMPROVEMENT, 
        BIDDING_STRATEGY, CREATIVE_OPTIMIZATION, TIMING_OPTIMIZATION,
        KEYWORD_OPTIMIZATION, PERFORMANCE_ENHANCEMENT
    }

    // Constructors
    public OptimizationRecommendation() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = RecommendationStatus.PENDING;
    }

    public OptimizationRecommendation(Campaign campaign, String recommendationType, String title) {
        this();
        this.campaign = campaign;
        this.recommendationType = recommendationType;
        this.title = title;
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActionRequired() { return actionRequired; }
    public void setActionRequired(String actionRequired) { this.actionRequired = actionRequired; }

    public Double getExpectedImpact() { return expectedImpact; }
    public void setExpectedImpact(Double expectedImpact) { this.expectedImpact = expectedImpact; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public RecommendationPriority getPriority() { return priority; }
    public void setPriority(RecommendationPriority priority) { this.priority = priority; }

    public RecommendationStatus getStatus() { return status; }
    public void setStatus(RecommendationStatus status) { this.status = status; }

    public RecommendationCategory getCategory() { return category; }
    public void setCategory(RecommendationCategory category) { this.category = category; }

    public String getMetricTarget() { return metricTarget; }
    public void setMetricTarget(String metricTarget) { this.metricTarget = metricTarget; }

    public Double getBaselineValue() { return baselineValue; }
    public void setBaselineValue(Double baselineValue) { this.baselineValue = baselineValue; }

    public Double getTargetValue() { return targetValue; }
    public void setTargetValue(Double targetValue) { this.targetValue = targetValue; }

    public String getImplementationEffort() { return implementationEffort; }
    public void setImplementationEffort(String implementationEffort) { this.implementationEffort = implementationEffort; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getImplementedAt() { return implementedAt; }
    public void setImplementedAt(LocalDateTime implementedAt) { this.implementedAt = implementedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Business methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isImplemented() {
        return status == RecommendationStatus.IMPLEMENTED;
    }

    public void markAsImplemented() {
        this.status = RecommendationStatus.IMPLEMENTED;
        this.implementedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void dismiss() {
        this.status = RecommendationStatus.DISMISSED;
        this.updatedAt = LocalDateTime.now();
    }

    public double calculatePriorityScore() {
        double score = 0.0;
        
        // Priority weight (0-40 points)
        switch (priority) {
            case CRITICAL: score += 40; break;
            case HIGH: score += 30; break;
            case MEDIUM: score += 20; break;
            case LOW: score += 10; break;
        }
        
        // Expected impact weight (0-30 points)
        if (expectedImpact != null) {
            score += Math.min(expectedImpact, 30);
        }
        
        // Confidence score weight (0-30 points)
        if (confidenceScore != null) {
            score += (confidenceScore / 100.0) * 30;
        }
        
        return score;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}