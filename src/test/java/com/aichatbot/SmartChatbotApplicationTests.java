package com.aichatbot;

import com.aichatbot.controller.ChatController;
import com.aichatbot.service.MemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Smart Chatbot Application
 * Tests that Spring context loads and beans are created
 */
@SpringBootTest
class ChatbotApplicationTests {
    
   // Mock to avoid API key requirement
    
    @Autowired(required = false)
    private ChatController chatController;
    
    @Autowired(required = false)
    private MemoryService memoryService;
    
    /**
     * Test that Spring application context loads successfully
     */
    @Test
    void contextLoads() {
        // If this test passes, it means Spring Boot started successfully
        // and all beans were created without errors
    }
    
    /**
     * Test that required beans are created
     */
    @Test
    void requiredBeansAreCreated() {
        // Verify controller exists
        assertThat(chatController).isNotNull();
        
        // Verify memory service exists
        assertThat(memoryService).isNotNull();
    }
    
    /**
     * Test MemoryService basic functionality
     */
    @Test
    void memoryServiceWorks() {
        MemoryService service = new MemoryService();
        
        // Add a message
        service.addMessage("test-session", "user", "Hello");
        
        // Verify it was stored
        assertThat(service.getHistory("test-session")).hasSize(1);
        
        // Clear and verify
        service.clearHistory("test-session");
        assertThat(service.getHistory("test-session")).isEmpty();
    }
}