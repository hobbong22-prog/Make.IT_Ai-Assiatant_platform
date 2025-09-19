package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Content;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.repository.ContentRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
@Transactional
public class ContentGenerationService {
    
    @Autowired
    private BedrockService bedrockService;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Async
    public CompletableFuture<Content> generateBlogPost(String topic, String targetAudience, User user) {
        String prompt = String.format(
            "Write a comprehensive blog post about '%s' targeted at %s. " +
            "The blog post should be engaging, informative, and SEO-optimized. " +
            "Include a compelling title, introduction, main content with subheadings, and conclusion. " +
            "Make it approximately 800-1000 words.",
            topic, targetAudience
        );
        
        Content content = new Content("Blog Post: " + topic, Content.ContentType.BLOG_POST, user);
        content.setPrompt(prompt);
        content.setAiModel("claude-v2");
        content = contentRepository.save(content);
        
        final Content finalContent = content;
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Enhanced error handling and retry logic
                String generatedText = generateWithRetry(() -> 
                    bedrockService.generateTextWithClaude(prompt, 2000), 3);
                
                finalContent.setBody(generatedText);
                finalContent.setStatus(Content.ContentStatus.GENERATED);
                Content savedContent = contentRepository.save(finalContent);
                
                return savedContent;
            } catch (Exception e) {
                finalContent.setStatus(Content.ContentStatus.DRAFT);
                finalContent.setBody("Error generating content: " + e.getMessage());
                contentRepository.save(finalContent);
                throw new RuntimeException("Failed to generate blog post", e);
            }
        });
    }
    
    @Async
    public CompletableFuture<Content> generateAdCopy(String product, String targetAudience, String platform, User user) {
        String prompt = String.format(
            "Create compelling ad copy for '%s' targeting %s on %s platform. " +
            "The ad should be attention-grabbing, persuasive, and include a clear call-to-action. " +
            "Keep it concise and platform-appropriate. Include multiple variations.",
            product, targetAudience, platform
        );
        
        Content content = new Content("Ad Copy: " + product, Content.ContentType.AD_COPY, user);
        content.setPrompt(prompt);
        content.setAiModel("titan-text-express-v1");
        content = contentRepository.save(content);
        
        try {
            String generatedText = bedrockService.generateTextWithTitan(prompt, 500);
            content.setBody(generatedText);
            content.setStatus(Content.ContentStatus.GENERATED);
            content = contentRepository.save(content);
            
            return CompletableFuture.completedFuture(content);
        } catch (Exception e) {
            content.setStatus(Content.ContentStatus.DRAFT);
            content.setBody("Error generating content: " + e.getMessage());
            contentRepository.save(content);
            throw new RuntimeException("Failed to generate ad copy", e);
        }
    }
    
    @Async
    public CompletableFuture<Content> generateSocialMediaPost(String topic, String platform, String tone, User user) {
        String prompt = String.format(
            "Create an engaging social media post about '%s' for %s platform. " +
            "Use a %s tone and include relevant hashtags. " +
            "Make it platform-appropriate in length and style. " +
            "Include emojis where appropriate.",
            topic, platform, tone
        );
        
        Content content = new Content("Social Media: " + topic, Content.ContentType.SOCIAL_MEDIA_POST, user);
        content.setPrompt(prompt);
        content.setAiModel("claude-v2");
        content = contentRepository.save(content);
        
        try {
            String generatedText = bedrockService.generateTextWithClaude(prompt, 300);
            content.setBody(generatedText);
            content.setStatus(Content.ContentStatus.GENERATED);
            content = contentRepository.save(content);
            
            return CompletableFuture.completedFuture(content);
        } catch (Exception e) {
            content.setStatus(Content.ContentStatus.DRAFT);
            content.setBody("Error generating content: " + e.getMessage());
            contentRepository.save(content);
            throw new RuntimeException("Failed to generate social media post", e);
        }
    }
    
    @Async
    public CompletableFuture<Content> generateEmailTemplate(String subject, String purpose, String targetAudience, User user) {
        String prompt = String.format(
            "Create a professional email template with subject '%s' for %s targeting %s. " +
            "Include a compelling subject line, personalized greeting, engaging body content, " +
            "clear call-to-action, and professional closing. " +
            "Make it conversion-focused and mobile-friendly.",
            subject, purpose, targetAudience
        );
        
        Content content = new Content("Email: " + subject, Content.ContentType.EMAIL_TEMPLATE, user);
        content.setPrompt(prompt);
        content.setAiModel("titan-text-express-v1");
        content = contentRepository.save(content);
        
        try {
            String generatedText = bedrockService.generateTextWithTitan(prompt, 1000);
            content.setBody(generatedText);
            content.setStatus(Content.ContentStatus.GENERATED);
            content = contentRepository.save(content);
            
            return CompletableFuture.completedFuture(content);
        } catch (Exception e) {
            content.setStatus(Content.ContentStatus.DRAFT);
            content.setBody("Error generating content: " + e.getMessage());
            contentRepository.save(content);
            throw new RuntimeException("Failed to generate email template", e);
        }
    }
    
    /**
     * Enhanced retry logic for AI operations
     */
    private String generateWithRetry(Supplier<String> operation, int maxRetries) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    try {
                        // Exponential backoff
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("Failed after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Batch content generation with progress tracking
     */
    @Async
    public CompletableFuture<Void> generateBatchContent(java.util.List<ContentRequest> requests, User user) {
        return CompletableFuture.runAsync(() -> {
            int total = requests.size();
            int completed = 0;
            
            for (ContentRequest request : requests) {
                try {
                    switch (request.getType()) {
                        case BLOG_POST:
                            generateBlogPost(request.getTopic(), request.getTargetAudience(), user);
                            break;
                        case AD_COPY:
                            generateAdCopy(request.getProduct(), request.getTargetAudience(), 
                                         request.getPlatform(), user);
                            break;
                        case SOCIAL_MEDIA_POST:
                            generateSocialMediaPost(request.getTopic(), request.getPlatform(), 
                                                  request.getTone(), user);
                            break;
                        case EMAIL_TEMPLATE:
                            generateEmailTemplate(request.getSubject(), request.getPurpose(), 
                                                request.getTargetAudience(), user);
                            break;
                    }
                    completed++;
                    
                    // Log progress
                    double progress = (double) completed / total * 100;
                    System.out.println(String.format("Batch progress: %.1f%% (%d/%d)", 
                                                    progress, completed, total));
                    
                } catch (Exception e) {
                    System.err.println("Failed to generate content for request: " + request);
                }
            }
        });
    }
    
    /**
     * Content request class for batch operations
     */
    public static class ContentRequest {
        private Content.ContentType type;
        private String topic;
        private String targetAudience;
        private String platform;
        private String tone;
        private String product;
        private String subject;
        private String purpose;
        
        // Constructors and getters/setters
        public ContentRequest(Content.ContentType type) {
            this.type = type;
        }
        
        public Content.ContentType getType() { return type; }
        public String getTopic() { return topic; }
        public ContentRequest setTopic(String topic) { this.topic = topic; return this; }
        public String getTargetAudience() { return targetAudience; }
        public ContentRequest setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; return this; }
        public String getPlatform() { return platform; }
        public ContentRequest setPlatform(String platform) { this.platform = platform; return this; }
        public String getTone() { return tone; }
        public ContentRequest setTone(String tone) { this.tone = tone; return this; }
        public String getProduct() { return product; }
        public ContentRequest setProduct(String product) { this.product = product; return this; }
        public String getSubject() { return subject; }
        public ContentRequest setSubject(String subject) { this.subject = subject; return this; }
        public String getPurpose() { return purpose; }
        public ContentRequest setPurpose(String purpose) { this.purpose = purpose; return this; }
    }
}