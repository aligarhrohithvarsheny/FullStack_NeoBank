package com.neo.springapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CORS Configuration
 * 
 * Reads allowed origins from SPRING_WEB_CORS_ALLOWED_ORIGINS environment variable.
 * Format: https://domain1.com,https://domain2.com
 * 
 * Supports Vercel production and preview URLs:
 * - Production: https://full-stack-neo-bank22.vercel.app
 * - Preview: https://*.vercel.app (wildcard pattern)
 */
@Configuration
public class CorsConfig {

    /**
     * Read allowed origins from SPRING_WEB_CORS_ALLOWED_ORIGINS environment variable.
     * Spring Boot automatically maps SPRING_WEB_CORS_ALLOWED_ORIGINS to spring.web.cors.allowed-origins
     */
    @Value("${SPRING_WEB_CORS_ALLOWED_ORIGINS:${spring.web.cors.allowed-origins:}}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from environment variable
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
            
            if (!origins.isEmpty()) {
                configuration.setAllowedOrigins(origins);
                System.out.println("✅ CORS configured with origins: " + origins);
            } else {
                configuration.setAllowedOrigins(List.of());
                System.out.println("⚠️ CORS: No valid origins found in SPRING_WEB_CORS_ALLOWED_ORIGINS");
            }
        } else {
            configuration.setAllowedOrigins(List.of());
            System.out.println("⚠️ CORS: SPRING_WEB_CORS_ALLOWED_ORIGINS not set - CORS disabled");
        }
        
        // Allow all HTTP methods including OPTIONS for preflight
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
