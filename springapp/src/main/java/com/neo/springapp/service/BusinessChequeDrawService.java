package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Service for Business Account Cheque Draw System
 * Handles current/business account cheque drawing with admin approval workflow
 * Mirrors the salary account cheque draw pattern
 */
@Service
public class BusinessChequeDrawService {

    @Autowired
    private BusinessChequeRequestRepository chequeRequestRepository;

    @Autowired
    private BusinessChequeAuditLogRepository auditLogRepository;

    @Autowired
    private BusinessChequeSequenceRepository sequenceRepository;

    @Autowired
    private BusinessChequeBankRangeRepository chequeBankRangeRepository;

    @Autowired
    private BusinessChequeLeafRepository chequeLeafRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private BusinessTransactionRepository businessTransactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GlobalTransactionIdGenerator globalTransactionIdGenerator;

    private static final int MAX_CHEQUE_LEAVES = 30;

    // ==================== USER OPERATIONS ====================

    @Transactional
    public Map<String, Object> applyChequeDrawRequest(Long currentAccountId, String serialNumber,
                                                       String chequeDate, Double amount, String payeeName,
                                                       String remarks) {
        if (amount <= 0) throw new RuntimeException("Amount must be greater than 0");
        if (amount >= 50_00_000) throw new RuntimeException("Amount cannot exceed ₹50,00,000");

        CurrentAccount account = currentAccountRepository.findById(currentAccountId)
                .orElseThrow(() -> new RuntimeException("Business account not found"));

        if (account.getAccountFrozen() != null && account.getAccountFrozen()) {
            throw new RuntimeException("Account is frozen. Cannot process cheque requests.");
        }

        BigDecimal availableBalance = BigDecimal.valueOf(account.getBalance() != null ? account.getBalance() : 0.0);
        if (availableBalance.compareTo(BigDecimal.valueOf(amount)) < 0) {
            throw new RuntimeException("Insufficient balance. Available: ₹" + availableBalance);
        }

        BusinessChequeLeaf leaf = chequeLeafRepository.findByLeafNumberAndCurrentAccountId(serialNumber, currentAccountId)
                .orElseThrow(() -> new RuntimeException("Invalid cheque leaf number. This leaf is not allocated to your account."));
        if (!"AVAILABLE".equals(leaf.getStatus())) {
            throw new RuntimeException("This cheque leaf has already been used.");
        }

        String chequeNumber = generateUniqueChequeNumber(currentAccountId);

        BusinessChequeRequest request = new BusinessChequeRequest();
        request.setUserId(account.getId());
        request.setCurrentAccountId(currentAccountId);
        request.setChequeNumber(chequeNumber);
        request.setSerialNumber(serialNumber);
        request.setRequestDate(LocalDate.now());
        request.setChequeDate(LocalDate.parse(chequeDate));
        request.setAmount(BigDecimal.valueOf(amount));
        request.setAvailableBalance(availableBalance);
        request.setPayeeName(payeeName);
        request.setRemarks(remarks);
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        BusinessChequeRequest saved = chequeRequestRepository.save(request);

        leaf.setStatus("USED");
        leaf.setUsedChequeRequestId(saved.getId());
        leaf.setUsedAt(LocalDateTime.now());
        chequeLeafRepository.save(leaf);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Business cheque draw request submitted successfully");
        response.put("chequeNumber", chequeNumber);
        response.put("requestId", saved.getId());
        response.put("status", "PENDING");
        return response;
    }

    public Map<String, Object> getUserChequeDrawRequests(Long currentAccountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BusinessChequeRequest> requests = chequeRequestRepository.findByCurrentAccountIdOrderByCreatedAtDesc(
                currentAccountId, pageable
        );

        List<Map<String, Object>> items = new ArrayList<>();
        for (BusinessChequeRequest req : requests.getContent()) {
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
        BusinessChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business cheque request not found"));
        return mapChequeRequestToDetail(request);
    }

    @Transactional
    public Map<String, Object> cancelChequeDrawRequest(Long id, String reason) {
        BusinessChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business cheque request not found"));

        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING cheques can be cancelled");
        }

        request.setStatus("CANCELLED");
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        releaseChequeLeaf(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Business cheque request cancelled successfully");
        response.put("chequeNumber", request.getChequeNumber());
        return response;
    }

    /**
     * User edits a pending business cheque draw request (payeeName and amount only)
     */
    @Transactional
    public Map<String, Object> editPendingChequeDrawRequest(Long id, String payeeName, Double amount) {
        BusinessChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business cheque request not found"));

        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING cheques can be edited");
        }

        if (payeeName != null && !payeeName.trim().isEmpty()) {
            request.setPayeeName(payeeName.trim());
        }

        if (amount != null) {
            if (amount <= 0) throw new RuntimeException("Amount must be greater than 0");
            if (amount >= 50_00_000) throw new RuntimeException("Amount cannot exceed ₹50,00,000");

            CurrentAccount account = currentAccountRepository.findById(request.getCurrentAccountId())
                    .orElseThrow(() -> new RuntimeException("Business account not found"));
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
        response.put("message", "Business cheque request updated successfully");
        response.put("chequeNumber", request.getChequeNumber());
        response.put("payeeName", request.getPayeeName());
        response.put("amount", request.getAmount().doubleValue());
        return response;
    }

    // ==================== ADMIN OPERATIONS ====================

    public Map<String, Object> getAdminChequeDrawRequests(String status, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BusinessChequeRequest> requests;

        if (status != null && !status.isEmpty() && search != null && !search.isEmpty()) {
            requests = chequeRequestRepository.findByStatusAndChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(
                    status, search, pageable);
        } else if (status != null && !status.isEmpty()) {
            requests = chequeRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (search != null && !search.isEmpty()) {
            requests = chequeRequestRepository.findByChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(search, pageable);
        } else {
            requests = chequeRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (BusinessChequeRequest req : requests.getContent()) {
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
        BusinessChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business cheque request not found"));

        Map<String, Object> detail = mapChequeRequestToAdminView(request);

        List<BusinessChequeAuditLog> auditLog = auditLogRepository.findByChequeRequestIdOrderByTimestampDesc(id);
        List<Map<String, Object>> auditData = new ArrayList<>();
        for (BusinessChequeAuditLog log : auditLog) {
            auditData.add(mapAuditLogToDetail(log));
        }
        detail.put("auditLog", auditData);
        return detail;
    }

    public Map<String, Object> verifyPayeeAccount(String payeeAccountNumber, String expectedPayeeName) {
        Map<String, Object> result = new HashMap<>();

        // Check in current/business accounts
        Optional<CurrentAccount> currentAccountOpt = currentAccountRepository.findByAccountNumber(payeeAccountNumber);
        if (currentAccountOpt.isPresent()) {
            CurrentAccount currentAccount = currentAccountOpt.get();
            String accountHolderName = currentAccount.getOwnerName();
            boolean nameMatch = accountHolderName != null &&
                    accountHolderName.trim().equalsIgnoreCase(expectedPayeeName.trim());
            result.put("found", true);
            result.put("accountType", "Business");
            result.put("accountHolderName", accountHolderName);
            result.put("businessName", currentAccount.getBusinessName());
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

        // Check in savings accounts
        Account account = accountRepository.findByAccountNumber(payeeAccountNumber);
        if (account != null) {
            String accountHolderName = account.getName();
            boolean nameMatch = accountHolderName != null &&
                    accountHolderName.trim().equalsIgnoreCase(expectedPayeeName.trim());
            result.put("found", true);
            result.put("accountType", account.getAccountType() != null ? account.getAccountType() : "Savings");
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

        result.put("found", false);
        result.put("verified", false);
        result.put("message", "No account found with number: " + payeeAccountNumber);
        return result;
    }

    @Transactional
    public Map<String, Object> approveChequeDrawRequest(Long id, String adminEmail, String remarks,
                                                         String payeeAccountNumber) {
        BusinessChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business cheque request not found"));

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

        CurrentAccount senderAccount = currentAccountRepository.findById(request.getCurrentAccountId())
                .orElseThrow(() -> new RuntimeException("Business account not found"));

        BigDecimal amount = request.getAmount();
        BigDecimal senderBalance = BigDecimal.valueOf(senderAccount.getBalance() != null ? senderAccount.getBalance() : 0.0);

        if (senderBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance. Available: ₹" + senderBalance + ", Required: ₹" + amount);
        }

        // Debit from sender business account
        BigDecimal newSenderBalance = senderBalance.subtract(amount);
        senderAccount.setBalance(newSenderBalance.doubleValue());
        senderAccount.setLastUpdated(LocalDateTime.now());
        currentAccountRepository.save(senderAccount);

        // Credit to payee account
        Double newPayeeBalance;
        if ("Business".equals(payeeAccountType)) {
            CurrentAccount payeeAccount = currentAccountRepository.findByAccountNumber(payeeAccountNumber)
                    .orElseThrow(() -> new RuntimeException("Payee business account not found"));
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

        String txnRef = "BCHQ-TXN-" + System.currentTimeMillis();

        // Create debit transaction for sender (business transaction)
        Long senderGlobalTxnId = null;
        try { senderGlobalTxnId = globalTransactionIdGenerator.getNextTransactionId(); } catch (Exception ignored) {}
        BusinessTransaction senderTxn = new BusinessTransaction();
        senderTxn.setGlobalTransactionSequence(senderGlobalTxnId);
        senderTxn.setAccountNumber(senderAccount.getAccountNumber());
        senderTxn.setTxnType("Debit");
        senderTxn.setAmount(amount.doubleValue());
        senderTxn.setChargeAmount(0.0);
        senderTxn.setRecipientAccount(payeeAccountNumber);
        senderTxn.setDescription("Business Cheque " + request.getChequeNumber() + " to " + request.getPayeeName() + " | Ref: " + txnRef);
        senderTxn.setBalance(newSenderBalance.doubleValue());
        senderTxn.setStatus("Completed");
        senderTxn.setDate(LocalDateTime.now());
        businessTransactionRepository.save(senderTxn);

        // Create credit transaction for payee
        if ("Business".equals(payeeAccountType)) {
            Long payeeGlobalTxnId = null;
            try { payeeGlobalTxnId = globalTransactionIdGenerator.getNextTransactionId(); } catch (Exception ignored) {}
            BusinessTransaction payeeTxn = new BusinessTransaction();
            payeeTxn.setGlobalTransactionSequence(payeeGlobalTxnId);
            payeeTxn.setAccountNumber(payeeAccountNumber);
            payeeTxn.setTxnType("Credit");
            payeeTxn.setAmount(amount.doubleValue());
            payeeTxn.setChargeAmount(0.0);
            payeeTxn.setRecipientAccount(senderAccount.getAccountNumber());
            payeeTxn.setDescription("Business Cheque " + request.getChequeNumber() + " from " + senderAccount.getBusinessName() + " | Ref: " + txnRef);
            payeeTxn.setBalance(newPayeeBalance);
            payeeTxn.setStatus("Completed");
            payeeTxn.setDate(LocalDateTime.now());
            businessTransactionRepository.save(payeeTxn);
        } else if ("Salary".equals(payeeAccountType)) {
            // Credit to salary account handled via SalaryNormalTransaction if available
            Long payeeGlobalTxnId = null;
            try { payeeGlobalTxnId = globalTransactionIdGenerator.getNextTransactionId(); } catch (Exception ignored) {}
            Transaction payeeTxn = new Transaction();
            payeeTxn.setGlobalTransactionSequence(payeeGlobalTxnId);
            payeeTxn.setAccountNumber(payeeAccountNumber);
            payeeTxn.setMerchant("Business Cheque Deposit");
            payeeTxn.setAmount(amount.doubleValue());
            payeeTxn.setType("Credit");
            payeeTxn.setDescription("Business Cheque " + request.getChequeNumber() + " from " + senderAccount.getBusinessName() + " | Ref: " + txnRef);
            payeeTxn.setBalance(newPayeeBalance);
            payeeTxn.setUserName(request.getPayeeName());
            payeeTxn.setSourceAccountNumber(senderAccount.getAccountNumber());
            payeeTxn.setStatus("Completed");
            payeeTxn.setDate(LocalDateTime.now());
            transactionRepository.save(payeeTxn);
        } else {
            Long payeeGlobalTxnId = null;
            try { payeeGlobalTxnId = globalTransactionIdGenerator.getNextTransactionId(); } catch (Exception ignored) {}
            Transaction payeeTxn = new Transaction();
            payeeTxn.setGlobalTransactionSequence(payeeGlobalTxnId);
            payeeTxn.setAccountNumber(payeeAccountNumber);
            payeeTxn.setMerchant("Business Cheque Deposit");
            payeeTxn.setAmount(amount.doubleValue());
            payeeTxn.setType("Credit");
            payeeTxn.setDescription("Business Cheque " + request.getChequeNumber() + " from " + senderAccount.getBusinessName() + " | Ref: " + txnRef);
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
        response.put("message", "Business cheque approved. ₹" + amount + " debited from " +
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
        BusinessChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business cheque request not found"));

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
        response.put("message", "Business cheque rejected successfully");
        response.put("chequeNumber", request.getChequeNumber());
        response.put("status", "REJECTED");
        response.put("chequeRequest", mapChequeRequestToAdminView(request));
        return response;
    }

    @Transactional
    public Map<String, Object> markChequeDrawPickedUp(Long id, String adminEmail) {
        BusinessChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business cheque request not found"));

        if (!request.getStatus().equals("APPROVED")) {
            throw new RuntimeException("Only APPROVED cheques can be marked as picked up");
        }

        request.setStatus("COMPLETED");
        request.setPickedUpAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        logAuditAction(id, adminEmail, "PICKUP", "Business cheque picked up by user");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Business cheque marked as picked up");
        response.put("status", "COMPLETED");
        return response;
    }

    @Transactional
    public Map<String, Object> clearChequeDrawRequest(Long id, String adminEmail, String clearedDate) {
        BusinessChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business cheque request not found"));

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

        logAuditAction(id, adminEmail, "CLEAR", "Business cheque cleared and processed");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Business cheque marked as cleared");
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
        List<BusinessChequeAuditLog> auditLog = auditLogRepository.findByChequeRequestIdOrderByTimestampDesc(chequeRequestId);
        List<Map<String, Object>> auditData = new ArrayList<>();

        for (BusinessChequeAuditLog log : auditLog) {
            auditData.add(mapAuditLogToDetail(log));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("auditLog", auditData);
        response.put("totalActions", auditData.size());
        return response;
    }

    public Map<String, Object> searchChequeDrawByNumber(String chequeNumber) {
        List<BusinessChequeRequest> requests = chequeRequestRepository.findByChequeNumberContainingIgnoreCase(chequeNumber);
        List<Map<String, Object>> results = new ArrayList<>();

        for (BusinessChequeRequest req : requests) {
            results.add(mapChequeRequestToAdminView(req));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("count", results.size());
        return response;
    }

    public byte[] exportChequeDrawToCSV(String status) {
        List<BusinessChequeRequest> requests;
        if (status != null && !status.isEmpty()) {
            requests = chequeRequestRepository.findByStatus(status);
        } else {
            requests = chequeRequestRepository.findAll();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Cheque #,Serial #,Business,Account,Payee,Amount,Date,Status,Request Date\n");

        for (BusinessChequeRequest req : requests) {
            csv.append(String.format("%s,%s,%d,%d,%s,₹%.2f,%s,%s,%s\n",
                    req.getChequeNumber(),
                    req.getSerialNumber(),
                    req.getUserId(),
                    req.getCurrentAccountId(),
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
        BusinessChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business cheque request not found"));

        if (!"APPROVED".equals(request.getStatus()) && !"COMPLETED".equals(request.getStatus())) {
            throw new RuntimeException("Cheque must be approved before downloading");
        }

        request.setChequeDownloaded(true);
        request.setChequeDownloadedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Business cheque marked as downloaded");
        response.put("downloadedAt", request.getChequeDownloadedAt());
        return response;
    }

    // ==================== CHEQUE LEAF ALLOCATION ====================

    @Transactional
    public Map<String, Object> getAvailableChequeLeaves(Long currentAccountId) {
        CurrentAccount account = currentAccountRepository.findById(currentAccountId)
                .orElseThrow(() -> new RuntimeException("Business account not found"));

        long existingCount = chequeLeafRepository.countByCurrentAccountId(currentAccountId);
        if (existingCount == 0) {
            allocateChequeLeaves(currentAccountId, account.getId());
        }

        List<BusinessChequeLeaf> availableLeaves = chequeLeafRepository
                .findByCurrentAccountIdAndStatusOrderByLeafNumberAsc(currentAccountId, "AVAILABLE");
        List<BusinessChequeLeaf> allLeaves = chequeLeafRepository
                .findByCurrentAccountIdOrderByLeafNumberAsc(currentAccountId);

        List<Map<String, Object>> leafList = new ArrayList<>();
        for (BusinessChequeLeaf leaf : availableLeaves) {
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

    private synchronized void allocateChequeLeaves(Long currentAccountId, Long userId) {
        if (chequeLeafRepository.countByCurrentAccountId(currentAccountId) > 0) {
            return;
        }

        // Use a different range prefix (B-series) to differentiate from salary cheque leaves
        List<BusinessChequeLeaf> allLeaves = chequeLeafRepository.findAll();
        long maxNum = 500000; // Start from 500000 to separate from salary leaves
        for (BusinessChequeLeaf existing : allLeaves) {
            try {
                long num = Long.parseLong(existing.getLeafNumber());
                if (num > maxNum) maxNum = num;
            } catch (NumberFormatException ignored) {}
        }

        List<BusinessChequeLeaf> newLeaves = new ArrayList<>();
        for (int i = 1; i <= MAX_CHEQUE_LEAVES; i++) {
            String leafNumber = String.format("%06d", maxNum + i);
            BusinessChequeLeaf leaf = new BusinessChequeLeaf(currentAccountId, userId, leafNumber);
            newLeaves.add(leaf);
        }
        chequeLeafRepository.saveAll(newLeaves);
    }

    @Transactional
    public void releaseChequeLeaf(Long chequeRequestId) {
        BusinessChequeRequest request = chequeRequestRepository.findById(chequeRequestId).orElse(null);
        if (request == null) return;

        BusinessChequeLeaf leaf = chequeLeafRepository
                .findByLeafNumberAndCurrentAccountId(request.getSerialNumber(), request.getCurrentAccountId())
                .orElse(null);
        if (leaf != null && "USED".equals(leaf.getStatus())) {
            leaf.setStatus("AVAILABLE");
            leaf.setUsedChequeRequestId(null);
            leaf.setUsedAt(null);
            chequeLeafRepository.save(leaf);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private String generateUniqueChequeNumber(Long currentAccountId) {
        BusinessChequeSequence sequence = sequenceRepository.findByCurrentAccountId(currentAccountId);
        if (sequence == null) {
            sequence = new BusinessChequeSequence();
            sequence.setCurrentAccountId(currentAccountId);
            sequence.setNextSequence(1000L);
        }

        String chequeNumber = String.format("BCHQ-%d-%06d",
                currentAccountId,
                sequence.getNextSequence()
        );

        sequence.setNextSequence(sequence.getNextSequence() + 1);
        sequence.setUpdatedAt(LocalDateTime.now());
        sequenceRepository.save(sequence);

        return chequeNumber;
    }

    private void logAuditAction(Long chequeRequestId, String adminEmail, String action, String remarks) {
        BusinessChequeAuditLog log = new BusinessChequeAuditLog();
        log.setChequeRequestId(chequeRequestId);
        log.setAdminEmail(adminEmail);
        log.setAction(action);
        log.setRemarks(remarks);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private Map<String, Object> mapChequeRequestToHistory(BusinessChequeRequest req) {
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

    private Map<String, Object> mapChequeRequestToDetail(BusinessChequeRequest req) {
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

    private Map<String, Object> mapChequeRequestToAdminView(BusinessChequeRequest req) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", req.getId());
        map.put("chequeNumber", req.getChequeNumber());
        map.put("serialNumber", req.getSerialNumber());
        map.put("userId", req.getUserId());
        map.put("currentAccountId", req.getCurrentAccountId());
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

        // Look up business account for user details
        try {
            Optional<CurrentAccount> accountOpt = currentAccountRepository.findById(req.getCurrentAccountId());
            if (accountOpt.isPresent()) {
                CurrentAccount ca = accountOpt.get();
                map.put("userName", ca.getOwnerName());
                map.put("businessName", ca.getBusinessName());
                map.put("businessType", ca.getBusinessType());
                map.put("userEmail", ca.getEmail());
                map.put("accountNumber", ca.getAccountNumber());
                map.put("currentBalance", ca.getBalance() != null ? ca.getBalance() : 0.0);
                map.put("gstNumber", ca.getGstNumber());
            } else {
                map.put("userName", "Unknown");
                map.put("businessName", "-");
                map.put("businessType", "-");
                map.put("userEmail", "-");
                map.put("accountNumber", "-");
                map.put("currentBalance", 0.0);
                map.put("gstNumber", "-");
            }
        } catch (Exception e) {
            map.put("userName", "Unknown");
            map.put("businessName", "-");
            map.put("businessType", "-");
            map.put("userEmail", "-");
            map.put("accountNumber", "-");
            map.put("currentBalance", 0.0);
            map.put("gstNumber", "-");
        }

        return map;
    }

    private Map<String, Object> mapAuditLogToDetail(BusinessChequeAuditLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("adminEmail", log.getAdminEmail());
        map.put("action", log.getAction());
        map.put("remarks", log.getRemarks());
        map.put("timestamp", log.getTimestamp());
        return map;
    }
}
