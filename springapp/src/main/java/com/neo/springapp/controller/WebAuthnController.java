package com.neo.springapp.controller;

import com.neo.springapp.service.WebAuthnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/webauthn")
public class WebAuthnController {

    @Autowired
    private WebAuthnService webauthnService;

    /**
     * Generate registration challenge for registering a new fingerprint credential
     */
    @PostMapping("/register/challenge")
    public ResponseEntity<Map<String, Object>> generateRegistrationChallenge(@RequestBody Map<String, String> request) {
        try {
            String adminEmail = request.get("email");
            if (adminEmail == null || adminEmail.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Email is required");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> challenge = webauthnService.generateRegistrationChallenge(adminEmail);
            challenge.put("success", true);
            return ResponseEntity.ok(challenge);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Register a new fingerprint credential
     */
    @PostMapping("/register")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> registerCredential(@RequestBody Map<String, Object> request) {
        try {
            String adminEmail = (String) request.get("email");
            Map<String, Object> credentialData = (Map<String, Object>) request.get("credential");

            if (adminEmail == null || credentialData == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Email and credential data are required");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> result = webauthnService.registerCredential(adminEmail, credentialData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Generate authentication challenge for fingerprint login
     */
    @PostMapping("/authenticate/challenge")
    public ResponseEntity<Map<String, Object>> generateAuthenticationChallenge(@RequestBody Map<String, String> request) {
        try {
            String adminEmail = request.get("email");
            if (adminEmail == null || adminEmail.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Email is required");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> challenge = webauthnService.generateAuthenticationChallenge(adminEmail);
            challenge.put("success", true);
            return ResponseEntity.ok(challenge);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Authenticate using fingerprint
     */
    @PostMapping("/authenticate")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> authenticate(@RequestBody Map<String, Object> request) {
        try {
            String adminEmail = (String) request.get("email");
            Map<String, Object> authenticationData = (Map<String, Object>) request.get("credential");

            if (adminEmail == null || authenticationData == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Email and credential data are required");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> result = webauthnService.authenticate(adminEmail, authenticationData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get all registered credentials for an admin
     */
    @GetMapping("/credentials/{email}")
    public ResponseEntity<Map<String, Object>> getCredentials(@PathVariable String email) {
        try {
            java.util.List<Map<String, Object>> credentials = webauthnService.getCredentials(email);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("credentials", credentials);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Delete a credential
     */
    @DeleteMapping("/credentials/{email}/{credentialId}")
    public ResponseEntity<Map<String, Object>> deleteCredential(
            @PathVariable String email,
            @PathVariable String credentialId) {
        try {
            webauthnService.deleteCredential(email, credentialId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Credential deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

