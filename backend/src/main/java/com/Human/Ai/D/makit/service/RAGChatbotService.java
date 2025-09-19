package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.ChatMessage;
import com.Human.Ai.D.makit.domain.ConversationContext;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RAGChatbotService {
    
    private static final Logger logger = LoggerFactory.getLogger(RAGChatbotService.class);
    private static final int MAX_CONTEXT_MESSAGES = 10;
    private static final int MAX_RETRIEVED_DOCUMENTS = 3;
    
    @Autowired
    private ConversationContextManager contextManager;
    
    @Autowired
    private KnowledgeRetriever knowledgeRetriever;
    
    @Autowired
    private IntentClassifier intentClassifier;
    
    @Autowired
    private BedrockService bedrockService;
    
    /**
     * 사용자 메시지를 처리하고 응답을 생성합니다.
     */
    public ChatbotResponse processMessage(String message, User user, String sessionId) {
        try {
            // 1. 대화 컨텍스트 가져오기 또는 생성
            ConversationContext context = contextManager.getOrCreateContext(user, sessionId);
            
            // 2. 사용자 메시지 저장
            ChatMessage userMessage = contextManager.addMessage(
                    context, user.getUsername(), message, ChatMessage.MessageType.CHAT, false);
            
            // 3. 의도 분류
            IntentClassifier.IntentClassificationResult intentResult = 
                    intentClassifier.classifyIntent(message);
            
            userMessage.setIntent(intentResult.getIntent().toString());
            userMessage.setConfidence(intentResult.getConfidence());
            
            // 4. 의도에 따른 응답 생성
            ChatbotResponse response = generateResponse(message, context, intentResult);
            
            // 5. 봇 응답 저장
            ChatMessage botMessage = contextManager.addMessage(
                    context, "assistant", response.getMessage(), ChatMessage.MessageType.CHAT, true);
            
            botMessage.setIntent(intentResult.getIntent().toString());
            botMessage.setConfidence(response.getConfidence());
            
            // 6. 컨텍스트 업데이트
            if (response.shouldEscalate()) {
                contextManager.escalateToHuman(context, response.getEscalationReason());
            }
            
            logger.info("Processed message for user: {} with intent: {}", 
                       user.getUsername(), intentResult.getIntent());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing chatbot message", e);
            return new ChatbotResponse(
                    "죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                    0.0,
                    false,
                    "시스템 오류"
            );
        }
    }
    
    /**
     * 의도에 따라 응답을 생성합니다.
     */
    private ChatbotResponse generateResponse(String message, ConversationContext context, 
                                           IntentClassifier.IntentClassificationResult intentResult) {
        
        switch (intentResult.getIntent()) {
            case GREETING:
                return handleGreeting(context);
                
            case PRODUCT_INQUIRY:
                return handleProductInquiry(message, context);
                
            case TECHNICAL_SUPPORT:
                return handleTechnicalSupport(message, context);
                
            case ACCOUNT_MANAGEMENT:
                return handleAccountManagement(message, context);
                
            case BILLING_INQUIRY:
                return handleBillingInquiry(message, context);
                
            case COMPLAINT:
                return handleComplaint(message, context);
                
            case FAREWELL:
                return handleFarewell(context);
                
            case ESCALATION_REQUEST:
                return handleEscalationRequest(context);
                
            default:
                return handleGeneralInquiry(message, context);
        }
    }
    
    /**
     * 인사 처리
     */
    private ChatbotResponse handleGreeting(ConversationContext context) {
        String response = "안녕하세요! MarKIT AI 어시스턴트입니다. " +
                         "제품 정보, 기술 지원, 계정 관리 등 어떤 도움이 필요하신가요?";
        
        return new ChatbotResponse(response, 0.9, false, null);
    }
    
    /**
     * 제품 문의 처리
     */
    private ChatbotResponse handleProductInquiry(String message, ConversationContext context) {
        // RAG를 사용하여 관련 문서 검색
        List<KnowledgeRetriever.RelevantDocument> relevantDocs = 
                knowledgeRetriever.hybridSearch(message, MAX_RETRIEVED_DOCUMENTS);
        
        if (relevantDocs.isEmpty()) {
            return new ChatbotResponse(
                    "죄송합니다. 해당 제품에 대한 정보를 찾을 수 없습니다. " +
                    "더 구체적인 질문을 해주시거나 상담원과 연결을 원하시면 말씀해주세요.",
                    0.5,
                    false,
                    null
            );
        }
        
        return generateRAGResponse(message, context, relevantDocs);
    }
    
    /**
     * 기술 지원 처리
     */
    private ChatbotResponse handleTechnicalSupport(String message, ConversationContext context) {
        // 기술 문서에서 검색
        List<KnowledgeRetriever.RelevantDocument> relevantDocs = 
                knowledgeRetriever.searchByType("TECHNICAL", MAX_RETRIEVED_DOCUMENTS);
        
        if (relevantDocs.isEmpty()) {
            relevantDocs = knowledgeRetriever.hybridSearch(message, MAX_RETRIEVED_DOCUMENTS);
        }
        
        if (relevantDocs.isEmpty()) {
            return new ChatbotResponse(
                    "기술적인 문제에 대한 해결책을 찾지 못했습니다. " +
                    "상담원과 연결해드릴까요?",
                    0.3,
                    true,
                    "기술 지원 필요"
            );
        }
        
        return generateRAGResponse(message, context, relevantDocs);
    }
    
    /**
     * 계정 관리 처리
     */
    private ChatbotResponse handleAccountManagement(String message, ConversationContext context) {
        return new ChatbotResponse(
                "계정 관리와 관련된 문의는 보안상 상담원과 직접 연결해드리겠습니다. " +
                "잠시만 기다려주세요.",
                0.9,
                true,
                "계정 보안"
        );
    }
    
    /**
     * 결제 문의 처리
     */
    private ChatbotResponse handleBillingInquiry(String message, ConversationContext context) {
        return new ChatbotResponse(
                "결제 및 청구와 관련된 문의는 상담원과 직접 연결해드리겠습니다. " +
                "개인정보 보호를 위해 상담원이 도와드리겠습니다.",
                0.9,
                true,
                "결제 문의"
        );
    }
    
    /**
     * 불만 처리
     */
    private ChatbotResponse handleComplaint(String message, ConversationContext context) {
        contextManager.setContextVariable(context, "complaint_message", message);
        
        return new ChatbotResponse(
                "고객님의 소중한 의견 감사합니다. " +
                "더 나은 서비스를 위해 상담원과 연결해드리겠습니다.",
                0.8,
                true,
                "고객 불만"
        );
    }
    
    /**
     * 작별 인사 처리
     */
    private ChatbotResponse handleFarewell(ConversationContext context) {
        contextManager.endContext(context);
        
        return new ChatbotResponse(
                "도움이 되었기를 바랍니다. 언제든지 다시 문의해주세요. 좋은 하루 되세요!",
                0.9,
                false,
                null
        );
    }
    
    /**
     * 상담원 연결 요청 처리
     */
    private ChatbotResponse handleEscalationRequest(ConversationContext context) {
        return new ChatbotResponse(
                "상담원과 연결해드리겠습니다. 잠시만 기다려주세요.",
                0.9,
                true,
                "고객 요청"
        );
    }
    
    /**
     * 일반 문의 처리
     */
    private ChatbotResponse handleGeneralInquiry(String message, ConversationContext context) {
        // RAG를 사용하여 관련 문서 검색
        List<KnowledgeRetriever.RelevantDocument> relevantDocs = 
                knowledgeRetriever.hybridSearch(message, MAX_RETRIEVED_DOCUMENTS);
        
        if (relevantDocs.isEmpty()) {
            return new ChatbotResponse(
                    "죄송합니다. 정확한 답변을 드리기 어렵습니다. " +
                    "다른 방식으로 질문해주시거나 상담원과 연결을 원하시면 말씀해주세요.",
                    0.3,
                    false,
                    null
            );
        }
        
        return generateRAGResponse(message, context, relevantDocs);
    }
    
    /**
     * RAG를 사용하여 응답을 생성합니다.
     */
    private ChatbotResponse generateRAGResponse(String message, ConversationContext context,
                                              List<KnowledgeRetriever.RelevantDocument> relevantDocs) {
        try {
            // 컨텍스트 정보 수집
            String conversationHistory = getConversationHistory(context);
            String knowledgeContext = buildKnowledgeContext(relevantDocs);
            
            // RAG 프롬프트 생성
            String prompt = buildRAGPrompt(message, conversationHistory, knowledgeContext);
            
            // AI 응답 생성
            String aiResponse = bedrockService.generateTextWithClaude(prompt, 500);
            
            // 신뢰도 계산 (관련 문서의 평균 유사도)
            double confidence = relevantDocs.stream()
                    .mapToDouble(KnowledgeRetriever.RelevantDocument::getSimilarity)
                    .average()
                    .orElse(0.5);
            
            return new ChatbotResponse(aiResponse, confidence, false, null);
            
        } catch (Exception e) {
            logger.error("Error generating RAG response", e);
            return new ChatbotResponse(
                    "죄송합니다. 응답 생성 중 오류가 발생했습니다.",
                    0.0,
                    false,
                    null
            );
        }
    }
    
    /**
     * 대화 히스토리를 문자열로 변환합니다.
     */
    private String getConversationHistory(ConversationContext context) {
        List<ChatMessage> recentMessages = contextManager.getRecentMessages(context, 30);
        
        return recentMessages.stream()
                .limit(MAX_CONTEXT_MESSAGES)
                .map(msg -> String.format("%s: %s", 
                           msg.getIsFromBot() ? "Assistant" : "User", 
                           msg.getContent()))
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * 지식 베이스 컨텍스트를 구축합니다.
     */
    private String buildKnowledgeContext(List<KnowledgeRetriever.RelevantDocument> relevantDocs) {
        return relevantDocs.stream()
                .map(doc -> String.format("문서: %s\n내용: %s", 
                                        doc.getDocument().getTitle(),
                                        doc.getSnippet(300)))
                .collect(Collectors.joining("\n\n"));
    }
    
    /**
     * RAG 프롬프트를 생성합니다.
     */
    private String buildRAGPrompt(String userMessage, String conversationHistory, String knowledgeContext) {
        return String.format(
                "당신은 MarKIT의 고객 지원 AI 어시스턴트입니다. " +
                "다음 정보를 바탕으로 고객의 질문에 정확하고 도움이 되는 답변을 제공해주세요.\n\n" +
                
                "대화 히스토리:\n%s\n\n" +
                
                "관련 지식 베이스 정보:\n%s\n\n" +
                
                "고객 질문: %s\n\n" +
                
                "답변 가이드라인:\n" +
                "- 제공된 지식 베이스 정보를 우선적으로 활용하세요\n" +
                "- 정확하지 않은 정보는 제공하지 마세요\n" +
                "- 친근하고 전문적인 톤을 유지하세요\n" +
                "- 필요시 추가 질문을 유도하세요\n" +
                "- 답변할 수 없는 경우 상담원 연결을 제안하세요\n\n" +
                
                "답변:",
                
                conversationHistory.isEmpty() ? "없음" : conversationHistory,
                knowledgeContext.isEmpty() ? "관련 정보 없음" : knowledgeContext,
                userMessage
        );
    }
    
    /**
     * 챗봇 응답 클래스
     */
    public static class ChatbotResponse {
        private final String message;
        private final double confidence;
        private final boolean shouldEscalate;
        private final String escalationReason;
        
        public ChatbotResponse(String message, double confidence, boolean shouldEscalate, String escalationReason) {
            this.message = message;
            this.confidence = confidence;
            this.shouldEscalate = shouldEscalate;
            this.escalationReason = escalationReason;
        }
        
        public String getMessage() { return message; }
        public double getConfidence() { return confidence; }
        public boolean shouldEscalate() { return shouldEscalate; }
        public String getEscalationReason() { return escalationReason; }
    }
}