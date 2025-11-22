package com.aichatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Main Spring Boot Application
 * Entry point for the Smart Chatbot application
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.aichatbot"})  // ⬅️ ADDED THIS
@EnableScheduling
public class SmartChatbotApplication {
    
    /**
     * Main method - starts the Spring Boot application
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Print banner
        printBanner();
        
        // Start Spring Boot application
        SpringApplication.run(SmartChatbotApplication.class, args);
        
        // Print success message
        printSuccessMessage();
    }
    
    /**
     * Configure CORS (Cross-Origin Resource Sharing)
     * Allows frontend to call backend from different origin
     * @return WebMvcConfigurer with CORS settings
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600);
            }
        };
    }
    
    /**
     * Print application banner
     */
    private static void printBanner() {
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║                                        ║");
        System.out.println("║        🤖 SMART CHATBOT 🤖            ║");
        System.out.println("║                                        ║");
        System.out.println("║     AI-Powered Conversational Agent    ║");
        System.out.println("║                                        ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("\n");
    }
    
    /**
     * Print success message after startup
     */
    private static void printSuccessMessage() {
        System.out.println("\n");
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  ✅ Smart Chatbot is running successfully!");
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  📱 Web Interface: http://localhost:8080");
        System.out.println("  🔌 API Endpoint:   http://localhost:8080/api/chat");
        System.out.println("  ❤️  Health Check:  http://localhost:8080/api/chat/health");
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  💡 Tips:");
        System.out.println("     - Open the web interface in your browser");
        System.out.println("     - Check console for logs and errors");
        System.out.println("     - Press Ctrl+C to stop the application");
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("\n");
    }
}