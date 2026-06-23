package com.neo.springapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight health endpoints that do not hit the database.
 * Use /api/ping for Render health checks and frontend wake-up calls so cold starts
 * do not fail when the DB pool is still initializing.
 */
@RestController
@RequestMapping("/api")
public class ApiHealthController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("status", "UP");
        body.put("service", "NeoBank API");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }
}
