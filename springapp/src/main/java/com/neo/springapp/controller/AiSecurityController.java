package com.neo.springapp.controller;

import com.neo.springapp.model.AiDeviceFingerprint;
import com.neo.springapp.model.AiSecurityEvent;
import com.neo.springapp.model.AiSecurityRule;
import com.neo.springapp.model.AiThreatScore;
import com.neo.springapp.service.AiSecurityService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI Security Controller - REST API for Advanced AI Security across all NeoBank channels.
 * Provides endpoints for threat analysis, dashboard analytics, event management,
 * device trust, watchlists, and security rule configuration.
 */
@RestController
@RequestMapping("/api/ai-security")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class AiSecurityController {

    private final AiSecurityService aiSecurityService;

    public AiSecurityController(AiSecurityService aiSecurityService) {
        this.aiSecurityService = aiSecurityService;
    }

    // ========================= DASHBOARD =========================

    /** Get comprehensive AI security dashboard data */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(aiSecurityService.getDashboardData());
    }

    // ========================= REAL-TIME ANALYSIS =========================

    /** Analyze login attempt for threats */
    @PostMapping("/analyze/login")
    public ResponseEntity<Map<String, Object>> analyzeLogin(@RequestBody Map<String, Object> request) {
        String entityId = (String) request.get("entityId");
        String entityType = (String) request.getOrDefault("entityType", "USER");
        String clientIp = (String) request.get("clientIp");
        String userAgent = (String) request.get("userAgent");
        String deviceInfo = (String) request.get("deviceInfo");
        String location = (String) request.get("location");
        Boolean loginSuccess = (Boolean) request.getOrDefault("loginSuccess", false);

        Map<String, Object> result = aiSecurityService.analyzeLoginAttempt(
                entityId, entityType, clientIp, userAgent, deviceInfo, location, loginSuccess);
        return ResponseEntity.ok(result);
    }

    /** Analyze transaction for anomalies */
    @PostMapping("/analyze/transaction")
    public ResponseEntity<Map<String, Object>> analyzeTransaction(@RequestBody Map<String, Object> request) {
        String accountNumber = (String) request.get("accountNumber");
        String entityName = (String) request.get("entityName");
        Double amount = request.get("amount") != null ? ((Number) request.get("amount")).doubleValue() : null;
        String recipientAccount = (String) request.get("recipientAccount");
        String transactionType = (String) request.get("transactionType");
        Double currentBalance = request.get("currentBalance") != null ? ((Number) request.get("currentBalance")).doubleValue() : null;
        String clientIp = (String) request.get("clientIp");
        String location = (String) request.get("location");
        String deviceInfo = (String) request.get("deviceInfo");

        Map<String, Object> result = aiSecurityService.analyzeTransaction(
                accountNumber, entityName, amount, recipientAccount,
                transactionType, currentBalance, clientIp, location, deviceInfo);
        return ResponseEntity.ok(result);
    }

    /** Analyze behavioral patterns */
    @PostMapping("/analyze/behavior")
    public ResponseEntity<Map<String, Object>> analyzeBehavior(@RequestBody Map<String, Object> request) {
        String entityId = (String) request.get("entityId");
        String entityType = (String) request.getOrDefault("entityType", "USER");
        Double typingSpeed = request.get("typingSpeedWpm") != null ? ((Number) request.get("typingSpeedWpm")).doubleValue() : null;
        Double mouseScore = request.get("mouseMovementScore") != null ? ((Number) request.get("mouseMovementScore")).doubleValue() : null;
        Double sessionDuration = request.get("sessionDurationMinutes") != null ? ((Number) request.get("sessionDurationMinutes")).doubleValue() : null;
        Integer pagesVisited = request.get("pagesVisited") != null ? ((Number) request.get("pagesVisited")).intValue() : null;
        String clientIp = (String) request.get("clientIp");
        String deviceInfo = (String) request.get("deviceInfo");

        Map<String, Object> result = aiSecurityService.analyzeBehavior(
                entityId, entityType, typingSpeed, mouseScore, sessionDuration, pagesVisited, clientIp, deviceInfo);
        return ResponseEntity.ok(result);
    }

    // ========================= EVENTS =========================

    /** Get paginated security events with filters */
    @GetMapping("/events")
    public ResponseEntity<Page<AiSecurityEvent>> getEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(aiSecurityService.getSecurityEvents(eventType, channel, severity, status, page, size));
    }

    /** Update event status */
    @PatchMapping("/events/{id}/status")
    public ResponseEntity<?> updateEventStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");
        String resolvedBy = request.get("resolvedBy");
        String notes = request.get("notes");
        AiSecurityEvent updated = aiSecurityService.updateEventStatus(id, newStatus, resolvedBy, notes);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    // ========================= THREAT SCORES =========================

    /** Get threat score for an entity */
    @GetMapping("/threat-score/{entityId}")
    public ResponseEntity<?> getThreatScore(
            @PathVariable String entityId,
            @RequestParam(defaultValue = "USER") String entityType) {
        AiThreatScore score = aiSecurityService.getThreatScore(entityId, entityType);
        if (score == null) return ResponseEntity.ok(Map.of("message", "No threat data for this entity", "riskLevel", "LOW"));
        return ResponseEntity.ok(score);
    }

    /** Toggle entity watchlist */
    @PostMapping("/watchlist")
    public ResponseEntity<AiThreatScore> toggleWatchlist(@RequestBody Map<String, Object> request) {
        String entityId = (String) request.get("entityId");
        String entityType = (String) request.getOrDefault("entityType", "USER");
        Boolean watchlist = (Boolean) request.getOrDefault("watchlist", true);
        String reason = (String) request.get("reason");
        return ResponseEntity.ok(aiSecurityService.toggleWatchlist(entityId, entityType, watchlist, reason));
    }

    // ========================= DEVICE MANAGEMENT =========================

    /** Get device fingerprints for an entity */
    @GetMapping("/devices/{entityId}")
    public ResponseEntity<List<AiDeviceFingerprint>> getDevices(@PathVariable String entityId) {
        return ResponseEntity.ok(aiSecurityService.getDeviceFingerprints(entityId));
    }

    /** Trust/untrust a device */
    @PatchMapping("/devices/{deviceId}/trust")
    public ResponseEntity<?> setDeviceTrust(
            @PathVariable Long deviceId,
            @RequestBody Map<String, Boolean> request) {
        Boolean trusted = request.getOrDefault("trusted", false);
        AiDeviceFingerprint updated = aiSecurityService.setDeviceTrust(deviceId, trusted);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    // ========================= SECURITY RULES =========================

    /** Get all security rules */
    @GetMapping("/rules")
    public ResponseEntity<List<AiSecurityRule>> getRules() {
        return ResponseEntity.ok(aiSecurityService.getAllRules());
    }

    /** Toggle rule active status */
    @PatchMapping("/rules/{id}/toggle")
    public ResponseEntity<?> toggleRule(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        Boolean active = request.getOrDefault("active", true);
        AiSecurityRule updated = aiSecurityService.toggleRule(id, active);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    /** Create a new security rule */
    @PostMapping("/rules")
    public ResponseEntity<AiSecurityRule> createRule(@RequestBody AiSecurityRule rule) {
        return ResponseEntity.ok(aiSecurityService.createRule(rule));
    }
}
