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

@Configuration
@ConditionalOnClass(WebSocketMessageBrokerConfigurer.class)
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.web.cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        List<String> originPatterns;
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            originPatterns = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
        } else {
            originPatterns = List.of();
        }

        if (!originPatterns.isEmpty()) {
            String[] patterns = originPatterns.toArray(new String[0]);
            registry.addEndpoint("/ws-chat")
                    .setAllowedOriginPatterns(patterns)
                    .withSockJS();
            registry.addEndpoint("/ws-chat")
                    .setAllowedOriginPatterns(patterns);
        } else {
            registry.addEndpoint("/ws-chat")
                    .withSockJS();
            registry.addEndpoint("/ws-chat");
        }
    }
}

