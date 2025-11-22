package com.aichatbot.model;

/**
 * Response object sent from backend to frontend
 * Contains the AI's reply and status information
 */
public class ChatResponse {
    
    private String reply;       // The AI's response
    private String response;    // Alternative field name for backward compatibility
    private String sessionId;   // Session identifier
    private boolean success;    // Whether the request was successful
    private String error;       // Error message if request failed
    
    /**
     * Default constructor
     */
    public ChatResponse() {
        this.success = true;  // Default to success
    }
    
    /**
     * Constructor for successful response (primary)
     * @param reply The AI's response text
     * @param sessionId Session identifier
     */
    public ChatResponse(String reply, String sessionId) {
        this.reply = reply;
        this.response = reply;  // Set both fields for compatibility
        this.sessionId = sessionId;
        this.success = true;
    }
    
    /**
     * Constructor for successful response (alternative)
     * @param reply The AI's response text
     */
    public ChatResponse(String reply) {
        this.reply = reply;
        this.response = reply;
        this.success = true;
    }
    
    /**
     * Static factory method to create error response
     * @param error Error message
     * @return ChatResponse with error
     */
    public static ChatResponse error(String error) {
        ChatResponse response = new ChatResponse();
        response.success = false;
        response.error = error;
        return response;
    }
    
    /**
     * Static factory method to create success response
     * @param reply AI's response
     * @param sessionId Session ID
     * @return ChatResponse with success
     */
    public static ChatResponse success(String reply, String sessionId) {
        return new ChatResponse(reply, sessionId);
    }
    
    // ==================== Getters and Setters ====================
    
    public String getReply() {
        return reply;
    }
    
    public void setReply(String reply) {
        this.reply = reply;
        this.response = reply;  // Keep both in sync
    }
    
    public String getResponse() {
        return response != null ? response : reply;
    }
    
    public void setResponse(String response) {
        this.response = response;
        this.reply = response;  // Keep both in sync
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    @Override
    public String toString() {
        return "ChatResponse{" +
                "reply='" + reply + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", success=" + success +
                ", error='" + error + '\'' +
                '}';
    }
}