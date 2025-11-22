package com.aichatbot.tools;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Weather tool using OpenWeatherMap API (free tier)
 */
@Component
public class WeatherTool implements Tool {
    
    private static final Logger logger = LoggerFactory.getLogger(WeatherTool.class);
    
    @Value("${openweather.api.key:demo}")
    private String apiKey;
    
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";
    
    public WeatherTool() {
        logger.info("✅ Weather Tool initialized");
    }
    
    @Override
    public String getName() {
        return "weather";
    }
    
    @Override
    public String getDescription() {
        return "Gets current weather information for any city worldwide. " +
               "Use this when user asks about weather, temperature, conditions, or climate.";
    }
    
    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "city": {
                        "type": "string",
                        "description": "City name (e.g., 'Mumbai', 'New York', 'London')"
                    }
                },
                "required": ["city"]
            }
            """;
    }
    
    @Override
    public String execute(String parameters) {
        try {
            logger.info("🌤️ Fetching weather with params: {}", parameters);
            
            // Check if API key is configured
            if (apiKey == null || apiKey.equals("demo") || apiKey.trim().isEmpty()) {
                return "Error: Weather API key not configured. Please add 'openweather.api.key' to application.properties";
            }
            
            // Parse parameters
            JSONObject params = new JSONObject(parameters);
            String city = params.getString("city");
            
            // Build API URL (metric = Celsius)
            String url = String.format("%s?q=%s&units=metric&appid=%s", 
                                      API_URL, city, apiKey);
            
            logger.debug("Calling weather API for: {}", city);
            
            // Call API
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);
            
            // Parse response
            JSONObject json = new JSONObject(response);
            
            // Extract weather data
            String cityName = json.getString("name");
            String country = json.getJSONObject("sys").getString("country");
            
            JSONObject main = json.getJSONObject("main");
            double temp = main.getDouble("temp");
            double feelsLike = main.getDouble("feels_like");
            int humidity = main.getInt("humidity");
            int pressure = main.getInt("pressure");
            
            JSONObject weather = json.getJSONArray("weather").getJSONObject(0);
            String description = weather.getString("description");
            String mainCondition = weather.getString("main");
            
            JSONObject wind = json.getJSONObject("wind");
            double windSpeed = wind.getDouble("speed");
            
            // Format result with emoji
            String emoji = getWeatherEmoji(mainCondition);
            
            String result = String.format(
                "%s Weather in %s, %s:\n" +
                "🌡️ Temperature: %.1f°C (feels like %.1f°C)\n" +
                "☁️ Conditions: %s\n" +
                "💧 Humidity: %d%%\n" +
                "🌬️ Wind Speed: %.1f m/s\n" +
                "🔽 Pressure: %d hPa",
                emoji, cityName, country, temp, feelsLike, 
                capitalize(description), humidity, windSpeed, pressure
            );
            
            logger.info("✅ Weather fetched successfully for: {}", city);
            return result;
            
        } catch (Exception e) {
            logger.error("❌ Weather error: {}", e.getMessage());
            
            String errorMsg = e.getMessage();
            
            if (errorMsg != null) {
                if (errorMsg.contains("404")) {
                    return "Error: City not found. Please check the spelling and try again.";
                } else if (errorMsg.contains("401")) {
                    return "Error: Invalid API key. Please check your OpenWeatherMap API key.";
                } else if (errorMsg.contains("429")) {
                    return "Error: Too many requests. Please wait a moment and try again.";
                }
            }
            
            return "Error getting weather: Unable to fetch data. Please try again.";
        }
    }
    
    @Override
    public boolean shouldUse(String userMessage) {
        String lower = userMessage.toLowerCase();
        return lower.contains("weather") ||
               lower.contains("temperature") ||
               lower.contains("forecast") ||
               lower.matches(".*how.*(hot|cold|warm|cool).*") ||
               lower.matches(".*what.*like outside.*") ||
               lower.matches(".*weather in.*") ||
               lower.matches(".*climate.*");
    }
    
    /**
     * Get appropriate emoji for weather condition
     */
    private String getWeatherEmoji(String condition) {
        return switch (condition.toLowerCase()) {
            case "clear" -> "☀️";
            case "clouds" -> "☁️";
            case "rain", "drizzle" -> "🌧️";
            case "thunderstorm" -> "⛈️";
            case "snow" -> "❄️";
            case "mist", "fog", "haze" -> "🌫️";
            default -> "🌤️";
        };
    }
    
    /**
     * Capitalize first letter of each word
     */
    private String capitalize(String text) {
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
}