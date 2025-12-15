package com.neo.springapp.controller;

import com.neo.springapp.service.AIAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class AIAnalysisController {

    @Autowired
    private AIAnalysisService aiAnalysisService;

    /**
     * Get AI analysis for user's spending patterns
     */
    @GetMapping("/analyze/{accountNumber}")
    public ResponseEntity<Map<String, Object>> analyzeSpending(@PathVariable String accountNumber) {
        try {
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



