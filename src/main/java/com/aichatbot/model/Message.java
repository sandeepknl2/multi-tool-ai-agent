package com.aichatbot.model;

/**
 * Represents a single message in the conversation
 * Can be from "user" or "assistant"
 */
public class Message {
    
    private String role;        // "user" or "assistant"
    private String content;     // The actual message text
    private long timestamp;     // When the message was created
    
    /**
     * Default constructor
     * Automatically sets timestamp to current time
     */
    public Message() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Constructor with role and content
     * @param role "user" or "assistant"
     * @param content The message text
     */
    public Message(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
    
    // ==================== Getters and Setters ====================
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}