package com.aichatbot.tools;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Time tool for getting current date and time
 */
@Component
public class TimeTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(TimeTool.class);
    
    @Override
    public String getName() {
        return "time";
    }
    
    @Override
    public String getDescription() {
        return "Gets the current date and time. " +
               "Use this when the user asks what time it is, what day it is, " +
               "or for current date/time information.";
    }
    
    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "timezone": {
                        "type": "string",
                        "description": "Timezone (e.g., 'Asia/Kolkata', 'America/New_York'). Defaults to 'Asia/Kolkata'",
                        "default": "Asia/Kolkata"
                    },
                    "format": {
                        "type": "string",
                        "description": "Output format: 'full', 'date', or 'time'. Defaults to 'full'",
                        "default": "full"
                    }
                }
            }
            """;
    }
    
    @Override
    public String execute(String parameters) {
        try {
            logger.info("🕐 Executing time tool with params: {}", parameters);
            
            // Parse parameters or use defaults
            String timezone = "Asia/Kolkata";
            String format = "full";
            
            if (parameters != null && !parameters.trim().isEmpty()) {
                try {
                    JSONObject params = new JSONObject(parameters);
                    timezone = params.optString("timezone", "Asia/Kolkata");
                    format = params.optString("format", "full");
                } catch (Exception e) {
                    logger.warn("Failed to parse parameters, using defaults");
                }
            }
            
            // Get current time in specified timezone
            ZoneId zoneId = ZoneId.of(timezone);
            LocalDateTime now = LocalDateTime.now(zoneId);
            
            String result = switch (format.toLowerCase()) {
                case "date" -> formatDate(now);
                case "time" -> formatTime(now);
                default -> formatFull(now, timezone);
            };
            
            logger.info("✅ Time result: {}", result);
            return result;
            
        } catch (Exception e) {
            logger.error("❌ Time tool error: {}", e.getMessage());
            return "Error getting time: " + e.getMessage();
        }
    }
    
    @Override
    public boolean shouldUse(String userMessage) {
        String lower = userMessage.toLowerCase();
        return lower.contains("what time") ||
               lower.contains("current time") ||
               lower.contains("what day") ||
               lower.contains("what date") ||
               lower.contains("today") && (lower.contains("date") || lower.contains("day")) ||
               lower.matches(".*(time|date|day).*now.*");
    }
    
    private String formatFull(LocalDateTime dateTime, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm:ss a");
        return dateTime.format(formatter) + " " + timezone;
    }
    
    private String formatDate(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");
        return dateTime.format(formatter);
    }
    
    private String formatTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        return dateTime.format(formatter);
    }
}