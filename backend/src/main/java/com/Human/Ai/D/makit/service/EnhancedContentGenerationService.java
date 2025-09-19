package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Content;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.dto.ContentGenerationRequest;
import com.Human.Ai.D.makit.repository.ContentRepository;
import com.Human.Ai.D.makit.repository.UserRepository;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import com.Human.Ai.D.makit.service.strategy.ContentGenerationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 향상된 콘텐츠 생성 서비스
 * 전략 패턴을 사용하여 다양한 AI 모델을 지원하고 멀티모달 콘텐츠 생성을 제공합니다.
 */
@Service
@Transactional
public class EnhancedContentGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedContentGenerationService.class);
    
    @Autowired
    private List<ContentGenerationStrategy> strategies;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BedrockService bedrockService;
    
    @Autowired
    private ContentQualityAnalyzer qualityAnalyzer;
    
    /**
     * 콘텐츠를 비동기적으로 생성합니다.
     * 
     * @param request 콘텐츠 생성 요청
     * @return 생성된 콘텐츠의 CompletableFuture
     */
    @Async
    public CompletableFuture<Content> generateContent(ContentGenerationRequest request) {
        logger.info("콘텐츠 생성 시작 - 사용자 ID: {}, 타입: {}", 
                   request.getUserId(), request.getContentType());
        
        try {
            // 사용자 조회
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + request.getUserId());
            }
            User user = userOpt.get();
            
            // 적절한 전략 선택
            ContentGenerationStrategy strategy = selectStrategy(request.getContentType(), request.isMultimodal());
            if (strategy == null) {
                throw new IllegalArgumentException("지원되지 않는 콘텐츠 타입입니다: " + request.getContentType());
            }
            
            logger.info("선택된 전략: {}, 모델: {}", 
                       strategy.getClass().getSimpleName(), strategy.getModelId());
            
            // 콘텐츠 생성
            CompletableFuture<Content> contentFuture = strategy.generateContent(request, user);
            
            // 멀티모달 콘텐츠인 경우 이미지 생성 추가
            if (request.isMultimodal() && strategy.supportsMultimodal()) {
                contentFuture = contentFuture.thenCompose(content -> generateMultimodalContent(content, request));
            }
            
            // 품질 분석 추가
            return contentFuture.thenCompose(this::analyzeAndEnhanceContent);
            
        } catch (Exception e) {
            logger.error("콘텐츠 생성 실패 - 사용자 ID: {}", request.getUserId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }    
    /*
*
     * 멀티모달 콘텐츠를 생성합니다 (텍스트 + 이미지).
     * 
     * @param content 기본 텍스트 콘텐츠
     * @param request 원본 요청
     * @return 이미지가 추가된 콘텐츠
     */
    private CompletableFuture<Content> generateMultimodalContent(Content content, ContentGenerationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("멀티모달 콘텐츠 생성 - 콘텐츠 ID: {}", content.getId());
                
                String imagePrompt = request.getImagePrompt();
                if (imagePrompt == null || imagePrompt.trim().isEmpty()) {
                    // 텍스트 콘텐츠를 기반으로 이미지 프롬프트 생성
                    imagePrompt = generateImagePromptFromText(content.getBody());
                }
                
                // Stable Diffusion으로 이미지 생성
                String base64Image = bedrockService.generateImageWithStableDiffusion(imagePrompt, 1024, 1024);
                
                // 이미지 URL 설정 (실제로는 S3에 업로드하고 URL을 반환해야 함)
                content.setImageUrl("data:image/png;base64," + base64Image);
                content.setUpdatedAt(LocalDateTime.now());
                
                Content savedContent = contentRepository.save(content);
                logger.info("멀티모달 콘텐츠 생성 완료 - 콘텐츠 ID: {}", savedContent.getId());
                
                return savedContent;
                
            } catch (Exception e) {
                logger.error("멀티모달 콘텐츠 생성 실패 - 콘텐츠 ID: {}", content.getId(), e);
                // 이미지 생성 실패 시에도 텍스트 콘텐츠는 반환
                return content;
            }
        });
    }
    
    /**
     * 텍스트 콘텐츠를 기반으로 이미지 프롬프트를 생성합니다.
     * 
     * @param textContent 텍스트 콘텐츠
     * @return 이미지 프롬프트
     */
    private String generateImagePromptFromText(String textContent) {
        try {
            String prompt = String.format(
                "다음 텍스트 콘텐츠를 기반으로 적절한 이미지를 생성하기 위한 영어 프롬프트를 작성해주세요. " +
                "프롬프트는 간결하고 구체적이어야 하며, 마케팅 콘텐츠에 적합해야 합니다.\n\n" +
                "텍스트 콘텐츠:\n%s\n\n" +
                "이미지 프롬프트 (영어로, 50단어 이내):",
                textContent.length() > 500 ? textContent.substring(0, 500) + "..." : textContent
            );
            
            String imagePrompt = bedrockService.generateTextWithClaude(prompt, 100);
            return imagePrompt.trim();
            
        } catch (Exception e) {
            logger.warn("이미지 프롬프트 생성 실패, 기본 프롬프트 사용", e);
            return "professional marketing content, modern design, clean background, high quality";
        }
    }
    
    /**
     * 콘텐츠 타입과 멀티모달 요구사항에 따라 적절한 전략을 선택합니다.
     * 
     * @param contentType 콘텐츠 타입
     * @param multimodal 멀티모달 요구사항
     * @return 선택된 전략
     */
    private ContentGenerationStrategy selectStrategy(Content.ContentType contentType, boolean multimodal) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(contentType))
                .filter(strategy -> !multimodal || strategy.supportsMultimodal())
                .min((s1, s2) -> Integer.compare(s1.getPriority(), s2.getPriority()))
                .orElse(null);
    }
    
    /**
     * 사용 가능한 모든 전략을 반환합니다.
     * 
     * @return 전략 목록
     */
    public List<ContentGenerationStrategy> getAvailableStrategies() {
        return strategies;
    }
    
    /**
     * 특정 콘텐츠 타입을 지원하는 전략들을 반환합니다.
     * 
     * @param contentType 콘텐츠 타입
     * @return 지원하는 전략 목록
     */
    public List<ContentGenerationStrategy> getStrategiesForContentType(Content.ContentType contentType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(contentType))
                .sorted((s1, s2) -> Integer.compare(s1.getPriority(), s2.getPriority()))
                .toList();
    }
    
    /**
     * 콘텐츠 품질을 분석하고 결과를 콘텐츠에 추가합니다.
     * 
     * @param content 분석할 콘텐츠
     * @return 품질 분석이 완료된 콘텐츠
     */
    private CompletableFuture<Content> analyzeAndEnhanceContent(Content content) {
        return qualityAnalyzer.analyzeContentQuality(content)
                .thenApply(qualityResult -> {
                    try {
                        logger.info("콘텐츠 품질 분석 완료 - 콘텐츠 ID: {}, 전체 점수: {}", 
                                   content.getId(), qualityResult.getOverallScore());
                        
                        // 품질 점수를 콘텐츠 메타데이터에 추가 (실제로는 별도 테이블에 저장해야 함)
                        String qualityInfo = String.format(
                            "품질 점수: %.1f/100 (가독성: %.1f, 참여도: %.1f, SEO: %.1f)",
                            qualityResult.getOverallScore(),
                            qualityResult.getReadabilityScore(),
                            qualityResult.getEngagementScore(),
                            qualityResult.getSeoScore()
                        );
                        
                        // 기존 프롬프트에 품질 정보 추가
                        String enhancedPrompt = content.getPrompt() + "\n\n[품질 분석 결과]\n" + qualityInfo;
                        if (qualityResult.getFeedback() != null) {
                            enhancedPrompt += "\n피드백: " + qualityResult.getFeedback();
                        }
                        content.setPrompt(enhancedPrompt);
                        
                        // 콘텐츠 저장
                        Content savedContent = contentRepository.save(content);
                        
                        return savedContent;
                        
                    } catch (Exception e) {
                        logger.error("품질 분석 결과 처리 실패 - 콘텐츠 ID: {}", content.getId(), e);
                        // 품질 분석 실패 시에도 원본 콘텐츠는 반환
                        return content;
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("콘텐츠 품질 분석 실패 - 콘텐츠 ID: {}", content.getId(), throwable);
                    // 품질 분석 실패 시에도 원본 콘텐츠는 반환
                    return content;
                });
    }
    
    /**
     * 콘텐츠의 품질을 분석합니다 (독립적인 메서드).
     * 
     * @param contentId 분석할 콘텐츠 ID
     * @return 품질 분석 결과
     */
    public CompletableFuture<ContentQualityAnalyzer.QualityAnalysisResult> analyzeContentQuality(Long contentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Content> contentOpt = contentRepository.findById(contentId);
                if (contentOpt.isEmpty()) {
                    throw new IllegalArgumentException("콘텐츠를 찾을 수 없습니다: " + contentId);
                }
                
                Content content = contentOpt.get();
                return qualityAnalyzer.analyzeContentQuality(content).join();
                
            } catch (Exception e) {
                logger.error("콘텐츠 품질 분석 실패 - 콘텐츠 ID: {}", contentId, e);
                throw new RuntimeException("콘텐츠 품질 분석 중 오류가 발생했습니다", e);
            }
        });
    }}
