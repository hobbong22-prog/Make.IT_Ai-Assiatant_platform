package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.ChatMessage;
import com.Human.Ai.D.makit.domain.ConversationContext;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.repository.ChatMessageRepository;
import com.Human.Ai.D.makit.repository.ConversationContextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationContextManagerTest {
    
    @Mock
    private ConversationContextRepository conversationContextRepository;
    
    @Mock
    private ChatMessageRepository chatMessageRepository;
    
    @InjectMocks
    private ConversationContextManager conversationContextManager;
    
    private User testUser;
    private ConversationContext testContext;
    private ChatMessage testMessage;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        testContext = new ConversationContext("test-context-id", testUser, "test-session-id");
        
        testMessage = new ChatMessage(testContext, "testuser", "Hello", ChatMessage.MessageType.CHAT, false);
    }
    
    @Test
    void testCreateContext() {
        // Given
        when(conversationContextRepository.save(any(ConversationContext.class))).thenReturn(testContext);
        
        // When
        ConversationContext result = conversationContextManager.createContext(testUser, "test-session-id");
        
        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals("test-session-id", result.getSessionId());
        assertEquals(ConversationContext.ConversationStatus.ACTIVE, result.getStatus());
        
        verify(conversationContextRepository, times(1)).save(any(ConversationContext.class));
    }
    
    @Test
    void testGetOrCreateContext_ExistingActiveContext() {
        // Given
        testContext.setStatus(ConversationContext.ConversationStatus.ACTIVE);
        testContext.setLastActivity(LocalDateTime.now().minusMinutes(10)); // Not expired
        
        when(conversationContextRepository.findBySessionId("test-session-id"))
                .thenReturn(Optional.of(testContext));
        when(conversationContextRepository.save(any(ConversationContext.class))).thenReturn(testContext);
        
        // When
        ConversationContext result = conversationContextManager.getOrCreateContext(testUser, "test-session-id");
        
        // Then
        assertEquals(testContext, result);
        verify(conversationContextRepository, times(1)).findBySessionId("test-session-id");
        verify(conversationContextRepository, times(1)).save(testContext);
    }
    
    @Test
    void testGetOrCreateContext_ExpiredContext() {
        // Given
        testContext.setStatus(ConversationContext.ConversationStatus.ACTIVE);
        testContext.setLastActivity(LocalDateTime.now().minusMinutes(60)); // Expired
        
        when(conversationContextRepository.findBySessionId("test-session-id"))
                .thenReturn(Optional.of(testContext));
        when(conversationContextRepository.save(any(ConversationContext.class))).thenReturn(testContext);
        
        // When
        ConversationContext result = conversationContextManager.getOrCreateContext(testUser, "test-session-id");
        
        // Then
        assertNotNull(result);
        verify(conversationContextRepository, times(1)).findBySessionId("test-session-id");
        verify(conversationContextRepository, times(1)).save(any(ConversationContext.class));
    }
    
    @Test
    void testGetContext() {
        // Given
        when(conversationContextRepository.findById("test-context-id")).thenReturn(Optional.of(testContext));
        
        // When
        Optional<ConversationContext> result = conversationContextManager.getContext("test-context-id");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testContext, result.get());
        verify(conversationContextRepository, times(1)).findById("test-context-id");
    }
    
    @Test
    void testAddMessage() {
        // Given
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);
        when(conversationContextRepository.save(any(ConversationContext.class))).thenReturn(testContext);
        
        // When
        ChatMessage result = conversationContextManager.addMessage(
                testContext, "testuser", "Hello", ChatMessage.MessageType.CHAT, false);
        
        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getSender());
        assertEquals("Hello", result.getContent());
        assertEquals(ChatMessage.MessageType.CHAT, result.getType());
        assertFalse(result.getIsFromBot());
        
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(conversationContextRepository, times(1)).save(testContext);
    }
    
    @Test
    void testSetContextVariable() {
        // Given
        when(conversationContextRepository.save(any(ConversationContext.class))).thenReturn(testContext);
        
        // When
        conversationContextManager.setContextVariable(testContext, "key1", "value1");
        
        // Then
        assertEquals("value1", testContext.getContextVariable("key1"));
        verify(conversationContextRepository, times(1)).save(testContext);
    }
    
    @Test
    void testGetContextVariable() {
        // Given
        testContext.setContextVariable("key1", "value1");
        
        // When
        String result = conversationContextManager.getContextVariable(testContext, "key1");
        
        // Then
        assertEquals("value1", result);
    }
    
    @Test
    void testGetConversationHistory() {
        // Given
        List<ChatMessage> expectedMessages = Arrays.asList(testMessage);
        when(chatMessageRepository.findByConversationContextOrderByTimestampAsc(testContext))
                .thenReturn(expectedMessages);
        
        // When
        List<ChatMessage> result = conversationContextManager.getConversationHistory(testContext);
        
        // Then
        assertEquals(expectedMessages, result);
        verify(chatMessageRepository, times(1)).findByConversationContextOrderByTimestampAsc(testContext);
    }
    
    @Test
    void testEndContext() {
        // Given
        when(conversationContextRepository.save(any(ConversationContext.class))).thenReturn(testContext);
        
        // When
        conversationContextManager.endContext(testContext);
        
        // Then
        assertEquals(ConversationContext.ConversationStatus.ENDED, testContext.getStatus());
        verify(conversationContextRepository, times(1)).save(testContext);
    }
    
    @Test
    void testPauseContext() {
        // Given
        when(conversationContextRepository.save(any(ConversationContext.class))).thenReturn(testContext);
        
        // When
        conversationContextManager.pauseContext(testContext);
        
        // Then
        assertEquals(ConversationContext.ConversationStatus.PAUSED, testContext.getStatus());
        verify(conversationContextRepository, times(1)).save(testContext);
    }
    
    @Test
    void testResumeContext() {
        // Given
        testContext.setStatus(ConversationContext.ConversationStatus.PAUSED);
        when(conversationContextRepository.save(any(ConversationContext.class))).thenReturn(testContext);
        
        // When
        conversationContextManager.resumeContext(testContext);
        
        // Then
        assertEquals(ConversationContext.ConversationStatus.ACTIVE, testContext.getStatus());
        verify(conversationContextRepository, times(1)).save(testContext);
    }
    
    @Test
    void testEscalateToHuman() {
        // Given
        when(conversationContextRepository.save(any(ConversationContext.class))).thenReturn(testContext);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);
        
        // When
        conversationContextManager.escalateToHuman(testContext, "Complex query");
        
        // Then
        assertEquals(ConversationContext.ConversationStatus.ESCALATED, testContext.getStatus());
        assertEquals("Complex query", testContext.getContextVariable("escalation_reason"));
        assertNotNull(testContext.getContextVariable("escalation_time"));
        
        verify(conversationContextRepository, times(1)).save(testContext);
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }
    
    @Test
    void testGetContextStats() {
        // Given
        when(chatMessageRepository.countByConversationContext(testContext)).thenReturn(5L);
        when(chatMessageRepository.findByConversationContextAndIsFromBot(testContext, true))
                .thenReturn(Arrays.asList(testMessage, testMessage));
        when(chatMessageRepository.findByConversationContextAndIsFromBot(testContext, false))
                .thenReturn(Arrays.asList(testMessage, testMessage, testMessage));
        
        // When
        ConversationContextManager.ConversationStats stats = 
                conversationContextManager.getContextStats(testContext);
        
        // Then
        assertEquals(5L, stats.getTotalMessages());
        assertEquals(2L, stats.getBotMessages());
        assertEquals(3L, stats.getUserMessages());
        assertNotNull(stats.getStartTime());
        assertNotNull(stats.getLastActivity());
    }
}