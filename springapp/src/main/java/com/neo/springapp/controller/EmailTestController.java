package com.neo.springapp.controller;

import com.neo.springapp.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Smoke-test OTP delivery (Gmail API when configured, otherwise SMTP / dev fallback).
 */
@RestController
@RequestMapping("/api")
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * POST /api/test-email
     * Body: { "to": "you@example.com" }
     */
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, Object>> testEmail(@RequestBody Map<String, String> body) {
        Map<String, Object> out = new LinkedHashMap<>();
        String to = body != null ? body.get("to") : null;
        if (to == null || to.isBlank()) {
            out.put("ok", false);
            out.put("message", "Field \"to\" is required.");
            return ResponseEntity.badRequest().body(out);
        }
        String normalized = to.trim().toLowerCase();
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        boolean sent = emailService.sendOtpEmail(normalized, otp);
        out.put("ok", sent);
        out.put("email", normalized);
        out.put("message", sent ? "OTP email dispatched." : "Failed to send OTP email.");
        return ResponseEntity.ok(out);
    }
}
