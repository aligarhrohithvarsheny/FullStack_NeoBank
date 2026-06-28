package com.neo.springapp.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

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
    private static final List<String> DEFAULT_ALLOWED_ORIGINS = Arrays.asList(
        "https://neo-bank-669.web.app",
        "https://neo-bank-669.firebaseapp.com",
        "https://fullstack-neobank.onrender.com",
        "http://localhost:4200",
        "http://localhost:4000"
    );

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = buildCorsConfiguration();

        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
        CorsConfiguration configuration = buildCorsConfiguration();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        FilterRegistrationBean<CorsFilter> registrationBean = new FilterRegistrationBean<>(new CorsFilter(source));
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    private CorsConfiguration buildCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse allowed origins from environment variable or system property.
        // If not provided, fall back to the known Firebase, Render, and local dev origins.
        String configuredOrigins = System.getenv("SPRING_WEB_CORS_ALLOWED_ORIGINS");
        if (configuredOrigins == null || configuredOrigins.trim().isEmpty()) {
            configuredOrigins = System.getProperty("spring.web.cors.allowed-origins");
        }

        if (configuredOrigins != null && !configuredOrigins.trim().isEmpty()) {
            List<String> origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());

            if (!origins.isEmpty()) {
                configuration.setAllowedOriginPatterns(origins);
                System.out.println("✅ CORS configured with origins: " + origins);
            } else {
                configuration.setAllowedOriginPatterns(DEFAULT_ALLOWED_ORIGINS);
                System.out.println("⚠️ CORS: No valid origins found in SPRING_WEB_CORS_ALLOWED_ORIGINS, using defaults: " + DEFAULT_ALLOWED_ORIGINS);
            }
        } else {
            configuration.setAllowedOriginPatterns(DEFAULT_ALLOWED_ORIGINS);
            System.out.println("⚠️ CORS: No configured origins found, using defaults: " + DEFAULT_ALLOWED_ORIGINS);
        }

        // Allow browser preflight and regular API calls from the frontend.
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Origin",
            "Content-Type",
            "Accept",
            "Authorization",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        return configuration;
    }
}
