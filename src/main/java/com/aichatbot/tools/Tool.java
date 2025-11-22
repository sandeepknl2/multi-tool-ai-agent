package com.aichatbot.tools;

/**
 * Base interface for all tools that the AI can use
 */
public interface Tool {
    
    /**
     * Get the name of this tool
     * @return Tool name (e.g., "calculator", "weather")
     */
    String getName();
    
    /**
     * Get description of what this tool does
     * This is shown to the AI to help it decide when to use the tool
     * @return Tool description
     */
    String getDescription();
    
    /**
     * Get the parameters this tool accepts
     * @return JSON schema describing parameters
     */
    String getParametersSchema();
    
    /**
     * Execute the tool with given parameters
     * @param parameters Tool parameters as JSON string
     * @return Tool execution result
     */
    String execute(String parameters);
    
    /**
     * Check if this tool can handle the given query
     * @param userMessage User's message
     * @return true if tool should be used
     */
    default boolean shouldUse(String userMessage) {
        return false;
    }
}