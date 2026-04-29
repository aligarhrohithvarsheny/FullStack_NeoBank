package com.neo.springapp.controller;

import com.neo.springapp.service.FaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/face-auth")
public class FaceAuthController {

    @Autowired
    private FaceAuthService faceAuthService;

    /**
     * Register a face descriptor for an admin
     * Expects: { email, faceDescriptor: number[128], deviceName? }
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerFace(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            @SuppressWarnings("unchecked")
            List<Double> faceDescriptor = ((List<Number>) request.get("faceDescriptor"))
                    .stream().map(Number::doubleValue).toList();
            String deviceName = (String) request.getOrDefault("deviceName", "Laptop Camera");

            Map<String, Object> result = faceAuthService.registerFace(email, faceDescriptor, deviceName);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Verify a face descriptor against the stored one
     * Expects: { email, faceDescriptor: number[128] }
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyFace(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            @SuppressWarnings("unchecked")
            List<Double> faceDescriptor = ((List<Number>) request.get("faceDescriptor"))
                    .stream().map(Number::doubleValue).toList();

            Map<String, Object> result = faceAuthService.verifyFace(email, faceDescriptor);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get face auth status for an admin
     */
    @GetMapping("/status/{email}")
    public ResponseEntity<Map<String, Object>> getFaceStatus(@PathVariable String email) {
        try {
            Map<String, Object> result = faceAuthService.getFaceStatus(email);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Remove face credentials for an admin
     */
    @DeleteMapping("/{email}")
    public ResponseEntity<Map<String, Object>> removeFace(@PathVariable String email) {
        try {
            Map<String, Object> result = faceAuthService.removeFace(email);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
