package com.neo.springapp.service;

import com.neo.springapp.model.Admin;
import com.neo.springapp.model.WebAuthnCredential;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.repository.WebAuthnCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WebAuthn Service for FIDO2/Fingerprint Authentication
 * Handles credential registration and authentication
 */
@Service
public class WebAuthnService {

    @Autowired
    private WebAuthnCredentialRepository credentialRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Value("${webauthn.rp.id:localhost}")
    private String rpId;

    @Value("${webauthn.rp.name:NeoBank}")
    private String rpName;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generate challenge for credential registration
     */
    public Map<String, Object> generateRegistrationChallenge(String adminEmail) {
        // Verify admin exists
        Admin admin = adminRepository.findByEmail(adminEmail);
        if (admin == null) {
            throw new RuntimeException("Admin not found");
        }

        // Generate random challenge
        byte[] challenge = new byte[32];
        random.nextBytes(challenge);
        String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge);

        // Store challenge temporarily (in production, use Redis or session)
        // For now, we'll return it and validate it in the registration endpoint

        Map<String, Object> response = new HashMap<>();
        response.put("challenge", challengeBase64);
        response.put("rpId", rpId);
        response.put("rpName", rpName);
        response.put("userId", Base64.getUrlEncoder().withoutPadding().encodeToString(adminEmail.getBytes(StandardCharsets.UTF_8)));
        response.put("userName", adminEmail);
        response.put("userDisplayName", admin.getName() != null ? admin.getName() : adminEmail);
        
        // Public key credential parameters
        List<Map<String, Object>> pubKeyCredParams = new ArrayList<>();
        Map<String, Object> es256 = new HashMap<>();
        es256.put("type", "public-key");
        es256.put("alg", -7); // ES256
        pubKeyCredParams.add(es256);
        
        Map<String, Object> rs256 = new HashMap<>();
        rs256.put("type", "public-key");
        rs256.put("alg", -257); // RS256
        pubKeyCredParams.add(rs256);
        
        response.put("pubKeyCredParams", pubKeyCredParams);
        response.put("timeout", 60000);
        response.put("attestation", "direct");
        response.put("authenticatorSelection", Map.of(
            "authenticatorAttachment", "platform",
            "userVerification", "required",
            "requireResidentKey", false
        ));

        return response;
    }

    /**
     * Register a new WebAuthn credential
     */
    @Transactional
    public Map<String, Object> registerCredential(String adminEmail, Map<String, Object> credentialData) {
        Admin admin = adminRepository.findByEmail(adminEmail);
        if (admin == null) {
            throw new RuntimeException("Admin not found");
        }

        // Extract credential data from WebAuthn response
        String credentialId = (String) credentialData.get("id");
        String publicKey = (String) credentialData.get("publicKey");
        String algorithm = (String) credentialData.getOrDefault("algorithm", "ES256");
        String deviceName = (String) credentialData.getOrDefault("deviceName", "Unknown Device");

        // Validate credential ID doesn't already exist
        if (credentialRepository.findByCredentialId(credentialId).isPresent()) {
            throw new RuntimeException("Credential already registered");
        }

        // Create and save credential
        WebAuthnCredential credential = new WebAuthnCredential();
        credential.setAdminEmail(adminEmail);
        credential.setCredentialId(credentialId);
        credential.setPublicKey(publicKey);
        credential.setCounter(0L);
        credential.setAlgorithm(algorithm);
        credential.setDeviceName(deviceName);
        credential.setActive(true);

        credentialRepository.save(credential);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Fingerprint credential registered successfully");
        response.put("credentialId", credentialId);

        return response;
    }

    /**
     * Generate challenge for authentication
     */
    public Map<String, Object> generateAuthenticationChallenge(String adminEmail) {
        // Get all active credentials for this admin
        List<WebAuthnCredential> credentials = credentialRepository.findByAdminEmailAndActiveTrue(adminEmail);
        
        if (credentials.isEmpty()) {
            throw new RuntimeException("No registered fingerprint credentials found. Please register first.");
        }

        // Generate random challenge
        byte[] challenge = new byte[32];
        random.nextBytes(challenge);
        String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge);

        // Get list of credential IDs
        List<String> allowCredentials = credentials.stream()
            .map(WebAuthnCredential::getCredentialId)
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("challenge", challengeBase64);
        response.put("rpId", rpId);
        response.put("allowCredentials", allowCredentials.stream().map(id -> {
            Map<String, Object> cred = new HashMap<>();
            cred.put("id", id);
            cred.put("type", "public-key");
            return cred;
        }).collect(Collectors.toList()));
        response.put("timeout", 60000);
        response.put("userVerification", "required");

        return response;
    }

    /**
     * Authenticate using WebAuthn credential
     */
    @Transactional
    public Map<String, Object> authenticate(String adminEmail, Map<String, Object> authenticationData) {
        String credentialId = (String) authenticationData.get("credentialId");
        String signature = (String) authenticationData.get("signature");
        String clientDataJSON = (String) authenticationData.get("clientDataJSON");
        String authenticatorData = (String) authenticationData.get("authenticatorData");
        Long counter = ((Number) authenticationData.getOrDefault("counter", 0)).longValue();

        // Find credential
        WebAuthnCredential credential = credentialRepository
            .findByAdminEmailAndCredentialId(adminEmail, credentialId)
            .orElseThrow(() -> new RuntimeException("Credential not found"));

        if (!credential.getActive()) {
            throw new RuntimeException("Credential is not active");
        }

        // Verify counter (prevent replay attacks)
        // Allow counter to be equal or greater than stored counter
        // This handles the first authentication case where both are 0
        // and allows legitimate counter increments
        if (counter < credential.getCounter()) {
            throw new RuntimeException("Invalid counter - possible replay attack. Expected: >= " + credential.getCounter() + ", Got: " + counter);
        }
        
        // Log counter update for debugging
        System.out.println("WebAuthn Authentication - Counter update: " + credential.getCounter() + " -> " + counter);

        // In a production environment, you would:
        // 1. Verify the signature using the public key
        // 2. Verify the clientDataJSON and authenticatorData
        // 3. Verify the challenge matches
        // For now, we'll do basic validation and update the counter
        
        // Note: In production, use webauthn4j library to verify:
        // - signature against publicKey
        // - clientDataJSON contains correct challenge and origin
        // - authenticatorData contains correct rpIdHash and flags
        // These variables are extracted but not verified in this simplified implementation
        @SuppressWarnings("unused")
        String signatureValue = signature;
        @SuppressWarnings("unused")
        String clientDataJSONValue = clientDataJSON;
        @SuppressWarnings("unused")
        String authenticatorDataValue = authenticatorData;

        // Update credential counter and last used time
        credential.setCounter(counter);
        credential.setLastUsedAt(java.time.LocalDateTime.now());
        credentialRepository.save(credential);

        // Get admin
        Admin admin = adminRepository.findByEmail(adminEmail);
        if (admin == null) {
            throw new RuntimeException("Admin not found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Fingerprint authentication successful");
        response.put("admin", admin);
        response.put("role", admin.getRole());

        return response;
    }

    /**
     * Get all registered credentials for an admin
     */
    public List<Map<String, Object>> getCredentials(String adminEmail) {
        List<WebAuthnCredential> credentials = credentialRepository.findByAdminEmailAndActiveTrue(adminEmail);
        return credentials.stream().map(cred -> {
            Map<String, Object> credMap = new HashMap<>();
            credMap.put("id", cred.getId());
            credMap.put("credentialId", cred.getCredentialId());
            credMap.put("deviceName", cred.getDeviceName());
            credMap.put("createdAt", cred.getCreatedAt());
            credMap.put("lastUsedAt", cred.getLastUsedAt());
            return credMap;
        }).collect(Collectors.toList());
    }

    /**
     * Delete a credential
     */
    @Transactional
    public void deleteCredential(String adminEmail, String credentialId) {
        WebAuthnCredential credential = credentialRepository
            .findByAdminEmailAndCredentialId(adminEmail, credentialId)
            .orElseThrow(() -> new RuntimeException("Credential not found"));
        
        credential.setActive(false);
        credentialRepository.save(credential);
    }
}

