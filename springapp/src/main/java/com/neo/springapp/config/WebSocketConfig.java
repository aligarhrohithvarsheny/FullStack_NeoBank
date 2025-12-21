package com.neo.springapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WebSocket Configuration
 * 
 * Uses the same CORS origins as HTTP endpoints from SPRING_WEB_CORS_ALLOWED_ORIGINS.
 * Supports WebSocket connections from allowed origins.
 */
@Configuration
@ConditionalOnClass(WebSocketMessageBrokerConfigurer.class)
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Read allowed origins from SPRING_WEB_CORS_ALLOWED_ORIGINS environment variable.
     * Same as CorsConfig to ensure consistency between HTTP and WebSocket CORS.
     */
    @Value("${SPRING_WEB_CORS_ALLOWED_ORIGINS:${spring.web.cors.allowed-origins:}}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // Parse allowed origins from environment variable
        List<String> originPatterns;
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            originPatterns = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
        } else {
            originPatterns = List.of();
        }

        // Register WebSocket endpoints with CORS configuration
        if (!originPatterns.isEmpty()) {
            String[] patterns = originPatterns.toArray(new String[0]);
            // Use setAllowedOriginPatterns for WebSocket (supports wildcards)
            registry.addEndpoint("/ws-chat")
                    .setAllowedOriginPatterns(patterns)
                    .withSockJS();
            registry.addEndpoint("/ws-chat")
                    .setAllowedOriginPatterns(patterns);
            System.out.println("✅ WebSocket CORS configured with origins: " + originPatterns);
        } else {
            // Allow all origins if not configured (development only)
            registry.addEndpoint("/ws-chat")
                    .setAllowedOriginPatterns("*")
                    .withSockJS();
            registry.addEndpoint("/ws-chat")
                    .setAllowedOriginPatterns("*");
            System.out.println("⚠️ WebSocket: SPRING_WEB_CORS_ALLOWED_ORIGINS not set - allowing all origins");
        }
    }
}

