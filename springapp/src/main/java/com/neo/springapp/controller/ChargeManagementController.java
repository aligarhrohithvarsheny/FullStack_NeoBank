package com.neo.springapp.controller;

import com.neo.springapp.entity.ChargeTransaction;
import com.neo.springapp.entity.FundsAllocation;
import com.neo.springapp.service.ChargeManagementService;
import com.neo.springapp.service.ChargeManagementService.ChargesSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Charge Management Controller
 * Handles automatic debit from users and credit to allocation account
 * Charges: Interest, CIBIL, Soundbox, UPI, Payment Gateway
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*")
public class ChargeManagementController {
    
    @Autowired
    private ChargeManagementService chargeManagementService;
    
    // ==================== CHARGE PROCESSING ENDPOINTS ====================
    
    /**
     * Process a charge - DEBIT from user, CREDIT to account
     * POST /api/charges/process
     * Body: {
     *   allocationId, chargeType, chargeDescription, chargeAmount,
     *   userAccountNumber, userName, userProductType,
     *   linkedTransactionId, linkedLoanId, linkedDepositId
     * }
     * 
     * chargeType: INTEREST, CIBIL, SOUNDBOX, UPI, PAYMENT_GATEWAY, OTHER
     */
    @PostMapping("/charges/process")
    public ResponseEntity<?> processCharge(@RequestBody Map<String, Object> request) {
        try {
            ChargeTransaction charge = chargeManagementService.processCharge(
                Long.parseLong(request.get("allocationId").toString()),
                request.get("chargeType").toString(),
                request.get("chargeDescription").toString(),
                new BigDecimal(request.get("chargeAmount").toString()),
                request.get("userAccountNumber").toString(),
                request.get("userName").toString(),
                request.get("userProductType").toString(),
                request.get("linkedTransactionId").toString(),
                request.getOrDefault("linkedLoanId", "").toString(),
                request.getOrDefault("linkedDepositId", "").toString()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Charge processed and credited successfully",
                "chargeTransactionId", charge.getChargeTransactionId(),
                "chargeAmount", charge.getChargeAmount(),
                "netCreditAmount", charge.getNetCreditAmount(),
                "creditStatus", charge.getCreditStatus(),
                "creditReferenceNumber", charge.getCreditReferenceNumber()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Process multiple charges in batch
     * POST /api/charges/process-batch
     * Body: [
     *   { allocationId, chargeType, chargeDescription, chargeAmount, ... },
     *   { ... }
     * ]
     */
    @PostMapping("/charges/process-batch")
    public ResponseEntity<?> processChargeBatch(@RequestBody List<Map<String, Object>> chargesList) {
        try {
            int successCount = 0;
            int failureCount = 0;
            List<ChargeTransaction> processedCharges = new java.util.ArrayList<>();
            List<String> failureReasons = new java.util.ArrayList<>();
            
            for (Map<String, Object> chargeRequest : chargesList) {
                try {
                    ChargeTransaction charge = chargeManagementService.processCharge(
                        Long.parseLong(chargeRequest.get("allocationId").toString()),
                        chargeRequest.get("chargeType").toString(),
                        chargeRequest.get("chargeDescription").toString(),
                        new BigDecimal(chargeRequest.get("chargeAmount").toString()),
                        chargeRequest.get("userAccountNumber").toString(),
                        chargeRequest.get("userName").toString(),
                        chargeRequest.get("userProductType").toString(),
                        chargeRequest.get("linkedTransactionId").toString(),
                        chargeRequest.getOrDefault("linkedLoanId", "").toString(),
                        chargeRequest.getOrDefault("linkedDepositId", "").toString()
                    );
                    processedCharges.add(charge);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    failureReasons.add(chargeRequest.get("chargeTransactionId") + ": " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalProcessed", chargesList.size(),
                "successCount", successCount,
                "failureCount", failureCount,
                "processedCharges", processedCharges,
                "failureReasons", failureReasons
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    // ==================== CHARGE VIEWING ENDPOINTS ====================
    
    /**
     * Get charge history for an allocation
     * GET /api/charges/history/{allocationId}
     */
    @GetMapping("/charges/history/{allocationId}")
    public ResponseEntity<?> getChargeHistory(@PathVariable Long allocationId) {
        try {
            List<ChargeTransaction> charges = chargeManagementService.getChargeHistory(allocationId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "allocationId", allocationId,
                "totalCharges", charges.size(),
                "charges", charges
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get charges by type for an allocation
     * GET /api/charges/{allocationId}/type/{chargeType}
     * chargeType: INTEREST, CIBIL, SOUNDBOX, UPI, PAYMENT_GATEWAY, OTHER
     */
    @GetMapping("/charges/{allocationId}/type/{chargeType}")
    public ResponseEntity<?> getChargesByType(
            @PathVariable Long allocationId,
            @PathVariable String chargeType) {
        try {
            List<ChargeTransaction> charges = chargeManagementService.getChargesByType(allocationId, chargeType);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "allocationId", allocationId,
                "chargeType", chargeType,
                "count", charges.size(),
                "charges", charges
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get charge summary for dashboard (total by charge type)
     * GET /api/charges/summary/{allocationId}
     */
    @GetMapping("/charges/summary/{allocationId}")
    public ResponseEntity<?> getChargeSummary(@PathVariable Long allocationId) {
        try {
            ChargesSummary summary = chargeManagementService.getChargeSummary(allocationId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "summary", summary
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    // ==================== ADMIN ENDPOINTS ====================
    
    /**
     * ADMIN: Get pending credit transactions (for reconciliation)
     * GET /api/admin/charges/pending-credits
     */
    @GetMapping("/admin/charges/pending-credits")
    public ResponseEntity<?> getPendingCreditTransactions() {
        try {
            List<ChargeTransaction> charges = chargeManagementService.getPendingCreditTransactions();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", charges.size(),
                "charges", charges
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * ADMIN: Get failed transactions
     * GET /api/admin/charges/failed
     */
    @GetMapping("/admin/charges/failed")
    public ResponseEntity<?> getFailedTransactions() {
        try {
            List<ChargeTransaction> charges = chargeManagementService.getFailedTransactions();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", charges.size(),
                "charges", charges
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * ADMIN: Enable/Disable charge management for allocation
     * PUT /api/admin/allocation/{allocationId}/charge-management
     * Body: { enabled: true/false }
     */
    @PutMapping("/admin/allocation/{allocationId}/charge-management")
    public ResponseEntity<?> setChargeManagement(
            @PathVariable Long allocationId,
            @RequestBody Map<String, Object> request) {
        try {
            boolean enabled = Boolean.parseBoolean(request.get("enabled").toString());
            
            FundsAllocation allocation = chargeManagementService.setChargeManagement(allocationId, enabled);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", enabled ? "Charge management enabled" : "Charge management disabled",
                "allocationId", allocationId,
                "chargeManagementEnabled", allocation.getChargeManagementEnabled()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    // ==================== DASHBOARD ENDPOINTS ====================
    
    /**
     * HOD Dashboard: Get all charges collected across allocations
     * GET /api/hod/charges/total-collected
     */
    @GetMapping("/hod/charges/total-collected")
    public ResponseEntity<?> getTotalChargesCollected() {
        try {
            // This would aggregate charges from all allocations
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Total charges collected retrieved"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Manager Dashboard: Get charges for manager's allocation
     * GET /api/manager/charges/{managerId}
     */
    @GetMapping("/manager/charges/{managerId}")
    public ResponseEntity<?> getManagerCharges(@PathVariable Long managerId) {
        try {
            // This would get charges for all manager's allocations
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Manager charges retrieved"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Admin Dashboard: Charge management overview
     * GET /api/admin/charges/overview
     */
    @GetMapping("/admin/charges/overview")
    public ResponseEntity<?> getChargeOverview() {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Charge overview retrieved"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
