package com.neo.springapp.config;

import com.neo.springapp.service.UserSessionTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class UserSessionTokenFilter extends OncePerRequestFilter {
    private final UserSessionTokenService tokenService;

    public UserSessionTokenFilter(UserSessionTokenService tokenService) { this.tokenService = tokenService; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            UserSessionTokenService.SessionPrincipal principal = tokenService.verify(authorization.substring(7));
            if (principal != null) {
                var authorities = switch (principal.scope()) {
                    case "ADMIN" -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    case "MANAGER" -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_MANAGER"));
                    default -> Collections.emptyList();
                };
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, authorities));
            }
        }
        filterChain.doFilter(request, response);
    }
}