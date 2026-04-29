package com.neo.springapp.controller;

import com.neo.springapp.model.DocumentVerification;
import com.neo.springapp.service.DocumentVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/document-verification")
@CrossOrigin(origins = "*")
public class DocumentVerificationController {

    @Autowired
    private DocumentVerificationService documentVerificationService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitDocument(@RequestBody DocumentVerification doc) {
        Map<String, Object> response = new HashMap<>();
        try {
            DocumentVerification submitted = documentVerificationService.submitDocument(doc);
            response.put("success", true);
            response.put("message", "Document submitted for verification");
            response.put("document", submitted);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error submitting document: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getByAccountNumber(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("documents", documentVerificationService.getByAccountNumber(accountNumber));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPending() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("documents", documentVerificationService.getPendingDocuments());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getByStatus(@PathVariable String status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("documents", documentVerificationService.getByStatus(status));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<DocumentVerification> doc = documentVerificationService.getById(id);
        if (doc.isPresent()) {
            response.put("success", true);
            response.put("document", doc.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Document not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyDocument(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String verifiedBy = body.get("verifiedBy");
        String remarks = body.get("remarks");
        DocumentVerification doc = documentVerificationService.verifyDocument(id, verifiedBy, remarks);
        if (doc != null) {
            response.put("success", true);
            response.put("message", "Document verified successfully");
            response.put("document", doc);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Document not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectDocument(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String verifiedBy = body.get("verifiedBy");
        String rejectionReason = body.get("rejectionReason");
        DocumentVerification doc = documentVerificationService.rejectDocument(id, verifiedBy, rejectionReason);
        if (doc != null) {
            response.put("success", true);
            response.put("message", "Document rejected");
            response.put("document", doc);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Document not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("pendingCount", documentVerificationService.getPendingCount());
        response.put("verifiedCount", documentVerificationService.getVerifiedCount());
        response.put("rejectedCount", documentVerificationService.getRejectedCount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("documents", documentVerificationService.getAll());
        return ResponseEntity.ok(response);
    }
}
