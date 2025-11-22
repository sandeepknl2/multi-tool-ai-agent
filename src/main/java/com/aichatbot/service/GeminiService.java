package com.aichatbot.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    // Use v1beta API with correct model name
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODEL_NAME = "gemini-2.5-flash";

    public String getGeminiResponse(String prompt) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Prepare request JSON body
            JSONObject content = new JSONObject()
                    .put("parts", new JSONArray()
                            .put(new JSONObject().put("text", prompt)));

            JSONObject requestBody = new JSONObject()
                    .put("contents", new JSONArray().put(content));

            logger.debug("Sending request to Gemini API: {}", requestBody.toString());

            // Construct correct URL
            String url = GEMINI_API_BASE + MODEL_NAME + ":generateContent?key=" + apiKey;

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            logger.debug("Request URL: {}", url);

            // Make POST request
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            logger.debug("Received response: {}", response.getBody());

            // Parse JSON response
            JSONObject jsonResponse = new JSONObject(response.getBody());
            JSONArray candidates = jsonResponse.optJSONArray("candidates");

            if (candidates != null && !candidates.isEmpty()) {
                JSONObject contentObj = candidates.getJSONObject(0).optJSONObject("content");
                if (contentObj != null) {
                    JSONArray parts = contentObj.optJSONArray("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return parts.getJSONObject(0).optString("text", "No response text found.");
                    }
                }
            }

            return "No valid response from Gemini API.";

        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            return "Error: " + e.getMessage();
        }
    }
}