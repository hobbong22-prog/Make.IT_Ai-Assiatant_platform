package com.Human.Ai.D.makit.dto;

import com.Human.Ai.D.makit.domain.Content;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ContentGenerationRequest {
    
    @NotNull
    private Long userId;
    
    @NotNull
    private Content.ContentType contentType;
    
    private String topic;
    private String product;
    private String subject;
    private String purpose;
    private String keywords;
    
    @NotBlank
    private String targetAudience;
    
    private String platform;
    private String tone;
    private boolean multimodal = false;
    private String imagePrompt;
    
    // Constructors
    public ContentGenerationRequest() {}
    
    public ContentGenerationRequest(Long userId, String targetAudience, Content.ContentType contentType) {
        this.userId = userId;
        this.targetAudience = targetAudience;
        this.contentType = contentType;
    }
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Content.ContentType getContentType() { return contentType; }
    public void setContentType(Content.ContentType contentType) { this.contentType = contentType; }
    
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    
    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    
    public boolean isMultimodal() { return multimodal; }
    public void setMultimodal(boolean multimodal) { this.multimodal = multimodal; }
    
    public String getImagePrompt() { return imagePrompt; }
    public void setImagePrompt(String imagePrompt) { this.imagePrompt = imagePrompt; }
}