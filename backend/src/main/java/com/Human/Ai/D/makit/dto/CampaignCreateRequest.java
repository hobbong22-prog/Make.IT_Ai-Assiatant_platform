package com.Human.Ai.D.makit.dto;

import com.Human.Ai.D.makit.domain.Campaign;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CampaignCreateRequest {
    
    @NotNull
    private Long userId;
    
    @NotBlank
    private String name;
    
    private String description;
    
    @NotNull
    private Campaign.CampaignType type;
    
    private String targetAudience;
    private Double budget;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // Constructors
    public CampaignCreateRequest() {}
    
    public CampaignCreateRequest(Long userId, String name, Campaign.CampaignType type) {
        this.userId = userId;
        this.name = name;
        this.type = type;
    }
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Campaign.CampaignType getType() { return type; }
    public void setType(Campaign.CampaignType type) { this.type = type; }
    
    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    
    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
}