package com.Human.Ai.D.makit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "conversation_contexts")
public class ConversationContext {
    
    @Id
    private String contextId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false)
    private String sessionId;
    
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    @Column(nullable = false)
    private LocalDateTime lastActivity;
    
    @OneToMany(mappedBy = "conversationContext", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "conversation_context_variables", joinColumns = @JoinColumn(name = "context_id"))
    @MapKeyColumn(name = "variable_key")
    @Column(name = "variable_value")
    private Map<String, String> contextVariables = new HashMap<>();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationStatus status;
    
    @Column
    private String currentTopic;
    
    @Column
    private Integer messageCount = 0;
    
    // Constructors
    public ConversationContext() {}
    
    public ConversationContext(String contextId, User user, String sessionId) {
        this.contextId = contextId;
        this.user = user;
        this.sessionId = sessionId;
        this.startTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.status = ConversationStatus.ACTIVE;
        this.messageCount = 0;
    }
    
    // Business methods
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.messageCount++;
        updateActivity();
    }
    
    public void setContextVariable(String key, String value) {
        this.contextVariables.put(key, value);
    }
    
    public String getContextVariable(String key) {
        return this.contextVariables.get(key);
    }
    
    public boolean isExpired(int timeoutMinutes) {
        return lastActivity.isBefore(LocalDateTime.now().minusMinutes(timeoutMinutes));
    }
    
    // Getters and Setters
    public String getContextId() {
        return contextId;
    }
    
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public Map<String, String> getContextVariables() {
        return contextVariables;
    }
    
    public void setContextVariables(Map<String, String> contextVariables) {
        this.contextVariables = contextVariables;
    }
    
    public ConversationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ConversationStatus status) {
        this.status = status;
    }
    
    public String getCurrentTopic() {
        return currentTopic;
    }
    
    public void setCurrentTopic(String currentTopic) {
        this.currentTopic = currentTopic;
    }
    
    public Integer getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }
    
    public enum ConversationStatus {
        ACTIVE, PAUSED, ENDED, ESCALATED
    }
}