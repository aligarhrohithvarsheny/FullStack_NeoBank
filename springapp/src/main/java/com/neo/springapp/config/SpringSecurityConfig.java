package com.neo.springapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security Configuration using SecurityFilterChain
 * 
 * This configuration:
 * - Allows CORS requests from https://full-stack-neo-bank22.vercel.app
 * - Explicitly allows OPTIONS requests for preflight
 * - Disables CSRF for API endpoints
 * - Permits all requests to /api/** endpoints
 * - Enables CORS inside SecurityFilterChain
 * 
 * Location: springapp/src/main/java/com/neo/springapp/config/SpringSecurityConfig.java
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    /**
     * Configure SecurityFilterChain to allow CORS and handle preflight requests
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for API endpoints (not needed for stateless APIs)
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS using the CORS configuration source
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Permit all requests to API endpoints
                .requestMatchers("/api/**").permitAll()
                // Permit all OPTIONS requests (preflight)
                .requestMatchers("/**").permitAll()
                // Allow all other requests (you can add authentication later if needed)
                .anyRequest().permitAll()
            )
            
            // Use stateless session (no session creation)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    /**
     * CORS Configuration Source
     * 
     * This bean provides CORS configuration that integrates with Spring Security.
     * It allows:
     * - Origin: https://full-stack-neo-bank22.vercel.app
     * - Methods: GET, POST, PUT, DELETE, OPTIONS
     * - All headers
     * - Credentials enabled
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow the Vercel frontend domain
        configuration.setAllowedOrigins(Arrays.asList(
            "https://full-stack-neo-bank22.vercel.app"
        ));
        
        // Allow localhost for local development
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        
        // Allow all required HTTP methods including OPTIONS for preflight
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Allow all headers (required for preflight requests)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Expose all headers in response
        configuration.setExposedHeaders(Arrays.asList("*"));
        
        // Enable credentials (cookies, authorization headers, etc.)
        configuration.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour (3600 seconds)
        configuration.setMaxAge(3600L);
        
        // Apply CORS configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
