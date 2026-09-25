package com.neo.springapp.controller;

import com.neo.springapp.service.AdminSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/search")
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

    @GetMapping("/barcode")
    public ResponseEntity<Map<String, Object>> searchByBarcode(@RequestParam String q) {
        try {
            Map<String, Object> results = adminSearchService.searchByBarcode(q);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Barcode search failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/barcode/generate")
    public ResponseEntity<Map<String, Object>> generateBarcode(
            @RequestParam String accountNumber,
            @RequestParam(required = false) String accountType) {
        try {
            Map<String, Object> result = adminSearchService.generateBarcodeForAccount(accountNumber, accountType);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Barcode generation failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}




