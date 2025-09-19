package com.Human.Ai.D.makit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context_id")
    private ConversationContext conversationContext;
    
    @Column(nullable = false)
    private String sender;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column
    private String messageId;
    
    @Column
    private Boolean isFromBot = false;
    
    @Column
    private String intent;
    
    @Column
    private Double confidence;
    
    // Constructors
    public ChatMessage() {}
    
    public ChatMessage(ConversationContext conversationContext, String sender, String content, MessageType type) {
        this.conversationContext = conversationContext;
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.isFromBot = false;
    }
    
    public ChatMessage(ConversationContext conversationContext, String sender, String content, 
                      MessageType type, boolean isFromBot) {
        this(conversationContext, sender, content, type);
        this.isFromBot = isFromBot;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ConversationContext getConversationContext() {
        return conversationContext;
    }
    
    public void setConversationContext(ConversationContext conversationContext) {
        this.conversationContext = conversationContext;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public Boolean getIsFromBot() {
        return isFromBot;
    }
    
    public void setIsFromBot(Boolean isFromBot) {
        this.isFromBot = isFromBot;
    }
    
    public String getIntent() {
        return intent;
    }
    
    public void setIntent(String intent) {
        this.intent = intent;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public enum MessageType {
        CHAT, JOIN, LEAVE, ERROR, SYSTEM, ESCALATION
    }
}