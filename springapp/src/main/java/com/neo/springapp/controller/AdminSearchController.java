package com.neo.springapp.controller;

import com.neo.springapp.service.AdminSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/search")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class AdminSearchController {

    @Autowired
    private AdminSearchService adminSearchService;

    /**
     * Comprehensive search endpoint
     * Searches across: Accounts, Users, Loans, Cheques, Transactions, Cards
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> searchAll(@RequestParam String q) {
        try {
            Map<String, Object> results = adminSearchService.searchAll(q);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Search failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}




