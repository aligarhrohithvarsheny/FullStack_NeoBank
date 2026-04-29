package com.neo.springapp.controller;

import com.neo.springapp.model.DepositRequest;
import com.neo.springapp.service.DepositRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deposit-requests")
public class DepositRequestController {

    private final DepositRequestService depositRequestService;

    public DepositRequestController(DepositRequestService depositRequestService) {
        this.depositRequestService = depositRequestService;
    }

    @PostMapping
    public ResponseEntity<?> createDepositRequest(@RequestBody DepositRequest request) {
        try {
            DepositRequest saved = depositRequestService.createRequest(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("request", saved);
            response.put("message", "Deposit request submitted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to submit deposit request: " + ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping
    public List<DepositRequest> getAllRequests(@RequestParam(required = false) String status) {
        return depositRequestService.getAll(status);
    }

    @GetMapping("/account/{accountNumber}")
    public List<DepositRequest> getRequestsForAccount(@PathVariable String accountNumber) {
        return depositRequestService.getByAccount(accountNumber);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable Long id,
                                            @RequestParam(required = false, defaultValue = "Admin") String processedBy) {
        try {
            DepositRequest approved = depositRequestService.approveRequest(id, processedBy);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("request", approved);
            response.put("message", "Deposit approved and amount credited");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to approve request: " + ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id,
                                           @RequestParam(required = false, defaultValue = "Admin") String processedBy,
                                           @RequestParam(required = false) String reason) {
        try {
            DepositRequest rejected = depositRequestService.rejectRequest(id, processedBy, reason);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("request", rejected);
            response.put("message", "Deposit request rejected");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to reject request: " + ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

