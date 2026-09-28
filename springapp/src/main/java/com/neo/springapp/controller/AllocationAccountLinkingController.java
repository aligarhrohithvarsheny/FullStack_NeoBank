package com.neo.springapp.controller;

import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.service.AllocationAccountLinkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Account Linking Controller
 * HOD Dashboard APIs for linking and verifying bank accounts
 * Manager Dashboard APIs for viewing account details
 * Admin Dashboard APIs for verification and management
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*")
public class AllocationAccountLinkingController {
    
    @Autowired
    private AllocationAccountLinkingService accountLinkingService;

    @GetMapping("/verify/ifsc/{ifscCode}")
    public ResponseEntity<?> verifyInternalIfsc(@PathVariable String ifscCode) {
        try {
            return ResponseEntity.ok(accountLinkingService.verifyIfscCode(ifscCode));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", exception.getMessage()));
        }
    }

    @GetMapping("/verify/account")
    public ResponseEntity<?> verifyInternalAccount(
            @RequestParam String accountNumber,
            @RequestParam String ifscCode) {
        try {
            return ResponseEntity.ok(accountLinkingService.verifyInternalAccount(accountNumber, ifscCode));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", exception.getMessage()));
        }
    }
    
    // ==================== HOD DASHBOARD ENDPOINTS ====================
    
    /**
     * HOD: Create and link account to allocation
     * POST /api/hod/link-account
     * Body: {
     *   allocationId, accountNumber, ifscCode, accountHolderName,
     *   bankName, accountType, branchName, city, location, state,
     *   adminId, adminName
     * }
     */
    @PostMapping("/hod/link-account")
    public ResponseEntity<?> linkAccountToAllocation(@RequestBody Map<String, Object> request) {
        try {
            AllocationAccount account = accountLinkingService.createAndLinkAccount(
                Long.parseLong(request.get("allocationId").toString()),
                request.get("accountNumber").toString(),
                request.get("ifscCode").toString(),
                request.get("accountHolderName").toString(),
                request.get("bankName").toString(),
                request.get("accountType").toString(),
                request.get("branchName").toString(),
                request.get("city").toString(),
                request.get("location").toString(),
                request.get("state").toString(),
                Long.parseLong(request.get("adminId").toString()),
                request.get("adminName").toString()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account linked successfully. Awaiting cheque verification.");
            response.put("account", account);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * HOD: Add cheque details for verification
     * POST /api/hod/add-cheque-details
     * Body: {
     *   allocationId, chequeNumber, chequeHolderName, chequeDate,
     *   chequeBank, chequeImageUrl
     * }
     */
    @PostMapping("/hod/add-cheque-details")
    public ResponseEntity<?> addChequeDetails(@RequestBody Map<String, Object> request) {
        try {
            AllocationAccount account = accountLinkingService.updateChequeDetails(
                Long.parseLong(request.get("allocationId").toString()),
                request.get("chequeNumber").toString(),
                request.get("chequeHolderName").toString(),
                LocalDate.parse(request.get("chequeDate").toString()),
                request.get("chequeBank").toString(),
                request.get("chequeImageUrl").toString()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cheque details added. Submitted for verification.",
                "account", account
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
    * HOD: Get linked accounts by city, optionally narrowed to a branch
    * GET /api/hod/accounts?city=Mumbai&branch=Marine-Lines
     */
    @GetMapping("/hod/accounts")
    public ResponseEntity<?> getAccountsByLocation(
            @RequestParam String city,
            @RequestParam(required = false) String branch) {
        try {
            List<AllocationAccount> accounts = accountLinkingService.getAccountsByLocationAndBranch(city, branch);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", accounts.size(),
                "accounts", accounts
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/hod/account/{allocationId}")
    public ResponseEntity<?> getHodAccountDetails(@PathVariable Long allocationId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "account", accountLinkingService.getAccountDetails(allocationId)));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", exception.getMessage()));
        }
    }
    
    /**
     * HOD: Get pending verification accounts (for review)
     * GET /api/hod/pending-verification-accounts
     */
    @GetMapping("/hod/pending-verification-accounts")
    public ResponseEntity<?> getPendingVerificationAccounts() {
        try {
            List<AllocationAccount> accounts = accountLinkingService.getPendingVerificationAccounts();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", accounts.size(),
                "accounts", accounts
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/admin/pending-cheques")
    public ResponseEntity<?> getPendingCheques() {
        try {
            List<AllocationAccount> accounts = accountLinkingService.getPendingVerificationAccounts();
            return ResponseEntity.ok(Map.of("success", true, "count", accounts.size(), "cheques", accounts));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", exception.getMessage()));
        }
    }

    // ==================== ADMIN DASHBOARD ENDPOINTS ====================
    
    /**
     * ADMIN: Verify/Reject account linking via cheque
     * POST /api/admin/verify-account
     * Body: {
     *   allocationId, approved, verificationNotes, verifiedByAdminId, verifiedByAdminName
     * }
     */
    @PostMapping("/admin/verify-account")
    public ResponseEntity<?> verifyAccount(@RequestBody Map<String, Object> request) {
        try {
            String allocationIdValue = requiredValue(request, "allocationId");
            String adminIdValue = requiredValue(request, "verifiedByAdminId");
            Object approvedValue = request.get("approved");
            if (approvedValue == null) {
                throw new IllegalArgumentException("Approval decision is required.");
            }
            boolean approved = approvedValue instanceof Boolean
                ? (Boolean) approvedValue
                : Boolean.parseBoolean(approvedValue.toString());
            String verificationNotes = request.get("verificationNotes") == null
                ? "" : request.get("verificationNotes").toString().trim();
            Long adminId = Long.parseLong(adminIdValue);
            
            AllocationAccount account = accountLinkingService.verifyChequeAndAccount(
                Long.parseLong(allocationIdValue),
                approved,
                verificationNotes,
                adminId,
                null
            );
            
            String message = approved ? 
                "Account verified successfully! Now ready to use." : 
                "Account verification rejected.";
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", message,
                "account", account
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    private String requiredValue(Map<String, Object> request, String fieldName) {
        Object value = request.get(fieldName);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.toString().trim();
    }
    
    /**
     * ADMIN: Get all verified accounts
     * GET /api/admin/verified-accounts
     */
    @GetMapping("/admin/verified-accounts")
    public ResponseEntity<?> getVerifiedAccounts() {
        try {
            List<AllocationAccount> accounts = accountLinkingService.getVerifiedAccounts();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", accounts.size(),
                "accounts", accounts
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * ADMIN: Update account status (ACTIVE, BLOCKED, INACTIVE)
     * PUT /api/admin/account/{accountId}/status
     * Body: {
     *   status, reason
     * }
     */
    @PutMapping("/admin/account/{accountId}/status")
    public ResponseEntity<?> updateAccountStatus(
            @PathVariable Long accountId,
            @RequestBody Map<String, Object> request) {
        try {
            AllocationAccount account = accountLinkingService.updateAccountStatus(
                accountId,
                request.get("status").toString(),
                request.get("reason").toString()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Account status updated.",
                "account", account
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    // ==================== MANAGER DASHBOARD ENDPOINTS ====================
    
    /**
     * MANAGER: Get linked account details for dashboard
     * GET /api/manager/account/{allocationId}
     */
    @GetMapping("/manager/account/{allocationId}")
    public ResponseEntity<?> getAccountDetails(@PathVariable Long allocationId) {
        try {
            AllocationAccount account = accountLinkingService.getAccountDetails(allocationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("account", account);
            response.put("accountStatus", account.getAccountStatus());
            response.put("verificationStatus", account.getVerificationStatus());
            response.put("currentBalance", account.getCurrentBalance());
            response.put("totalAllocated", account.getTotalAllocated());
            response.put("totalDebited", account.getTotalDebited());
            response.put("totalCredited", account.getTotalCredited());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * MANAGER: Verify if account is ready to use
     * GET /api/manager/is-account-active/{allocationId}
     */
    @GetMapping("/manager/is-account-active/{allocationId}")
    public ResponseEntity<?> isAccountActive(@PathVariable Long allocationId) {
        try {
            boolean isActive = accountLinkingService.isAccountVerifiedAndActive(allocationId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "isActive", isActive,
                "message", isActive ? 
                    "Account is verified and active. Ready to use." : 
                    "Account is not verified yet."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * MANAGER: Get all allocations for manager with account details
     * GET /api/manager/allocations/{managerId}
     */
    @GetMapping("/manager/allocations/{managerId}")
    public ResponseEntity<?> getManagerAllocations(@PathVariable Long managerId) {
        try {
            // This would fetch from AllocationRepository findByManagerId
            // and include account linking status
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Manager allocations retrieved"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
