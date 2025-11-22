package com.aichatbot.service;

import com.aichatbot.model.Message;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage conversation memory for different sessions
 * Stores message history and handles conversation context
 */
@Service
public class MemoryService {
    
    // Store conversations for each session (thread-safe)
    private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();
    
    // Maximum messages to keep in memory (to avoid token limits)
    private static final int MAX_HISTORY_SIZE = 20;
    
    // Maximum age of messages in milliseconds (24 hours)
    private static final long MAX_MESSAGE_AGE = 24 * 60 * 60 * 1000;
    
    /**
     * Add a message to conversation history
     * @param sessionId Session identifier
     * @param role "user" or "assistant"
     * @param content Message content
     */
    public void addMessage(String sessionId, String role, String content) {
        // Get or create conversation list for this session
        conversationHistory
            .computeIfAbsent(sessionId, k -> new ArrayList<>())
            .add(new Message(role, content));
        
        // Keep only recent messages to avoid token limits
        trimHistory(sessionId);
        
        System.out.println("✅ Added message to session: " + sessionId + " (Role: " + role + ")");
    }
    
    /**
     * Get all messages for a specific session
     * @param sessionId Session identifier
     * @return List of messages
     */
    public List<Message> getHistory(String sessionId) {
        return conversationHistory.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * Get conversation history as formatted string for AI context
     * @param sessionId Session identifier
     * @return Formatted conversation string
     */
    public String getFormattedHistory(String sessionId) {
        List<Message> messages = getHistory(sessionId);
        StringBuilder formatted = new StringBuilder();
        
        for (Message msg : messages) {
            formatted.append(msg.getRole().toUpperCase())
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n\n");
        }
        
        return formatted.toString();
    }
    
    /**
     * Get only recent messages (last N messages)
     * @param sessionId Session identifier
     * @param count Number of recent messages to retrieve
     * @return List of recent messages
     */
    public List<Message> getRecentMessages(String sessionId, int count) {
        List<Message> allMessages = getHistory(sessionId);
        if (allMessages.isEmpty()) {
            return new ArrayList<>();
        }
        
        int startIndex = Math.max(0, allMessages.size() - count);
        return new ArrayList<>(allMessages.subList(startIndex, allMessages.size()));
    }
    
    /**
     * Clear conversation history for a specific session
     * @param sessionId Session identifier
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        System.out.println("🗑️ Cleared history for session: " + sessionId);
    }
    
    /**
     * Clear all old conversations (cleanup task)
     */
    public void clearOldConversations() {
        long now = System.currentTimeMillis();
        int removedCount = 0;
        
        Iterator<Map.Entry<String, List<Message>>> iterator = 
            conversationHistory.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, List<Message>> entry = iterator.next();
            List<Message> messages = entry.getValue();
            
            if (!messages.isEmpty()) {
                Message lastMessage = messages.get(messages.size() - 1);
                if (now - lastMessage.getTimestamp() > MAX_MESSAGE_AGE) {
                    iterator.remove();
                    removedCount++;
                }
            }
        }
        
        if (removedCount > 0) {
            System.out.println("🧹 Cleaned up " + removedCount + " old conversations");
        }
    }
    
    /**
     * Keep only the most recent messages to avoid token limits
     * @param sessionId Session identifier
     */
    private void trimHistory(String sessionId) {
        List<Message> history = conversationHistory.get(sessionId);
        
        if (history != null && history.size() > MAX_HISTORY_SIZE) {
            // Keep only the last MAX_HISTORY_SIZE messages
            List<Message> trimmed = new ArrayList<>(
                history.subList(history.size() - MAX_HISTORY_SIZE, history.size())
            );
            conversationHistory.put(sessionId, trimmed);
            
            System.out.println("✂️ Trimmed history for session: " + sessionId);
        }
    }
    
    /**
     * Get number of active sessions
     * @return Count of active sessions
     */
    public int getActiveSessionCount() {
        return conversationHistory.size();
    }
    
    /**
     * Get total number of messages across all sessions
     * @return Total message count
     */
    public int getTotalMessageCount() {
        return conversationHistory.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    /**
     * Check if a session exists
     * @param sessionId Session identifier
     * @return true if session has messages
     */
    public boolean sessionExists(String sessionId) {
        return conversationHistory.containsKey(sessionId) && 
               !conversationHistory.get(sessionId).isEmpty();
    }
    
    /**
     * Get statistics about memory usage
     * @return Map with statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSessions", getActiveSessionCount());
        stats.put("totalMessages", getTotalMessageCount());
        stats.put("maxHistorySize", MAX_HISTORY_SIZE);
        return stats;
    }
}