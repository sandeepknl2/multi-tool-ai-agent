package com.aichatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to handle AI chat interactions using Gemini with Tool support
 * Integrates with GeminiService and manages conversation context
 */
@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final GeminiService geminiService;
    private final MemoryService memoryService;
    private final ToolManager toolManager;
    
    private static final int MAX_TOOL_ITERATIONS = 5;  // Prevent infinite loops
    
    /**
     * Constructor with dependency injection
     */
    @Autowired
    public ChatService(GeminiService geminiService, 
                      MemoryService memoryService,
                      ToolManager toolManager) {
        this.geminiService = geminiService;
        this.memoryService = memoryService;
        this.toolManager = toolManager;
        logger.info("✅ ChatService initialized with Gemini and Tools");
    }
    
    /**
     * Main chat method with tool support
     */
    public String chat(String sessionId, String userMessage) {
        try {
            logger.info("💬 Processing message for session: {}", sessionId);
            
            // Add user message to memory
            memoryService.addMessage(sessionId, "user", userMessage);
            
            // Check if multiple tools are needed
            var matchingTools = toolManager.getAllTools().values().stream()
                .filter(tool -> tool.shouldUse(userMessage))
                .toList();
            
            if (!matchingTools.isEmpty()) {
                logger.info("🎯 Found {} matching tool(s)", matchingTools.size());
                return handleMultipleTools(sessionId, matchingTools, userMessage);
            }
            
            // Let AI decide if it needs tools
            String aiResponse = chatWithTools(sessionId, userMessage, 0);
            
            logger.info("✅ Received final response");
            
            // Add AI response to memory
            memoryService.addMessage(sessionId, "assistant", aiResponse);
            
            return aiResponse;
            
        } catch (Exception e) {
            logger.error("❌ Error in chat: {}", e.getMessage(), e);
            return "I apologize, but I encountered an error processing your message. " +
                   "Please try again or rephrase your question.";
        }
    }
    
    /**
     * Handle multiple tools in one message
     */
    private String handleMultipleTools(String sessionId, 
                                      java.util.List<com.aichatbot.tools.Tool> tools, 
                                      String userMessage) {
        try {
            StringBuilder results = new StringBuilder();
            
            for (com.aichatbot.tools.Tool tool : tools) {
                logger.info("🔧 Executing tool: {}", tool.getName());
                
                // Extract parameters for this tool
                String extractPrompt = String.format(
                    "Extract parameters for the %s tool from this message: '%s'\n" +
                    "Tool parameters schema: %s\n" +
                    "Return ONLY a JSON object with the parameters, nothing else.",
                    tool.getName(), userMessage, tool.getParametersSchema()
                );
                
                String params = geminiService.getGeminiResponse(extractPrompt);
                params = params.replaceAll("```json|```", "").trim();
                
                // Execute tool
                String result = toolManager.executeTool(tool.getName(), params);
                results.append(tool.getName()).append(": ").append(result).append("\n");
            }
            
            // Generate natural language response
            String responsePrompt = String.format(
                "The user asked: '%s'\n\n" +
                "I used these tools and got these results:\n%s\n" +
                "Please provide a friendly, natural response that incorporates all the results.",
                userMessage, results.toString()
            );
            
            return geminiService.getGeminiResponse(responsePrompt);
            
        } catch (Exception e) {
            logger.error("Error handling multiple tools: {}", e.getMessage());
            return "I encountered an error trying to help with that. Please try again.";
        }
    }
    
    /**
     * Chat with tool calling support (recursive with iteration limit)
     */
    private String chatWithTools(String sessionId, String userMessage, int iteration) {
        if (iteration >= MAX_TOOL_ITERATIONS) {
            logger.warn("⚠️ Max tool iterations reached");
            return "I apologize, but I'm having trouble completing this task. Please try rephrasing your question.";
        }
        
        // Build context with conversation history and tool descriptions
        String context = buildContextWithTools(sessionId, userMessage);
        
        logger.info("🤖 Sending request to Gemini (iteration {})...", iteration + 1);
        
        // Get AI response
        String aiResponse = geminiService.getGeminiResponse(context);
        
        // Check if AI wants to use a tool
        if (toolManager.isToolCall(aiResponse)) {
            logger.info("🔧 AI requested tool call");
            
            ToolManager.ToolCall toolCall = toolManager.parseToolCall(aiResponse);
            if (toolCall == null) {
                return "I had trouble processing that request. Please try again.";
            }
            
            // Execute the tool
            String toolResult = toolManager.executeTool(
                toolCall.getToolName(), 
                toolCall.getParameters()
            );
            
            logger.info("✅ Tool result: {}", toolResult);
            
            // Add tool result to context and continue conversation
            String followUp = "The " + toolCall.getToolName() + " tool returned: " + toolResult + 
                            "\n\nPlease provide a natural language response to the user based on this result.";
            
            return chatWithTools(sessionId, followUp, iteration + 1);
        }
        
        // No tool call needed, return AI response
        return aiResponse;
    }
    
    /**
     * Handle direct tool execution when we detect tool need from user message
     */
    private String handleDirectToolCall(String sessionId, com.aichatbot.tools.Tool tool, String userMessage) {
        try {
            // Ask AI to extract parameters from user message
            String extractPrompt = String.format(
                "Extract parameters for the %s tool from this message: '%s'\n" +
                "Tool parameters schema: %s\n" +
                "Return ONLY a JSON object with the parameters, nothing else.",
                tool.getName(), userMessage, tool.getParametersSchema()
            );
            
            String params = geminiService.getGeminiResponse(extractPrompt);
            
            // Clean up response to get just JSON
            params = params.replaceAll("```json|```", "").trim();
            
            // Execute tool
            String result = toolManager.executeTool(tool.getName(), params);
            
            // Get natural language response
            String responsePrompt = String.format(
                "The user asked: '%s'\n" +
                "I used the %s tool and got result: %s\n" +
                "Please provide a friendly, natural response to the user.",
                userMessage, tool.getName(), result
            );
            
            return geminiService.getGeminiResponse(responsePrompt);
            
        } catch (Exception e) {
            logger.error("Error in direct tool call: {}", e.getMessage());
            return "I encountered an error trying to help with that. Please try rephrasing your question.";
        }
    }
    
    /**
     * Build context with tools available
     */
    private String buildContextWithTools(String sessionId, String currentMessage) {
        StringBuilder context = new StringBuilder();
        
        // System prompt with tool instructions
        context.append("You are a helpful AI assistant with access to tools. ");
        context.append("When users ask questions that require tools, use them.\n\n");
        
        // Add tool descriptions
        context.append(toolManager.getToolsDescription());
        context.append("\n\n");
        
        // Add conversation history
        String history = memoryService.getFormattedHistory(sessionId);
        if (!history.isEmpty()) {
            context.append("=== Previous Conversation ===\n");
            context.append(history);
            context.append("=== End ===\n\n");
        }
        
        // Add current message
        context.append("USER: ").append(currentMessage).append("\n\n");
        context.append("ASSISTANT:");
        
        return context.toString();
    }
    
    /**
     * Original buildContext for non-tool conversations
     */
    private String buildContext(String sessionId, String currentMessage) {
        StringBuilder context = new StringBuilder();
        
        context.append("You are a helpful, friendly, and knowledgeable AI assistant. ");
        context.append("Your name is SmartBot. ");
        context.append("Be concise but informative.\n\n");
        
        String history = memoryService.getFormattedHistory(sessionId);
        if (!history.isEmpty()) {
            context.append("=== Previous Conversation ===\n");
            context.append(history);
            context.append("=== End ===\n\n");
        }
        
        context.append("USER: ").append(currentMessage).append("\n\n");
        context.append("ASSISTANT:");
        
        return context.toString();
    }
    
    /**
     * Generate a response with custom system prompt
     * @param sessionId Session identifier
     * @param userMessage User's message
     * @param systemPrompt Custom system prompt
     * @return AI's response
     */
    public String chatWithCustomPrompt(String sessionId, String userMessage, String systemPrompt) {
        try {
            memoryService.addMessage(sessionId, "user", userMessage);
            
            StringBuilder context = new StringBuilder();
            context.append(systemPrompt).append("\n\n");
            context.append("USER: ").append(userMessage).append("\n\n");
            context.append("ASSISTANT:");
            
            String aiResponse = geminiService.getGeminiResponse(context.toString());
            memoryService.addMessage(sessionId, "assistant", aiResponse);
            
            return aiResponse;
            
        } catch (Exception e) {
            logger.error("Error in custom prompt chat: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Clear conversation history for a session
     * @param sessionId Session identifier
     */
    public void clearConversation(String sessionId) {
        memoryService.clearHistory(sessionId);
        logger.info("🗑️ Conversation cleared for session: {}", sessionId);
    }
    
    /**
     * Get conversation summary
     * @param sessionId Session identifier
     * @return Summary of conversation
     */
    public String getConversationSummary(String sessionId) {
        String history = memoryService.getFormattedHistory(sessionId);
        
        if (history.isEmpty()) {
            return "No conversation history.";
        }
        
        try {
            String prompt = "Summarize the following conversation in 2-3 sentences:\n\n" + history;
            return geminiService.getGeminiResponse(prompt);
        } catch (Exception e) {
            return "Unable to generate summary: " + e.getMessage();
        }
    }
    
    /**
     * Check if Gemini service is available and working
     * @return true if service responds successfully
     */
    public boolean isModelAvailable() {
        try {
            String testResponse = geminiService.getGeminiResponse("Say 'OK' if you can read this.");
            return testResponse != null && !testResponse.contains("Error");
        } catch (Exception e) {
            logger.error("Gemini service not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get response without storing in memory (for one-off queries)
     * @param prompt The prompt to send
     * @return AI response
     */
    public String getQuickResponse(String prompt) {
        try {
            return geminiService.getGeminiResponse(prompt);
        } catch (Exception e) {
            logger.error("Error getting quick response: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
}