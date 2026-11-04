package com.neo.springapp.controller;

import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.entity.ChargeTransaction;
import com.neo.springapp.model.AllocationUtilization;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.repository.AllocationUtilizationRepository;
import com.neo.springapp.repository.AllocationAccountRepository;
import com.neo.springapp.repository.ChargeTransactionRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import com.neo.springapp.service.PasswordService;
import com.neo.springapp.service.UserSessionTokenService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AllocationAccountLoginController {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private final FundsAllocationRepository allocationRepository;
    private final AllocationAccountRepository linkedAccountRepository;
    private final AllocationUtilizationRepository utilizationRepository;
    private final ChargeTransactionRepository chargeTransactionRepository;
    private final PasswordService passwordService;
    private final UserSessionTokenService tokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AllocationAccountLoginController(
            FundsAllocationRepository allocationRepository,
            AllocationAccountRepository linkedAccountRepository,
            AllocationUtilizationRepository utilizationRepository,
            ChargeTransactionRepository chargeTransactionRepository,
            PasswordService passwordService,
            UserSessionTokenService tokenService) {
        this.allocationRepository = allocationRepository;
        this.linkedAccountRepository = linkedAccountRepository;
        this.utilizationRepository = utilizationRepository;
        this.chargeTransactionRepository = chargeTransactionRepository;
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

    @GetMapping("/allocation-account/dashboard")
    public ResponseEntity<Map<String, Object>> getMyAllocationDashboard(
            @AuthenticationPrincipal SessionPrincipal principal,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "25") int size) {
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
        if (!isAllocationActive(allocation)) return error(HttpStatus.FORBIDDEN, "This allocation is inactive or expired.");
        Optional<AllocationAccount> linkedAccountResult = linkedAccountRepository.findByAllocationId(allocationRecordId);
        if (linkedAccountResult.isEmpty() || !isLinkedAccountApproved(linkedAccountResult.get())) {
            return error(HttpStatus.FORBIDDEN, "The linked account is not verified and active.");
        }

        AllocationAccount account = linkedAccountResult.get();
        Page<AllocationUtilization> transactionPage = utilizationRepository.findByAllocationIdOrderByTransactionDateDesc(
                allocationRecordId,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)),
                        Sort.by(Sort.Direction.DESC, "transactionDate")));
        Page<ChargeTransaction> chargePage = chargeTransactionRepository.findByAllocationIdOrderByCreatedAtDesc(
            allocationRecordId, PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createdAt")));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("allocation", allocationDashboardData(allocation));
        response.put("account", accountDashboardData(account));
        response.put("summary", dashboardSummary(allocation, account));
        response.put("transactions", transactionPage.getContent().stream().map(this::transactionDashboardData).toList());
        response.put("transactionPage", transactionPage.getNumber());
        response.put("transactionTotalPages", transactionPage.getTotalPages());
        response.put("transactionTotalElements", transactionPage.getTotalElements());
        response.put("charges", chargePage.getContent().stream().map(this::chargeDashboardData).toList());
        response.put("lastUpdated", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> allocationDashboardData(FundsAllocation allocation) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", allocation.getId());
        data.put("allocationId", allocation.getAllocationId());
        data.put("allocatedAmount", allocation.getAllocatedAmount());
        data.put("currentBalance", allocation.getCurrentBalance());
        data.put("totalDebited", allocation.getTotalDebited());
        data.put("totalCredited", allocation.getTotalCredited());
        data.put("status", allocation.getStatus());
        data.put("validTill", allocation.getValidTill());
        data.put("productTypes", allocation.getProductTypes());
        data.put("description", allocation.getDescription());
        data.put("updatedAt", allocation.getUpdatedAt());
        return data;
    }

    private Map<String, Object> accountDashboardData(AllocationAccount account) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accountNumber", account.getAccountNumber());
        data.put("ifscCode", account.getIfscCode());
        data.put("accountHolderName", account.getAccountHolderName());
        data.put("bankName", account.getBankName());
        data.put("accountType", account.getAccountType());
        data.put("branchName", account.getBranchName());
        data.put("city", account.getCity());
        data.put("verificationStatus", account.getVerificationStatus());
        data.put("accountStatus", account.getAccountStatus());
        data.put("accountBalance", account.getAccountBalance());
        data.put("currentBalance", account.getCurrentBalance());
        data.put("totalAllocated", account.getTotalAllocated());
        data.put("totalDebited", account.getTotalDebited());
        data.put("totalCredited", account.getTotalCredited());
        data.put("interestCharges", account.getInterestCharges());
        data.put("cibilCharges", account.getCibilCharges());
        data.put("soundboxCharges", account.getSoundboxCharges());
        data.put("upiCharges", account.getUpiCharges());
        data.put("paymentGatewayCharges", account.getPaymentGatewayCharges());
        data.put("totalChargesCollected", account.getTotalChargesCollected());
        return data;
    }

    private Map<String, Object> dashboardSummary(FundsAllocation allocation, AllocationAccount account) {
        Long allocationId = allocation.getId();
        List<String> loanTypes = List.of("LOAN", "LOANS", "GOLD_LOAN", "PERSONAL_LOAN", "HOME_LOAN");
        List<String> fdTypes = List.of("FD", "FD_DEPOSIT", "FIXED_DEPOSIT");
        List<String> rdTypes = List.of("RD", "RD_DEPOSIT", "RECURRING_DEPOSIT");
        List<String> depositTypes = List.of("DEPOSIT", "DEPOSITS");
        double loanDebits = sumByProductTypes(allocationId, "DEBIT", loanTypes);
        double loanCredits = sumByProductTypes(allocationId, "CREDIT", loanTypes);
        double fdDeposits = sumByProductTypes(allocationId, "CREDIT", fdTypes);
        double rdDeposits = sumByProductTypes(allocationId, "CREDIT", rdTypes);
        double genericDeposits = sumByProductTypes(allocationId, "CREDIT", depositTypes);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAllocated", safeDouble(allocation.getAllocatedAmount()));
        summary.put("balance", safeDouble(account.getCurrentBalance() == null
                ? (account.getAccountBalance() == null ? null : account.getAccountBalance().doubleValue())
                : account.getCurrentBalance().doubleValue()));
        summary.put("totalCredits", safeDouble(account.getTotalCredited() == null ? null : account.getTotalCredited().doubleValue()));
        summary.put("totalDebits", safeDouble(account.getTotalDebited() == null ? null : account.getTotalDebited().doubleValue()));
        summary.put("allocationDebited", safeDouble(allocation.getTotalDebited()));
        summary.put("creditCount", utilizationRepository.getCreditCount(allocationId));
        summary.put("debitCount", utilizationRepository.getDebitCount(allocationId));
        summary.put("fundsAdded", safeDouble(account.getTotalCredited() == null ? null : account.getTotalCredited().doubleValue()));
        summary.put("transfersCount", utilizationRepository.countByAllocationId(allocationId));
        summary.put("loansOpened", countByProductTypes(allocationId, "DEBIT", loanTypes));
        summary.put("loansClosedOrCredited", countByProductTypes(allocationId, "CREDIT", loanTypes));
        summary.put("loanDebits", loanDebits);
        summary.put("loanCredits", loanCredits);
        summary.put("fdDeposits", fdDeposits);
        summary.put("rdDeposits", rdDeposits);
        summary.put("otherDeposits", genericDeposits);
        summary.put("interestCollected", zeroIfNull(chargeTransactionRepository.sumSuccessfulInterestCharges(allocationId)));
        summary.put("allChargesCollected", zeroIfNull(chargeTransactionRepository.sumTotalCharges(allocationId)));
        summary.put("allChargesCredited", zeroIfNull(chargeTransactionRepository.sumCreditedCharges(allocationId)));
        summary.put("pendingChargeCredits", zeroIfNull(chargeTransactionRepository.sumPendingChargeCredits(allocationId)));
        summary.put("chargeDebitCount", chargeTransactionRepository.countCollectedCharges(allocationId));
        summary.put("fineCharges", zeroIfNull(chargeTransactionRepository.sumSuccessfulFineCharges(allocationId)));
        summary.put("chequeCharges", zeroIfNull(chargeTransactionRepository.sumSuccessfulChequeCharges(allocationId)));
        summary.put("cibilCharges", zeroIfNull(account.getCibilCharges()));
        summary.put("soundboxCharges", zeroIfNull(account.getSoundboxCharges()));
        summary.put("upiCharges", zeroIfNull(account.getUpiCharges()));
        summary.put("paymentGatewayCharges", zeroIfNull(account.getPaymentGatewayCharges()));
        summary.put("totalCharges", zeroIfNull(account.getTotalChargesCollected()));
        summary.put("transactionCount", utilizationRepository.countByAllocationId(allocationId));
        Long creditedChargeCount = chargeTransactionRepository.countByCreditStatus(allocationId, "CREDITED");
        Long pendingChargeCount = chargeTransactionRepository.countByCreditStatus(allocationId, "PENDING_CREDIT");
        summary.put("chargeCount", (creditedChargeCount == null ? 0 : creditedChargeCount)
            + (pendingChargeCount == null ? 0 : pendingChargeCount));
        return summary;
    }

    private double sumByProductTypes(Long allocationId, String transactionType, List<String> productTypes) {
        return safeDouble(utilizationRepository.sumByAllocationAndProductTypes(allocationId, transactionType, productTypes));
    }

    private long countByProductTypes(Long allocationId, String transactionType, List<String> productTypes) {
        Long count = utilizationRepository.countByAllocationAndProductTypes(allocationId, transactionType, productTypes);
        return count == null ? 0 : count;
    }

    private Map<String, Object> transactionDashboardData(AllocationUtilization transaction) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionId", transaction.getUtilizationId());
        data.put("type", transaction.getTransactionType());
        data.put("amount", transaction.getAmount());
        data.put("balanceAfter", transaction.getRemainingBalance());
        data.put("productType", transaction.getProductType());
        data.put("userAccountNumber", transaction.getUserAccountNumber());
        data.put("userName", transaction.getUserName());
        data.put("linkedTransactionId", transaction.getLinkedTransactionId());
        data.put("description", transaction.getDescription());
        data.put("performedByName", transaction.getPerformedByName());
        data.put("status", transaction.getStatus());
        data.put("transactionDate", transaction.getTransactionDate());
        return data;
    }

    private Map<String, Object> chargeDashboardData(ChargeTransaction charge) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionId", charge.getChargeTransactionId());
        data.put("chargeType", charge.getChargeType());
        data.put("description", charge.getChargeDescription());
        data.put("amount", charge.getChargeAmount());
        data.put("creditedAmount", charge.getNetCreditAmount());
        data.put("collectionStatus", charge.getCollectionStatus());
        data.put("creditStatus", charge.getCreditStatus());
        data.put("creditReference", charge.getCreditReferenceNumber());
        data.put("userAccountNumber", charge.getUserAccountNumber());
        data.put("userName", charge.getUserName());
        data.put("createdAt", charge.getCreatedAt());
        data.put("creditedAt", charge.getCreditedAt());
        return data;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private double safeDouble(Double value) { return value == null ? 0 : value; }
    private BigDecimal zeroIfNull(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

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
