package com.Human.Ai.D.makit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "audience_segments")
public class AudienceSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "segment_type")
    private SegmentType segmentType;
    
    @Column(name = "size_estimate")
    private Long sizeEstimate;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @ElementCollection
    @CollectionTable(name = "segment_criteria", joinColumns = @JoinColumn(name = "segment_id"))
    @MapKeyColumn(name = "criteria_key")
    @Column(name = "criteria_value")
    private Map<String, String> segmentationCriteria;
    
    @ElementCollection
    @CollectionTable(name = "segment_characteristics", joinColumns = @JoinColumn(name = "segment_id"))
    @MapKeyColumn(name = "characteristic_key")
    @Column(name = "characteristic_value")
    private Map<String, String> characteristics;
    
    @Column(name = "performance_score")
    private Double performanceScore;
    
    @Column(name = "engagement_rate")
    private Double engagementRate;
    
    @Column(name = "conversion_rate")
    private Double conversionRate;
    
    @Column(name = "average_order_value")
    private Double averageOrderValue;
    
    @Column(name = "lifetime_value")
    private Double lifetimeValue;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SegmentStatus status;
    
    @Column(name = "ai_insights", columnDefinition = "TEXT")
    private String aiInsights;
    
    @Column(name = "recommended_actions", columnDefinition = "TEXT")
    private String recommendedActions;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_analyzed_at")
    private LocalDateTime lastAnalyzedAt;

    // Enums
    public enum SegmentType {
        DEMOGRAPHIC, BEHAVIORAL, PSYCHOGRAPHIC, GEOGRAPHIC, 
        TECHNOGRAPHIC, CUSTOM, AI_GENERATED
    }

    public enum SegmentStatus {
        ACTIVE, INACTIVE, ANALYZING, ARCHIVED
    }

    // Constructors
    public AudienceSegment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = SegmentStatus.ANALYZING;
    }

    public AudienceSegment(String name, User user, SegmentType segmentType) {
        this();
        this.name = name;
        this.user = user;
        this.segmentType = segmentType;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public SegmentType getSegmentType() { return segmentType; }
    public void setSegmentType(SegmentType segmentType) { this.segmentType = segmentType; }

    public Long getSizeEstimate() { return sizeEstimate; }
    public void setSizeEstimate(Long sizeEstimate) { this.sizeEstimate = sizeEstimate; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Map<String, String> getSegmentationCriteria() { return segmentationCriteria; }
    public void setSegmentationCriteria(Map<String, String> segmentationCriteria) { 
        this.segmentationCriteria = segmentationCriteria; 
    }

    public Map<String, String> getCharacteristics() { return characteristics; }
    public void setCharacteristics(Map<String, String> characteristics) { 
        this.characteristics = characteristics; 
    }

    public Double getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(Double performanceScore) { this.performanceScore = performanceScore; }

    public Double getEngagementRate() { return engagementRate; }
    public void setEngagementRate(Double engagementRate) { this.engagementRate = engagementRate; }

    public Double getConversionRate() { return conversionRate; }
    public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }

    public Double getAverageOrderValue() { return averageOrderValue; }
    public void setAverageOrderValue(Double averageOrderValue) { this.averageOrderValue = averageOrderValue; }

    public Double getLifetimeValue() { return lifetimeValue; }
    public void setLifetimeValue(Double lifetimeValue) { this.lifetimeValue = lifetimeValue; }

    public SegmentStatus getStatus() { return status; }
    public void setStatus(SegmentStatus status) { this.status = status; }

    public String getAiInsights() { return aiInsights; }
    public void setAiInsights(String aiInsights) { this.aiInsights = aiInsights; }

    public String getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(String recommendedActions) { this.recommendedActions = recommendedActions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastAnalyzedAt() { return lastAnalyzedAt; }
    public void setLastAnalyzedAt(LocalDateTime lastAnalyzedAt) { this.lastAnalyzedAt = lastAnalyzedAt; }

    // Business methods
    public void activate() {
        this.status = SegmentStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = SegmentStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void archive() {
        this.status = SegmentStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == SegmentStatus.ACTIVE;
    }

    public double calculateSegmentValue() {
        double value = 0.0;
        
        // Size contribution (0-30 points)
        if (sizeEstimate != null) {
            value += Math.min(sizeEstimate / 1000.0, 30);
        }
        
        // Performance contribution (0-40 points)
        if (performanceScore != null) {
            value += (performanceScore / 100.0) * 40;
        }
        
        // Engagement contribution (0-30 points)
        if (engagementRate != null) {
            value += (engagementRate / 100.0) * 30;
        }
        
        return value;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}