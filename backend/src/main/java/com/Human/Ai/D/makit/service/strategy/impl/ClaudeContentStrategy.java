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
 * Anthropic Claude 모델을 사용한 콘텐츠 생성 전략
 * Human.Ai.D MaKIT 플랫폼의 고품질 창작 콘텐츠 생성 전략
 */
@Component
public class ClaudeContentStrategy implements ContentGenerationStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(ClaudeContentStrategy.class);
    
    @Autowired
    private BedrockService bedrockService;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Override
    @Async
    public CompletableFuture<Content> generateContent(ContentGenerationRequest request, User user) {
        logger.info("Claude 모델을 사용하여 콘텐츠 생성 시작 - 사용자: {}, 타입: {}", 
                   user.getUsername(), request.getContentType());
        
        try {
            String prompt = buildPrompt(request);
            String generatedText = bedrockService.generateTextWithClaude(prompt, 1000);
            
            Content content = new Content(generateTitle(request), request.getContentType(), user);
            content.setBody(generatedText);
            content.setAiModel("anthropic.claude-v2");
            content.setPrompt(prompt);
            content.setStatus(Content.ContentStatus.GENERATED);
            
            Content savedContent = contentRepository.save(content);
            
            logger.info("Claude 모델로 콘텐츠 생성 완료 - ID: {}", savedContent.getId());
            return CompletableFuture.completedFuture(savedContent);
            
        } catch (Exception e) {
            logger.error("Claude 모델 콘텐츠 생성 실패 - 사용자: {}", user.getUsername(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public boolean supports(Content.ContentType contentType) {
        return contentType == Content.ContentType.SOCIAL_MEDIA_POST || 
               contentType == Content.ContentType.EMAIL_TEMPLATE || 
               contentType == Content.ContentType.AD_COPY;
    }
    
    @Override
    public String getModelId() {
        return "anthropic.claude-3-sonnet-20240229-v1:0";
    }
    
    @Override
    public int getPriority() {
        return 5; // 매우 높은 우선순위
    }
    
    @Override
    public boolean supportsMultimodal() {
        return true; // Claude는 멀티모달 지원
    }
    
    private String buildPrompt(ContentGenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 Human.Ai.D의 전문 마케팅 콘텐츠 작가입니다. ");
        
        switch (request.getContentType()) {
            case SOCIAL_MEDIA_POST:
                prompt.append("매력적인 소셜미디어 포스트를 작성해주세요.\n\n");
                prompt.append("요구사항:\n");
                prompt.append("- 간결하고 임팩트 있는 메시지\n");
                prompt.append("- 적절한 해시태그 포함\n");
                prompt.append("- 참여를 유도하는 CTA\n");
                break;
            case EMAIL_TEMPLATE:
                prompt.append("전문적인 이메일 템플릿을 작성해주세요.\n\n");
                prompt.append("요구사항:\n");
                prompt.append("- 명확한 제목과 본문 구조\n");
                prompt.append("- 개인화된 인사말\n");
                prompt.append("- 명확한 행동 유도\n");
                break;
            case AD_COPY:
                prompt.append("효과적인 광고 카피를 작성해주세요.\n\n");
                prompt.append("요구사항:\n");
                prompt.append("- 주목을 끄는 헤드라인\n");
                prompt.append("- 혜택 중심의 메시지\n");
                prompt.append("- 강력한 CTA\n");
                break;
            default:
                prompt.append("창의적인 마케팅 콘텐츠를 작성해주세요.\n\n");
                prompt.append("요구사항:\n");
                prompt.append("- 독창적이고 기억에 남는 내용\n");
                prompt.append("- 브랜드 스토리텔링 포함\n");
                prompt.append("- 감정적 연결 유도\n");
                break;
        }
        
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            prompt.append("\n키워드: ").append(request.getKeywords());
        }
        
        if (request.getTargetAudience() != null && !request.getTargetAudience().isEmpty()) {
            prompt.append("\n타겟 오디언스: ").append(request.getTargetAudience());
        }
        
        if (request.getProduct() != null && !request.getProduct().isEmpty()) {
            prompt.append("\n제품/서비스: ").append(request.getProduct());
        }
        
        prompt.append("\n\nHuman.Ai.D의 브랜드 가치:\n");
        prompt.append("- 혁신적인 AI 기술\n");
        prompt.append("- 사용자 중심의 솔루션\n");
        prompt.append("- 신뢰할 수 있는 파트너\n");
        prompt.append("- 지속적인 성장과 발전\n");
        
        return prompt.toString();
    }
    
    private String generateTitle(ContentGenerationRequest request) {
        String baseTitle = "Human.Ai.D MaKIT";
        
        switch (request.getContentType()) {
            case SOCIAL_MEDIA_POST:
                return baseTitle + " - 소셜미디어 포스트";
            case EMAIL_TEMPLATE:
                return baseTitle + " - 이메일 템플릿";
            case AD_COPY:
                return baseTitle + " - 광고 카피";
            default:
                return baseTitle + " - " + request.getContentType().name();
        }
    }
}