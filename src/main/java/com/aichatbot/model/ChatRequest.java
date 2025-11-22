package com.aichatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object sent from frontend to backend
 * Contains the user's message and session ID
 * 
 */


public class ChatRequest {
    
    private String message;     // The user's message
    private String sessionId;   // Session identifier for conversation tracking
    
    /**
     * Default constructor (required for JSON deserialization)
     */
    public ChatRequest() {
    }
    
    /**
     * Constructor with message and sessionId
     * @param message User's message
     * @param sessionId Session identifier
     */
    public ChatRequest(String message, String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
    }
    
    // ==================== Getters and Setters ====================
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public String toString() {
        return "ChatRequest{" +
                "message='" + message + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}