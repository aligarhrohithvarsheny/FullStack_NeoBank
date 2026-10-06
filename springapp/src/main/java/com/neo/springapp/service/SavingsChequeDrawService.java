package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * Service for Savings Account Cheque Draw System
 * Handles savings account cheque drawing with admin approval workflow
 * Mirrors the salary account cheque draw pattern
 */
@Service
public class SavingsChequeDrawService {

    @Autowired
    private SavingsChequeRequestRepository chequeRequestRepository;

    @Autowired
    private SavingsChequeAuditLogRepository auditLogRepository;

    @Autowired
    private SavingsChequeSequenceRepository sequenceRepository;

    @Autowired
    private SavingsChequeBankRangeRepository chequeBankRangeRepository;

    @Autowired
    private SavingsChequeLeafRepository chequeLeafRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ChequeRequestRepository salaryChequeRequestRepository;

    @Autowired
    private BusinessChequeRequestRepository businessChequeRequestRepository;

    @Autowired
    private ChequeRepository legacyChequeRepository;

    @Autowired
    private PositivePayRequestRepository positivePayRequestRepository;

    @Autowired
    private BusinessTransactionRepository businessTransactionRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GlobalTransactionIdGenerator globalTransactionIdGenerator;

    private static final int MAX_CHEQUE_LEAVES = 30;

    @Value("${positivepay.minimum.amount:10000}")
    private BigDecimal positivePayMinimumAmount;

    // ==================== USER OPERATIONS ====================

    @Transactional
    public Map<String, Object> applyChequeDrawRequest(Long accountId, String serialNumber,
                                                       String chequeDate, Double amount, String payeeName,
                                                       String remarks) {
        if (amount <= 0) throw new RuntimeException("Amount must be greater than 0");
        if (amount >= 50_00_000) throw new RuntimeException("Amount cannot exceed ₹50,00,000");

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Savings account not found"));

        if (account.getStatus() != null && !"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new RuntimeException("Account is " + account.getStatus().toLowerCase() + ". Cannot process cheque requests.");
        }

        BigDecimal availableBalance = BigDecimal.valueOf(account.getBalance() != null ? account.getBalance() : 0.0);
        if (availableBalance.compareTo(BigDecimal.valueOf(amount)) < 0) {
            throw new RuntimeException("Insufficient balance. Available: ₹" + availableBalance);
        }

        SavingsChequeLeaf leaf = chequeLeafRepository.findByLeafNumberAndAccountId(serialNumber, accountId)
                .orElseThrow(() -> new RuntimeException("Invalid cheque leaf number. This leaf is not allocated to your account."));
        if (!"AVAILABLE".equals(leaf.getStatus())) {
            throw new RuntimeException("This cheque leaf has already been used.");
        }

        String chequeNumber = generateUniqueChequeNumber(accountId);

        SavingsChequeRequest request = new SavingsChequeRequest();
        request.setUserId(account.getId());
        request.setAccountId(accountId);
        request.setChequeNumber(chequeNumber);
        request.setSerialNumber(serialNumber);
        request.setRequestDate(LocalDate.now());
        request.setChequeDate(LocalDate.parse(chequeDate));
        request.setAmount(BigDecimal.valueOf(amount));
        request.setAvailableBalance(availableBalance);
        request.setPayeeName(payeeName);
        request.setRemarks(remarks);
        boolean selfPayee = payeeName != null && payeeName.trim().equalsIgnoreCase("SELF");
        boolean positivePayRequired = !selfPayee && BigDecimal.valueOf(amount).compareTo(positivePayMinimumAmount) >= 0;
        request.setStatus(selfPayee ? "SELF_CASH" : (positivePayRequired ? "AWAITING_POSITIVE_PAY" : "PENDING"));
        if (selfPayee) request.setPayeeName("SELF");
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        SavingsChequeRequest saved = chequeRequestRepository.save(request);

        leaf.setStatus("USED");
        leaf.setUsedChequeRequestId(saved.getId());
        leaf.setUsedAt(LocalDateTime.now());
        chequeLeafRepository.save(leaf);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Savings cheque draw request submitted successfully");
        response.put("chequeNumber", chequeNumber);
        response.put("requestId", saved.getId());
        response.put("status", request.getStatus());
        if (positivePayRequired) {
            response.put("positivePayRequired", true);
            response.put("positivePayMessage", "This cheque is ₹10,000 or above. Register it in Positive Pay before admin draw verification.");
            response.put("positivePayAccountNumber", account.getAccountNumber());
        }
        return response;
    }

    public Map<String, Object> getUserChequeDrawRequests(Long accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SavingsChequeRequest> requests = chequeRequestRepository.findByAccountIdOrderByCreatedAtDesc(
                accountId, pageable
        );

        List<Map<String, Object>> items = new ArrayList<>();
        for (SavingsChequeRequest req : requests.getContent()) {
            items.add(mapChequeRequestToHistory(req));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", items);
        response.put("totalPages", requests.getTotalPages());
        response.put("totalItems", requests.getTotalElements());
        response.put("currentPage", page);
        return response;
    }

    public Map<String, Object> getChequeDrawDetails(Long id) {
        SavingsChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings cheque request not found"));
        return mapChequeRequestToDetail(request);
    }

    @Transactional
    public Map<String, Object> cancelChequeDrawRequest(Long id, String reason) {
        SavingsChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings cheque request not found"));

        if (!request.getStatus().equals("PENDING") && !request.getStatus().equals("AWAITING_POSITIVE_PAY")) {
            throw new RuntimeException("Only pending cheques can be cancelled");
        }

        request.setStatus("CANCELLED");
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        releaseChequeLeaf(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Savings cheque request cancelled successfully");
        response.put("chequeNumber", request.getChequeNumber());
        return response;
    }

    /**
     * User edits a pending savings cheque draw request (payeeName and amount only)
     */
    @Transactional
    public Map<String, Object> editPendingChequeDrawRequest(Long id, String payeeName, Double amount) {
        SavingsChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings cheque request not found"));

        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING cheques can be edited");
        }

        if (payeeName != null && !payeeName.trim().isEmpty()) {
            request.setPayeeName(payeeName.trim());
        }

        if (amount != null) {
            if (amount <= 0) throw new RuntimeException("Amount must be greater than 0");
            if (amount >= 50_00_000) throw new RuntimeException("Amount cannot exceed ₹50,00,000");

            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new RuntimeException("Savings account not found"));
            BigDecimal availableBalance = BigDecimal.valueOf(account.getBalance() != null ? account.getBalance() : 0.0);
            if (availableBalance.compareTo(BigDecimal.valueOf(amount)) < 0) {
                throw new RuntimeException("Insufficient balance. Available: ₹" + availableBalance);
            }
            request.setAmount(BigDecimal.valueOf(amount));
            request.setAvailableBalance(availableBalance);
        }

        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Savings cheque request updated successfully");
        response.put("chequeNumber", request.getChequeNumber());
        response.put("payeeName", request.getPayeeName());
        response.put("amount", request.getAmount().doubleValue());
        return response;
    }

    /**
     * Admin reverts a drawn/approved savings cheque request within 24 hours
     */
    @Transactional
    public Map<String, Object> revertChequeDrawRequest(Long id, String adminEmail, String reason) {
        SavingsChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings cheque request not found"));

        if (!"APPROVED".equals(request.getStatus()) && !"COMPLETED".equals(request.getStatus()) && !"CLEARED".equals(request.getStatus())) {
            throw new RuntimeException("Only approved or drawn cheques can be reverted. Current status: " + request.getStatus());
        }

        LocalDateTime actionTime = request.getApprovedAt() != null ? request.getApprovedAt() : request.getUpdatedAt();
        if (actionTime != null && LocalDateTime.now().isAfter(actionTime.plusHours(24))) {
            throw new RuntimeException("Cheque drawing can only be reverted within 24 hours of approval.");
        }

        Account senderAccount = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Savings account not found"));

        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        BigDecimal senderBalance = BigDecimal.valueOf(senderAccount.getBalance() != null ? senderAccount.getBalance() : 0.0);
        BigDecimal newSenderBalance = senderBalance.add(amount);

        // Refund/credit amount back to sender
        senderAccount.setBalance(newSenderBalance.doubleValue());
        senderAccount.setLastUpdated(LocalDateTime.now());
        accountRepository.save(senderAccount);

        // Update status
        request.setStatus("REVERTED");
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        // Log audit
        logAuditAction(id, adminEmail, "REVERT",
                "Savings cheque reverted by admin within 24h. Refunded ₹" + amount + " to " + senderAccount.getAccountNumber() +
                ". Reason: " + (reason != null ? reason : "Admin Revert"));

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Savings cheque request reverted successfully. Amount ₹" + amount + " credited back to account " + senderAccount.getAccountNumber());
        resp.put("chequeNumber", request.getChequeNumber());
        resp.put("revertedAmount", amount.doubleValue());
        resp.put("revertedAt", LocalDateTime.now());
        resp.put("revertedBy", adminEmail);
        return resp;
    }

    // ==================== ADMIN OPERATIONS ====================

    public Map<String, Object> getAdminChequeDrawRequests(String status, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SavingsChequeRequest> requests;

        if ("AWAITING_POSITIVE_PAY".equalsIgnoreCase(status) || "SELF_CASH".equalsIgnoreCase(status)) {
            requests = Page.empty(pageable);
        } else if (status != null && !status.isEmpty() && search != null && !search.isEmpty()) {
            requests = chequeRequestRepository.findByStatusAndChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(
                    status, search, pageable);
        } else if (status != null && !status.isEmpty()) {
            requests = chequeRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (search != null && !search.isEmpty()) {
            requests = chequeRequestRepository.findByStatusNotInAndChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(
                    List.of("AWAITING_POSITIVE_PAY", "SELF_CASH"), search, pageable);
        } else {
            requests = chequeRequestRepository.findByStatusNotInOrderByCreatedAtDesc(
                    List.of("AWAITING_POSITIVE_PAY", "SELF_CASH"), pageable);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (SavingsChequeRequest req : requests.getContent()) {
            items.add(mapChequeRequestToAdminView(req));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", items);
        response.put("totalPages", requests.getTotalPages());
        response.put("totalItems", requests.getTotalElements());
        response.put("currentPage", page);
        return response;
    }

    public Map<String, Object> getAdminChequeDrawDetails(Long id) {
        SavingsChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings cheque request not found"));

        Map<String, Object> detail = mapChequeRequestToAdminView(request);

        List<SavingsChequeAuditLog> auditLog = auditLogRepository.findByChequeRequestIdOrderByTimestampDesc(id);
        List<Map<String, Object>> auditData = new ArrayList<>();
        for (SavingsChequeAuditLog log : auditLog) {
            auditData.add(mapAuditLogToDetail(log));
        }
        detail.put("auditLog", auditData);
        return detail;
    }

    public Map<String, Object> verifyPayeeAccount(String payeeAccountNumber, String expectedPayeeName) {
        Map<String, Object> result = new HashMap<>();

        // Check in savings accounts
        Account savingsPayee = accountRepository.findByAccountNumber(payeeAccountNumber);
        if (savingsPayee != null) {
            String accountHolderName = savingsPayee.getName();
            boolean nameMatch = accountHolderName != null &&
                    accountHolderName.trim().equalsIgnoreCase(expectedPayeeName.trim());
            result.put("found", true);
            result.put("accountType", "Savings");
            result.put("accountHolderName", accountHolderName);
            result.put("accountNumber", payeeAccountNumber);
            result.put("nameMatch", nameMatch);
            result.put("verified", nameMatch);
            if (!nameMatch) {
                result.put("message", "Name mismatch: Account holder is '" + accountHolderName +
                        "' but payee name is '" + expectedPayeeName + "'");
            } else {
                result.put("message", "Payee verified successfully");
            }
            return result;
        }

        // Check in salary accounts
        SalaryAccount salaryAccount = salaryAccountRepository.findByAccountNumber(payeeAccountNumber);
        if (salaryAccount != null) {
            String accountHolderName = salaryAccount.getEmployeeName();
            boolean nameMatch = accountHolderName != null &&
                    accountHolderName.trim().equalsIgnoreCase(expectedPayeeName.trim());
            result.put("found", true);
            result.put("accountType", "Salary");
            result.put("accountHolderName", accountHolderName);
            result.put("accountNumber", payeeAccountNumber);
            result.put("nameMatch", nameMatch);
            result.put("verified", nameMatch);
            if (!nameMatch) {
                result.put("message", "Name mismatch: Account holder is '" + accountHolderName +
                        "' but payee name is '" + expectedPayeeName + "'");
            } else {
                result.put("message", "Payee verified successfully");
            }
            return result;
        }

        // Check in current accounts
        Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(payeeAccountNumber);
        if (currentOpt.isPresent()) {
            CurrentAccount current = currentOpt.get();
            String accountHolderName = current.getOwnerName();
            boolean nameMatch = accountHolderName != null &&
                    accountHolderName.trim().equalsIgnoreCase(expectedPayeeName.trim());
            result.put("found", true);
            result.put("accountType", "Business");
            result.put("accountHolderName", accountHolderName);
            result.put("businessName", current.getBusinessName());
            result.put("accountNumber", payeeAccountNumber);
            result.put("nameMatch", nameMatch);
            result.put("verified", nameMatch);
            if (!nameMatch) {
                result.put("message", "Name mismatch: Account holder is '" + accountHolderName +
                        "' but payee name is '" + expectedPayeeName + "'");
            } else {
                result.put("message", "Payee verified successfully");
            }
            return result;
        }

        result.put("found", false);
        result.put("verified", false);
        result.put("message", "No account found with number: " + payeeAccountNumber);
        return result;
    }

        private void requirePositivePayApproval(String accountNumber, String chequeNumber, BigDecimal amount) {
        if (amount.compareTo(positivePayMinimumAmount) < 0) return;
        boolean approved = positivePayRequestRepository
            .findFirstByAccountNumberAndChequeNumberAndStatusIn(accountNumber, chequeNumber,
                List.of(PositivePayStatus.PENDING_ADMIN_APPROVAL, PositivePayStatus.APPROVED,
                    PositivePayStatus.MATCHED, PositivePayStatus.MISMATCH))
            .map(request -> request.getStatus() == PositivePayStatus.APPROVED)
            .orElse(false);
        if (!approved) throw new RuntimeException("Positive Pay must be approved before cheque approval");
        }

        @Transactional
        public Map<String, Object> approveChequeDrawRequest(Long id, String adminEmail, String remarks,
                                                         String payeeAccountNumber) {
        SavingsChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings cheque request not found"));

        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING cheques can be approved");
        }

        if (payeeAccountNumber == null || payeeAccountNumber.trim().isEmpty()) {
            throw new RuntimeException("Payee account number is required for approval");
        }

        Map<String, Object> verification = verifyPayeeAccount(payeeAccountNumber, request.getPayeeName());
        if (!(Boolean) verification.get("verified")) {
            throw new RuntimeException((String) verification.get("message"));
        }

        String payeeAccountType = (String) verification.get("accountType");

        Account senderAccount = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Savings account not found"));

        BigDecimal amount = request.getAmount();
        requirePositivePayApproval(senderAccount.getAccountNumber(), request.getChequeNumber(), amount);
        BigDecimal senderBalance = BigDecimal.valueOf(senderAccount.getBalance() != null ? senderAccount.getBalance() : 0.0);

        if (senderBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance. Available: ₹" + senderBalance + ", Required: ₹" + amount);
        }

        // Debit from sender savings account
        BigDecimal newSenderBalance = senderBalance.subtract(amount);
        senderAccount.setBalance(newSenderBalance.doubleValue());
        senderAccount.setLastUpdated(LocalDateTime.now());
        accountRepository.save(senderAccount);

        // Credit to payee account
        Double newPayeeBalance;
        if ("Business".equals(payeeAccountType)) {
            CurrentAccount payeeAccount = currentAccountRepository.findByAccountNumber(payeeAccountNumber)
                    .orElseThrow(() -> new RuntimeException("Payee current account not found"));
            BigDecimal payeeBalance = BigDecimal.valueOf(payeeAccount.getBalance() != null ? payeeAccount.getBalance() : 0.0);
            newPayeeBalance = payeeBalance.add(amount).doubleValue();
            payeeAccount.setBalance(newPayeeBalance);
            payeeAccount.setLastUpdated(LocalDateTime.now());
            currentAccountRepository.save(payeeAccount);
        } else if ("Salary".equals(payeeAccountType)) {
            SalaryAccount payeeAccount = salaryAccountRepository.findByAccountNumber(payeeAccountNumber);
            BigDecimal payeeBalance = BigDecimal.valueOf(payeeAccount.getBalance() != null ? payeeAccount.getBalance() : 0.0);
            newPayeeBalance = payeeBalance.add(amount).doubleValue();
            payeeAccount.setBalance(newPayeeBalance);
            payeeAccount.setUpdatedAt(LocalDateTime.now());
            salaryAccountRepository.save(payeeAccount);
        } else {
            Account payeeAccount = accountRepository.findByAccountNumber(payeeAccountNumber);
            double payeeBalance = payeeAccount.getBalance() != null ? payeeAccount.getBalance() : 0.0;
            newPayeeBalance = payeeBalance + amount.doubleValue();
            payeeAccount.setBalance(newPayeeBalance);
            payeeAccount.setLastUpdated(LocalDateTime.now());
            accountRepository.save(payeeAccount);
        }

        String txnRef = "SCHQ-TXN-" + System.currentTimeMillis();

        // Debit transaction for sender
        Long senderGlobalTxnId = null;
        try { senderGlobalTxnId = globalTransactionIdGenerator.getNextTransactionId(); } catch (Exception ignored) {}
        Transaction senderTxn = new Transaction();
        senderTxn.setGlobalTransactionSequence(senderGlobalTxnId);
        senderTxn.setAccountNumber(senderAccount.getAccountNumber());
        senderTxn.setMerchant("Savings Cheque Payment");
        senderTxn.setAmount(amount.doubleValue());
        senderTxn.setType("Debit");
        senderTxn.setDescription("Savings Cheque " + request.getChequeNumber() + " to " + request.getPayeeName() + " | Ref: " + txnRef);
        senderTxn.setBalance(newSenderBalance.doubleValue());
        senderTxn.setUserName(senderAccount.getName());
        senderTxn.setRecipientAccountNumber(payeeAccountNumber);
        senderTxn.setRecipientName(request.getPayeeName());
        senderTxn.setStatus("Completed");
        senderTxn.setDate(LocalDateTime.now());
        transactionRepository.save(senderTxn);

        // Credit transaction for payee
        Long payeeGlobalTxnId = null;
        try { payeeGlobalTxnId = globalTransactionIdGenerator.getNextTransactionId(); } catch (Exception ignored) {}
        if ("Business".equals(payeeAccountType)) {
            BusinessTransaction payeeTxn = new BusinessTransaction();
            payeeTxn.setGlobalTransactionSequence(payeeGlobalTxnId);
            payeeTxn.setAccountNumber(payeeAccountNumber);
            payeeTxn.setTxnType("Credit");
            payeeTxn.setAmount(amount.doubleValue());
            payeeTxn.setChargeAmount(0.0);
            payeeTxn.setRecipientAccount(senderAccount.getAccountNumber());
            payeeTxn.setDescription("Savings Cheque " + request.getChequeNumber() + " from " + senderAccount.getName() + " | Ref: " + txnRef);
            payeeTxn.setBalance(newPayeeBalance);
            payeeTxn.setStatus("Completed");
            payeeTxn.setDate(LocalDateTime.now());
            businessTransactionRepository.save(payeeTxn);
        } else {
            Transaction payeeTxn = new Transaction();
            payeeTxn.setGlobalTransactionSequence(payeeGlobalTxnId);
            payeeTxn.setAccountNumber(payeeAccountNumber);
            payeeTxn.setMerchant("Savings Cheque Deposit");
            payeeTxn.setAmount(amount.doubleValue());
            payeeTxn.setType("Credit");
            payeeTxn.setDescription("Savings Cheque " + request.getChequeNumber() + " from " + senderAccount.getName() + " | Ref: " + txnRef);
            payeeTxn.setBalance(newPayeeBalance);
            payeeTxn.setUserName(request.getPayeeName());
            payeeTxn.setSourceAccountNumber(senderAccount.getAccountNumber());
            payeeTxn.setStatus("Completed");
            payeeTxn.setDate(LocalDateTime.now());
            transactionRepository.save(payeeTxn);
        }

        // Update cheque request
        request.setStatus("APPROVED");
        request.setApprovedBy(adminEmail);
        request.setApprovedAt(LocalDateTime.now());
        request.setPayeeAccountNumber(payeeAccountNumber);
        request.setPayeeAccountVerified(true);
        request.setPayeeAccountType(payeeAccountType);
        request.setTransactionReference(txnRef);
        request.setDebitedFromAccount(senderAccount.getAccountNumber());
        request.setCreditedToAccount(payeeAccountNumber);
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        logAuditAction(id, adminEmail, "APPROVE",
                (remarks != null ? remarks + " | " : "") +
                "Debited ₹" + amount + " from " + senderAccount.getAccountNumber() +
                ", Credited to " + payeeAccountNumber + " | Txn: " + txnRef);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Savings cheque approved. ₹" + amount + " debited from " +
                senderAccount.getAccountNumber() + " and credited to " + payeeAccountNumber);
        response.put("chequeNumber", request.getChequeNumber());
        response.put("transactionReference", txnRef);
        response.put("debitedFrom", senderAccount.getAccountNumber());
        response.put("creditedTo", payeeAccountNumber);
        response.put("newSenderBalance", newSenderBalance);
        response.put("status", "APPROVED");
        response.put("chequeRequest", mapChequeRequestToAdminView(request));
        return response;
    }

    @Transactional
    public Map<String, Object> rejectChequeDrawRequest(Long id, String adminEmail, String rejectionReason) {
        SavingsChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings cheque request not found"));

        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING cheques can be rejected");
        }

        request.setStatus("REJECTED");
        request.setRejectedBy(adminEmail);
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectionReason(rejectionReason);
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        logAuditAction(id, adminEmail, "REJECT", rejectionReason);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Savings cheque rejected successfully");
        response.put("chequeNumber", request.getChequeNumber());
        response.put("status", "REJECTED");
        response.put("chequeRequest", mapChequeRequestToAdminView(request));
        return response;
    }

    @Transactional
    public Map<String, Object> markChequeDrawPickedUp(Long id, String adminEmail) {
        SavingsChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings cheque request not found"));

        if (!request.getStatus().equals("APPROVED")) {
            throw new RuntimeException("Only APPROVED cheques can be marked as picked up");
        }

        request.setStatus("COMPLETED");
        request.setPickedUpAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        logAuditAction(id, adminEmail, "PICKUP", "Savings cheque picked up by user");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Savings cheque marked as picked up");
        response.put("status", "COMPLETED");
        return response;
    }

    @Transactional
    public Map<String, Object> clearChequeDrawRequest(Long id, String adminEmail, String clearedDate) {
        SavingsChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings cheque request not found"));

        if (!request.getStatus().equals("COMPLETED") && !request.getStatus().equals("APPROVED")) {
            throw new RuntimeException("Only APPROVED or COMPLETED cheques can be cleared");
        }

        request.setStatus("CLEARED");
        if (clearedDate != null) {
            request.setClearedAt(LocalDate.parse(clearedDate).atStartOfDay());
        } else {
            request.setClearedAt(LocalDateTime.now());
        }
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        logAuditAction(id, adminEmail, "CLEAR", "Savings cheque cleared and processed");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Savings cheque marked as cleared");
        response.put("status", "CLEARED");
        return response;
    }

    public Map<String, Object> getChequeDrawStats() {
        long pending = chequeRequestRepository.countByStatus("PENDING");
        long approved = chequeRequestRepository.countByStatus("APPROVED");
        long completed = chequeRequestRepository.countByStatus("COMPLETED");
        long rejected = chequeRequestRepository.countByStatus("REJECTED");

        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingApproval", pending);
        stats.put("approvedCount", approved);
        stats.put("completedCount", completed);
        stats.put("rejectedCount", rejected);
        stats.put("totalRequests", pending + approved + completed + rejected);
        return stats;
    }

    public Map<String, Object> getChequeDrawAuditLog(Long chequeRequestId) {
        List<SavingsChequeAuditLog> auditLog = auditLogRepository.findByChequeRequestIdOrderByTimestampDesc(chequeRequestId);
        List<Map<String, Object>> auditData = new ArrayList<>();

        for (SavingsChequeAuditLog log : auditLog) {
            auditData.add(mapAuditLogToDetail(log));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("auditLog", auditData);
        response.put("totalActions", auditData.size());
        return response;
    }

    public Map<String, Object> searchChequeDrawByNumber(String chequeNumber) {
        List<SavingsChequeRequest> requests = chequeRequestRepository.findByChequeNumberContainingIgnoreCase(chequeNumber);
        List<Map<String, Object>> results = new ArrayList<>();

        for (SavingsChequeRequest req : requests) {
            results.add(mapChequeRequestToAdminView(req));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("count", results.size());
        return response;
    }

    public byte[] exportChequeDrawToCSV(String status) {
        List<SavingsChequeRequest> requests;
        if (status != null && !status.isEmpty()) {
            requests = chequeRequestRepository.findByStatus(status);
        } else {
            requests = chequeRequestRepository.findAll();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Cheque #,Serial #,Account Holder,Account,Payee,Amount,Date,Status,Request Date\n");

        for (SavingsChequeRequest req : requests) {
            csv.append(String.format("%s,%s,%d,%d,%s,₹%.2f,%s,%s,%s\n",
                    req.getChequeNumber(),
                    req.getSerialNumber(),
                    req.getUserId(),
                    req.getAccountId(),
                    req.getPayeeName(),
                    req.getAmount(),
                    req.getChequeDate(),
                    req.getStatus(),
                    req.getCreatedAt()
            ));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public Map<String, Object> markChequeDownloaded(Long id) {
        SavingsChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings cheque request not found"));

        if (!"APPROVED".equals(request.getStatus()) && !"COMPLETED".equals(request.getStatus())) {
            throw new RuntimeException("Cheque must be approved before downloading");
        }

        request.setChequeDownloaded(true);
        request.setChequeDownloadedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Savings cheque marked as downloaded");
        response.put("downloadedAt", request.getChequeDownloadedAt());
        return response;
    }

    // ==================== CHEQUE LEAF ALLOCATION ====================

    @Transactional
    public Map<String, Object> getAvailableChequeLeaves(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Savings account not found"));

        long existingCount = chequeLeafRepository.countByAccountId(accountId);
        if (existingCount == 0) {
            allocateChequeLeaves(accountId, account.getId());
        }

        List<SavingsChequeLeaf> availableLeaves = chequeLeafRepository
                .findByAccountIdAndStatusOrderByLeafNumberAsc(accountId, "AVAILABLE");
        List<SavingsChequeLeaf> allLeaves = chequeLeafRepository
                .findByAccountIdOrderByLeafNumberAsc(accountId);

        List<Map<String, Object>> leafList = new ArrayList<>();
        for (SavingsChequeLeaf leaf : availableLeaves) {
            Map<String, Object> leafMap = new HashMap<>();
            leafMap.put("id", leaf.getId());
            leafMap.put("leafNumber", leaf.getLeafNumber());
            leafMap.put("status", leaf.getStatus());
            leafList.add(leafMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("leaves", leafList);
        response.put("totalAllocated", allLeaves.size());
        response.put("totalAvailable", availableLeaves.size());
        response.put("totalUsed", allLeaves.size() - availableLeaves.size());
        return response;
    }

    private synchronized void allocateChequeLeaves(Long accountId, Long userId) {
        if (chequeLeafRepository.countByAccountId(accountId) > 0) {
            return;
        }

        // Use a different range prefix (B-series) to differentiate from salary cheque leaves
        List<SavingsChequeLeaf> allLeaves = chequeLeafRepository.findAll();
        long maxNum = 500000; // Start from 500000 to separate from salary leaves
        for (SavingsChequeLeaf existing : allLeaves) {
            try {
                long num = Long.parseLong(existing.getLeafNumber());
                if (num > maxNum) maxNum = num;
            } catch (NumberFormatException ignored) {}
        }

        List<SavingsChequeLeaf> newLeaves = new ArrayList<>();
        for (int i = 1; i <= MAX_CHEQUE_LEAVES; i++) {
            String leafNumber = String.format("%06d", maxNum + i);
            SavingsChequeLeaf leaf = new SavingsChequeLeaf(accountId, userId, leafNumber);
            newLeaves.add(leaf);
        }
        chequeLeafRepository.saveAll(newLeaves);
    }

    @Transactional
    public void releaseChequeLeaf(Long chequeRequestId) {
        SavingsChequeRequest request = chequeRequestRepository.findById(chequeRequestId).orElse(null);
        if (request == null) return;

        SavingsChequeLeaf leaf = chequeLeafRepository
                .findByLeafNumberAndAccountId(request.getSerialNumber(), request.getAccountId())
                .orElse(null);
        if (leaf != null && "USED".equals(leaf.getStatus())) {
            leaf.setStatus("AVAILABLE");
            leaf.setUsedChequeRequestId(null);
            leaf.setUsedAt(null);
            chequeLeafRepository.save(leaf);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private String generateUniqueChequeNumber(Long accountId) {
        SavingsChequeSequence sequence = sequenceRepository.findByAccountId(accountId);
        if (sequence == null) {
            sequence = new SavingsChequeSequence();
            sequence.setAccountId(accountId);
            sequence.setNextSequence(1000L);
        }

        String chequeNumber;
        do {
            chequeNumber = String.format("SCHQ-%d-%06d", accountId, sequence.getNextSequence());
            sequence.setNextSequence(sequence.getNextSequence() + 1);
        } while (isChequeNumberUsed(chequeNumber));

        sequence.setUpdatedAt(LocalDateTime.now());
        sequenceRepository.save(sequence);

        return chequeNumber;
    }

    // Cheque numbers must be unique across every cheque system, not just savings draw requests
    private boolean isChequeNumberUsed(String chequeNumber) {
        return !chequeRequestRepository.findAllByChequeNumber(chequeNumber).isEmpty()
                || !salaryChequeRequestRepository.findAllByChequeNumber(chequeNumber).isEmpty()
                || !businessChequeRequestRepository.findAllByChequeNumber(chequeNumber).isEmpty()
                || legacyChequeRepository.findByChequeNumber(chequeNumber).isPresent();
    }

    private void logAuditAction(Long chequeRequestId, String adminEmail, String action, String remarks) {
        SavingsChequeAuditLog log = new SavingsChequeAuditLog();
        log.setChequeRequestId(chequeRequestId);
        log.setAdminEmail(adminEmail);
        log.setAction(action);
        log.setRemarks(remarks);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private Map<String, Object> mapChequeRequestToHistory(SavingsChequeRequest req) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", req.getId());
        map.put("chequeNumber", req.getChequeNumber());
        map.put("chequeDate", req.getChequeDate());
        map.put("payeeName", req.getPayeeName());
        map.put("amount", req.getAmount().doubleValue());
        map.put("status", req.getStatus());
        map.put("createdAt", req.getCreatedAt());
        map.put("chequeDownloaded", req.getChequeDownloaded() != null ? req.getChequeDownloaded() : false);
        map.put("chequeDownloadedAt", req.getChequeDownloadedAt());
        map.put("payeeAccountNumber", req.getPayeeAccountNumber());
        map.put("payeeAccountVerified", req.getPayeeAccountVerified() != null ? req.getPayeeAccountVerified() : false);
        map.put("payeeAccountType", req.getPayeeAccountType());
        map.put("transactionReference", req.getTransactionReference());
        map.put("debitedFromAccount", req.getDebitedFromAccount());
        map.put("creditedToAccount", req.getCreditedToAccount());
        map.put("approvedAt", req.getApprovedAt());
        return map;
    }

    private Map<String, Object> mapChequeRequestToDetail(SavingsChequeRequest req) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", req.getId());
        map.put("chequeNumber", req.getChequeNumber());
        map.put("serialNumber", req.getSerialNumber());
        map.put("chequeDate", req.getChequeDate());
        map.put("payeeName", req.getPayeeName());
        map.put("amount", req.getAmount().doubleValue());
        map.put("remarks", req.getRemarks());
        map.put("status", req.getStatus());
        map.put("createdAt", req.getCreatedAt());
        map.put("chequeDownloaded", req.getChequeDownloaded() != null ? req.getChequeDownloaded() : false);
        map.put("chequeDownloadedAt", req.getChequeDownloadedAt());
        map.put("payeeAccountNumber", req.getPayeeAccountNumber());
        map.put("payeeAccountVerified", req.getPayeeAccountVerified() != null ? req.getPayeeAccountVerified() : false);
        map.put("payeeAccountType", req.getPayeeAccountType());
        map.put("transactionReference", req.getTransactionReference());
        map.put("debitedFromAccount", req.getDebitedFromAccount());
        map.put("creditedToAccount", req.getCreditedToAccount());
        return map;
    }

    private Map<String, Object> mapChequeRequestToAdminView(SavingsChequeRequest req) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", req.getId());
        map.put("chequeNumber", req.getChequeNumber());
        map.put("serialNumber", req.getSerialNumber());
        map.put("userId", req.getUserId());
        map.put("accountId", req.getAccountId());
        map.put("chequeDate", req.getChequeDate());
        map.put("payeeName", req.getPayeeName());
        map.put("amount", req.getAmount().doubleValue());
        map.put("availableBalance", req.getAvailableBalance() != null ? req.getAvailableBalance().doubleValue() : 0.0);
        map.put("remarks", req.getRemarks());
        map.put("status", req.getStatus());
        map.put("approvedBy", req.getApprovedBy());
        map.put("approvedAt", req.getApprovedAt());
        map.put("rejectedBy", req.getRejectedBy());
        map.put("rejectionReason", req.getRejectionReason());
        map.put("createdAt", req.getCreatedAt());
        map.put("chequeDownloaded", req.getChequeDownloaded() != null ? req.getChequeDownloaded() : false);
        map.put("chequeDownloadedAt", req.getChequeDownloadedAt());
        map.put("payeeAccountNumber", req.getPayeeAccountNumber());
        map.put("payeeAccountVerified", req.getPayeeAccountVerified() != null ? req.getPayeeAccountVerified() : false);
        map.put("payeeAccountType", req.getPayeeAccountType());
        map.put("transactionReference", req.getTransactionReference());
        map.put("debitedFromAccount", req.getDebitedFromAccount());
        map.put("creditedToAccount", req.getCreditedToAccount());

        // Look up savings account for user details
        try {
            Optional<Account> accountOpt = accountRepository.findById(req.getAccountId());
            if (accountOpt.isPresent()) {
                Account ca = accountOpt.get();
                map.put("userName", ca.getName());
                map.put("userEmail", "-");
                map.put("accountNumber", ca.getAccountNumber());
                map.put("currentBalance", ca.getBalance() != null ? ca.getBalance() : 0.0);
            } else {
                map.put("userName", "Unknown");
                map.put("userEmail", "-");
                map.put("accountNumber", "-");
                map.put("currentBalance", 0.0);
            }
        } catch (Exception e) {
            map.put("userName", "Unknown");
            map.put("userEmail", "-");
            map.put("accountNumber", "-");
            map.put("currentBalance", 0.0);
        }

        return map;
    }

    private Map<String, Object> mapAuditLogToDetail(SavingsChequeAuditLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("adminEmail", log.getAdminEmail());
        map.put("action", log.getAction());
        map.put("remarks", log.getRemarks());
        map.put("timestamp", log.getTimestamp());
        return map;
    }
}
