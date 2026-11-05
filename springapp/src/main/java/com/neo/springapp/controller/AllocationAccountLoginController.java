package com.neo.springapp.controller;

import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.entity.ChargeTransaction;
import com.neo.springapp.model.AllocationUtilization;
import com.neo.springapp.model.Admin;
import com.neo.springapp.model.AllocationAccountOperationRequest;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.repository.AllocationAccountOperationRequestRepository;
import com.neo.springapp.repository.AllocationUtilizationRepository;
import com.neo.springapp.repository.AllocationAccountRepository;
import com.neo.springapp.repository.ChargeTransactionRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import com.neo.springapp.repository.EmiPaymentRepository;
import com.neo.springapp.repository.EcsMandateRepository;
import com.neo.springapp.repository.EcsMandateEventRepository;
import com.neo.springapp.repository.LoanRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.HomeLoanRepository;
import com.neo.springapp.service.PasswordService;
import com.neo.springapp.service.UserSessionTokenService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/api")
public class AllocationAccountLoginController {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private final FundsAllocationRepository allocationRepository;
    private final AllocationAccountRepository linkedAccountRepository;
    private final AllocationUtilizationRepository utilizationRepository;
    private final ChargeTransactionRepository chargeTransactionRepository;
    private final AllocationAccountOperationRequestRepository operationRequestRepository;
    private final AdminRepository adminRepository;
    private final PasswordService passwordService;
    private final UserSessionTokenService tokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired private EmiPaymentRepository emiPaymentRepository;
    @Autowired private EcsMandateRepository ecsMandateRepository;
    @Autowired private EcsMandateEventRepository ecsMandateEventRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private GoldLoanRepository goldLoanRepository;
    @Autowired private HomeLoanRepository homeLoanRepository;

    public AllocationAccountLoginController(
            FundsAllocationRepository allocationRepository,
            AllocationAccountRepository linkedAccountRepository,
            AllocationUtilizationRepository utilizationRepository,
            ChargeTransactionRepository chargeTransactionRepository,
            AllocationAccountOperationRequestRepository operationRequestRepository,
            AdminRepository adminRepository,
            PasswordService passwordService,
            UserSessionTokenService tokenService) {
        this.allocationRepository = allocationRepository;
        this.linkedAccountRepository = linkedAccountRepository;
        this.utilizationRepository = utilizationRepository;
        this.chargeTransactionRepository = chargeTransactionRepository;
        this.operationRequestRepository = operationRequestRepository;
        this.adminRepository = adminRepository;
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
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "25") int size,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int chargePage,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") int chargeSize) {
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
        Page<ChargeTransaction> chargeHistoryPage = chargeTransactionRepository.findByAllocationIdOrderByCreatedAtDesc(
            allocationRecordId, PageRequest.of(Math.max(0, chargePage), Math.max(1, Math.min(chargeSize, 100)),
                    Sort.by(Sort.Direction.DESC, "createdAt")));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("allocation", allocationDashboardData(allocation));
        response.put("account", accountDashboardData(account));
        response.put("summary", dashboardSummary(allocation, account));
        response.put("productBreakdown", productBreakdown(allocationRecordId));
        response.put("transactions", transactionPage.getContent().stream().map(this::transactionDashboardData).toList());
        response.put("transactionPage", transactionPage.getNumber());
        response.put("transactionTotalPages", transactionPage.getTotalPages());
        response.put("transactionTotalElements", transactionPage.getTotalElements());
        response.put("charges", chargeHistoryPage.getContent().stream().map(this::chargeDashboardData).toList());
        response.put("chargePage", chargeHistoryPage.getNumber());
        response.put("chargeTotalPages", chargeHistoryPage.getTotalPages());
        response.put("chargeTotalElements", chargeHistoryPage.getTotalElements());
        response.put("loans", allocationLoansDashboardData(allocationRecordId));
        response.put("emiPayments", allocationEmiDashboardData(allocationRecordId));
        response.put("ecsMandates", allocationEcsDashboardData(allocationRecordId));
        response.put("accountDeposits", allocationAccountDepositData(allocationRecordId));
        response.put("profitability", allocationProfitabilityData(allocationRecordId));
        response.put("lastUpdated", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/allocation-account/requests")
    public ResponseEntity<Map<String, Object>> getMyOperationRequests(
            @AuthenticationPrincipal SessionPrincipal principal) {
        FundsAllocation allocation = resolveAllocationSession(principal);
        if (allocation == null) return error(HttpStatus.FORBIDDEN, "An allocation account session is required.");
        List<Map<String, Object>> requests = operationRequestRepository
                .findByAllocationRecordIdOrderByCreatedAtDesc(allocation.getId()).stream()
                .map(this::operationRequestData).toList();
        return ResponseEntity.ok(Map.of("success", true, "requests", requests));
    }

    @PostMapping("/allocation-account/requests")
    public ResponseEntity<Map<String, Object>> submitOperationRequest(
            @RequestBody OperationRequestPayload payload,
            @AuthenticationPrincipal SessionPrincipal principal) {
        FundsAllocation allocation = resolveAllocationSession(principal);
        if (allocation == null || !isAllocationActive(allocation)) {
            return error(HttpStatus.FORBIDDEN, "An active allocation account session is required.");
        }
        if (payload == null || payload.amount() == null || payload.amount().signum() <= 0
                || payload.amount().precision() > 19 || payload.amount().scale() > 2) {
            return error(HttpStatus.BAD_REQUEST, "Enter a valid request amount greater than zero.");
        }
        String requestType = payload.requestType() == null ? "" : payload.requestType().trim().toUpperCase(java.util.Locale.ROOT);
        if (!List.of("TOP_UP", "DEPOSIT", "TRANSFER").contains(requestType)) {
            return error(HttpStatus.BAD_REQUEST, "Request type must be TOP_UP, DEPOSIT, or TRANSFER.");
        }
        if ("TRANSFER".equals(requestType)
                && (payload.destinationAccountNumber() == null || payload.destinationAccountNumber().isBlank())) {
            return error(HttpStatus.BAD_REQUEST, "A destination account number is required for transfer requests.");
        }
        if (payload.destinationAccountNumber() != null && payload.destinationAccountNumber().length() > 50
                || payload.destinationName() != null && payload.destinationName().length() > 150
                || payload.externalReference() != null && payload.externalReference().length() > 100
                || payload.description() != null && payload.description().length() > 500) {
            return error(HttpStatus.BAD_REQUEST, "One or more request details exceed the allowed length.");
        }

        AllocationAccountOperationRequest request = new AllocationAccountOperationRequest();
        request.setAllocationRecordId(allocation.getId());
        request.setAllocationReference(allocation.getAllocationId());
        request.setRequestType(requestType);
        request.setAmount(payload.amount());
        request.setDestinationAccountNumber(clean(payload.destinationAccountNumber()));
        request.setDestinationName(clean(payload.destinationName()));
        request.setExternalReference(clean(payload.externalReference()));
        request.setDescription(clean(payload.description()));
        request.setStatus("PENDING");
        AllocationAccountOperationRequest saved = operationRequestRepository.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Request submitted for HOD review. No funds have moved.",
                "request", operationRequestData(saved)));
    }

    @GetMapping("/hod/allocations/{allocationId}/account-requests")
    public ResponseEntity<Map<String, Object>> getAllocationOperationRequests(
            @PathVariable Long allocationId,
            @AuthenticationPrincipal SessionPrincipal principal) {
        FundsAllocation allocation = requireHodOwnedAllocation(allocationId, principal);
        if (allocation == null) return error(HttpStatus.FORBIDDEN, "A HOD session for this allocation is required.");
        List<Map<String, Object>> requests = operationRequestRepository
                .findByAllocationRecordIdOrderByCreatedAtDesc(allocationId).stream()
                .map(this::operationRequestData).toList();
        return ResponseEntity.ok(Map.of("success", true, "requests", requests));
    }

    @org.springframework.web.bind.annotation.PutMapping("/hod/allocations/{allocationId}/account-requests/{requestId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> reviewAllocationOperationRequest(
            @PathVariable Long allocationId,
            @PathVariable Long requestId,
            @RequestBody ReviewDecisionPayload payload,
            @AuthenticationPrincipal SessionPrincipal principal) {
        FundsAllocation allocation = requireHodOwnedAllocation(allocationId, principal);
        if (allocation == null) return error(HttpStatus.FORBIDDEN, "A HOD session for this allocation is required.");
        String decision = payload == null || payload.decision() == null
                ? "" : payload.decision().trim().toUpperCase(java.util.Locale.ROOT);
        if (!List.of("APPROVED", "REJECTED").contains(decision)) {
            return error(HttpStatus.BAD_REQUEST, "Decision must be APPROVED or REJECTED.");
        }
        Optional<AllocationAccountOperationRequest> requestResult = operationRequestRepository
                .findByIdAndAllocationRecordId(requestId, allocationId);
        if (requestResult.isEmpty()) return error(HttpStatus.NOT_FOUND, "Operation request not found.");
        AllocationAccountOperationRequest request = requestResult.get();
        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            return error(HttpStatus.CONFLICT, "This operation request has already been reviewed.");
        }
        String reviewNote = clean(payload.reviewNote());
        if (reviewNote != null && reviewNote.length() > 500) {
            return error(HttpStatus.BAD_REQUEST, "Review note cannot exceed 500 characters.");
        }
        Admin reviewer = adminRepository.findById(principal.userId()).orElse(null);
        request.setStatus(decision);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedByAdminId(principal.userId());
        request.setReviewedByName(reviewer == null ? null : reviewer.getName());
        request.setReviewNote(reviewNote);
        AllocationAccountOperationRequest saved = operationRequestRepository.save(request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Request " + decision.toLowerCase(java.util.Locale.ROOT)
                + ". No funds moved automatically; settle through the approved banking workflow.");
        response.put("request", operationRequestData(saved));
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
        data.put("linkedAt", account.getLinkedAt());
        data.put("verifiedAt", account.getVerifiedAt());
        data.put("linkedByAdminName", account.getLinkedByAdminName());
        data.put("verifiedByAdminId", account.getVerifiedByAdminId());
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
        summary.put("netProfitLoss", safeDouble(allocationProfitabilityData(allocationId).get("netProfitLoss")));
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

    private List<Map<String, Object>> productBreakdown(Long allocationId) {
        Map<String, Map<String, Object>> products = new LinkedHashMap<>();
        for (Object[] row : utilizationRepository.summarizeSuccessfulActivityByProduct(allocationId)) {
            String productType = String.valueOf(row[0]);
            String transactionType = String.valueOf(row[1]);
            Number count = (Number) row[2];
            Number amount = (Number) row[3];
            Map<String, Object> product = products.computeIfAbsent(productType, key -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("productType", key);
                entry.put("debitCount", 0L);
                entry.put("debitAmount", 0.0);
                entry.put("creditCount", 0L);
                entry.put("creditAmount", 0.0);
                return entry;
            });
            if ("DEBIT".equals(transactionType)) {
                product.put("debitCount", count.longValue());
                product.put("debitAmount", amount.doubleValue());
            } else if ("CREDIT".equals(transactionType)) {
                product.put("creditCount", count.longValue());
                product.put("creditAmount", amount.doubleValue());
            }
        }
        return new ArrayList<>(products.values());
    }

    private Set<String> allocationCustomerAccounts(Long allocationId) {
        Set<String> accounts = new HashSet<>();
        if (utilizationRepository == null) return accounts;
        for (AllocationUtilization entry : utilizationRepository.findByAllocationIdOrderByTransactionDateDesc(allocationId)) {
            if (entry.getUserAccountNumber() != null && !entry.getUserAccountNumber().isBlank()) {
                accounts.add(entry.getUserAccountNumber().trim());
            }
        }
        return accounts;
    }

    private List<Map<String, Object>> allocationLoansDashboardData(Long allocationId) {
        Set<String> customerAccounts = allocationCustomerAccounts(allocationId);
        List<Map<String, Object>> loans = new ArrayList<>();
        if (loanRepository != null) {
            for (com.neo.springapp.model.Loan loan : loanRepository.findAll()) {
                if (customerAccounts.contains(loan.getAccountNumber()) || customerAccounts.contains(loan.getChildAccountNumber())) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("source", "LOAN");
                    row.put("loanId", loan.getId());
                    row.put("loanType", loan.getType());
                    row.put("loanAccountNumber", loan.getLoanAccountNumber());
                    row.put("accountNumber", loan.getAccountNumber());
                    row.put("customerName", loan.getUserName());
                    row.put("sanctionedAmount", safeDouble(loan.getAmount()));
                    row.put("principalPaid", safeDouble(loan.getPrincipalPaid()));
                    row.put("interestPaid", safeDouble(loan.getInterestPaid()));
                    row.put("remainingPrincipal", safeDouble(loan.getRemainingPrincipal()));
                    row.put("remainingInterest", safeDouble(loan.getRemainingInterest()));
                    row.put("status", loan.getStatus());
                    row.put("applicationDate", loan.getApplicationDate());
                    row.put("approvalDate", loan.getApprovalDate());
                    row.put("interestRate", loan.getInterestRate());
                    row.put("tenure", loan.getTenure());
                    loans.add(row);
                }
            }
        }
        if (goldLoanRepository != null) {
            for (com.neo.springapp.model.GoldLoan loan : goldLoanRepository.findAll()) {
                if (customerAccounts.contains(loan.getAccountNumber())) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("source", "GOLD_LOAN");
                    row.put("loanId", loan.getId());
                    row.put("loanType", "GOLD");
                    row.put("loanAccountNumber", loan.getLoanAccountNumber());
                    row.put("accountNumber", loan.getAccountNumber());
                    row.put("customerName", loan.getUserName());
                    row.put("sanctionedAmount", safeDouble(loan.getLoanAmount()));
                    row.put("principalPaid", safeDouble(loan.getPrincipalPaid()));
                    row.put("interestPaid", safeDouble(loan.getInterestPaid()));
                    row.put("remainingPrincipal", safeDouble(loan.getRemainingPrincipal()));
                    row.put("remainingInterest", safeDouble(loan.getRemainingInterest()));
                    row.put("currentEmi", safeDouble(loan.getCurrentEmi()));
                    row.put("status", loan.getStatus());
                    row.put("applicationDate", loan.getApplicationDate());
                    row.put("approvalDate", loan.getApprovalDate());
                    row.put("interestRate", loan.getInterestRate());
                    row.put("tenure", loan.getTenure());
                    loans.add(row);
                }
            }
        }
        if (homeLoanRepository != null) {
            for (com.neo.springapp.model.HomeLoan loan : homeLoanRepository.findAll()) {
                if (customerAccounts.contains(loan.getAccountNumber())) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("source", "HOME_CATEGORY_LOAN");
                    row.put("loanId", loan.getId());
                    row.put("loanType", loan.getLoanType());
                    row.put("loanAccountNumber", loan.getLoanAccountNumber());
                    row.put("accountNumber", loan.getAccountNumber());
                    row.put("customerName", loan.getUserName());
                    row.put("sanctionedAmount", safeDouble(loan.getAmount()));
                    row.put("principalPaid", safeDouble(loan.getPrincipalPaid()));
                    row.put("interestPaid", safeDouble(loan.getInterestPaid()));
                    row.put("remainingPrincipal", safeDouble(loan.getRemainingPrincipal()));
                    row.put("remainingInterest", 0.0);
                    row.put("status", loan.getStatus());
                    row.put("applicationDate", loan.getApplicationDate());
                    row.put("approvalDate", loan.getApprovalDate());
                    row.put("interestRate", loan.getInterestRate());
                    row.put("tenure", loan.getTenure());
                    row.put("purpose", loan.getPurpose());
                    loans.add(row);
                }
            }
        }
        return loans;
    }

    private List<Map<String, Object>> allocationEmiDashboardData(Long allocationId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (emiPaymentRepository == null) return result;
        Set<String> customerAccounts = allocationCustomerAccounts(allocationId);
        for (com.neo.springapp.model.EmiPayment emi : emiPaymentRepository.findAll()) {
            if (!customerAccounts.contains(emi.getAccountNumber()) && !customerAccounts.contains(emi.getLoanAccountNumber())) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("emiId", emi.getId());
            row.put("loanId", emi.getLoanId());
            row.put("loanAccountNumber", emi.getLoanAccountNumber());
            row.put("accountNumber", emi.getAccountNumber());
            row.put("emiNumber", emi.getEmiNumber());
            row.put("dueDate", emi.getDueDate());
            row.put("paymentDate", emi.getPaymentDate());
            row.put("principalAmount", safeDouble(emi.getPrincipalAmount()));
            row.put("interestAmount", safeDouble(emi.getInterestAmount()));
            row.put("totalAmount", safeDouble(emi.getTotalAmount()));
            row.put("remainingPrincipal", safeDouble(emi.getRemainingPrincipal()));
            row.put("status", emi.getStatus());
            row.put("transactionId", emi.getTransactionId());
            row.put("balanceBeforePayment", safeDouble(emi.getBalanceBeforePayment()));
            row.put("balanceAfterPayment", safeDouble(emi.getBalanceAfterPayment()));
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> allocationEcsDashboardData(Long allocationId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (ecsMandateRepository == null) return result;
        Set<String> customerAccounts = allocationCustomerAccounts(allocationId);
        for (com.neo.springapp.model.EcsMandate mandate : ecsMandateRepository.findAllByOrderByCreatedAtDesc()) {
            if (!customerAccounts.contains(mandate.getSavingsAccountNumber())
                    && !customerAccounts.contains(mandate.getLoanAccountNumber())) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("mandateId", mandate.getMandateId());
            row.put("loanAccountNumber", mandate.getLoanAccountNumber());
            row.put("loanType", mandate.getLoanType());
            row.put("savingsAccountNumber", mandate.getSavingsAccountNumber());
            row.put("customerName", mandate.getCustomerName());
            row.put("debitDay", mandate.getDebitDay());
            row.put("amountLimit", safeDouble(mandate.getAmountLimit()));
            row.put("emiAmount", safeDouble(mandate.getEmiAmount()));
            row.put("status", mandate.getStatus());
            row.put("lastAttemptDate", mandate.getLastAttemptDate());
            row.put("lastDebitAt", mandate.getLastDebitAt());
            row.put("lastDebitStatus", mandate.getLastDebitStatus());
            row.put("lastDebitMessage", mandate.getLastDebitMessage());
            row.put("successfulDebits", mandate.getSuccessfulDebits());
            row.put("failedDebits", mandate.getFailedDebits());
            row.put("linked", mandate.getCustomerConsentAt() != null || mandate.getStatus() != null);
            row.put("autoDebit", "ACTIVE".equalsIgnoreCase(mandate.getStatus()));
            if (ecsMandateEventRepository != null && mandate.getId() != null) {
                row.put("events", ecsMandateEventRepository.findByMandateDbIdOrderByCreatedAtDesc(mandate.getId())
                        .stream().map(event -> {
                            Map<String, Object> eventData = new LinkedHashMap<>();
                            eventData.put("eventType", event.getEventType());
                            eventData.put("emiNumber", event.getEmiNumber());
                            eventData.put("amount", safeDouble(event.getAmount()));
                            eventData.put("message", event.getMessage());
                            eventData.put("createdAt", event.getCreatedAt());
                            return eventData;
                        }).toList());
            } else {
                row.put("events", List.of());
            }
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> allocationAccountDepositData(Long allocationId) {
        Map<String, Map<String, Object>> accounts = new LinkedHashMap<>();
        for (AllocationUtilization entry : utilizationRepository.findByAllocationIdOrderByTransactionDateDesc(allocationId)) {
            String account = entry.getUserAccountNumber() == null || entry.getUserAccountNumber().isBlank()
                    ? "Account not recorded" : entry.getUserAccountNumber().trim();
            Map<String, Object> row = accounts.computeIfAbsent(account, key -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("accountNumber", key);
                data.put("customerName", "");
                data.put("depositTotal", 0.0);
                data.put("withdrawalTotal", 0.0);
                data.put("allocatedDebitTotal", 0.0);
                data.put("allocationCreditTotal", 0.0);
                data.put("transactionCount", 0L);
                data.put("lastTransactionDate", null);
                return data;
            });
            if (entry.getUserName() != null && entry.getUserName().length() > 0) row.put("customerName", entry.getUserName());
            double amount = safeDouble(entry.getAmount());
            String product = String.valueOf(entry.getProductType()).toUpperCase();
            if ("DEBIT".equalsIgnoreCase(entry.getTransactionType())) {
                if (product.contains("FD") || product.contains("RD") || product.contains("DEPOSIT")) {
                    row.put("depositTotal", safeDouble(row.get("depositTotal")) + amount);
                } else {
                    row.put("allocatedDebitTotal", safeDouble(row.get("allocatedDebitTotal")) + amount);
                }
            } else if ("CREDIT".equalsIgnoreCase(entry.getTransactionType())) {
                row.put("allocationCreditTotal", safeDouble(row.get("allocationCreditTotal")) + amount);
            }
            row.put("transactionCount", safeLong(row.get("transactionCount")) + 1L);
            if (entry.getTransactionDate() != null && row.get("lastTransactionDate") == null) row.put("lastTransactionDate", entry.getTransactionDate());
        }
        return new ArrayList<>(accounts.values());
    }

    private Map<String, Object> allocationProfitabilityData(Long allocationId) {
        Map<String, Object> profit = new LinkedHashMap<>();
        double interestCollected = safeDouble(chargeTransactionRepository.sumSuccessfulInterestCharges(allocationId));
        double feesCollected = safeDouble(chargeTransactionRepository.sumCreditedCharges(allocationId));
        double principalDistributed = safeDouble(utilizationRepository.sumByAllocationAndProductTypes(
                allocationId, "DEBIT", List.of("LOAN", "LOANS", "PERSONAL_LOAN", "GOLD_LOAN", "HOME_LOAN",
                        "CAR_LOAN", "AUTO_LOAN", "CROP_LOAN", "EDUCATION_LOAN", "CREDIT_CARD_LOAN")));
        double principalRecovered = safeDouble(utilizationRepository.sumByAllocationAndProductTypes(
                allocationId, "CREDIT", List.of("LOAN", "LOANS", "PERSONAL_LOAN", "GOLD_LOAN", "HOME_LOAN",
                        "CAR_LOAN", "AUTO_LOAN", "CROP_LOAN", "EDUCATION_LOAN", "CREDIT_CARD_LOAN")));
        double net = interestCollected + feesCollected + principalRecovered - principalDistributed;
        profit.put("principalDistributed", principalDistributed);
        profit.put("principalRecovered", principalRecovered);
        profit.put("interestCollected", interestCollected);
        profit.put("feesCollected", feesCollected);
        profit.put("lossesAndBounces", 0.0);
        profit.put("netProfitLoss", net);
        profit.put("result", net >= 0 ? "PROFIT" : "LOSS");
        return profit;
    }

    private long safeLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Map<String, Object> transactionDashboardData(AllocationUtilization transaction) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionId", transaction.getUtilizationId());
        data.put("type", transaction.getTransactionType());
        data.put("transactionType", transaction.getTransactionType());
        data.put("amount", transaction.getAmount());
        data.put("balanceAfter", transaction.getRemainingBalance());
        data.put("productType", transaction.getProductType());
        data.put("userAccountNumber", transaction.getUserAccountNumber());
        data.put("userName", transaction.getUserName());
        data.put("linkedTransactionId", transaction.getLinkedTransactionId());
        data.put("referenceNumber", transaction.getReferenceNumber());
        data.put("description", transaction.getDescription());
        data.put("remarks", transaction.getRemarks());
        data.put("performedByName", transaction.getPerformedByName());
        data.put("performedByAdminId", transaction.getPerformedByAdminId());
        data.put("status", transaction.getStatus());
        data.put("sourceStatus", transaction.getSourceStatus());
        data.put("transactionDate", transaction.getTransactionDate());
        return data;
    }

    private Map<String, Object> chargeDashboardData(ChargeTransaction charge) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionId", charge.getChargeTransactionId());
        data.put("chargeType", charge.getChargeType());
        data.put("chargeCategory", chargeCategory(charge));
        data.put("description", charge.getChargeDescription());
        data.put("amount", charge.getChargeAmount());
        data.put("creditedAmount", charge.getNetCreditAmount());
        data.put("collectionStatus", charge.getCollectionStatus());
        data.put("collectionDate", charge.getCollectionDate());
        data.put("collectionDetails", charge.getCollectionDetails());
        data.put("creditStatus", charge.getCreditStatus());
        data.put("status", charge.getStatus());
        data.put("creditReference", charge.getCreditReferenceNumber());
        data.put("userAccountNumber", charge.getUserAccountNumber());
        data.put("userName", charge.getUserName());
        data.put("userProductType", charge.getUserProductType());
        data.put("linkedTransactionId", charge.getLinkedTransactionId());
        data.put("linkedLoanId", charge.getLinkedLoanId());
        data.put("linkedDepositId", charge.getLinkedDepositId());
        data.put("taxAmount", charge.getTaxAmount());
        data.put("failureReason", charge.getFailureReason());
        data.put("processedByAdminId", charge.getProcessedByAdminId());
        data.put("processedByAdminName", charge.getProcessedByAdminName());
        data.put("processedAt", charge.getProcessedAt());
        data.put("reconciliationStatus", charge.getReconciliationStatus());
        data.put("reconciliationDate", charge.getReconciliationDate());
        data.put("reconciliationNotes", charge.getReconciliationNotes());
        data.put("createdAt", charge.getCreatedAt());
        data.put("updatedAt", charge.getUpdatedAt());
        data.put("creditedAt", charge.getCreditedAt());
        data.put("allocationId", charge.getAllocationId());
        data.put("allocationAccountId", charge.getAllocationAccountId());
        return data;
    }

    private String chargeCategory(ChargeTransaction charge) {
        String type = charge.getChargeType() == null ? "" : charge.getChargeType().toUpperCase(java.util.Locale.ROOT);
        String text = (type + " " + (charge.getChargeDescription() == null ? "" : charge.getChargeDescription()))
                .toUpperCase(java.util.Locale.ROOT).replace('_', ' ').replace('-', ' ');
        if (text.contains("CIBIL")) return "CIBIL";
        if (text.contains("CREDIT CARD") && text.contains("PROCESS")) return "CREDIT_CARD_PROCESSING_FEE";
        if (text.contains("GOLD") && text.contains("INTEREST")) return "GOLD_LOAN_INTEREST";
        if (text.contains("EMI") && (text.contains("BOUNCE") || text.contains("RETURN"))) return "EMI_BOUNCE";
        if (text.contains("CHEQUE") || text.contains("CHECK")) return "CHEQUE";
        if (text.contains("SMS") || text.contains("MESSAGE")) return "SMS";
        if (text.contains("MAINTENANCE") || text.contains("MAINTAIN")) return "MAINTENANCE";
        if (text.contains("IMPS")) return "IMPS";
        if (text.contains("ATM")) return "ATM";
        if (text.contains("PROCESS")) return "PROCESSING";
        if (text.contains("EMI")) return "EMI";
        if (text.contains("INTEREST")) return "INTEREST";
        return "OTHER";
    }

    private Map<String, Object> operationRequestData(AllocationAccountOperationRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", request.getId());
        data.put("allocationId", request.getAllocationReference());
        data.put("requestType", request.getRequestType());
        data.put("amount", request.getAmount());
        data.put("destinationAccountNumber", request.getDestinationAccountNumber());
        data.put("destinationName", request.getDestinationName());
        data.put("externalReference", request.getExternalReference());
        data.put("description", request.getDescription());
        data.put("status", request.getStatus());
        data.put("createdAt", request.getCreatedAt());
        data.put("reviewedAt", request.getReviewedAt());
        data.put("reviewedByName", request.getReviewedByName());
        data.put("reviewedByAdminId", request.getReviewedByAdminId());
        data.put("reviewNote", request.getReviewNote());
        return data;
    }

    private FundsAllocation resolveAllocationSession(SessionPrincipal principal) {
        if (principal == null || !"ALLOCATION_ACCOUNT".equals(principal.scope())
                || principal.userId() == null || principal.userId() >= 0
                || principal.accountNumber() == null || !principal.accountNumber().startsWith("ALLOCATION:")) {
            return null;
        }
        FundsAllocation allocation = allocationRepository.findById(-principal.userId()).orElse(null);
        if (allocation == null || !("ALLOCATION:" + allocation.getAllocationId()).equals(principal.accountNumber())
                || !isAllocationActive(allocation)) return null;
        AllocationAccount account = linkedAccountRepository.findByAllocationId(allocation.getId()).orElse(null);
        return account != null && isLinkedAccountApproved(account) ? allocation : null;
    }

    private FundsAllocation requireHodOwnedAllocation(Long allocationId, SessionPrincipal principal) {
        if (principal == null || !"HOD".equals(principal.scope()) || principal.userId() == null) return null;
        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        return allocation != null && principal.userId().equals(allocation.getAllocatedByAdminId()) ? allocation : null;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private double safeDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value == null) return 0.0;
        try { return Double.parseDouble(value.toString()); } catch (Exception ignored) { return 0.0; }
    }
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
    public record OperationRequestPayload(String requestType, BigDecimal amount,
                                          String destinationAccountNumber, String destinationName,
                                          String externalReference, String description) {}
    public record ReviewDecisionPayload(String decision, String reviewNote) {}
}
