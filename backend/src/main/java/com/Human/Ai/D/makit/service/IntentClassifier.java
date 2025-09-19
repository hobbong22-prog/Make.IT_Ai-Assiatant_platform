package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class IntentClassifier {
    
    private static final Logger logger = LoggerFactory.getLogger(IntentClassifier.class);
    
    @Autowired
    private BedrockService bedrockService;
    
    // 의도 분류를 위한 키워드 패턴들
    private static final List<IntentPattern> INTENT_PATTERNS = Arrays.asList(
            new IntentPattern(Intent.GREETING, Arrays.asList("안녕", "hello", "hi", "좋은", "반가워"), 0.9),
            new IntentPattern(Intent.PRODUCT_INQUIRY, Arrays.asList("제품", "상품", "서비스", "기능", "가격", "비용"), 0.8),
            new IntentPattern(Intent.TECHNICAL_SUPPORT, Arrays.asList("문제", "오류", "에러", "작동", "설정", "도움"), 0.8),
            new IntentPattern(Intent.ACCOUNT_MANAGEMENT, Arrays.asList("계정", "로그인", "비밀번호", "회원", "가입"), 0.8),
            new IntentPattern(Intent.BILLING_INQUIRY, Arrays.asList("결제", "청구", "요금", "환불", "구독"), 0.8),
            new IntentPattern(Intent.COMPLAINT, Arrays.asList("불만", "문제", "개선", "피드백", "불편"), 0.7),
            new IntentPattern(Intent.FAREWELL, Arrays.asList("안녕", "goodbye", "bye", "감사", "끝"), 0.9),
            new IntentPattern(Intent.ESCALATION_REQUEST, Arrays.asList("상담원", "직원", "사람", "담당자", "연결"), 0.9)
    );
    
    /**
     * 사용자 메시지의 의도를 분류합니다.
     */
    public IntentClassificationResult classifyIntent(String message) {
        try {
            // 1. 키워드 기반 분류
            IntentClassificationResult keywordResult = classifyByKeywords(message);
            
            // 2. AI 기반 분류 (높은 신뢰도가 필요한 경우)
            if (keywordResult.getConfidence() < 0.7) {
                IntentClassificationResult aiResult = classifyWithAI(message);
                
                // AI 결과가 더 신뢰도가 높으면 사용
                if (aiResult.getConfidence() > keywordResult.getConfidence()) {
                    return aiResult;
                }
            }
            
            return keywordResult;
            
        } catch (Exception e) {
            logger.error("Error classifying intent for message: {}", message, e);
            return new IntentClassificationResult(Intent.UNKNOWN, 0.0, "분류 오류");
        }
    }
    
    /**
     * 키워드 기반으로 의도를 분류합니다.
     */
    private IntentClassificationResult classifyByKeywords(String message) {
        String lowerMessage = message.toLowerCase();
        
        Intent bestIntent = Intent.UNKNOWN;
        double bestConfidence = 0.0;
        
        for (IntentPattern pattern : INTENT_PATTERNS) {
            double score = calculateKeywordScore(lowerMessage, pattern);
            
            if (score > bestConfidence) {
                bestConfidence = score;
                bestIntent = pattern.getIntent();
            }
        }
        
        return new IntentClassificationResult(bestIntent, bestConfidence, "키워드 기반 분류");
    }
    
    /**
     * AI를 사용하여 의도를 분류합니다.
     */
    private IntentClassificationResult classifyWithAI(String message) {
        try {
            String prompt = buildIntentClassificationPrompt(message);
            String response = bedrockService.generateTextWithClaude(prompt, 200);
            
            return parseAIResponse(response);
            
        } catch (Exception e) {
            logger.error("Error in AI intent classification", e);
            return new IntentClassificationResult(Intent.UNKNOWN, 0.0, "AI 분류 오류");
        }
    }
    
    /**
     * 키워드 점수를 계산합니다.
     */
    private double calculateKeywordScore(String message, IntentPattern pattern) {
        int matchCount = 0;
        
        for (String keyword : pattern.getKeywords()) {
            if (message.contains(keyword)) {
                matchCount++;
            }
        }
        
        if (matchCount == 0) {
            return 0.0;
        }
        
        // 매치된 키워드 비율과 패턴 가중치를 곱함
        double matchRatio = (double) matchCount / pattern.getKeywords().size();
        return matchRatio * pattern.getWeight();
    }
    
    /**
     * 의도 분류를 위한 프롬프트를 생성합니다.
     */
    private String buildIntentClassificationPrompt(String message) {
        return String.format(
                "다음 고객 메시지의 의도를 분류해주세요:\n\n" +
                "메시지: \"%s\"\n\n" +
                "가능한 의도:\n" +
                "- GREETING: 인사\n" +
                "- PRODUCT_INQUIRY: 제품 문의\n" +
                "- TECHNICAL_SUPPORT: 기술 지원\n" +
                "- ACCOUNT_MANAGEMENT: 계정 관리\n" +
                "- BILLING_INQUIRY: 결제 문의\n" +
                "- COMPLAINT: 불만 사항\n" +
                "- FAREWELL: 작별 인사\n" +
                "- ESCALATION_REQUEST: 상담원 연결 요청\n" +
                "- UNKNOWN: 알 수 없음\n\n" +
                "응답 형식: INTENT:신뢰도(0.0-1.0)\n" +
                "예시: PRODUCT_INQUIRY:0.85",
                message
        );
    }
    
    /**
     * AI 응답을 파싱합니다.
     */
    private IntentClassificationResult parseAIResponse(String response) {
        try {
            String[] parts = response.trim().split(":");
            if (parts.length == 2) {
                Intent intent = Intent.valueOf(parts[0].trim());
                double confidence = Double.parseDouble(parts[1].trim());
                
                return new IntentClassificationResult(intent, confidence, "AI 기반 분류");
            }
        } catch (Exception e) {
            logger.error("Error parsing AI response: {}", response, e);
        }
        
        return new IntentClassificationResult(Intent.UNKNOWN, 0.0, "AI 응답 파싱 오류");
    }
    
    /**
     * 의도 분류 결과
     */
    public static class IntentClassificationResult {
        private final Intent intent;
        private final double confidence;
        private final String method;
        
        public IntentClassificationResult(Intent intent, double confidence, String method) {
            this.intent = intent;
            this.confidence = confidence;
            this.method = method;
        }
        
        public Intent getIntent() { return intent; }
        public double getConfidence() { return confidence; }
        public String getMethod() { return method; }
        
        public boolean isHighConfidence() {
            return confidence >= 0.8;
        }
        
        public boolean isLowConfidence() {
            return confidence < 0.5;
        }
    }
    
    /**
     * 의도 패턴
     */
    private static class IntentPattern {
        private final Intent intent;
        private final List<String> keywords;
        private final double weight;
        
        public IntentPattern(Intent intent, List<String> keywords, double weight) {
            this.intent = intent;
            this.keywords = keywords;
            this.weight = weight;
        }
        
        public Intent getIntent() { return intent; }
        public List<String> getKeywords() { return keywords; }
        public double getWeight() { return weight; }
    }
    
    /**
     * 의도 열거형
     */
    public enum Intent {
        GREETING,
        PRODUCT_INQUIRY,
        TECHNICAL_SUPPORT,
        ACCOUNT_MANAGEMENT,
        BILLING_INQUIRY,
        COMPLAINT,
        FAREWELL,
        ESCALATION_REQUEST,
        UNKNOWN
    }
}