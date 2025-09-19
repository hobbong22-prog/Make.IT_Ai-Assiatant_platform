package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.dto.ChatMessage;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.service.RAGChatbotService;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ChatbotController {
    
    @Autowired
    private BedrockService bedrockService;
    
    @Autowired
    private RAGChatbotService ragChatbotService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/chat")
    @SendTo("/topic/chat")
    public ChatMessage handleChatMessage(ChatMessage message) {
        try {
            User user = getCurrentUser();
            String sessionId = getSessionId();
            
            // Use RAG chatbot service for enhanced responses
            RAGChatbotService.ChatbotResponse ragResponse = 
                    ragChatbotService.processMessage(message.getContent(), user, sessionId);
            
            ChatMessage response = new ChatMessage();
            response.setSender("MaKIT Assistant");
            response.setContent(ragResponse.getMessage());
            response.setType(ChatMessage.MessageType.CHAT);
            response.setTimestamp(System.currentTimeMillis());
            
            // Handle escalation if needed
            if (ragResponse.shouldEscalate()) {
                // Send escalation notification
                messagingTemplate.convertAndSend("/topic/escalation", 
                    "User " + user.getUsername() + " needs human support: " + ragResponse.getEscalationReason());
            }
            
            return response;
            
        } catch (Exception e) {
            ChatMessage errorResponse = new ChatMessage();
            errorResponse.setSender("MaKIT Assistant");
            errorResponse.setContent("죄송합니다. 일시적인 오류가 발생했습니다. 다시 시도해 주세요.");
            errorResponse.setType(ChatMessage.MessageType.ERROR);
            errorResponse.setTimestamp(System.currentTimeMillis());
            
            return errorResponse;
        }
    }
    
    @PostMapping("/api/chat")
    @ResponseBody
    public ChatbotResponseDto handleRestChatMessage(@RequestBody ChatMessage message, 
                                                   @RequestParam(required = false) String sessionId) {
        try {
            User user = getCurrentUser();
            if (sessionId == null) {
                sessionId = getSessionId();
            }
            
            // Use RAG chatbot service for enhanced responses
            RAGChatbotService.ChatbotResponse ragResponse = 
                    ragChatbotService.processMessage(message.getContent(), user, sessionId);
            
            ChatMessage response = new ChatMessage();
            response.setSender("MaKIT Assistant");
            response.setContent(ragResponse.getMessage());
            response.setType(ChatMessage.MessageType.CHAT);
            response.setTimestamp(System.currentTimeMillis());
            
            return new ChatbotResponseDto(response, ragResponse.getConfidence(), 
                                        ragResponse.shouldEscalate(), ragResponse.getEscalationReason());
            
        } catch (Exception e) {
            ChatMessage errorResponse = new ChatMessage();
            errorResponse.setSender("MaKIT Assistant");
            errorResponse.setContent("죄송합니다. 일시적인 오류가 발생했습니다. 다시 시도해 주세요.");
            errorResponse.setType(ChatMessage.MessageType.ERROR);
            errorResponse.setTimestamp(System.currentTimeMillis());
            
            return new ChatbotResponseDto(errorResponse, 0.0, false, null);
        }
    }
    
    @GetMapping("/api/chat/context/{contextId}")
    @ResponseBody
    public ConversationContextDto getConversationContext(@PathVariable String contextId) {
        // Implementation for getting conversation context
        // This would require additional service methods
        return new ConversationContextDto(contextId, "active", System.currentTimeMillis());
    }
    
    private User getCurrentUser() {
        // For now, return a mock user. In production, this would get the user from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            // Extract user from authentication
            // This is a simplified implementation
            User user = new User();
            user.setId(1L);
            user.setUsername(auth.getName());
            return user;
        }
        
        // Fallback for testing
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        return user;
    }
    
    private String getSessionId() {
        // Generate or retrieve session ID
        return "session-" + System.currentTimeMillis();
    }
    
    private String generateAIResponse(String userMessage) {
        // Fallback method for simple AI responses
        String systemPrompt = 
            "당신은 MaKIT 플랫폼의 AI 마케팅 어시스턴트입니다. " +
            "사용자의 마케팅 관련 질문에 전문적이고 도움이 되는 답변을 제공하세요. " +
            "콘텐츠 생성, 캠페인 최적화, 데이터 분석 등에 대한 조언을 할 수 있습니다. " +
            "답변은 친근하면서도 전문적인 톤으로 작성하세요.";
        
        String fullPrompt = systemPrompt + "\n\n사용자 질문: " + userMessage + "\n\n답변:";
        
        return bedrockService.generateTextWithClaude(fullPrompt, 500);
    }
    
    // Response DTOs
    public static class ChatbotResponseDto {
        private ChatMessage message;
        private double confidence;
        private boolean shouldEscalate;
        private String escalationReason;
        
        public ChatbotResponseDto(ChatMessage message, double confidence, 
                                boolean shouldEscalate, String escalationReason) {
            this.message = message;
            this.confidence = confidence;
            this.shouldEscalate = shouldEscalate;
            this.escalationReason = escalationReason;
        }
        
        // Getters and Setters
        public ChatMessage getMessage() { return message; }
        public void setMessage(ChatMessage message) { this.message = message; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public boolean isShouldEscalate() { return shouldEscalate; }
        public void setShouldEscalate(boolean shouldEscalate) { this.shouldEscalate = shouldEscalate; }
        
        public String getEscalationReason() { return escalationReason; }
        public void setEscalationReason(String escalationReason) { this.escalationReason = escalationReason; }
    }
    
    public static class ConversationContextDto {
        private String contextId;
        private String status;
        private long lastActivity;
        
        public ConversationContextDto(String contextId, String status, long lastActivity) {
            this.contextId = contextId;
            this.status = status;
            this.lastActivity = lastActivity;
        }
        
        // Getters and Setters
        public String getContextId() { return contextId; }
        public void setContextId(String contextId) { this.contextId = contextId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getLastActivity() { return lastActivity; }
        public void setLastActivity(long lastActivity) { this.lastActivity = lastActivity; }
    }
}