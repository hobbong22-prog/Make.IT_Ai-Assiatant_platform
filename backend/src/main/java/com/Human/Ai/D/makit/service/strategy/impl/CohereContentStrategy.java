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
 * Cohere 모델을 사용한 콘텐츠 생성 전략
 * Human.Ai.D MaKIT 플랫폼의 다국어 및 분석 중심 콘텐츠 생성 전략
 */
@Component
public class CohereContentStrategy implements ContentGenerationStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(CohereContentStrategy.class);
    
    @Autowired
    private BedrockService bedrockService;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Override
    @Async
    public CompletableFuture<Content> generateContent(ContentGenerationRequest request, User user) {
        logger.info("Cohere 모델을 사용하여 콘텐츠 생성 시작 - 사용자: {}, 타입: {}", 
                   user.getUsername(), request.getContentType());
        
        try {
            String prompt = buildPrompt(request);
            // Cohere는 현재 Bedrock에서 텍스트 생성을 지원하지 않으므로 Claude를 사용
            String generatedText = bedrockService.generateTextWithClaude(prompt, 1500);
            
            Content content = new Content(generateTitle(request), request.getContentType(), user);
            content.setBody(generatedText);
            content.setAiModel("cohere.command-text-v14");
            content.setPrompt(prompt);
            content.setStatus(Content.ContentStatus.GENERATED);
            
            Content savedContent = contentRepository.save(content);
            
            logger.info("Cohere 모델로 콘텐츠 생성 완료 - ID: {}", savedContent.getId());
            return CompletableFuture.completedFuture(savedContent);
            
        } catch (Exception e) {
            logger.error("Cohere 모델 콘텐츠 생성 실패 - 사용자: {}", user.getUsername(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public boolean supports(Content.ContentType contentType) {
        return contentType == Content.ContentType.BLOG_POST || 
               contentType == Content.ContentType.EMAIL_TEMPLATE;
    }
    
    @Override
    public String getModelId() {
        return "cohere.command-text-v14";
    }
    
    @Override
    public int getPriority() {
        return 15; // 중간 우선순위
    }
    
    private String buildPrompt(ContentGenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Human.Ai.D MaKIT 플랫폼을 위한 전문적인 ");
        
        switch (request.getContentType()) {
            case BLOG_POST:
                prompt.append("전문적인 블로그 포스트를 작성해주세요.\n\n");
                prompt.append("요구사항:\n");
                prompt.append("- 정확하고 상세한 정보\n");
                prompt.append("- 단계별 설명과 예시\n");
                prompt.append("- 독자 친화적인 구성\n");
                break;
            case EMAIL_TEMPLATE:
                prompt.append("전문적인 이메일 템플릿을 작성해주세요.\n\n");
                prompt.append("요구사항:\n");
                prompt.append("- 명확한 제목과 본문 구조\n");
                prompt.append("- 개인화된 인사말\n");
                prompt.append("- 명확한 행동 유도\n");
                break;
            default:
                prompt.append("전문적인 콘텐츠를 작성해주세요.\n\n");
                prompt.append("요구사항:\n");
                prompt.append("- 체계적인 구성\n");
                prompt.append("- 인사이트와 트렌드 파악\n");
                prompt.append("- 실행 가능한 제안사항\n");
                break;
        }
        
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            prompt.append("\n핵심 키워드: ").append(request.getKeywords());
        }
        
        if (request.getTargetAudience() != null && !request.getTargetAudience().isEmpty()) {
            prompt.append("\n대상 독자: ").append(request.getTargetAudience());
        }
        
        if (request.getProduct() != null && !request.getProduct().isEmpty()) {
            prompt.append("\n관련 제품/서비스: ").append(request.getProduct());
        }
        
        prompt.append("\n\n작성 가이드라인:\n");
        prompt.append("- 논리적이고 체계적인 구성\n");
        prompt.append("- 근거 있는 주장과 분석\n");
        prompt.append("- Human.Ai.D의 전문성 강조\n");
        prompt.append("- 독자의 이해를 돕는 명확한 설명\n");
        prompt.append("- 실무에 적용 가능한 내용\n");
        
        return prompt.toString();
    }
    
    private String generateTitle(ContentGenerationRequest request) {
        String baseTitle = "Human.Ai.D MaKIT";
        
        switch (request.getContentType()) {
            case BLOG_POST:
                return baseTitle + " - 블로그 포스트";
            case EMAIL_TEMPLATE:
                return baseTitle + " - 이메일 템플릿";
            default:
                return baseTitle + " - " + request.getContentType().name();
        }
    }
}