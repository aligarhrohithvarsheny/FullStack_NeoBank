package com.neo.springapp.controller;

import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.repository.AllocationAccountRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import com.neo.springapp.service.PasswordService;
import com.neo.springapp.service.UserSessionTokenService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AllocationAccountLoginController {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private final FundsAllocationRepository allocationRepository;
    private final AllocationAccountRepository linkedAccountRepository;
    private final PasswordService passwordService;
    private final UserSessionTokenService tokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AllocationAccountLoginController(
            FundsAllocationRepository allocationRepository,
            AllocationAccountRepository linkedAccountRepository,
            PasswordService passwordService,
            UserSessionTokenService tokenService) {
        this.allocationRepository = allocationRepository;
        this.linkedAccountRepository = linkedAccountRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
    }

    @PostMapping("/hod/allocations/{allocationId}/account-login")
    @Transactional
    public ResponseEntity<Map<String, Object>> provisionLogin(
            @PathVariable Long allocationId,
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null || !"HOD".equals(principal.scope())) {
            return error(HttpStatus.FORBIDDEN, "A HOD session is required.");
        }

        Optional<FundsAllocation> allocationResult = allocationRepository.findById(allocationId);
        if (allocationResult.isEmpty()) return error(HttpStatus.NOT_FOUND, "Allocation not found.");
        FundsAllocation allocation = allocationResult.get();
        if (!principal.userId().equals(allocation.getAllocatedByAdminId())) {
            return error(HttpStatus.FORBIDDEN, "You can only create access for allocations you manage.");
        }
        if (!isAllocationActive(allocation)) {
            return error(HttpStatus.CONFLICT, "Only active allocations can receive login access.");
        }

        Optional<AllocationAccount> linkedAccount = linkedAccountRepository.findByAllocationId(allocationId);
        if (linkedAccount.isEmpty() || !isLinkedAccountApproved(linkedAccount.get())) {
            return error(HttpStatus.CONFLICT, "The linked account must be verified and active before login access is created.");
        }

        String username = allocation.getAllocationLoginUsername();
        if (username == null || username.isBlank()) username = "fund-" + allocation.getId();
        String temporaryPassword = generateTemporaryPassword();
        allocation.setAllocationLoginUsername(username);
        allocation.setAllocationLoginPasswordHash(passwordService.encryptPassword(temporaryPassword));
        allocation.setAllocationLoginFailedAttempts(0);
        allocation.setAllocationLoginLocked(false);
        allocationRepository.save(allocation);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "username", username,
                "password", temporaryPassword,
                "allocationId", allocation.getAllocationId(),
                "accountHolderName", linkedAccount.get().getAccountHolderName()));
    }

    @PostMapping("/allocation-account/login")
    @Transactional
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest credentials) {
        String username = credentials == null || credentials.username() == null
                ? "" : credentials.username().trim().toLowerCase();
        String password = credentials == null ? null : credentials.password();
        if (username.isBlank() || password == null || password.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Username and password are required.");
        }

        Optional<FundsAllocation> allocationResult = allocationRepository.findByAllocationLoginUsername(username);
        if (allocationResult.isEmpty()) return error(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        FundsAllocation allocation = allocationResult.get();
        if (Boolean.TRUE.equals(allocation.getAllocationLoginLocked())) {
            return error(HttpStatus.UNAUTHORIZED, "Login is locked. Contact the HOD to reset access.");
        }
        if (!passwordService.verifyPassword(password, allocation.getAllocationLoginPasswordHash())) {
            int failedAttempts = Optional.ofNullable(allocation.getAllocationLoginFailedAttempts()).orElse(0) + 1;
            allocation.setAllocationLoginFailedAttempts(failedAttempts);
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) allocation.setAllocationLoginLocked(true);
            allocationRepository.save(allocation);
            return error(HttpStatus.UNAUTHORIZED, failedAttempts >= MAX_FAILED_ATTEMPTS
                    ? "Login is locked. Contact the HOD to reset access."
                    : "Invalid username or password.");
        }
        if (!isAllocationActive(allocation)) {
            return error(HttpStatus.FORBIDDEN, "This allocation is inactive or expired.");
        }
        Optional<AllocationAccount> linkedAccount = linkedAccountRepository.findByAllocationId(allocation.getId());
        if (linkedAccount.isEmpty() || !isLinkedAccountApproved(linkedAccount.get())) {
            return error(HttpStatus.FORBIDDEN, "The linked account is not verified and active.");
        }

        allocation.setAllocationLoginFailedAttempts(0);
        allocationRepository.save(allocation);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("role", "ALLOCATION_ACCOUNT");
        response.put("sessionToken", tokenService.issueAllocationAccount(allocation.getId(), allocation.getAllocationId()));
        response.put("allocationRecordId", allocation.getId());
        response.put("allocationId", allocation.getAllocationId());
        response.put("accountHolderName", linkedAccount.get().getAccountHolderName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/allocation-account/me")
    public ResponseEntity<Map<String, Object>> getMyAllocation(
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null || !"ALLOCATION_ACCOUNT".equals(principal.scope())) {
            return error(HttpStatus.FORBIDDEN, "An allocation account session is required.");
        }

        if (principal.userId() == null || principal.userId() >= 0
                || principal.accountNumber() == null || !principal.accountNumber().startsWith("ALLOCATION:")) {
            return error(HttpStatus.FORBIDDEN, "The session is not scoped to an allocation account.");
        }
        Long allocationRecordId = -principal.userId();
        Optional<FundsAllocation> allocationResult = allocationRepository.findById(allocationRecordId);
        if (allocationResult.isEmpty()) return error(HttpStatus.NOT_FOUND, "Allocation not found.");
        FundsAllocation allocation = allocationResult.get();
        if (!("ALLOCATION:" + allocation.getAllocationId()).equals(principal.accountNumber())) {
            return error(HttpStatus.FORBIDDEN, "The session does not match this allocation.");
        }
        if (!isAllocationActive(allocation)) {
            return error(HttpStatus.FORBIDDEN, "This allocation is inactive or expired.");
        }
        Optional<AllocationAccount> linkedAccountResult = linkedAccountRepository.findByAllocationId(allocation.getId());
        if (linkedAccountResult.isEmpty() || !isLinkedAccountApproved(linkedAccountResult.get())) {
            return error(HttpStatus.FORBIDDEN, "The linked account is not verified and active.");
        }

        AllocationAccount linkedAccount = linkedAccountResult.get();
        Map<String, Object> allocationData = new LinkedHashMap<>();
        allocationData.put("id", allocation.getId());
        allocationData.put("allocationId", allocation.getAllocationId());
        allocationData.put("managerName", allocation.getManagerName());
        allocationData.put("branchName", allocation.getBranchName());
        allocationData.put("allocatedAmount", allocation.getAllocatedAmount());
        allocationData.put("currentBalance", allocation.getCurrentBalance());
        allocationData.put("totalDebited", allocation.getTotalDebited());
        allocationData.put("totalCredited", allocation.getTotalCredited());
        allocationData.put("productTypes", allocation.getProductTypes());
        allocationData.put("status", allocation.getStatus());
        allocationData.put("validFrom", allocation.getValidFrom());
        allocationData.put("validTill", allocation.getValidTill());
        allocationData.put("description", allocation.getDescription());

        Map<String, Object> accountData = new LinkedHashMap<>();
        accountData.put("accountNumber", linkedAccount.getAccountNumber());
        accountData.put("ifscCode", linkedAccount.getIfscCode());
        accountData.put("accountHolderName", linkedAccount.getAccountHolderName());
        accountData.put("bankName", linkedAccount.getBankName());
        accountData.put("accountType", linkedAccount.getAccountType());
        accountData.put("branchName", linkedAccount.getBranchName());
        accountData.put("city", linkedAccount.getCity());
        accountData.put("verificationStatus", linkedAccount.getVerificationStatus());
        accountData.put("accountStatus", linkedAccount.getAccountStatus());
        accountData.put("accountBalance", linkedAccount.getAccountBalance());
        accountData.put("totalAllocated", linkedAccount.getTotalAllocated());
        accountData.put("totalDebited", linkedAccount.getTotalDebited());
        accountData.put("totalCredited", linkedAccount.getTotalCredited());
        accountData.put("currentBalance", linkedAccount.getCurrentBalance());
        accountData.put("interestCharges", linkedAccount.getInterestCharges());
        accountData.put("cibilCharges", linkedAccount.getCibilCharges());
        accountData.put("soundboxCharges", linkedAccount.getSoundboxCharges());
        accountData.put("upiCharges", linkedAccount.getUpiCharges());
        accountData.put("paymentGatewayCharges", linkedAccount.getPaymentGatewayCharges());
        accountData.put("totalChargesCollected", linkedAccount.getTotalChargesCollected());
        accountData.put("linkedAt", linkedAccount.getLinkedAt());
        accountData.put("verifiedAt", linkedAccount.getVerifiedAt());
        accountData.put("linkedByAdminName", linkedAccount.getLinkedByAdminName());
        accountData.put("verifiedByAdminName", linkedAccount.getVerifiedByAdminId() == null
            ? null : "Admin #" + linkedAccount.getVerifiedByAdminId());

        return ResponseEntity.ok(Map.of("success", true, "allocation", allocationData, "account", accountData));
    }

    private boolean isAllocationActive(FundsAllocation allocation) {
        return "ACTIVE".equalsIgnoreCase(allocation.getStatus())
                && (allocation.getValidTill() == null || !allocation.getValidTill().isBefore(LocalDate.now()));
    }

    private boolean isLinkedAccountApproved(AllocationAccount account) {
        return "VERIFIED".equalsIgnoreCase(account.getVerificationStatus())
                && "ACTIVE".equalsIgnoreCase(account.getAccountStatus());
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(20);
        for (int index = 0; index < 20; index++) {
            password.append(PASSWORD_ALPHABET.charAt(secureRandom.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }

    public record LoginRequest(String username, String password) {}
}
