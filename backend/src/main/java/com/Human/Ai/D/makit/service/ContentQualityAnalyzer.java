package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.Content;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 콘텐츠 품질 분석 서비스
 * AI 모델을 사용하여 생성된 콘텐츠의 품질을 평가합니다.
 */
@Service
public class ContentQualityAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(ContentQualityAnalyzer.class);
    
    @Autowired
    private BedrockService bedrockService;
    
    /**
     * 콘텐츠 품질 분석 결과를 나타내는 클래스
     */
    public static class QualityAnalysisResult {
        private double overallScore;
        private double readabilityScore;
        private double engagementScore;
        private double seoScore;
        private String feedback;
        private Map<String, String> suggestions;
        
        public QualityAnalysisResult() {
            this.suggestions = new HashMap<>();
        }
        
        // Getters and Setters
        public double getOverallScore() { return overallScore; }
        public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
        
        public double getReadabilityScore() { return readabilityScore; }
        public void setReadabilityScore(double readabilityScore) { this.readabilityScore = readabilityScore; }
        
        public double getEngagementScore() { return engagementScore; }
        public void setEngagementScore(double engagementScore) { this.engagementScore = engagementScore; }
        
        public double getSeoScore() { return seoScore; }
        public void setSeoScore(double seoScore) { this.seoScore = seoScore; }
        
        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
        
        public Map<String, String> getSuggestions() { return suggestions; }
        public void setSuggestions(Map<String, String> suggestions) { this.suggestions = suggestions; }
    }
    
    /**
     * 콘텐츠의 품질을 종합적으로 분석합니다.
     * 
     * @param content 분석할 콘텐츠
     * @return 품질 분석 결과
     */
    public CompletableFuture<QualityAnalysisResult> analyzeContentQuality(Content content) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("콘텐츠 품질 분석 시작 - 콘텐츠 ID: {}, 타입: {}", 
                       content.getId(), content.getType());
            
            try {
                QualityAnalysisResult result = new QualityAnalysisResult();
                
                // 각 품질 지표 분석
                double readabilityScore = analyzeReadability(content);
                double engagementScore = analyzeEngagement(content);
                double seoScore = analyzeSEO(content);
                
                result.setReadabilityScore(readabilityScore);
                result.setEngagementScore(engagementScore);
                result.setSeoScore(seoScore);
                
                // 전체 점수 계산 (가중 평균)
                double overallScore = calculateOverallScore(readabilityScore, engagementScore, seoScore);
                result.setOverallScore(overallScore);
                
                // AI 기반 피드백 생성
                String feedback = generateAIFeedback(content, result);
                result.setFeedback(feedback);
                
                // 개선 제안 생성
                Map<String, String> suggestions = generateSuggestions(result);
                result.setSuggestions(suggestions);
                
                logger.info("콘텐츠 품질 분석 완료 - 콘텐츠 ID: {}, 전체 점수: {}", 
                           content.getId(), overallScore);
                
                return result;
                
            } catch (Exception e) {
                logger.error("콘텐츠 품질 분석 실패 - 콘텐츠 ID: {}", content.getId(), e);
                throw new RuntimeException("콘텐츠 품질 분석 중 오류가 발생했습니다", e);
            }
        });
    }
    
    /**
     * 콘텐츠의 가독성을 분석합니다.
     * 
     * @param content 분석할 콘텐츠
     * @return 가독성 점수 (0-100)
     */
    private double analyzeReadability(Content content) {
        logger.debug("가독성 분석 시작 - 콘텐츠 ID: {}", content.getId());
        
        try {
            String text = content.getBody();
            if (text == null || text.trim().isEmpty()) {
                return 0.0;
            }
            
            // 기본 가독성 지표 계산
            double basicScore = calculateBasicReadabilityScore(text);
            
            // AI 기반 가독성 분석
            double aiScore = getAIReadabilityScore(text);
            
            // 가중 평균 (기본 지표 40%, AI 분석 60%)
            double finalScore = (basicScore * 0.4) + (aiScore * 0.6);
            
            logger.debug("가독성 분석 완료 - 콘텐츠 ID: {}, 점수: {}", content.getId(), finalScore);
            return Math.min(100.0, Math.max(0.0, finalScore));
            
        } catch (Exception e) {
            logger.warn("가독성 분석 실패 - 콘텐츠 ID: {}, 기본 점수 반환", content.getId(), e);
            return 50.0; // 기본 점수
        }
    }
    
    /**
     * 콘텐츠의 참여 잠재력을 분석합니다.
     * 
     * @param content 분석할 콘텐츠
     * @return 참여 점수 (0-100)
     */
    private double analyzeEngagement(Content content) {
        logger.debug("참여 잠재력 분석 시작 - 콘텐츠 ID: {}", content.getId());
        
        try {
            String text = content.getBody();
            if (text == null || text.trim().isEmpty()) {
                return 0.0;
            }
            
            // 기본 참여 지표 계산
            double basicScore = calculateBasicEngagementScore(text, content.getType());
            
            // AI 기반 참여 분석
            double aiScore = getAIEngagementScore(text, content.getType());
            
            // 가중 평균 (기본 지표 30%, AI 분석 70%)
            double finalScore = (basicScore * 0.3) + (aiScore * 0.7);
            
            logger.debug("참여 잠재력 분석 완료 - 콘텐츠 ID: {}, 점수: {}", content.getId(), finalScore);
            return Math.min(100.0, Math.max(0.0, finalScore));
            
        } catch (Exception e) {
            logger.warn("참여 잠재력 분석 실패 - 콘텐츠 ID: {}, 기본 점수 반환", content.getId(), e);
            return 50.0; // 기본 점수
        }
    }
    
    /**
     * 콘텐츠의 SEO 점수를 분석합니다.
     * 
     * @param content 분석할 콘텐츠
     * @return SEO 점수 (0-100)
     */
    private double analyzeSEO(Content content) {
        logger.debug("SEO 분석 시작 - 콘텐츠 ID: {}", content.getId());
        
        try {
            String text = content.getBody();
            String title = content.getTitle();
            
            if (text == null || text.trim().isEmpty()) {
                return 0.0;
            }
            
            // 기본 SEO 지표 계산
            double basicScore = calculateBasicSEOScore(title, text);
            
            // AI 기반 SEO 분석
            double aiScore = getAISEOScore(title, text);
            
            // 가중 평균 (기본 지표 50%, AI 분석 50%)
            double finalScore = (basicScore * 0.5) + (aiScore * 0.5);
            
            logger.debug("SEO 분석 완료 - 콘텐츠 ID: {}, 점수: {}", content.getId(), finalScore);
            return Math.min(100.0, Math.max(0.0, finalScore));
            
        } catch (Exception e) {
            logger.warn("SEO 분석 실패 - 콘텐츠 ID: {}, 기본 점수 반환", content.getId(), e);
            return 50.0; // 기본 점수
        }
    }
    
    /**
     * 기본 가독성 점수를 계산합니다.
     */
    private double calculateBasicReadabilityScore(String text) {
        // 문장 길이, 단어 길이, 문단 구조 등을 기반으로 점수 계산
        String[] sentences = text.split("[.!?]+");
        String[] words = text.split("\\s+");
        
        double avgSentenceLength = (double) words.length / sentences.length;
        double avgWordLength = text.replaceAll("\\s+", "").length() / (double) words.length;
        
        // 적절한 문장 길이 (10-20 단어)와 단어 길이 (3-8 글자) 기준
        double sentenceScore = Math.max(0, 100 - Math.abs(avgSentenceLength - 15) * 3);
        double wordScore = Math.max(0, 100 - Math.abs(avgWordLength - 5) * 10);
        
        return (sentenceScore + wordScore) / 2;
    }
    
    /**
     * 기본 참여 점수를 계산합니다.
     */
    private double calculateBasicEngagementScore(String text, Content.ContentType contentType) {
        double score = 50.0; // 기본 점수
        
        // 콘텐츠 타입별 가중치
        switch (contentType) {
            case SOCIAL_MEDIA_POST:
                // 해시태그, 이모지, 질문 등 확인
                if (text.contains("#")) score += 10;
                if (text.contains("?")) score += 15;
                if (text.length() <= 280) score += 10; // 적절한 길이
                break;
            case EMAIL_TEMPLATE:
                // 개인화, CTA, 구조 확인
                if (text.contains("님") || text.contains("고객")) score += 10;
                if (text.toLowerCase().contains("click") || text.contains("클릭")) score += 15;
                break;
            case AD_COPY:
                // 강력한 CTA, 혜택 강조 확인
                if (text.contains("!")) score += 10;
                if (text.contains("무료") || text.contains("할인")) score += 15;
                break;
            default:
                break;
        }
        
        return Math.min(100.0, score);
    }
    
    /**
     * 기본 SEO 점수를 계산합니다.
     */
    private double calculateBasicSEOScore(String title, String text) {
        double score = 50.0; // 기본 점수
        
        // 제목 길이 확인 (30-60 글자)
        if (title != null && title.length() >= 30 && title.length() <= 60) {
            score += 20;
        }
        
        // 텍스트 길이 확인 (최소 300 글자)
        if (text.length() >= 300) {
            score += 15;
        }
        
        // 키워드 밀도 확인 (간단한 휴리스틱)
        String[] words = text.split("\\s+");
        if (words.length > 100) {
            score += 15;
        }
        
        return Math.min(100.0, score);
    }
    
    /**
     * AI 기반 가독성 점수를 가져옵니다.
     */
    private double getAIReadabilityScore(String text) {
        try {
            String prompt = String.format(
                "다음 텍스트의 가독성을 0-100 점수로 평가해주세요. " +
                "문장 구조, 어휘 선택, 논리적 흐름을 고려하여 점수만 숫자로 답해주세요.\n\n" +
                "텍스트: %s",
                text.length() > 1000 ? text.substring(0, 1000) + "..." : text
            );
            
            String response = bedrockService.generateTextWithClaude(prompt, 50);
            return parseScoreFromResponse(response);
            
        } catch (Exception e) {
            logger.warn("AI 가독성 분석 실패, 기본 점수 사용", e);
            return 70.0;
        }
    }
    
    /**
     * AI 기반 참여 점수를 가져옵니다.
     */
    private double getAIEngagementScore(String text, Content.ContentType contentType) {
        try {
            String prompt = String.format(
                "다음 %s 콘텐츠의 사용자 참여 잠재력을 0-100 점수로 평가해주세요. " +
                "흥미도, 감정적 어필, 행동 유도 요소를 고려하여 점수만 숫자로 답해주세요.\n\n" +
                "콘텐츠: %s",
                contentType.name(),
                text.length() > 1000 ? text.substring(0, 1000) + "..." : text
            );
            
            String response = bedrockService.generateTextWithClaude(prompt, 50);
            return parseScoreFromResponse(response);
            
        } catch (Exception e) {
            logger.warn("AI 참여 분석 실패, 기본 점수 사용", e);
            return 70.0;
        }
    }
    
    /**
     * AI 기반 SEO 점수를 가져옵니다.
     */
    private double getAISEOScore(String title, String text) {
        try {
            String prompt = String.format(
                "다음 콘텐츠의 SEO 최적화 정도를 0-100 점수로 평가해주세요. " +
                "키워드 사용, 구조, 메타 정보를 고려하여 점수만 숫자로 답해주세요.\n\n" +
                "제목: %s\n텍스트: %s",
                title != null ? title : "제목 없음",
                text.length() > 800 ? text.substring(0, 800) + "..." : text
            );
            
            String response = bedrockService.generateTextWithClaude(prompt, 50);
            return parseScoreFromResponse(response);
            
        } catch (Exception e) {
            logger.warn("AI SEO 분석 실패, 기본 점수 사용", e);
            return 70.0;
        }
    }
    
    /**
     * 전체 품질 점수를 계산합니다.
     */
    private double calculateOverallScore(double readability, double engagement, double seo) {
        // 가중 평균: 가독성 30%, 참여 40%, SEO 30%
        return (readability * 0.3) + (engagement * 0.4) + (seo * 0.3);
    }
    
    /**
     * AI 기반 피드백을 생성합니다.
     */
    private String generateAIFeedback(Content content, QualityAnalysisResult result) {
        try {
            String prompt = String.format(
                "다음 콘텐츠의 품질 분석 결과를 바탕으로 간결한 피드백을 한국어로 작성해주세요.\n\n" +
                "콘텐츠 타입: %s\n" +
                "가독성 점수: %.1f\n" +
                "참여 점수: %.1f\n" +
                "SEO 점수: %.1f\n" +
                "전체 점수: %.1f\n\n" +
                "콘텐츠: %s\n\n" +
                "2-3문장으로 핵심 피드백을 제공해주세요.",
                content.getType().name(),
                result.getReadabilityScore(),
                result.getEngagementScore(),
                result.getSeoScore(),
                result.getOverallScore(),
                content.getBody().length() > 500 ? content.getBody().substring(0, 500) + "..." : content.getBody()
            );
            
            return bedrockService.generateTextWithClaude(prompt, 200);
            
        } catch (Exception e) {
            logger.warn("AI 피드백 생성 실패, 기본 피드백 사용", e);
            return generateDefaultFeedback(result);
        }
    }
    
    /**
     * 개선 제안을 생성합니다.
     */
    private Map<String, String> generateSuggestions(QualityAnalysisResult result) {
        Map<String, String> suggestions = new HashMap<>();
        
        if (result.getReadabilityScore() < 70) {
            suggestions.put("readability", "문장을 더 짧고 명확하게 작성하고, 복잡한 어휘를 간단한 표현으로 바꿔보세요.");
        }
        
        if (result.getEngagementScore() < 70) {
            suggestions.put("engagement", "더 감정적이고 개인적인 언어를 사용하고, 독자의 참여를 유도하는 질문이나 CTA를 추가해보세요.");
        }
        
        if (result.getSeoScore() < 70) {
            suggestions.put("seo", "관련 키워드를 자연스럽게 포함하고, 제목과 본문의 구조를 개선해보세요.");
        }
        
        if (result.getOverallScore() >= 80) {
            suggestions.put("overall", "훌륭한 품질의 콘텐츠입니다! 현재 수준을 유지하세요.");
        } else if (result.getOverallScore() >= 60) {
            suggestions.put("overall", "좋은 콘텐츠입니다. 몇 가지 개선사항을 적용하면 더욱 효과적일 것입니다.");
        } else {
            suggestions.put("overall", "콘텐츠 품질 개선이 필요합니다. 위의 제안사항들을 참고해주세요.");
        }
        
        return suggestions;
    }
    
    /**
     * AI 응답에서 점수를 파싱합니다.
     */
    private double parseScoreFromResponse(String response) {
        try {
            // 숫자만 추출
            String numericPart = response.replaceAll("[^0-9.]", "");
            if (!numericPart.isEmpty()) {
                double score = Double.parseDouble(numericPart);
                return Math.min(100.0, Math.max(0.0, score));
            }
        } catch (NumberFormatException e) {
            logger.warn("점수 파싱 실패: {}", response);
        }
        return 70.0; // 기본 점수
    }
    
    /**
     * 기본 피드백을 생성합니다.
     */
    private String generateDefaultFeedback(QualityAnalysisResult result) {
        if (result.getOverallScore() >= 80) {
            return "우수한 품질의 콘텐츠입니다. 가독성, 참여도, SEO 모든 면에서 좋은 점수를 받았습니다.";
        } else if (result.getOverallScore() >= 60) {
            return "양호한 품질의 콘텐츠입니다. 일부 영역에서 개선의 여지가 있습니다.";
        } else {
            return "콘텐츠 품질 개선이 필요합니다. 가독성, 참여도, SEO 최적화에 더 신경써주세요.";
        }
    }
}