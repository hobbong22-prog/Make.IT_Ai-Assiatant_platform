package com.Human.Ai.D.makit.repository;

import com.Human.Ai.D.makit.domain.ConversationContext;
import com.Human.Ai.D.makit.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationContextRepository extends JpaRepository<ConversationContext, String> {
    
    List<ConversationContext> findByUser(User user);
    
    List<ConversationContext> findByUserAndStatus(User user, ConversationContext.ConversationStatus status);
    
    Optional<ConversationContext> findBySessionId(String sessionId);
    
    List<ConversationContext> findByStatus(ConversationContext.ConversationStatus status);
    
    @Query("SELECT cc FROM ConversationContext cc WHERE cc.lastActivity < :cutoffTime")
    List<ConversationContext> findExpiredContexts(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT cc FROM ConversationContext cc WHERE cc.user = :user AND cc.status = :status ORDER BY cc.lastActivity DESC")
    List<ConversationContext> findByUserAndStatusOrderByLastActivityDesc(@Param("user") User user, 
                                                                        @Param("status") ConversationContext.ConversationStatus status);
    
    @Query("SELECT COUNT(cc) FROM ConversationContext cc WHERE cc.user = :user")
    Long countByUser(@Param("user") User user);
    
    @Query("SELECT cc FROM ConversationContext cc WHERE cc.user = :user ORDER BY cc.startTime DESC")
    List<ConversationContext> findByUserOrderByStartTimeDesc(@Param("user") User user);
}