package com.neo.springapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security Configuration
 * 
 * - Disables CSRF (for stateless API)
 * - Enables CORS using CorsConfigurationSource bean
 * - Permits all /api/** endpoints
 * - Permits OPTIONS requests for CORS preflight
 * - Uses stateless session management
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API (using JWT or session-based auth)
            .csrf(csrf -> csrf.disable())
            
            // Enable CORS using the CorsConfigurationSource bean
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // Configure authorization
            .authorizeHttpRequests(auth -> auth
                // Allow OPTIONS requests for CORS preflight (MUST be first)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Permit all API endpoints (public API)
                .requestMatchers("/api/**").permitAll()
                
                // Allow all other requests (static resources, health checks, etc.)
                .anyRequest().permitAll()
            )
            
            // Use stateless session management (no session cookies)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}
