package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.ChatMessage;
import com.Human.Ai.D.makit.domain.ConversationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findByConversationContext(ConversationContext conversationContext);
    
    List<ChatMessage> findByConversationContextOrderByTimestampAsc(ConversationContext conversationContext);
    
    List<ChatMessage> findByConversationContextAndType(ConversationContext conversationContext, ChatMessage.MessageType type);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationContext = :context AND cm.timestamp >= :since ORDER BY cm.timestamp ASC")
    List<ChatMessage> findRecentMessages(@Param("context") ConversationContext context, 
                                       @Param("since") LocalDateTime since);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationContext = :context ORDER BY cm.timestamp DESC")
    List<ChatMessage> findByConversationContextOrderByTimestampDesc(@Param("context") ConversationContext context);
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.conversationContext = :context")
    Long countByConversationContext(@Param("context") ConversationContext context);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationContext = :context AND cm.isFromBot = :isFromBot")
    List<ChatMessage> findByConversationContextAndIsFromBot(@Param("context") ConversationContext context, 
                                                           @Param("isFromBot") Boolean isFromBot);
}