package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.KnowledgeDocument;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.service.ai.BedrockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@Transactional
class RAGChatbotServiceIntegrationTest {
    
    @Autowired
    private RAGChatbotService ragChatbotService;
    
    @Autowired
    private KnowledgeBaseManager knowledgeBaseManager;
    
    @MockBean
    private BedrockService bedrockService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        // Mock Bedrock service responses
        when(bedrockService.generateEmbedding(anyString())).thenReturn("[0.1, 0.2, 0.3, 0.4, 0.5]");
        when(bedrockService.generateTextWithClaude(anyString(), anyInt()))
                .thenReturn("안녕하세요! MarKIT에 대해 도움을 드릴 수 있습니다.");
    }
    
    @Test
    void testProcessGreetingMessage() {
        // Given
        String message = "안녕하세요";
        String sessionId = "test-session-1";
        
        // When
        RAGChatbotService.ChatbotResponse response = 
                ragChatbotService.processMessage(message, testUser, sessionId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertTrue(response.getConfidence() > 0.5);
        assertFalse(response.shouldEscalate());
        assertTrue(response.getMessage().contains("MarKIT"));
    }
    
    @Test
    void testProcessProductInquiryWithKnowledgeBase() {
        // Given
        // Add some knowledge documents first
        CompletableFuture<KnowledgeDocument> doc1 = knowledgeBaseManager.addDocument(
                "MarKIT 기능 소개", 
                "MarKIT은 AI 기반 마케팅 자동화 플랫폼입니다. 콘텐츠 생성, 캠페인 관리, 분석 기능을 제공합니다.",
                "PRODUCT",
                "internal",
                Arrays.asList("기능", "제품", "소개")
        );
        
        doc1.join(); // Wait for completion
        
        String message = "MarKIT의 주요 기능이 무엇인가요?";
        String sessionId = "test-session-2";
        
        // When
        RAGChatbotService.ChatbotResponse response = 
                ragChatbotService.processMessage(message, testUser, sessionId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertTrue(response.getConfidence() > 0.0);
        assertFalse(response.shouldEscalate());
    }
    
    @Test
    void testProcessTechnicalSupportRequest() {
        // Given
        String message = "로그인이 안 되는 문제가 있어요";
        String sessionId = "test-session-3";
        
        // When
        RAGChatbotService.ChatbotResponse response = 
                ragChatbotService.processMessage(message, testUser, sessionId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getMessage());
        // Technical support might escalate if no relevant docs found
        assertTrue(response.getConfidence() >= 0.0);
    }
    
    @Test
    void testProcessAccountManagementRequest() {
        // Given
        String message = "계정 비밀번호를 변경하고 싶어요";
        String sessionId = "test-session-4";
        
        // When
        RAGChatbotService.ChatbotResponse response = 
                ragChatbotService.processMessage(message, testUser, sessionId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertTrue(response.shouldEscalate()); // Account management should escalate
        assertEquals("계정 보안", response.getEscalationReason());
    }
    
    @Test
    void testProcessEscalationRequest() {
        // Given
        String message = "상담원과 연결해주세요";
        String sessionId = "test-session-5";
        
        // When
        RAGChatbotService.ChatbotResponse response = 
                ragChatbotService.processMessage(message, testUser, sessionId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertTrue(response.shouldEscalate());
        assertEquals("고객 요청", response.getEscalationReason());
    }
    
    @Test
    void testProcessFarewellMessage() {
        // Given
        String message = "감사합니다. 안녕히 계세요";
        String sessionId = "test-session-6";
        
        // When
        RAGChatbotService.ChatbotResponse response = 
                ragChatbotService.processMessage(message, testUser, sessionId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertTrue(response.getConfidence() > 0.8);
        assertFalse(response.shouldEscalate());
        assertTrue(response.getMessage().contains("좋은 하루"));
    }
    
    @Test
    void testConversationContextPersistence() {
        // Given
        String sessionId = "test-session-7";
        
        // When - Send multiple messages in the same session
        RAGChatbotService.ChatbotResponse response1 = 
                ragChatbotService.processMessage("안녕하세요", testUser, sessionId);
        
        RAGChatbotService.ChatbotResponse response2 = 
                ragChatbotService.processMessage("MarKIT에 대해 알려주세요", testUser, sessionId);
        
        // Then
        assertNotNull(response1);
        assertNotNull(response2);
        // Both responses should be generated successfully
        assertTrue(response1.getConfidence() > 0.0);
        assertTrue(response2.getConfidence() > 0.0);
    }
    
    @Test
    void testComplaintHandling() {
        // Given
        String message = "서비스가 너무 느려서 불만입니다";
        String sessionId = "test-session-8";
        
        // When
        RAGChatbotService.ChatbotResponse response = 
                ragChatbotService.processMessage(message, testUser, sessionId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertTrue(response.shouldEscalate()); // Complaints should escalate
        assertEquals("고객 불만", response.getEscalationReason());
        assertTrue(response.getMessage().contains("상담원"));
    }
}