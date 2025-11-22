package com.aichatbot.tools;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Enhanced converter tool with all common conversions
 */
@Component
public class ConverterTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(ConverterTool.class);
    
    @Override
    public String getName() {
        return "converter";
    }
    
    @Override
    public String getDescription() {
        return "Converts between units. " +
               "Length: km, m, miles, feet, inches. " +
               "Weight: kg, g, lbs, oz. " +
               "Temperature: celsius, fahrenheit.";
    }
    
    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "value": {"type": "number", "description": "Value to convert"},
                    "conversion": {
                        "type": "string",
                        "description": "Type of conversion",
                        "enum": [
                            "km_to_miles", "miles_to_km",
                            "m_to_feet", "feet_to_m",
                            "inches_to_cm", "cm_to_inches",
                            "kg_to_lbs", "lbs_to_kg",
                            "g_to_oz", "oz_to_g",
                            "c_to_f", "f_to_c"
                        ]
                    }
                },
                "required": ["value", "conversion"]
            }
            """;
    }
    
    @Override
    public String execute(String parameters) {
        try {
            logger.info("🔄 Converting: {}", parameters);
            
            JSONObject params = new JSONObject(parameters);
            double value = params.getDouble("value");
            String conversion = params.getString("conversion").toLowerCase();
            
            double result = switch (conversion) {
                // Length - Distance
                case "km_to_miles" -> value * 0.621371;
                case "miles_to_km" -> value * 1.60934;
                
                // Length - Height
                case "m_to_feet" -> value * 3.28084;
                case "feet_to_m" -> value * 0.3048;
                case "m_to_ft" -> value * 3.28084;
                case "ft_to_m" -> value * 0.3048;
                
                // Length - Small
                case "inches_to_cm" -> value * 2.54;
                case "cm_to_inches" -> value * 0.393701;
                case "in_to_cm" -> value * 2.54;
                case "cm_to_in" -> value * 0.393701;
                
                // Weight - Large
                case "kg_to_lbs" -> value * 2.20462;
                case "lbs_to_kg" -> value * 0.453592;
                case "kg_to_lb" -> value * 2.20462;
                case "lb_to_kg" -> value * 0.453592;
                
                // Weight - Small
                case "g_to_oz" -> value * 0.035274;
                case "oz_to_g" -> value * 28.3495;
                
                // Temperature
                case "c_to_f" -> (value * 9.0/5.0) + 32;
                case "f_to_c" -> (value - 32) * 5.0/9.0;
                case "celsius_to_fahrenheit" -> (value * 9.0/5.0) + 32;
                case "fahrenheit_to_celsius" -> (value - 32) * 5.0/9.0;
                
                default -> throw new IllegalArgumentException(
                    "Unknown conversion: " + conversion + 
                    ". Supported: km_to_miles, m_to_feet, inches_to_cm, kg_to_lbs, g_to_oz, c_to_f, etc."
                );
            };
            
            String formatted = formatConversion(value, result, conversion);
            logger.info("✅ Result: {}", formatted);
            return formatted;
            
        } catch (Exception e) {
            logger.error("❌ Error: {}", e.getMessage(), e);
            return "Conversion error: " + e.getMessage() + 
                   "\nSupported conversions: km↔miles, m↔feet, inches↔cm, kg↔lbs, g↔oz, celsius↔fahrenheit";
        }
    }
    
    @Override
    public boolean shouldUse(String userMessage) {
        String lower = userMessage.toLowerCase();
        
        // Check for convert keyword
        if (lower.contains("convert")) return true;
        
        // Check for common patterns
        if (lower.matches(".*(km|mile|meter|feet|inch|cm).*to.*(km|mile|meter|feet|inch|cm).*")) return true;
        if (lower.matches(".*(kg|lb|pound|gram|ounce).*to.*(kg|lb|pound|gram|ounce).*")) return true;
        if (lower.matches(".*(celsius|fahrenheit|degree).*to.*(celsius|fahrenheit|degree).*")) return true;
        
        // Check for "how many" questions
        if (lower.matches(".*how many.*(km|mile|meter|feet|kg|lb|gram).*")) return true;
        
        return false;
    }
    
    private String formatConversion(double from, double to, String type) {
        return switch (type) {
            // Length - Distance
            case "km_to_miles" -> String.format("📏 %.2f km = %.2f miles", from, to);
            case "miles_to_km" -> String.format("📏 %.2f miles = %.2f km", from, to);
            
            // Length - Height
            case "m_to_feet", "m_to_ft" -> String.format("📏 %.2f meters = %.2f feet", from, to);
            case "feet_to_m", "ft_to_m" -> String.format("📏 %.2f feet = %.2f meters", from, to);
            
            // Length - Small
            case "inches_to_cm", "in_to_cm" -> String.format("📏 %.2f inches = %.2f cm", from, to);
            case "cm_to_inches", "cm_to_in" -> String.format("📏 %.2f cm = %.2f inches", from, to);
            
            // Weight - Large
            case "kg_to_lbs", "kg_to_lb" -> String.format("⚖️ %.2f kg = %.2f lbs", from, to);
            case "lbs_to_kg", "lb_to_kg" -> String.format("⚖️ %.2f lbs = %.2f kg", from, to);
            
            // Weight - Small
            case "g_to_oz" -> String.format("⚖️ %.2f g = %.2f oz", from, to);
            case "oz_to_g" -> String.format("⚖️ %.2f oz = %.2f g", from, to);
            
            // Temperature
            case "c_to_f", "celsius_to_fahrenheit" -> String.format("🌡️ %.1f°C = %.1f°F", from, to);
            case "f_to_c", "fahrenheit_to_celsius" -> String.format("🌡️ %.1f°F = %.1f°C", from, to);
            
            default -> String.format("%.2f → %.2f", from, to);
        };
    }
}