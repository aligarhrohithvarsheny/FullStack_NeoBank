package com.neo.springapp;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA Forwarding Controller
 * 
 * Forwards all non-API routes to index.html for Angular routing.
 * This controller has low priority and only handles routes that:
 * - Don't start with /api
 * - Don't start with /actuator
 * - Don't start with /v3/api-docs
 * - Don't start with /swagger-ui
 * - Don't contain a dot (static files like .js, .css, .png)
 */
@Controller
@Order(Integer.MAX_VALUE)  // Lowest priority - only matches if no other controller matches
public class SpaForwardingController {
    
    /**
     * Forward to index.html for SPA routing.
     * Only matches if the request path doesn't start with excluded prefixes.
     */
    @RequestMapping(value = { "/", "/{path:[^.]*}", "/{path:[^.]*}/**" })
    public String forward(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Explicitly exclude API and system endpoints
        if (path.startsWith("/api") ||
            path.startsWith("/actuator") ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-ui") ||
            path.contains(".")) {  // Static files have extensions
            // Don't forward - let other handlers process
            return null;
        }
        
        // Forward to index.html for SPA routing
        return "forward:/index.html";
    }
}