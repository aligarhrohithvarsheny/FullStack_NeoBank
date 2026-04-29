package com.neo.springapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
 * - Prevents redirects for API endpoints
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            
            // Enable CORS using the CorsConfigurationSource bean
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // Configure authorization
            .authorizeHttpRequests(auth -> auth
                // Allow OPTIONS requests for CORS preflight (MUST be first)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Explicitly allow GET and POST methods for all API endpoints
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/**").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/**").permitAll()
                
                // Permit all API endpoints (public API) - catch-all for any other methods
                .requestMatchers("/api/**").permitAll()
                
                // Permit Actuator endpoints (GET and POST)
                .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/actuator/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                
                // Permit Swagger/OpenAPI endpoints (GET)
                .requestMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                
                // Allow GET and POST for all other requests (static resources, SPA routes, etc.)
                .requestMatchers(HttpMethod.GET, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/**").permitAll()
                
                // Allow all other requests (static resources, SPA routes, etc.)
                .anyRequest().permitAll()
            )
            
            // Use stateless session management (no session cookies, no redirects)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Disable default login/logout pages (prevents redirects)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable());

        return http.build();
    }
}
