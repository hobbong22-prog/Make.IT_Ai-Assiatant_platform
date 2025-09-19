package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.ChatMessage;
import com.Human.Ai.D.makit.domain.ConversationContext;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.repository.ChatMessageRepository;
import com.Human.Ai.D.makit.repository.ConversationContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ConversationContextManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationContextManager.class);
    private static final int DEFAULT_TIMEOUT_MINUTES = 30;
    private static final int MAX_CONTEXT_HISTORY = 50;
    
    @Autowired
    private ConversationContextRepository conversationContextRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    /**
     * 새로운 대화 컨텍스트를 생성합니다.
     */
    public ConversationContext createContext(User user, String sessionId) {
        String contextId = UUID.randomUUID().toString();
        ConversationContext context = new ConversationContext(contextId, user, sessionId);
        
        context = conversationContextRepository.save(context);
        logger.info("Created new conversation context: {} for user: {}", contextId, user.getId());
        
        return context;
    }
    
    /**
     * 기존 컨텍스트를 가져오거나 새로 생성합니다.
     */
    public ConversationContext getOrCreateContext(User user, String sessionId) {
        Optional<ConversationContext> existingContext = conversationContextRepository.findBySessionId(sessionId);
        
        if (existingContext.isPresent() && 
            existingContext.get().getStatus() == ConversationContext.ConversationStatus.ACTIVE &&
            !existingContext.get().isExpired(DEFAULT_TIMEOUT_MINUTES)) {
            
            ConversationContext context = existingContext.get();
            context.updateActivity();
            return conversationContextRepository.save(context);
        }
        
        return createContext(user, sessionId);
    }
    
    /**
     * 컨텍스트 ID로 컨텍스트를 가져옵니다.
     */
    public Optional<ConversationContext> getContext(String contextId) {
        return conversationContextRepository.findById(contextId);
    }
    
    /**
     * 사용자의 활성 컨텍스트를 가져옵니다.
     */
    public List<ConversationContext> getActiveContextsForUser(User user) {
        return conversationContextRepository.findByUserAndStatusOrderByLastActivityDesc(
                user, ConversationContext.ConversationStatus.ACTIVE);
    }
    
    /**
     * 메시지를 컨텍스트에 추가합니다.
     */
    public ChatMessage addMessage(ConversationContext context, String sender, String content, 
                                ChatMessage.MessageType type, boolean isFromBot) {
        
        ChatMessage message = new ChatMessage(context, sender, content, type, isFromBot);
        message.setMessageId(UUID.randomUUID().toString());
        
        message = chatMessageRepository.save(message);
        
        // 컨텍스트 업데이트
        context.addMessage(message);
        conversationContextRepository.save(context);
        
        // 메시지 히스토리 제한
        limitMessageHistory(context);
        
        logger.debug("Added message to context: {}", context.getContextId());
        return message;
    }
    
    /**
     * 컨텍스트 변수를 설정합니다.
     */
    public void setContextVariable(ConversationContext context, String key, String value) {
        context.setContextVariable(key, value);
        conversationContextRepository.save(context);
        logger.debug("Set context variable {} = {} for context: {}", key, value, context.getContextId());
    }
    
    /**
     * 컨텍스트 변수를 가져옵니다.
     */
    public String getContextVariable(ConversationContext context, String key) {
        return context.getContextVariable(key);
    }
    
    /**
     * 대화 히스토리를 가져옵니다.
     */
    public List<ChatMessage> getConversationHistory(ConversationContext context) {
        return chatMessageRepository.findByConversationContextOrderByTimestampAsc(context);
    }
    
    /**
     * 최근 메시지들을 가져옵니다.
     */
    public List<ChatMessage> getRecentMessages(ConversationContext context, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return chatMessageRepository.findRecentMessages(context, since);
    }
    
    /**
     * 컨텍스트를 종료합니다.
     */
    public void endContext(ConversationContext context) {
        context.setStatus(ConversationContext.ConversationStatus.ENDED);
        context.updateActivity();
        conversationContextRepository.save(context);
        
        logger.info("Ended conversation context: {}", context.getContextId());
    }
    
    /**
     * 컨텍스트를 일시정지합니다.
     */
    public void pauseContext(ConversationContext context) {
        context.setStatus(ConversationContext.ConversationStatus.PAUSED);
        context.updateActivity();
        conversationContextRepository.save(context);
        
        logger.info("Paused conversation context: {}", context.getContextId());
    }
    
    /**
     * 컨텍스트를 재개합니다.
     */
    public void resumeContext(ConversationContext context) {
        context.setStatus(ConversationContext.ConversationStatus.ACTIVE);
        context.updateActivity();
        conversationContextRepository.save(context);
        
        logger.info("Resumed conversation context: {}", context.getContextId());
    }
    
    /**
     * 인간 지원으로 에스컬레이션합니다.
     */
    public void escalateToHuman(ConversationContext context, String reason) {
        context.setStatus(ConversationContext.ConversationStatus.ESCALATED);
        context.setContextVariable("escalation_reason", reason);
        context.setContextVariable("escalation_time", LocalDateTime.now().toString());
        context.updateActivity();
        
        // 에스컬레이션 메시지 추가
        addMessage(context, "system", "대화가 인간 지원으로 에스컬레이션되었습니다: " + reason, 
                  ChatMessage.MessageType.ESCALATION, true);
        
        conversationContextRepository.save(context);
        logger.info("Escalated conversation context: {} - Reason: {}", context.getContextId(), reason);
    }
    
    /**
     * 메시지 히스토리를 제한합니다.
     */
    private void limitMessageHistory(ConversationContext context) {
        List<ChatMessage> messages = chatMessageRepository.findByConversationContextOrderByTimestampDesc(context);
        
        if (messages.size() > MAX_CONTEXT_HISTORY) {
            List<ChatMessage> messagesToDelete = messages.subList(MAX_CONTEXT_HISTORY, messages.size());
            chatMessageRepository.deleteAll(messagesToDelete);
            logger.debug("Trimmed message history for context: {}", context.getContextId());
        }
    }
    
    /**
     * 만료된 컨텍스트들을 정리합니다.
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void cleanupExpiredContexts() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(DEFAULT_TIMEOUT_MINUTES);
        List<ConversationContext> expiredContexts = conversationContextRepository.findExpiredContexts(cutoffTime);
        
        for (ConversationContext context : expiredContexts) {
            if (context.getStatus() == ConversationContext.ConversationStatus.ACTIVE) {
                endContext(context);
            }
        }
        
        if (!expiredContexts.isEmpty()) {
            logger.info("Cleaned up {} expired conversation contexts", expiredContexts.size());
        }
    }
    
    /**
     * 컨텍스트 통계를 가져옵니다.
     */
    public ConversationStats getContextStats(ConversationContext context) {
        Long messageCount = chatMessageRepository.countByConversationContext(context);
        List<ChatMessage> botMessages = chatMessageRepository.findByConversationContextAndIsFromBot(context, true);
        List<ChatMessage> userMessages = chatMessageRepository.findByConversationContextAndIsFromBot(context, false);
        
        return new ConversationStats(
                messageCount,
                (long) botMessages.size(),
                (long) userMessages.size(),
                context.getStartTime(),
                context.getLastActivity()
        );
    }
    
    public static class ConversationStats {
        private final Long totalMessages;
        private final Long botMessages;
        private final Long userMessages;
        private final LocalDateTime startTime;
        private final LocalDateTime lastActivity;
        
        public ConversationStats(Long totalMessages, Long botMessages, Long userMessages, 
                               LocalDateTime startTime, LocalDateTime lastActivity) {
            this.totalMessages = totalMessages;
            this.botMessages = botMessages;
            this.userMessages = userMessages;
            this.startTime = startTime;
            this.lastActivity = lastActivity;
        }
        
        // Getters
        public Long getTotalMessages() { return totalMessages; }
        public Long getBotMessages() { return botMessages; }
        public Long getUserMessages() { return userMessages; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getLastActivity() { return lastActivity; }
    }
}