package com.neo.springapp.controller;

import com.neo.springapp.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller for managing allocation fund utilization.
 * Handles debit/credit transactions against allocations.
 * Branch Managers use these endpoints to record fund usage from their allocations.
 * 
 * Base URL: /api/manager/utilization
 */
@RestController
@RequestMapping("/api/manager/utilization")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
@SuppressWarnings("null")
public class AllocationUtilizationController {

    @Autowired
    private AllocationUtilizationService utilizationService;

    @Autowired
    private AllocationValidatorService validatorService;

    /**
     * Record a debit transaction against an allocation.
     * Called when funds are used from allocation (Gold Loan disbursed, Withdrawal made, etc).
     * 
     * POST /api/manager/utilization/debit
     * 
     * Request Body:
     * {
     *   "allocationId": 1,
     *   "amount": 50000,
     *   "productType": "GOLD_LOAN",
     *   "userAccountNumber": "ACC123456",
     *   "userName": "John Doe",
     *   "linkedTransactionId": "TXN-2025-12345",
     *   "description": "Gold Loan Disbursement",
     *   "performedByAdminId": 5
     * }
     */
    @PostMapping("/debit")
    public ResponseEntity<?> debitAllocationFunds(@RequestBody Map<String, Object> request) {
        try {
            Long allocationId = Long.parseLong(request.get("allocationId").toString());
            Double amount = Double.parseDouble(request.get("amount").toString());
            String productType = (String) request.get("productType");
            String userAccountNumber = (String) request.get("userAccountNumber");
            String userName = (String) request.get("userName");
            String linkedTransactionId = (String) request.get("linkedTransactionId");
            String description = (String) request.get("description");
            Long performedByAdminId = Long.parseLong(request.get("performedByAdminId").toString());

            // Validate debit transaction
            Map<String, Object> validation = validatorService.validateDebitTransaction(
                allocationId, amount, productType, userAccountNumber
            );
            if (!((Boolean) validation.get("isValid"))) {
                return ResponseEntity.badRequest().body(validation);
            }

            // Process debit
            Map<String, Object> result = utilizationService.debitAllocationFunds(
                allocationId, amount, productType, userAccountNumber, userName,
                linkedTransactionId, description, performedByAdminId
            );

            if ((Boolean) result.get("success")) {
                return ResponseEntity.status(HttpStatus.CREATED).body(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error debiting allocation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Record a credit transaction against an allocation.
     * Called when funds are returned to allocation (Loan early repayment, Deposit reversal, etc).
     * 
     * POST /api/manager/utilization/credit
     * 
     * Request Body:
     * {
     *   "allocationId": 1,
     *   "amount": 10000,
     *   "productType": "LOAN",
     *   "userAccountNumber": "ACC123456",
     *   "userName": "John Doe",
     *   "linkedTransactionId": "TXN-2025-12346",
     *   "description": "Loan Early Repayment",
     *   "performedByAdminId": 5
     * }
     */
    @PostMapping("/credit")
    public ResponseEntity<?> creditAllocationFunds(@RequestBody Map<String, Object> request) {
        try {
            Long allocationId = Long.parseLong(request.get("allocationId").toString());
            Double amount = Double.parseDouble(request.get("amount").toString());
            String productType = (String) request.get("productType");
            String userAccountNumber = (String) request.get("userAccountNumber");
            String userName = (String) request.get("userName");
            String linkedTransactionId = (String) request.get("linkedTransactionId");
            String description = (String) request.get("description");
            Long performedByAdminId = Long.parseLong(request.get("performedByAdminId").toString());

            // Process credit
            Map<String, Object> result = utilizationService.creditAllocationFunds(
                allocationId, amount, productType, userAccountNumber, userName,
                linkedTransactionId, description, performedByAdminId
            );

            if ((Boolean) result.get("success")) {
                return ResponseEntity.status(HttpStatus.CREATED).body(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error crediting allocation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get utilization history for an allocation.
     * GET /api/manager/utilization/{allocationId}/history?page=0&size=20
     * 
     * Returns paginated list of all debit/credit transactions.
     */
    @GetMapping("/{allocationId}/history")
    public ResponseEntity<?> getUtilizationHistory(
            @PathVariable Long allocationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Map<String, Object> result = utilizationService.getUtilizationHistory(allocationId, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching history: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get utilization filtered by product type.
     * GET /api/manager/utilization/{allocationId}/by-product?productType=GOLD_LOAN&page=0&size=20
     */
    @GetMapping("/{allocationId}/by-product")
    public ResponseEntity<?> getUtilizationByProduct(
            @PathVariable Long allocationId,
            @RequestParam String productType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Map<String, Object> result = utilizationService.getUtilizationByProduct(allocationId, productType, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching product utilization: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get debit transactions only.
     * GET /api/manager/utilization/{allocationId}/debits?page=0&size=20
     */
    @GetMapping("/{allocationId}/debits")
    public ResponseEntity<?> getDebitTransactions(
            @PathVariable Long allocationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Map<String, Object> result = utilizationService.getDebitTransactions(allocationId, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching debits: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get credit transactions only.
     * GET /api/manager/utilization/{allocationId}/credits?page=0&size=20
     */
    @GetMapping("/{allocationId}/credits")
    public ResponseEntity<?> getCreditTransactions(
            @PathVariable Long allocationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Map<String, Object> result = utilizationService.getCreditTransactions(allocationId, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching credits: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get product type summary - breakdown of usage by product.
     * GET /api/manager/utilization/{allocationId}/product-summary
     * 
     * Returns total debited/credited by product type with counts.
     */
    @GetMapping("/{allocationId}/product-summary")
    public ResponseEntity<?> getProductTypeSummary(@PathVariable Long allocationId) {
        try {
            Map<String, Object> result = utilizationService.getProductTypeSummary(allocationId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching product summary: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Pre-debit validation to check if debit can proceed.
     * POST /api/manager/utilization/validate-debit
     * 
     * Request Body:
     * {
     *   "allocationId": 1,
     *   "amount": 50000
     * }
     */
    @PostMapping("/validate-debit")
    public ResponseEntity<?> preDebitValidation(@RequestBody Map<String, Object> request) {
        try {
            Long allocationId = Long.parseLong(request.get("allocationId").toString());
            Double amount = Double.parseDouble(request.get("amount").toString());

            Map<String, Object> result = validatorService.preDebitValidationReport(allocationId, amount);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error validating debit: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
