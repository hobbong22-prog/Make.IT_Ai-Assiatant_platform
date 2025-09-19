package com.Human.Ai.D.makit.model;

import com.Human.Ai.D.makit.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 콘텐츠 템플릿 엔티티
 * Human.Ai.D MaKIT 플랫폼의 재사용 가능한 콘텐츠 템플릿
 */
@Entity
@Table(name = "content_templates")
public class ContentTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(unique = true)
    private String templateId;
    
    @NotBlank
    private String name;
    
    private String description;
    
    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String promptTemplate;
    
    @NotNull
    private String contentType;
    
    @NotNull
    private String category;
    
    @ElementCollection
    @CollectionTable(name = "template_parameters", 
                    joinColumns = @JoinColumn(name = "template_id"))
    @MapKeyColumn(name = "parameter_name")
    @Column(name = "parameter_value")
    private Map<String, String> defaultParameters;
    
    @ElementCollection
    @CollectionTable(name = "template_tags", 
                    joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "tag")
    private java.util.Set<String> tags;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    private boolean isActive = true;
    
    private boolean isPublic = false;
    
    private int usageCount = 0;
    
    private double rating = 0.0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 기본 생성자
    public ContentTemplate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // 생성자
    public ContentTemplate(String templateId, String name, String promptTemplate, 
                          String contentType, String category, User createdBy) {
        this();
        this.templateId = templateId;
        this.name = name;
        this.promptTemplate = promptTemplate;
        this.contentType = contentType;
        this.category = category;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPromptTemplate() { return promptTemplate; }
    public void setPromptTemplate(String promptTemplate) { this.promptTemplate = promptTemplate; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Map<String, String> getDefaultParameters() { return defaultParameters; }
    public void setDefaultParameters(Map<String, String> defaultParameters) { 
        this.defaultParameters = defaultParameters; 
    }
    
    public java.util.Set<String> getTags() { return tags; }
    public void setTags(java.util.Set<String> tags) { this.tags = tags; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    /**
     * 템플릿 사용 횟수를 증가시킵니다.
     */
    public void incrementUsageCount() {
        this.usageCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 템플릿 평점을 업데이트합니다.
     * 
     * @param newRating 새로운 평점
     * @param totalRatings 총 평점 수
     */
    public void updateRating(double newRating, int totalRatings) {
        this.rating = ((this.rating * (totalRatings - 1)) + newRating) / totalRatings;
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}