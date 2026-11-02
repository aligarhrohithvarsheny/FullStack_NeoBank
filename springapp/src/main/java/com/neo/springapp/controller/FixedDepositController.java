package com.neo.springapp.controller;

import com.neo.springapp.model.FixedDeposit;
import com.neo.springapp.model.DepositManagementHistory;
import com.neo.springapp.service.FixedDepositService;
import com.neo.springapp.service.DepositManagementHistoryService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fixed-deposits")
public class FixedDepositController {

    @Autowired
    private FixedDepositService fixedDepositService;

    @Autowired
    private DepositManagementHistoryService depositHistoryService;

    // Create new FD application
    @PostMapping
    public ResponseEntity<?> createFixedDeposit(@RequestBody FixedDeposit fixedDeposit,
                                                Authentication authentication) {
        SessionPrincipal principal = principal(authentication);
        if (principal == null || !"USER".equals(principal.scope())
                || !sameAccount(principal.accountNumber(), fixedDeposit.getAccountNumber())) {
            return unauthorized();
        }
        Map<String, Object> response = fixedDepositService.createFixedDeposit(fixedDeposit);
        return ResponseEntity.ok(response);
    }

    // Get all FDs (admin/manager)
    @GetMapping
    public ResponseEntity<?> getAllFixedDeposits(Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        List<FixedDeposit> fixedDeposits = fixedDepositService.getAllFixedDeposits();
        return ResponseEntity.ok(fixedDeposits);
    }

    // Get FDs by account number (user)
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<?> getFixedDepositsByAccountNumber(@PathVariable String accountNumber,
                                                              Authentication authentication) {
        SessionPrincipal principal = principal(authentication);
        if (!isStaff(authentication) && (principal == null || !"USER".equals(principal.scope())
                || !sameAccount(principal.accountNumber(), accountNumber))) return forbidden();
        List<FixedDeposit> fixedDeposits = fixedDepositService.getFixedDepositsByAccountNumber(accountNumber);
        return ResponseEntity.ok(fixedDeposits);
    }

    // Get FD by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getFixedDepositById(@PathVariable Long id, Authentication authentication) {
        FixedDeposit fd = fixedDepositService.getFixedDepositById(id).orElse(null);
        if (fd == null) return ResponseEntity.notFound().build();
        if (!canAccess(authentication, fd)) return forbidden();
        return ResponseEntity.ok(fd);
    }

    // Get FD by FD account number
    @GetMapping("/fd-account/{fdAccountNumber}")
    public ResponseEntity<?> getFixedDepositByFdAccountNumber(@PathVariable String fdAccountNumber,
                                                               Authentication authentication) {
        FixedDeposit fd = fixedDepositService.getFixedDepositByFdAccountNumber(fdAccountNumber).orElse(null);
        if (fd == null) return ResponseEntity.notFound().build();
        if (!canAccess(authentication, fd)) return forbidden();
        return ResponseEntity.ok(fd);
    }

    // Get pending FDs (admin)
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingFixedDeposits(Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        List<FixedDeposit> fixedDeposits = fixedDepositService.getPendingFixedDeposits();
        return ResponseEntity.ok(fixedDeposits);
    }

    // Get FDs by status
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getFixedDepositsByStatus(@PathVariable String status, Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        List<FixedDeposit> fixedDeposits = fixedDepositService.getFixedDepositsByStatus(status);
        return ResponseEntity.ok(fixedDeposits);
    }

    // Get matured FDs that need processing
    @GetMapping("/matured")
    public ResponseEntity<?> getMaturedFixedDeposits(Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        List<FixedDeposit> fixedDeposits = fixedDepositService.getMaturedFixedDeposits();
        return ResponseEntity.ok(fixedDeposits);
    }

    // Approve FD (admin)
    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveFixedDeposit(
            @PathVariable Long id,
            @RequestParam String approvedBy,
            @RequestParam(required = false) String approvalReason,
            Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        Map<String, Object> response = fixedDepositService.approveFixedDeposit(id, approvedBy, approvalReason);
        return ResponseEntity.ok(response);
    }

    // Reject FD (admin)
    @PutMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectFixedDeposit(
            @PathVariable Long id,
            @RequestParam String rejectedBy,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        Map<String, Object> response = fixedDepositService.rejectFixedDeposit(id, rejectedBy, reason);
        return ResponseEntity.ok(response);
    }

    // Process FD maturity (admin)
    @PutMapping("/{id}/process-maturity")
    public ResponseEntity<Map<String, Object>> processMaturity(
            @PathVariable Long id,
            @RequestParam String processedBy,
            Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        Map<String, Object> response = fixedDepositService.processMaturity(id, processedBy);
        return ResponseEntity.ok(response);
    }

    // Update FD (manager/admin)
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateFixedDeposit(
            @PathVariable Long id,
            @RequestBody FixedDeposit fdDetails,
            @RequestParam String reason,
            Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        Map<String, Object> response = fixedDepositService.updateFixedDeposit(
                id, fdDetails, reason, actor(authentication));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/admin/top-up")
    public ResponseEntity<?> adminTopUp(@PathVariable Long id, @RequestParam Double amount,
                                        @RequestParam String admin, Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        return ResponseEntity.ok(fixedDepositService.adminTopUp(id, amount, actor(authentication)));
    }

    @PostMapping("/{id}/admin/close")
    public ResponseEntity<?> adminClose(@PathVariable Long id, @RequestParam String reason,
                                        Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        return ResponseEntity.ok(fixedDepositService.adminClose(id, actor(authentication), reason));
    }

    @GetMapping("/{id}/history/events")
    public ResponseEntity<?> getManagementHistory(@PathVariable Long id, Authentication authentication) {
        FixedDeposit fd = fixedDepositService.getFixedDepositById(id).orElse(null);
        if (fd == null) return ResponseEntity.notFound().build();
        if (!canAccess(authentication, fd)) return forbidden();
        List<DepositManagementHistory> events = depositHistoryService.getHistory("FD", id);
        return ResponseEntity.ok(events);
    }

    // Get FD history (all FDs for an account or all FDs)
    @GetMapping("/history")
    public ResponseEntity<?> getFDHistory(
            @RequestParam(required = false) String accountNumber,
            Authentication authentication) {
        if (accountNumber == null || accountNumber.isBlank()) {
            if (!isStaff(authentication)) return forbidden();
        } else {
            SessionPrincipal principal = principal(authentication);
            if (!isStaff(authentication) && (principal == null || !"USER".equals(principal.scope())
                    || !sameAccount(principal.accountNumber(), accountNumber))) return forbidden();
        }
        List<FixedDeposit> history = fixedDepositService.getFDHistory(accountNumber);
        return ResponseEntity.ok(history);
    }

    // Get FD history by status
    @GetMapping("/history/status/{status}")
    public ResponseEntity<?> getFDHistoryByStatus(
            @PathVariable String status,
            @RequestParam(required = false) String accountNumber,
            Authentication authentication) {
        if (accountNumber == null || accountNumber.isBlank()) {
            if (!isStaff(authentication)) return forbidden();
        } else {
            SessionPrincipal principal = principal(authentication);
            if (!isStaff(authentication) && (principal == null || !"USER".equals(principal.scope())
                    || !sameAccount(principal.accountNumber(), accountNumber))) return forbidden();
        }
        List<FixedDeposit> history = fixedDepositService.getFDHistoryByStatus(accountNumber, status);
        return ResponseEntity.ok(history);
    }

    // Request FD withdrawal with cheque deposit (user)
    @PostMapping("/{id}/withdrawal/request")
    public ResponseEntity<Map<String, Object>> requestWithdrawal(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        FixedDeposit fd = fixedDepositService.getFixedDepositById(id).orElse(null);
        SessionPrincipal principal = principal(authentication);
        if (fd == null) return ResponseEntity.notFound().build();
        if (principal == null || !"USER".equals(principal.scope())
                || !sameAccount(principal.accountNumber(), fd.getAccountNumber())) return forbidden();
        Double withdrawalAmount = ((Number) request.get("withdrawalAmount")).doubleValue();
        String requestedBy = (String) request.get("requestedBy");
        Map<String, Object> response = fixedDepositService.requestWithdrawal(id, withdrawalAmount, requestedBy);
        return ResponseEntity.ok(response);
    }

    // Approve withdrawal request and process cheque deposit (admin)
    @PutMapping("/{id}/withdrawal/approve")
    public ResponseEntity<Map<String, Object>> approveWithdrawal(
            @PathVariable Long id,
            @RequestParam String chequeNumber,
            @RequestParam String approvedBy,
            Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        Map<String, Object> response = fixedDepositService.approveWithdrawal(id, chequeNumber, approvedBy);
        return ResponseEntity.ok(response);
    }

    // Reject withdrawal request (admin)
    @PutMapping("/{id}/withdrawal/reject")
    public ResponseEntity<Map<String, Object>> rejectWithdrawal(
            @PathVariable Long id,
            @RequestParam String rejectedBy,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        Map<String, Object> response = fixedDepositService.rejectWithdrawal(id, rejectedBy, reason);
        return ResponseEntity.ok(response);
    }

    // Process monthly interest credit for a specific FD (admin/manual trigger)
    @PostMapping("/{id}/interest/credit")
    public ResponseEntity<Map<String, Object>> processMonthlyInterest(
            @PathVariable Long id, Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        Map<String, Object> response = fixedDepositService.processMonthlyInterestCredit(id);
        return ResponseEntity.ok(response);
    }

    // Process monthly interest credit for all active FDs (admin/manual trigger)
    @PostMapping("/interest/credit-all")
    public ResponseEntity<Map<String, Object>> processAllMonthlyInterest(Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        Map<String, Object> response = fixedDepositService.processAllMonthlyInterestCredits();
        return ResponseEntity.ok(response);
    }

    // Calculate premature closure amount (user)
    @GetMapping("/{id}/foreclosure/calculate")
    public ResponseEntity<?> calculatePrematureClosure(@PathVariable Long id, Authentication authentication) {
        FixedDeposit fd = fixedDepositService.getFixedDepositById(id).orElse(null);
        if (fd == null) return ResponseEntity.notFound().build();
        if (!canAccess(authentication, fd)) return forbidden();
        Map<String, Object> response = fixedDepositService.calculatePrematureClosure(id);
        return ResponseEntity.ok(response);
    }

    // Send OTP to registered email for FD foreclosure (reason in email)
    @PostMapping("/{id}/foreclosure/request-otp")
    public ResponseEntity<?> requestForeclosureOtp(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of("message", "Customer OTP foreclosure is disabled; contact an administrator."));
    }

    // Process premature closure (user). If otp is provided, verifies and sets foreclosureOtpVerified.
    @PostMapping("/{id}/foreclosure")
    public ResponseEntity<Map<String, Object>> processPrematureClosure(
            @PathVariable Long id,
            @RequestParam String closedBy,
            @RequestParam String reason,
            Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        Map<String, Object> response = fixedDepositService.adminClose(id, actor(authentication), reason);
        return ResponseEntity.ok(response);
    }

    // Increase FD amount (top-up) (user)
    @PostMapping("/{id}/top-up")
    public ResponseEntity<Map<String, Object>> increaseFDAmount(
            @PathVariable Long id,
            @RequestParam Double additionalAmount,
            @RequestParam String requestedBy,
            Authentication authentication) {
        FixedDeposit fd = fixedDepositService.getFixedDepositById(id).orElse(null);
        SessionPrincipal principal = principal(authentication);
        if (fd == null) return ResponseEntity.notFound().build();
        if (principal == null || !"USER".equals(principal.scope())
                || !sameAccount(principal.accountNumber(), fd.getAccountNumber())) return forbidden();
        Map<String, Object> response = fixedDepositService.increaseFDAmount(id, additionalAmount, requestedBy);
        return ResponseEntity.ok(response);
    }

    private boolean canAccess(Authentication authentication, FixedDeposit fd) {
        if (isStaff(authentication)) return true;
        SessionPrincipal principal = principal(authentication);
        return principal != null && "USER".equals(principal.scope())
                && sameAccount(principal.accountNumber(), fd.getAccountNumber());
    }

    private boolean isStaff(Authentication authentication) {
        SessionPrincipal principal = principal(authentication);
        return principal != null
                && ("ADMIN".equals(principal.scope()) || "MANAGER".equals(principal.scope()));
    }

    private SessionPrincipal principal(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal
                ? principal : null;
    }

    private boolean sameAccount(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    private String actor(Authentication authentication) {
        SessionPrincipal principal = principal(authentication);
        return principal.scope() + ":" + principal.userId();
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Staff sign-in required."));
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Customer sign-in required."));
    }
}
