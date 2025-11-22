package com.aichatbot.tools;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Calculator tool for mathematical operations
 */
@Component
public class CalculatorTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(CalculatorTool.class);
    private final ScriptEngine engine;
    
    public CalculatorTool() {
        this.engine = new ScriptEngineManager().getEngineByName("JavaScript");
        logger.info("✅ Calculator Tool initialized");
    }
    
    @Override
    public String getName() {
        return "calculator";
    }
    
    @Override
    public String getDescription() {
        return "Performs mathematical calculations. " +
               "Use this tool when the user asks to calculate, compute, or solve math problems. " +
               "Supports: addition (+), subtraction (-), multiplication (*), division (/), " +
               "exponents (**), and parentheses.";
    }
    
    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "expression": {
                        "type": "string",
                        "description": "Mathematical expression to calculate (e.g., '25 * 47', '(10 + 5) / 3')"
                    }
                },
                "required": ["expression"]
            }
            """;
    }
    
    @Override
    public String execute(String parameters) {
        try {
            logger.info("🔢 Executing calculator with params: {}", parameters);
            
            // Parse parameters
            JSONObject params = new JSONObject(parameters);
            String expression = params.getString("expression");
            
            // Clean and validate expression
            expression = cleanExpression(expression);
            
            if (!isValidExpression(expression)) {
                return "Error: Invalid mathematical expression";
            }
            
            // Evaluate expression
            Object result = engine.eval(expression);
            
            logger.info("✅ Calculation result: {}", result);
            return String.valueOf(result);
            
        } catch (Exception e) {
            logger.error("❌ Calculator error: {}", e.getMessage());
            return "Error calculating: " + e.getMessage();
        }
    }
    
    @Override
    public boolean shouldUse(String userMessage) {
        String lower = userMessage.toLowerCase();
        return lower.contains("calculate") || 
               lower.contains("compute") || 
               lower.contains("what is") && containsMathOperator(userMessage) ||
               lower.contains("solve") ||
               lower.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*");
    }
    
    /**
     * Clean expression by removing non-math characters
     */
    private String cleanExpression(String expression) {
        // Replace ** with Math.pow for exponents
        expression = expression.replaceAll("(\\d+)\\s*\\*\\*\\s*(\\d+)", "Math.pow($1, $2)");
        
        // Remove any potentially harmful characters
        expression = expression.replaceAll("[^0-9+\\-*/()\\s.Math,pow]", "");
        
        return expression.trim();
    }
    
    /**
     * Validate if expression contains only safe characters
     */
    private boolean isValidExpression(String expression) {
        // Must contain at least one number and one operator
        return expression.matches(".*\\d+.*") && 
               expression.matches(".*[+\\-*/()].*");
    }
    
    /**
     * Check if message contains math operators
     */
    private boolean containsMathOperator(String message) {
        return message.matches(".*[+\\-*/×÷].*") || 
               message.matches(".*\\d+.*");
    }
}