package com.Human.Ai.D.makit.dto;

public class ChatMessage {
    
    private String sender;
    private String content;
    private MessageType type;
    private long timestamp;
    
    public enum MessageType {
        CHAT, JOIN, LEAVE, ERROR
    }
    
    // Constructors
    public ChatMessage() {}
    
    public ChatMessage(String sender, String content, MessageType type) {
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}