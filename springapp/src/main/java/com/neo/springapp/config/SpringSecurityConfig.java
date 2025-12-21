package com.neo.springapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // Allow OPTIONS for CORS preflight (must be first)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Explicitly allow all HTTP methods for admin and manager endpoints
                .requestMatchers("/api/admins/**").permitAll()
                .requestMatchers("/api/managers/**").permitAll()
                // Allow all HTTP methods for all other API endpoints
                .requestMatchers("/api/**").permitAll()
                // Allow all other requests
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}
