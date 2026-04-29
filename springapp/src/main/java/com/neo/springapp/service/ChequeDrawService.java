package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * Service for Cheque Draw System
 * Handles salary account cheque drawing with admin approval workflow
 */
@Service
public class ChequeDrawService {

    @Autowired
    private ChequeRequestRepository chequeRequestRepository;

    @Autowired
    private ChequeAuditLogRepository auditLogRepository;

    @Autowired
    private ChequeSequenceRepository sequenceRepository;

    @Autowired
    private ChequeBankRangeRepository chequeBankRangeRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SalaryNormalTransactionRepository normalTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GlobalTransactionIdGenerator globalTransactionIdGenerator;

    @Autowired
    private ChequeLeafRepository chequeLeafRepository;

    private static final int MAX_CHEQUE_LEAVES = 30;

    // ==================== USER OPERATIONS ====================

    /**
     * User applies for a cheque draw request
     */
    @Transactional
    public Map<String, Object> applyChequeDrawRequest(Long salaryAccountId, String serialNumber,
                                                       String chequeDate, Double amount, String payeeName,
                                                       String remarks) {
        // Validate inputs
        if (amount <= 0) throw new RuntimeException("Amount must be greater than 0");
        if (amount >= 10_00_000) throw new RuntimeException("Amount cannot exceed ₹10,00,000");

        // Get salary account
        SalaryAccount account = salaryAccountRepository.findById(salaryAccountId)
                .orElseThrow(() -> new RuntimeException("Salary account not found"));

        // Validate available balance
        BigDecimal availableBalance = BigDecimal.valueOf(account.getBalance() != null ? account.getBalance() : 0.0);
        if (availableBalance.compareTo(BigDecimal.valueOf(amount)) < 0) {
            throw new RuntimeException("Insufficient balance. Available: ₹" + availableBalance);
        }

        // Validate serial number belongs to this user's allocated leaves
        ChequeLeaf leaf = chequeLeafRepository.findByLeafNumberAndSalaryAccountId(serialNumber, salaryAccountId)
                .orElseThrow(() -> new RuntimeException("Invalid cheque leaf number. This leaf is not allocated to your account."));
        if (!"AVAILABLE".equals(leaf.getStatus())) {
            throw new RuntimeException("This cheque leaf has already been used.");
        }

        // Generate unique cheque number
        String chequeNumber = generateUniqueChequeNumber(salaryAccountId);

        // Create cheque request
        ChequeRequest request = new ChequeRequest();
        request.setUserId(account.getId());
        request.setSalaryAccountId(salaryAccountId);
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

        ChequeRequest saved = chequeRequestRepository.save(request);

        // Mark cheque leaf as used
        leaf.setStatus("USED");
        leaf.setUsedChequeRequestId(saved.getId());
        leaf.setUsedAt(LocalDateTime.now());
        chequeLeafRepository.save(leaf);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cheque draw request submitted successfully");
        response.put("chequeNumber", chequeNumber);
        response.put("requestId", saved.getId());
        response.put("status", "PENDING");

        return response;
    }

    /**
     * Get user's cheque draw requests with pagination
     */
    public Map<String, Object> getUserChequeDrawRequests(Long salaryAccountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChequeRequest> requests = chequeRequestRepository.findBySalaryAccountIdOrderByCreatedAtDesc(
                salaryAccountId, pageable
        );

        List<Map<String, Object>> items = new ArrayList<>();
        for (ChequeRequest req : requests.getContent()) {
            items.add(mapChequeRequestToHistory(req));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", items);
        response.put("totalPages", requests.getTotalPages());
        response.put("totalItems", requests.getTotalElements());
        response.put("currentPage", page);

        return response;
    }

    /**
     * Get specific cheque draw request details
     */
    public Map<String, Object> getChequeDrawDetails(Long id) {
        ChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cheque request not found"));

        return mapChequeRequestToDetail(request);
    }

    /**
     * User cancels a pending cheque draw request
     */
    @Transactional
    public Map<String, Object> cancelChequeDrawRequest(Long id, String reason) {
        ChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cheque request not found"));

        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING cheques can be cancelled");
        }

        request.setStatus("CANCELLED");
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        // Release the cheque leaf back to available
        releaseChequeLeaf(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cheque request cancelled successfully");
        response.put("chequeNumber", request.getChequeNumber());

        return response;
    }

    /**
     * User edits a pending cheque draw request (payeeName and amount only)
     */
    @Transactional
    public Map<String, Object> editPendingChequeDrawRequest(Long id, String payeeName, Double amount) {
        ChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cheque request not found"));

        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING cheques can be edited");
        }

        if (payeeName != null && !payeeName.trim().isEmpty()) {
            request.setPayeeName(payeeName.trim());
        }

        if (amount != null) {
            if (amount <= 0) throw new RuntimeException("Amount must be greater than 0");
            if (amount >= 10_00_000) throw new RuntimeException("Amount cannot exceed ₹10,00,000");

            SalaryAccount account = salaryAccountRepository.findById(request.getSalaryAccountId())
                    .orElseThrow(() -> new RuntimeException("Salary account not found"));
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
        response.put("message", "Cheque request updated successfully");
        response.put("chequeNumber", request.getChequeNumber());
        response.put("payeeName", request.getPayeeName());
        response.put("amount", request.getAmount().doubleValue());
        return response;
    }

    // ==================== ADMIN OPERATIONS ====================

    /**
     * Get all cheque draw requests for admin with filters
     */
    public Map<String, Object> getAdminChequeDrawRequests(String status, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChequeRequest> requests;

        if (status != null && !status.isEmpty() && search != null && !search.isEmpty()) {
            requests = chequeRequestRepository.findByStatusAndChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(
                    status, search, pageable
            );
        } else if (status != null && !status.isEmpty()) {
            requests = chequeRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (search != null && !search.isEmpty()) {
            requests = chequeRequestRepository.findByChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(
                    search, pageable
            );
        } else {
            requests = chequeRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (ChequeRequest req : requests.getContent()) {
            items.add(mapChequeRequestToAdminView(req));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", items);
        response.put("totalPages", requests.getTotalPages());
        response.put("totalItems", requests.getTotalElements());
        response.put("currentPage", page);

        return response;
    }

    /**
     * Get admin view of cheque with audit log
     */
    public Map<String, Object> getAdminChequeDrawDetails(Long id) {
        ChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cheque request not found"));

        Map<String, Object> detail = mapChequeRequestToAdminView(request);

        // Add audit log
        List<ChequeAuditLog> auditLog = auditLogRepository.findByChequeRequestIdOrderByTimestampDesc(id);
        List<Map<String, Object>> auditData = new ArrayList<>();
        for (ChequeAuditLog log : auditLog) {
            auditData.add(mapAuditLogToDetail(log));
        }
        detail.put("auditLog", auditData);

        return detail;
    }

    /**
     * Verify payee account number and match with payee name
     */
    public Map<String, Object> verifyPayeeAccount(String payeeAccountNumber, String expectedPayeeName) {
        Map<String, Object> result = new HashMap<>();

        // Check in salary accounts first
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

        // Check in savings/current accounts
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

    /**
     * Admin approves a pending cheque draw request with payee verification
     */
    @Transactional
    public Map<String, Object> approveChequeDrawRequest(Long id, String adminEmail, String remarks,
                                                         String payeeAccountNumber) {
        ChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cheque request not found"));

        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING cheques can be approved");
        }

        if (payeeAccountNumber == null || payeeAccountNumber.trim().isEmpty()) {
            throw new RuntimeException("Payee account number is required for approval");
        }

        // Verify payee account and name match
        Map<String, Object> verification = verifyPayeeAccount(payeeAccountNumber, request.getPayeeName());
        if (!(Boolean) verification.get("verified")) {
            throw new RuntimeException((String) verification.get("message"));
        }

        String payeeAccountType = (String) verification.get("accountType");

        // Get sender's salary account and debit amount
        SalaryAccount senderAccount = salaryAccountRepository.findById(request.getSalaryAccountId())
                .orElseThrow(() -> new RuntimeException("Salary account not found"));

        BigDecimal amount = request.getAmount();
        BigDecimal senderBalance = BigDecimal.valueOf(senderAccount.getBalance() != null ? senderAccount.getBalance() : 0.0);

        if (senderBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance. Available: ₹" + senderBalance + ", Required: ₹" + amount);
        }

        // Debit from sender
        BigDecimal newSenderBalance = senderBalance.subtract(amount);
        senderAccount.setBalance(newSenderBalance.doubleValue());
        senderAccount.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(senderAccount);

        // Credit to payee account
        Double newPayeeBalance;
        if ("Salary".equals(payeeAccountType)) {
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

        // Generate transaction reference
        String txnRef = "CHQ-TXN-" + System.currentTimeMillis();

        // --- Create Statement / Transaction Records ---

        // 1. Debit statement for sender (salary account)
        Long senderGlobalTxnId = null;
        try { senderGlobalTxnId = globalTransactionIdGenerator.getNextTransactionId(); } catch (Exception ignored) {}
        SalaryNormalTransaction senderTxn = new SalaryNormalTransaction();
        senderTxn.setGlobalTransactionSequence(senderGlobalTxnId);
        senderTxn.setSalaryAccountId(senderAccount.getId());
        senderTxn.setAccountNumber(senderAccount.getAccountNumber());
        senderTxn.setType("Debit");
        senderTxn.setAmount(amount.doubleValue());
        senderTxn.setCharge(0.0);
        senderTxn.setRecipientAccount(payeeAccountNumber);
        senderTxn.setRemark("Cheque " + request.getChequeNumber() + " to " + request.getPayeeName() + " | Ref: " + txnRef);
        senderTxn.setPreviousBalance(senderBalance.doubleValue());
        senderTxn.setNewBalance(newSenderBalance.doubleValue());
        senderTxn.setStatus("Success");
        senderTxn.setCreatedAt(LocalDateTime.now());
        normalTransactionRepository.save(senderTxn);

        // 2. Credit statement for payee
        if ("Salary".equals(payeeAccountType)) {
            SalaryAccount payeeAcct = salaryAccountRepository.findByAccountNumber(payeeAccountNumber);
            Long payeeSalaryGlobalTxnId = null;
            try { payeeSalaryGlobalTxnId = globalTransactionIdGenerator.getNextTransactionId(); } catch (Exception ignored) {}
            SalaryNormalTransaction payeeTxn = new SalaryNormalTransaction();
            payeeTxn.setGlobalTransactionSequence(payeeSalaryGlobalTxnId);
            payeeTxn.setSalaryAccountId(payeeAcct.getId());
            payeeTxn.setAccountNumber(payeeAccountNumber);
            payeeTxn.setType("Credit");
            payeeTxn.setAmount(amount.doubleValue());
            payeeTxn.setCharge(0.0);
            payeeTxn.setRecipientAccount(senderAccount.getAccountNumber());
            payeeTxn.setRemark("Cheque " + request.getChequeNumber() + " from " + senderAccount.getEmployeeName() + " | Ref: " + txnRef);
            payeeTxn.setPreviousBalance(newPayeeBalance - amount.doubleValue());
            payeeTxn.setNewBalance(newPayeeBalance);
            payeeTxn.setStatus("Success");
            payeeTxn.setCreatedAt(LocalDateTime.now());
            normalTransactionRepository.save(payeeTxn);
        } else {
            Long payeeGlobalTxnId = null;
            try { payeeGlobalTxnId = globalTransactionIdGenerator.getNextTransactionId(); } catch (Exception ignored) {}
            Transaction payeeTxn = new Transaction();
            payeeTxn.setGlobalTransactionSequence(payeeGlobalTxnId);
            payeeTxn.setAccountNumber(payeeAccountNumber);
            payeeTxn.setMerchant("Cheque Deposit");
            payeeTxn.setAmount(amount.doubleValue());
            payeeTxn.setType("Credit");
            payeeTxn.setDescription("Cheque " + request.getChequeNumber() + " from " + senderAccount.getEmployeeName() + " | Ref: " + txnRef);
            payeeTxn.setBalance(newPayeeBalance);
            payeeTxn.setUserName(request.getPayeeName());
            payeeTxn.setSourceAccountNumber(senderAccount.getAccountNumber());
            payeeTxn.setStatus("Completed");
            payeeTxn.setDate(LocalDateTime.now());
            transactionRepository.save(payeeTxn);
        }

        // Update cheque request status
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

        // Log audit
        logAuditAction(id, adminEmail, "APPROVE",
                (remarks != null ? remarks + " | " : "") +
                "Debited ₹" + amount + " from " + senderAccount.getAccountNumber() +
                ", Credited to " + payeeAccountNumber + " | Txn: " + txnRef);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cheque approved. ₹" + amount + " debited from " +
                senderAccount.getAccountNumber() + " and credited to " + payeeAccountNumber);
        response.put("chequeNumber", request.getChequeNumber());
        response.put("transactionReference", txnRef);
        response.put("debitedFrom", senderAccount.getAccountNumber());
        response.put("creditedTo", payeeAccountNumber);
        response.put("newSenderBalance", newSenderBalance);
        response.put("status", "APPROVED");

        // Include chequeRequest map for frontend compatibility
        response.put("chequeRequest", mapChequeRequestToAdminView(request));

        return response;
    }

    /**
     * Admin rejects a pending cheque draw request
     */
    @Transactional
    public Map<String, Object> rejectChequeDrawRequest(Long id, String adminEmail, String rejectionReason) {
        ChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cheque request not found"));

        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Only PENDING cheques can be rejected");
        }

        request.setStatus("REJECTED");
        request.setRejectedBy(adminEmail);
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectionReason(rejectionReason);
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        // Log audit
        logAuditAction(id, adminEmail, "REJECT", rejectionReason);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cheque rejected successfully");
        response.put("chequeNumber", request.getChequeNumber());
        response.put("status", "REJECTED");
        response.put("chequeRequest", mapChequeRequestToAdminView(request));

        return response;
    }

    /**
     * Admin marks cheque as picked up
     */
    @Transactional
    public Map<String, Object> markChequeDrawPickedUp(Long id, String adminEmail) {
        ChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cheque request not found"));

        if (!request.getStatus().equals("APPROVED")) {
            throw new RuntimeException("Only APPROVED cheques can be marked as picked up");
        }

        request.setStatus("COMPLETED");
        request.setPickedUpAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        // Log audit
        logAuditAction(id, adminEmail, "PICKUP", "Cheque picked up by user");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cheque marked as picked up");
        response.put("status", "COMPLETED");

        return response;
    }

    /**
     * Admin marks cheque as cleared
     */
    @Transactional
    public Map<String, Object> clearChequeDrawRequest(Long id, String adminEmail, String clearedDate) {
        ChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cheque request not found"));

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

        // Log audit
        logAuditAction(id, adminEmail, "CLEAR", "Cheque cleared and processed");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cheque marked as cleared");
        response.put("status", "CLEARED");

        return response;
    }

    /**
     * Get dashboard statistics
     */
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

    /**
     * Get audit log for a cheque
     */
    public Map<String, Object> getChequeDrawAuditLog(Long chequeRequestId) {
        List<ChequeAuditLog> auditLog = auditLogRepository.findByChequeRequestIdOrderByTimestampDesc(chequeRequestId);
        List<Map<String, Object>> auditData = new ArrayList<>();

        for (ChequeAuditLog log : auditLog) {
            auditData.add(mapAuditLogToDetail(log));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("auditLog", auditData);
        response.put("totalActions", auditData.size());

        return response;
    }

    /**
     * Search cheque draw requests by cheque number
     */
    public Map<String, Object> searchChequeDrawByNumber(String chequeNumber) {
        List<ChequeRequest> requests = chequeRequestRepository.findByChequeNumberContainingIgnoreCase(chequeNumber);
        List<Map<String, Object>> results = new ArrayList<>();

        for (ChequeRequest req : requests) {
            results.add(mapChequeRequestToAdminView(req));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("count", results.size());

        return response;
    }

    /**
     * Get cheque draw requests by status
     */
    public Map<String, Object> getChequeDrawByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChequeRequest> requests = chequeRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (ChequeRequest req : requests.getContent()) {
            items.add(mapChequeRequestToAdminView(req));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", items);
        response.put("totalPages", requests.getTotalPages());
        response.put("currentPage", page);

        return response;
    }

    /**
     * Export to CSV
     */
    public byte[] exportChequeDrawToCSV(String status) {
        List<ChequeRequest> requests;
        if (status != null && !status.isEmpty()) {
            requests = chequeRequestRepository.findByStatus(status);
        } else {
            requests = (List<ChequeRequest>) chequeRequestRepository.findAll();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Cheque #,Serial #,User,Account,Payee,Amount,Date,Status,Request Date\n");

        for (ChequeRequest req : requests) {
            csv.append(String.format("%s,%s,%d,%d,%s,₹%.2f,%s,%s,%s\n",
                    req.getChequeNumber(),
                    req.getSerialNumber(),
                    req.getUserId(),
                    req.getSalaryAccountId(),
                    req.getPayeeName(),
                    req.getAmount(),
                    req.getChequeDate(),
                    req.getStatus(),
                    req.getCreatedAt()
            ));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==================== VALIDATION & UTILITIES ====================

    /**
     * Mark cheque as downloaded by user
     */
    @Transactional
    public Map<String, Object> markChequeDownloaded(Long id) {
        ChequeRequest request = chequeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cheque request not found"));

        if (!"APPROVED".equals(request.getStatus()) && !"COMPLETED".equals(request.getStatus())) {
            throw new RuntimeException("Cheque must be approved before downloading");
        }

        request.setChequeDownloaded(true);
        request.setChequeDownloadedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        chequeRequestRepository.save(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cheque marked as downloaded");
        response.put("downloadedAt", request.getChequeDownloadedAt());
        return response;
    }

    /**
     * Validate cheque serial number
     */
    public boolean validateChequeSerialNumber(String serialNumber, Long salaryAccountId) {
        List<ChequeBankRange> ranges = chequeBankRangeRepository.findBySalaryAccountId(salaryAccountId);

        // If no cheque book ranges are configured, skip validation
        if (ranges == null || ranges.isEmpty()) {
            return true;
        }

        for (ChequeBankRange range : ranges) {
            if (isBetween(serialNumber, range.getSerialFrom(), range.getSerialTo())) {
                return true;
            }
        }
        throw new RuntimeException("Invalid cheque serial number. Please check the serial number from your cheque book.");
    }

    /**
     * Get available balance
     */
    public Double getAccountAvailableBalance(Long salaryAccountId) {
        SalaryAccount account = salaryAccountRepository.findById(salaryAccountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return account.getBalance() != null ? account.getBalance() : 0.0;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private String generateUniqueChequeNumber(Long salaryAccountId) {
        ChequeSequence sequence = sequenceRepository.findBySalaryAccountId(salaryAccountId);
        if (sequence == null) {
            sequence = new ChequeSequence();
            sequence.setSalaryAccountId(salaryAccountId);
            sequence.setNextSequence(1000L);
        }

        String chequeNumber = String.format("CHQ-%d-%06d",
                salaryAccountId,
                sequence.getNextSequence()
        );

        sequence.setNextSequence(sequence.getNextSequence() + 1);
        sequence.setUpdatedAt(LocalDateTime.now());
        sequenceRepository.save(sequence);

        return chequeNumber;
    }

    private boolean isBetween(String value, String from, String to) {
        return value.compareTo(from) >= 0 && value.compareTo(to) <= 0;
    }

    private void logAuditAction(Long chequeRequestId, String adminEmail, String action, String remarks) {
        ChequeAuditLog log = new ChequeAuditLog();
        log.setChequeRequestId(chequeRequestId);
        log.setAdminEmail(adminEmail);
        log.setAction(action);
        log.setRemarks(remarks);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private Map<String, Object> mapChequeRequestToHistory(ChequeRequest req) {
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

    private Map<String, Object> mapChequeRequestToDetail(ChequeRequest req) {
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

    private Map<String, Object> mapChequeRequestToAdminView(ChequeRequest req) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", req.getId());
        map.put("chequeNumber", req.getChequeNumber());
        map.put("serialNumber", req.getSerialNumber());
        map.put("userId", req.getUserId());
        map.put("salaryAccountId", req.getSalaryAccountId());
        map.put("chequeDate", req.getChequeDate());
        map.put("payeeName", req.getPayeeName());
        map.put("amount", req.getAmount().doubleValue());
        map.put("availableBalance", req.getAvailableBalance().doubleValue());
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

        // Look up salary account for user details
        try {
            Optional<SalaryAccount> salaryAccountOpt = salaryAccountRepository.findById(req.getSalaryAccountId());
            if (salaryAccountOpt.isPresent()) {
                SalaryAccount sa = salaryAccountOpt.get();
                map.put("userName", sa.getEmployeeName());
                map.put("userEmail", sa.getEmail());
                map.put("accountNumber", sa.getAccountNumber());
                map.put("currentBalance", sa.getBalance() != null ? sa.getBalance() : 0.0);
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

    private Map<String, Object> mapAuditLogToDetail(ChequeAuditLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("adminEmail", log.getAdminEmail());
        map.put("action", log.getAction());
        map.put("remarks", log.getRemarks());
        map.put("timestamp", log.getTimestamp());
        return map;
    }

    // ==================== CHEQUE LEAF ALLOCATION ====================

    /**
     * Get available (unused) cheque leaves for a salary account.
     * Auto-allocates 30 leaves if none exist yet.
     */
    @Transactional
    public Map<String, Object> getAvailableChequeLeaves(Long salaryAccountId) {
        SalaryAccount account = salaryAccountRepository.findById(salaryAccountId)
                .orElseThrow(() -> new RuntimeException("Salary account not found"));

        // Auto-allocate if no leaves exist for this account
        long existingCount = chequeLeafRepository.countBySalaryAccountId(salaryAccountId);
        if (existingCount == 0) {
            allocateChequeLeaves(salaryAccountId, account.getId());
        }

        List<ChequeLeaf> availableLeaves = chequeLeafRepository
                .findBySalaryAccountIdAndStatusOrderByLeafNumberAsc(salaryAccountId, "AVAILABLE");
        List<ChequeLeaf> allLeaves = chequeLeafRepository
                .findBySalaryAccountIdOrderByLeafNumberAsc(salaryAccountId);

        List<Map<String, Object>> leafList = new ArrayList<>();
        for (ChequeLeaf leaf : availableLeaves) {
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

    /**
     * Allocate 30 unique cheque leaves to a salary account.
     * Serial numbers are globally unique across all users.
     */
    private synchronized void allocateChequeLeaves(Long salaryAccountId, Long userId) {
        // Double-check inside synchronized block
        if (chequeLeafRepository.countBySalaryAccountId(salaryAccountId) > 0) {
            return;
        }

        // Find highest existing leaf number to continue sequence
        List<ChequeLeaf> allLeaves = chequeLeafRepository.findAll();
        long maxNum = 0;
        for (ChequeLeaf existing : allLeaves) {
            try {
                long num = Long.parseLong(existing.getLeafNumber());
                if (num > maxNum) maxNum = num;
            } catch (NumberFormatException ignored) {}
        }

        List<ChequeLeaf> newLeaves = new ArrayList<>();
        for (int i = 1; i <= MAX_CHEQUE_LEAVES; i++) {
            String leafNumber = String.format("%06d", maxNum + i);
            ChequeLeaf leaf = new ChequeLeaf(salaryAccountId, userId, leafNumber);
            newLeaves.add(leaf);
        }
        chequeLeafRepository.saveAll(newLeaves);
    }

    /**
     * Release a cheque leaf back to available when a cheque request is cancelled.
     */
    @Transactional
    public void releaseChequeLeaf(Long chequeRequestId) {
        ChequeRequest request = chequeRequestRepository.findById(chequeRequestId).orElse(null);
        if (request == null) return;

        ChequeLeaf leaf = chequeLeafRepository
                .findByLeafNumberAndSalaryAccountId(request.getSerialNumber(), request.getSalaryAccountId())
                .orElse(null);
        if (leaf != null && "USED".equals(leaf.getStatus())) {
            leaf.setStatus("AVAILABLE");
            leaf.setUsedChequeRequestId(null);
            leaf.setUsedAt(null);
            chequeLeafRepository.save(leaf);
        }
    }
}
