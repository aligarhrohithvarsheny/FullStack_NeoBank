package com.neo.springapp.controller;

import com.neo.springapp.model.FraudAlert;
import com.neo.springapp.service.FraudAlertService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Fraud alerts API for manager dashboard: list, filter, and update status (review/dismiss).
 */
@RestController
@RequestMapping("/api/fraud-alerts")
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    public FraudAlertController(FraudAlertService fraudAlertService) {
        this.fraudAlertService = fraudAlertService;
    }

    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<FraudAlert> alerts = fraudAlertService.findAllPendingReview(page, size);
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("alerts", alerts.getContent());
        body.put("totalElements", alerts.getTotalElements());
        body.put("totalPages", alerts.getTotalPages());
        body.put("pendingCount", fraudAlertService.countPendingReview());
        return ResponseEntity.ok(body);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAlertsWithFilters(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String sourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        FraudAlert.Status s = status != null && !status.isEmpty() ? FraudAlert.Status.valueOf(status) : null;
        FraudAlert.AlertType at = alertType != null && !alertType.isEmpty() ? FraudAlert.AlertType.valueOf(alertType) : null;
        FraudAlert.SourceType st = sourceType != null && !sourceType.isEmpty() ? FraudAlert.SourceType.valueOf(sourceType) : null;
        Page<FraudAlert> alerts = fraudAlertService.findWithFilters(s, at, st, page, size);
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("alerts", alerts.getContent());
        body.put("totalElements", alerts.getTotalElements());
        body.put("totalPages", alerts.getTotalPages());
        body.put("pendingCount", fraudAlertService.countPendingReview());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/count-pending")
    public ResponseEntity<Map<String, Object>> getPendingCount() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("pendingCount", fraudAlertService.countPendingReview());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAlertById(@PathVariable Long id) {
        return fraudAlertService.findById(id)
                .map(alert -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("success", true);
                    body.put("alert", alert);
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String statusStr = payload.get("status");
        String reviewedBy = payload.get("reviewedBy");
        String reviewNotes = payload.get("reviewNotes");
        if (statusStr == null || statusStr.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "status is required");
            return ResponseEntity.badRequest().body(err);
        }
        FraudAlert.Status status = FraudAlert.Status.valueOf(statusStr);
        FraudAlert updated = fraudAlertService.updateStatus(id, status, reviewedBy != null ? reviewedBy : "", reviewNotes != null ? reviewNotes : "");
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("alert", updated);
        return ResponseEntity.ok(body);
    }

    /** Scan text for phishing indicators (keywords, suspicious links). Used by user/admin/manager. */
    @PostMapping("/scan-phishing")
    public ResponseEntity<Map<String, Object>> scanPhishing(@RequestBody Map<String, Object> payload) {
        String text = (String) payload.get("text");
        String sourceId = (String) payload.get("sourceId");
        String clientIp = (String) payload.get("clientIp");
        if (text == null || text.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "text is required");
            return ResponseEntity.badRequest().body(err);
        }
        boolean suspicious = false;
        StringBuilder reasons = new StringBuilder();
        String lower = text.toLowerCase();
        // Simple rule-based phishing detection (can be replaced with ML/NLP later)
        if (lower.contains("urgent") && (lower.contains("password") || lower.contains("verify") || lower.contains("click"))) {
            suspicious = true;
            reasons.append("Urgency + credential/verify pattern. ");
        }
        if (lower.contains("verify your account") || lower.contains("confirm your identity")) {
            suspicious = true;
            reasons.append("Account verification demand. ");
        }
        if (lower.contains("http://") || lower.matches(".*https?://[^\\s]+.*")) {
            if (lower.contains("bit.ly") || lower.contains("tinyurl") || lower.contains("redirect")) {
                suspicious = true;
                reasons.append("Short/redirect URL. ");
            }
        }
        if (lower.contains("otp") && lower.contains("send") && !lower.contains("we have sent")) {
            suspicious = true;
            reasons.append("Possible OTP phishing. ");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("suspicious", suspicious);
        body.put("reasons", reasons.toString());
        if (suspicious && sourceId != null) {
            String detailsJson = "{\"textPreview\":\"" + text.substring(0, Math.min(500, text.length())).replace("\"", "'") + "\",\"reasons\":\"" + reasons.toString().replace("\"", "'") + "\"}";
            fraudAlertService.recordPhishingSuspect(sourceId, "Suspicious message/email content", reasons.toString(), detailsJson, clientIp != null ? clientIp : "");
        }
        return ResponseEntity.ok(body);
    }

    /** Submit behavioral biometrics (typing speed, mouse movements). If deviation from baseline, create alert. */
    @PostMapping("/behavioral")
    public ResponseEntity<Map<String, Object>> submitBehavioral(@RequestBody Map<String, Object> payload) {
        String email = (String) payload.get("email");
        Double typingSpeedWpm = payload.get("typingSpeedWpm") != null ? ((Number) payload.get("typingSpeedWpm")).doubleValue() : null;
        Double mouseMovementScore = payload.get("mouseMovementScore") != null ? ((Number) payload.get("mouseMovementScore")).doubleValue() : null;
        String clientIp = (String) payload.get("clientIp");
        String deviceInfo = (String) payload.get("deviceInfo");
        if (email == null || email.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "email is required");
            return ResponseEntity.badRequest().body(err);
        }
        // Simple heuristic: very low typing speed or zero mouse movement can indicate bot/script
        boolean anomaly = false;
        StringBuilder reasons = new StringBuilder();
        if (typingSpeedWpm != null && typingSpeedWpm < 10 && typingSpeedWpm > 0) {
            anomaly = true;
            reasons.append("Unusually low typing speed. ");
        }
        if (typingSpeedWpm != null && typingSpeedWpm > 200) {
            anomaly = true;
            reasons.append("Unusually high typing speed (possible automation). ");
        }
        if (mouseMovementScore != null && mouseMovementScore == 0) {
            anomaly = true;
            reasons.append("No mouse movement (possible headless/automation). ");
        }
        if (anomaly && reasons.length() > 0) {
            String detailsJson = String.format("{\"typingSpeedWpm\":%s,\"mouseMovementScore\":%s,\"reasons\":\"%s\"}",
                    typingSpeedWpm != null ? typingSpeedWpm : "null",
                    mouseMovementScore != null ? mouseMovementScore : "null",
                    reasons.toString().replace("\"", "'"));
            fraudAlertService.recordBehavioralAnomaly(email, "Behavioral biometric deviation", reasons.toString(), detailsJson, clientIp != null ? clientIp : "", deviceInfo != null ? deviceInfo : "");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("anomalyDetected", anomaly);
        body.put("reasons", reasons.toString());
        return ResponseEntity.ok(body);
    }

    /** Report KYC document fraud (alteration, forgery, inconsistency). Call from admin when rejecting KYC. */
    @PostMapping("/report-kyc-fraud")
    public ResponseEntity<Map<String, Object>> reportKycDocumentFraud(@RequestBody Map<String, Object> payload) {
        String userAccountNumber = (String) payload.get("userAccountNumber");
        String userName = (String) payload.get("userName");
        String title = (String) payload.get("title");
        String description = (String) payload.get("description");
        String detailsJson = (String) payload.get("detailsJson");
        if (userAccountNumber == null || userAccountNumber.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "userAccountNumber is required");
            return ResponseEntity.badRequest().body(err);
        }
        FraudAlert a = fraudAlertService.recordKycDocumentFraud(
                userAccountNumber,
                userName != null ? userName : userAccountNumber,
                title != null ? title : "KYC document suspected fraud",
                description != null ? description : "Document verification flagged: possible alteration or forgery.",
                detailsJson != null ? detailsJson : "{}");
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("alert", a);
        return ResponseEntity.ok(body);
    }
}
