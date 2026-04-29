package com.neo.springapp.service;

import com.neo.springapp.model.Admin;
import com.neo.springapp.model.FaceAuthCredential;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.repository.FaceAuthRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Face Authentication Service
 * Handles face descriptor registration, verification via Euclidean distance comparison
 */
@Service
public class FaceAuthService {

    @Autowired
    private FaceAuthRepository faceAuthRepository;

    @Autowired
    private AdminRepository adminRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Euclidean distance threshold for face matching
    // Lower = stricter matching. 0.45 ensures only the registered face passes.
    private static final double MATCH_THRESHOLD = 0.45;

    /**
     * Register a face descriptor for an admin
     */
    @Transactional
    public Map<String, Object> registerFace(String adminEmail, List<Double> faceDescriptor, String deviceName) {
        Admin admin = adminRepository.findByEmail(adminEmail);
        if (admin == null) {
            throw new RuntimeException("Admin not found");
        }

        if (faceDescriptor == null || faceDescriptor.size() != 128) {
            throw new RuntimeException("Invalid face descriptor: must be a 128-dimension float array");
        }

        // Deactivate any existing face credentials for this admin
        List<FaceAuthCredential> existing = faceAuthRepository.findByAdminEmailAndActiveTrue(adminEmail);
        for (FaceAuthCredential cred : existing) {
            cred.setActive(false);
        }
        if (!existing.isEmpty()) {
            faceAuthRepository.saveAll(existing);
        }

        // Save new face credential
        FaceAuthCredential credential = new FaceAuthCredential();
        credential.setAdminEmail(adminEmail);
        try {
            credential.setFaceDescriptor(objectMapper.writeValueAsString(faceDescriptor));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize face descriptor");
        }
        credential.setDeviceName(deviceName != null ? deviceName : "Laptop Camera");
        credential.setActive(true);

        faceAuthRepository.save(credential);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Face ID registered successfully");
        return response;
    }

    /**
     * Verify a face descriptor against the stored one for an admin
     */
    @Transactional
    public Map<String, Object> verifyFace(String adminEmail, List<Double> faceDescriptor) {
        Admin admin = adminRepository.findByEmail(adminEmail);
        if (admin == null) {
            throw new RuntimeException("Admin not found");
        }

        // Check account lock
        if (admin.getAccountLocked() != null && admin.getAccountLocked()) {
            Map<String, Object> lockedResponse = new HashMap<>();
            lockedResponse.put("success", false);
            lockedResponse.put("accountLocked", true);
            lockedResponse.put("message", "Account is locked due to failed login attempts. Contact manager to unlock.");
            return lockedResponse;
        }

        if (faceDescriptor == null || faceDescriptor.size() != 128) {
            throw new RuntimeException("Invalid face descriptor");
        }

        Optional<FaceAuthCredential> credOpt = faceAuthRepository.findFirstByAdminEmailAndActiveTrue(adminEmail);
        if (credOpt.isEmpty()) {
            throw new RuntimeException("No Face ID registered. Please register from the Admin Dashboard.");
        }

        FaceAuthCredential credential = credOpt.get();
        List<Double> storedDescriptor;
        try {
            storedDescriptor = objectMapper.readValue(credential.getFaceDescriptor(), new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to read stored face descriptor");
        }

        // Compute Euclidean distance between descriptors
        double distance = euclideanDistance(faceDescriptor, storedDescriptor);
        System.out.println("Face Auth - Euclidean distance: " + distance + " (threshold: " + MATCH_THRESHOLD + ")");

        if (distance < MATCH_THRESHOLD) {
            // Face matched — successful authentication
            credential.setLastUsedAt(LocalDateTime.now());
            faceAuthRepository.save(credential);

            // Reset failed login attempts
            if (admin.getFailedLoginAttempts() != null && admin.getFailedLoginAttempts() > 0) {
                admin.setFailedLoginAttempts(0);
                admin.setLastFailedLoginTime(null);
                admin.setAccountLocked(false);
                adminRepository.save(admin);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Face ID authentication successful");
            response.put("admin", admin);
            response.put("role", admin.getRole());
            response.put("distance", distance);
            return response;
        } else {
            // Face did not match
            int currentAttempts = admin.getFailedLoginAttempts() != null ? admin.getFailedLoginAttempts() : 0;
            admin.setFailedLoginAttempts(currentAttempts + 1);
            admin.setLastFailedLoginTime(LocalDateTime.now());
            if (admin.getFailedLoginAttempts() >= 3) {
                admin.setAccountLocked(true);
            }
            adminRepository.save(admin);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Face does not match. Please try again.");
            response.put("distance", distance);
            if (admin.getAccountLocked() != null && admin.getAccountLocked()) {
                response.put("accountLocked", true);
                response.put("message", "Account locked due to 3 failed attempts. Contact manager to unlock.");
            }
            return response;
        }
    }

    /**
     * Get face auth status for an admin
     */
    public Map<String, Object> getFaceStatus(String adminEmail) {
        List<FaceAuthCredential> credentials = faceAuthRepository.findByAdminEmailAndActiveTrue(adminEmail);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("registered", !credentials.isEmpty());
        if (!credentials.isEmpty()) {
            FaceAuthCredential cred = credentials.get(0);
            response.put("deviceName", cred.getDeviceName());
            response.put("createdAt", cred.getCreatedAt());
            response.put("lastUsedAt", cred.getLastUsedAt());
        }
        response.put("credentials", credentials.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("deviceName", c.getDeviceName());
            m.put("createdAt", c.getCreatedAt());
            m.put("lastUsedAt", c.getLastUsedAt());
            m.put("active", c.getActive());
            return m;
        }).collect(Collectors.toList()));
        return response;
    }

    /**
     * Remove face credentials for an admin
     */
    @Transactional
    public Map<String, Object> removeFace(String adminEmail) {
        List<FaceAuthCredential> credentials = faceAuthRepository.findByAdminEmailAndActiveTrue(adminEmail);
        for (FaceAuthCredential cred : credentials) {
            cred.setActive(false);
        }
        if (!credentials.isEmpty()) {
            faceAuthRepository.saveAll(credentials);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Face ID removed successfully");
        return response;
    }

    /**
     * Compute Euclidean distance between two 128-dim float arrays
     */
    private double euclideanDistance(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            throw new IllegalArgumentException("Descriptor dimensions must match");
        }
        double sum = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double diff = a.get(i) - b.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
