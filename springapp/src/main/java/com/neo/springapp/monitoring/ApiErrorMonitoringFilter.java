package com.neo.springapp.monitoring;

import com.neo.springapp.service.SystemMonitoringService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ApiErrorMonitoringFilter extends OncePerRequestFilter {

    private final SystemMonitoringService monitoringService;

    public ApiErrorMonitoringFilter(SystemMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            if (request.getRequestURI().startsWith("/api/") && status >= 400) {
                monitoringService.recordApiError(request.getMethod(), request.getRequestURI(), status);
            }
        }
    }
}
