package com.neo.springapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS Configuration for Spring Boot Application
 * 
 * This configuration allows cross-origin requests from the Angular frontend
 * deployed on Vercel and handles preflight OPTIONS requests.
 * 
 * Location: springapp/src/main/java/com/neo/springapp/config/CorsConfig.java
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * Configure CORS mappings for all endpoints
     * 
     * This method:
     * - Allows requests from https://full-stack-neo-bank22.vercel.app
     * - Allows all required HTTP methods including OPTIONS for preflight
     * - Allows all headers
     * - Enables credentials
     * - Applies to all endpoints (/**)
     * - Sets max age for preflight cache
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                // Use allowedOriginPatterns to support both exact domains and wildcards
                // This is required when allowCredentials is true
                .allowedOriginPatterns(
                    // Production Vercel domain (exact match)
                    "https://full-stack-neo-bank22.vercel.app",
                    // Local development with any port
                    "http://localhost:*",
                    "http://127.0.0.1:*"
                )
                // Allow all required HTTP methods including OPTIONS for preflight
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // Allow all headers (required for preflight requests)
                .allowedHeaders("*")
                // Expose all headers in response
                .exposedHeaders("*")
                // Enable credentials (cookies, authorization headers, etc.)
                .allowCredentials(true)
                // Cache preflight requests for 1 hour (3600 seconds)
                .maxAge(3600);
    }
}
