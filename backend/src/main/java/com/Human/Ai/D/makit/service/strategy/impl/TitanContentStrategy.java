package com.Human.Ai.D.makit.service.strategy.impl;

import com.Human.Ai.D.makit.dto.ContentGenerationRequest;
import com.Human.Ai.D.makit.domain.Content;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.repository.ContentRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import com.Human.Ai.D.makit.service.strategy.ContentGenerationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Amazon Titan 모델을 사용한 콘텐츠 생성 전략
 * Human.Ai.D MaKIT 플랫폼의 기본 텍스트 생성 전략
 */
@Component
public class TitanContentStrategy implements ContentGenerationStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(TitanContentStrategy.class);
    
    @Autowired
    private BedrockService bedrockService;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Override
    @Async
    public CompletableFuture<Content> generateContent(ContentGenerationRequest request, User user) {
        logger.info("Titan 모델을 사용하여 콘텐츠 생성 시작 - 사용자: {}, 타입: {}", 
                   user.getUsername(), request.getContentType());
        
        try {
            String prompt = buildPrompt(request);
            String generatedText = bedrockService.generateTextWithTitan(prompt, 1000);
            
            Content content = new Content(generateTitle(request), request.getContentType(), user);
            content.setBody(generatedText);
            content.setAiModel("amazon.titan-text-express-v1");
            content.setPrompt(prompt);
            content.setStatus(Content.ContentStatus.GENERATED);
            
            Content savedContent = contentRepository.save(content);
            
            logger.info("Titan 모델로 콘텐츠 생성 완료 - ID: {}", savedContent.getId());
            return CompletableFuture.completedFuture(savedContent);
            
        } catch (Exception e) {
            logger.error("Titan 모델 콘텐츠 생성 실패 - 사용자: {}", user.getUsername(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public boolean supports(Content.ContentType contentType) {
        return contentType == Content.ContentType.BLOG_POST || 
               contentType == Content.ContentType.AD_COPY ||
               contentType == Content.ContentType.EMAIL_TEMPLATE;
    }
    
    @Override
    public String getModelId() {
        return "amazon.titan-text-express-v1";
    }
    
    @Override
    public int getPriority() {
        return 10; // 높은 우선순위
    }
    
    private String buildPrompt(ContentGenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Human.Ai.D MaKIT 플랫폼을 위한 ");
        
        switch (request.getContentType()) {
            case BLOG_POST:
                prompt.append("블로그 포스트를 작성해주세요.\n\n");
                break;
            case AD_COPY:
                prompt.append("광고 카피를 작성해주세요.\n\n");
                break;
            case EMAIL_TEMPLATE:
                prompt.append("이메일 템플릿을 작성해주세요.\n\n");
                break;
            default:
                prompt.append("콘텐츠를 작성해주세요.\n\n");
        }
        
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            prompt.append("키워드: ").append(request.getKeywords()).append("\n");
        }
        
        if (request.getTargetAudience() != null && !request.getTargetAudience().isEmpty()) {
            prompt.append("타겟 오디언스: ").append(request.getTargetAudience()).append("\n");
        }
        
        if (request.getProduct() != null && !request.getProduct().isEmpty()) {
            prompt.append("제품/서비스: ").append(request.getProduct()).append("\n");
        }
        
        prompt.append("\n요구사항:\n");
        prompt.append("- 한국어로 작성\n");
        prompt.append("- 전문적이고 매력적인 톤\n");
        prompt.append("- Human.Ai.D의 브랜드 가치 반영\n");
        prompt.append("- SEO 최적화 고려\n");
        prompt.append("- 독자의 관심을 끌 수 있는 구성\n");
        
        return prompt.toString();
    }
    
    private String generateTitle(ContentGenerationRequest request) {
        if (request.getProduct() != null && !request.getProduct().isEmpty()) {
            return request.getProduct() + " - " + request.getContentType().name();
        }
        return "Human.Ai.D MaKIT - " + request.getContentType().name();
    }
}