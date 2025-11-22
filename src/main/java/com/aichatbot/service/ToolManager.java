package com.aichatbot.service;

import com.aichatbot.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to manage and execute tools
 */
@Service
public class ToolManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolManager.class);
    private final Map<String, Tool> tools = new HashMap<>();
    
    /**
     * Constructor - auto-discovers all Tool beans
     */
    @Autowired
    public ToolManager(List<Tool> toolList) {
        for (Tool tool : toolList) {
            tools.put(tool.getName(), tool);
            logger.info("📦 Registered tool: {}", tool.getName());
        }
        logger.info("✅ ToolManager initialized with {} tools", tools.size());
    }
    
    /**
     * Get all available tools
     */
    public Map<String, Tool> getAllTools() {
        return tools;
    }
    
    /**
     * Get a specific tool by name
     */
    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }
    
    /**
     * Execute a tool by name with parameters
     */
    public String executeTool(String toolName, String parameters) {
        logger.info("🔧 Executing tool: {} with params: {}", toolName, parameters);
        
        Optional<Tool> tool = getTool(toolName);
        if (tool.isEmpty()) {
            logger.error("❌ Tool not found: {}", toolName);
            return "Error: Tool '" + toolName + "' not found";
        }
        
        try {
            String result = tool.get().execute(parameters);
            logger.info("✅ Tool execution successful: {}", toolName);
            return result;
        } catch (Exception e) {
            logger.error("❌ Tool execution failed: {}", e.getMessage());
            return "Error executing tool: " + e.getMessage();
        }
    }
    
    /**
     * Find which tool should handle a user message
     */
    public Optional<Tool> findToolForMessage(String userMessage) {
        for (Tool tool : tools.values()) {
            if (tool.shouldUse(userMessage)) {
                logger.info("🎯 Found matching tool: {} for message", tool.getName());
                return Optional.of(tool);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Get all tools as a description string for AI prompt
     */
    public String getToolsDescription() {
        StringBuilder description = new StringBuilder();
        description.append("You have access to the following tools:\n\n");
        
        for (Tool tool : tools.values()) {
            description.append("Tool: ").append(tool.getName()).append("\n");
            description.append("Description: ").append(tool.getDescription()).append("\n");
            description.append("Parameters: ").append(tool.getParametersSchema()).append("\n\n");
        }
        
        description.append("To use a tool, respond with: TOOL_CALL: {tool_name} {parameters_json}\n");
        description.append("Example: TOOL_CALL: calculator {\"expression\": \"25 * 47\"}\n");
        
        return description.toString();
    }
    
    /**
     * Check if a message is a tool call
     */
    public boolean isToolCall(String message) {
        return message != null && message.trim().startsWith("TOOL_CALL:");
    }
    
    /**
     * Parse tool call from AI response
     * Expected format: "TOOL_CALL: tool_name {parameters_json}"
     */
    public ToolCall parseToolCall(String message) {
        try {
            String content = message.substring("TOOL_CALL:".length()).trim();
            
            int firstSpace = content.indexOf(' ');
            if (firstSpace == -1) {
                return new ToolCall(content, "{}");
            }
            
            String toolName = content.substring(0, firstSpace);
            String parameters = content.substring(firstSpace + 1).trim();
            
            return new ToolCall(toolName, parameters);
            
        } catch (Exception e) {
            logger.error("Failed to parse tool call: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Inner class to represent a parsed tool call
     */
    public static class ToolCall {
        private final String toolName;
        private final String parameters;
        
        public ToolCall(String toolName, String parameters) {
            this.toolName = toolName;
            this.parameters = parameters;
        }
        
        public String getToolName() {
            return toolName;
        }
        
        public String getParameters() {
            return parameters;
        }
    }
}