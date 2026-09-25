package com.neo.springapp.controller;

import com.neo.springapp.service.AIAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.neo.springapp.service.UserSessionTokenService;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIAnalysisController {

    @Autowired
    private AIAnalysisService aiAnalysisService;

    @Autowired
    private UserSessionTokenService userSessionTokenService;

    /**
     * Get AI analysis for user's spending patterns
     */
    @GetMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeSpending(Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof UserSessionTokenService.SessionPrincipal principal)) {
                return ResponseEntity.status(401).build();
            }
            String accountNumber = principal.accountNumber();
            Map<String, Object> analysis = aiAnalysisService.analyzeSpending(accountNumber);
            if (analysis.containsKey("error")) {
                return ResponseEntity.badRequest().body(analysis);
            }
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", "Failed to analyze spending: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}



