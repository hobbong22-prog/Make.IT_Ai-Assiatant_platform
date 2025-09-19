package com.Human.Ai.D.makit.service.strategy;

import com.Human.Ai.D.makit.dto.ContentGenerationRequest;
import com.Human.Ai.D.makit.domain.Content;
import com.Human.Ai.D.makit.domain.User;

import java.util.concurrent.CompletableFuture;

/**
 * 콘텐츠 생성 전략 인터페이스
 * Human.Ai.D MaKIT 플랫폼의 다양한 AI 모델을 위한 전략 패턴 구현
 */
public interface ContentGenerationStrategy {
    
    /**
     * 콘텐츠를 비동기적으로 생성합니다.
     * 
     * @param request 콘텐츠 생성 요청
     * @param user 요청한 사용자
     * @return 생성된 콘텐츠의 CompletableFuture
     */
    CompletableFuture<Content> generateContent(ContentGenerationRequest request, User user);
    
    /**
     * 이 전략이 지원하는 콘텐츠 타입인지 확인합니다.
     * 
     * @param contentType 콘텐츠 타입
     * @return 지원 여부
     */
    boolean supports(Content.ContentType contentType);
    
    /**
     * 사용하는 AI 모델 ID를 반환합니다.
     * 
     * @return 모델 ID
     */
    String getModelId();
    
    /**
     * 전략의 우선순위를 반환합니다. (낮을수록 높은 우선순위)
     * 
     * @return 우선순위
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * 멀티모달 콘텐츠 생성을 지원하는지 확인합니다.
     * 
     * @return 멀티모달 지원 여부
     */
    default boolean supportsMultimodal() {
        return false;
    }
}