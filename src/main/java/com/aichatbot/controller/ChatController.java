package com.aichatbot.controller;

import com.aichatbot.model.ChatRequest;
import com.aichatbot.model.ChatResponse;
import com.aichatbot.service.ChatService;
import com.aichatbot.service.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for chat endpoints with Gemini AI
 * Handles HTTP requests from the frontend
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")  // Allow frontend to connect from any origin
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private MemoryService memoryService;
    
    public ChatController() {
        logger.info("✅ ChatController initialized");
    }
    
    /**
     * Main chat endpoint - sends message and gets AI response with memory
     * POST /api/chat/message
     * 
     * @param request ChatRequest containing message and sessionId
     * @return ChatResponse with AI reply
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        try {
            logger.info("📨 Received message: {}", request.getMessage());
            
            // Generate session ID if not provided
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = generateSessionId();
                logger.info("🆔 Generated new session ID: {}", sessionId);
            }
            
            // Validate message
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                logger.warn("Empty message received");
                return ResponseEntity
                    .badRequest()
                    .body(ChatResponse.error("Message cannot be empty"));
            }
            
            // Trim message to avoid excessive tokens
            String userMessage = request.getMessage().trim();
            if (userMessage.length() > 2000) {
                logger.warn("Message too long: {} characters", userMessage.length());
                return ResponseEntity
                    .badRequest()
                    .body(ChatResponse.error("Message is too long (max 2000 characters)"));
            }
            
            // Get AI response using ChatService (which uses GeminiService internally)
            String reply = chatService.chat(sessionId, userMessage);
            
            // Check if response is an error
            if (reply.startsWith("Error:")) {
                logger.error("Service returned error: {}", reply);
                return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ChatResponse.error(reply));
            }
            
            // Return success response
            ChatResponse response = new ChatResponse(reply, sessionId);
            logger.info("✅ Successfully sent response");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error processing message: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ChatResponse.error("Failed to process message: " + e.getMessage()));
        }
    }
    
    /**
     * Legacy endpoint for backward compatibility
     * POST /api/chat
     * 
     * @param request Map with "prompt" key
     * @return Map with "response" key
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            
            if (prompt == null || prompt.trim().isEmpty()) {
                logger.warn("Empty prompt received");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Prompt cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            logger.info("Received legacy chat request with prompt length: {}", prompt.length());
            
            // Use ChatService to get response (without session/memory)
            String response = chatService.getQuickResponse(prompt);
            
            if (response.startsWith("Error:")) {
                logger.error("Service returned error: {}", response);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", response);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
            
            logger.info("Successfully generated response");
            
            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("response", response);
            return ResponseEntity.ok(successResponse);
            
        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Clear conversation history for a session
     * DELETE /api/chat/clear/{sessionId}
     * 
     * @param sessionId Session identifier
     * @return Success message
     */
    @DeleteMapping("/clear/{sessionId}")
    public ResponseEntity<Map<String, String>> clearConversation(@PathVariable String sessionId) {
        try {
            logger.info("🗑️ Clearing conversation for session: {}", sessionId);
            chatService.clearConversation(sessionId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Conversation cleared successfully");
            response.put("sessionId", sessionId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error clearing conversation", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
        }
    }
    
    /**
     * Get conversation history for a session
     * GET /api/chat/history/{sessionId}
     * 
     * @param sessionId Session identifier
     * @return List of messages
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getHistory(@PathVariable String sessionId) {
        try {
            logger.info("📜 Getting history for session: {}", sessionId);
            return ResponseEntity.ok(memoryService.getHistory(sessionId));
        } catch (Exception e) {
            logger.error("Error getting history", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
        }
    }
    
    /**
     * Get conversation history in a readable format
     * GET /api/chat/history/{sessionId}/formatted
     * 
     * @param sessionId Session identifier
     * @return Formatted conversation
     */
    @GetMapping("/history/{sessionId}/formatted")
    public ResponseEntity<Map<String, Object>> getFormattedHistory(@PathVariable String sessionId) {
        try {
            String formatted = memoryService.getFormattedHistory(sessionId);
            int messageCount = memoryService.getHistory(sessionId).size();
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("messageCount", messageCount);
            response.put("conversation", formatted);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting formatted history", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Export conversation as JSON
     * GET /api/chat/export/{sessionId}
     * 
     * @param sessionId Session identifier
     * @return Conversation messages with metadata
     */
    @GetMapping("/export/{sessionId}")
    public ResponseEntity<?> exportConversation(@PathVariable String sessionId) {
        try {
            logger.info("💾 Exporting conversation for session: {}", sessionId);
            
            Map<String, Object> export = new HashMap<>();
            export.put("sessionId", sessionId);
            export.put("exportTime", System.currentTimeMillis());
            export.put("messages", memoryService.getHistory(sessionId));
            export.put("messageCount", memoryService.getHistory(sessionId).size());
            
            return ResponseEntity.ok(export);
            
        } catch (Exception e) {
            logger.error("Error exporting conversation", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get conversation summary for a session
     * GET /api/chat/summary/{sessionId}
     * 
     * @param sessionId Session identifier
     * @return Summary text
     */
    @GetMapping("/summary/{sessionId}")
    public ResponseEntity<Map<String, String>> getSummary(@PathVariable String sessionId) {
        try {
            logger.info("📝 Getting summary for session: {}", sessionId);
            String summary = chatService.getConversationSummary(sessionId);
            
            Map<String, String> response = new HashMap<>();
            response.put("summary", summary);
            response.put("sessionId", sessionId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting summary", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
        }
    }
    
    /**
     * Get statistics about active sessions and messages
     * GET /api/chat/stats
     * 
     * @return Statistics map
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            logger.info("📊 Getting statistics");
            
            Map<String, Object> stats = memoryService.getStatistics();
            stats.put("modelAvailable", chatService.isModelAvailable());
            stats.put("aiProvider", "Google Gemini");
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error getting statistics", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
        }
    }
    
    /**
     * Health check endpoint
     * GET /api/chat/health
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        logger.debug("Health check requested");
        
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Smart Chatbot API");
        health.put("version", "1.0.0");
        health.put("aiProvider", "Google Gemini");
        health.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Test endpoint to check if API is working
     * GET /api/chat/test
     * 
     * @return Test message
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        logger.info("Test endpoint called");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Chatbot API is working!");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        response.put("aiProvider", "Google Gemini");
        
        return ResponseEntity.ok(response);
    }
    
    
    /**
     * Generate a unique session ID
     * @return UUID-based session identifier
     */
    private String generateSessionId() {
        return "session-" + UUID.randomUUID().toString();
    }
    
    /**
     * Exception handler for all controller exceptions
     * @param e Exception thrown
     * @return Error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("❌ Controller exception: {}", e.getMessage(), e);
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("message", e.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
    
}