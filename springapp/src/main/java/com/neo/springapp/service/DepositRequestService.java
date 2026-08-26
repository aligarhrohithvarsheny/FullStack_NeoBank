package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.DepositRequest;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.DepositRequestRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.service.ChequeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
@SuppressWarnings("null")
public class DepositRequestService {

    private final DepositRequestRepository depositRequestRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final ChequeService chequeService;
    private final CurrentAccountRepository currentAccountRepository;
    private final SalaryAccountRepository salaryAccountRepository;

    public DepositRequestService(DepositRequestRepository depositRequestRepository,
                                 AccountService accountService,
                                 TransactionService transactionService,
                                 ChequeService chequeService,
                                 CurrentAccountRepository currentAccountRepository,
                                 SalaryAccountRepository salaryAccountRepository) {
        this.depositRequestRepository = depositRequestRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.chequeService = chequeService;
        this.currentAccountRepository = currentAccountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
    }

    /** Resolves an account number across savings, current, and salary accounts. */
    private static class ResolvedAccount {
        String type; // SAVINGS, CURRENT, SALARY
        String name;
        String status;
    }

    private ResolvedAccount resolveAnyAccount(String accountNumber) {
        Account savings = accountService.getAccountByNumber(accountNumber);
        if (savings != null) {
            ResolvedAccount r = new ResolvedAccount();
            r.type = "SAVINGS"; r.name = savings.getName(); r.status = savings.getStatus();
            return r;
        }
        Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(accountNumber);
        if (currentOpt.isPresent()) {
            CurrentAccount ca = currentOpt.get();
            ResolvedAccount r = new ResolvedAccount();
            r.type = "CURRENT"; r.name = ca.getBusinessName() != null ? ca.getBusinessName() : ca.getOwnerName(); r.status = ca.getStatus();
            return r;
        }
        SalaryAccount sal = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (sal != null) {
            ResolvedAccount r = new ResolvedAccount();
            r.type = "SALARY"; r.name = sal.getEmployeeName(); r.status = sal.getStatus();
            return r;
        }
        return null;
    }

    private Double creditAnyAccount(String accountNumber, String type, Double amount) {
        switch (type) {
            case "CURRENT": {
                CurrentAccount ca = currentAccountRepository.findByAccountNumber(accountNumber)
                        .orElseThrow(() -> new IllegalArgumentException("Account not found for number: " + accountNumber));
                ca.setBalance((ca.getBalance() == null ? 0.0 : ca.getBalance()) + amount);
                currentAccountRepository.save(ca);
                return ca.getBalance();
            }
            case "SALARY": {
                SalaryAccount sal = salaryAccountRepository.findByAccountNumber(accountNumber);
                if (sal == null) throw new IllegalArgumentException("Account not found for number: " + accountNumber);
                sal.setBalance((sal.getBalance() == null ? 0.0 : sal.getBalance()) + amount);
                salaryAccountRepository.save(sal);
                return sal.getBalance();
            }
            default:
                return accountService.creditBalance(accountNumber, amount);
        }
    }

    @Transactional
    public DepositRequest createRequest(DepositRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (request.getAccountNumber() == null || request.getAccountNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Account number is required");
        }

        // Ensure account exists (savings, current, or salary)
        ResolvedAccount account = resolveAnyAccount(request.getAccountNumber());
        if (account == null) {
            throw new IllegalArgumentException("Account not found for number: " + request.getAccountNumber());
        }

        // Prefill user friendly fields
        request.setUserName(account.name);
        if ("CHEQUE".equalsIgnoreCase(request.getMethod())) {
            if (request.getReferenceNumber() == null || request.getReferenceNumber().isBlank()) {
                throw new IllegalArgumentException("Cheque number is required for cheque deposits");
            }
            Map<String, Object> cheque = chequeService.verifyForDeposit(request.getReferenceNumber(), request.getAccountNumber());
            if (!Boolean.TRUE.equals(cheque.get("valid"))) {
                throw new IllegalArgumentException(String.valueOf(cheque.get("message")));
            }
            request.setChequeValid(true);
            request.setChequeAccountNumber(String.valueOf(cheque.get("accountNumber")));
            request.setChequeAccountHolderName(String.valueOf(cheque.get("accountHolderName")));
            request.setChequeStatus(String.valueOf(cheque.get("status")));
            request.setChequeAvailableBalance((Double) cheque.get("availableBalance"));
        }
        request.setStatus("PENDING");
        String requestedSlipId = request.getRequestId() == null ? null : request.getRequestId().trim();
        if (requestedSlipId != null && !requestedSlipId.isEmpty()
            && depositRequestRepository.findByRequestId(requestedSlipId).isPresent()) {
            throw new IllegalArgumentException("Cash deposit slip ID is already in use");
        }
        request.setRequestId(requestedSlipId == null || requestedSlipId.isEmpty()
            ? "DEP" + System.currentTimeMillis() : requestedSlipId);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        return depositRequestRepository.save(request);
    }

    public List<DepositRequest> getAll(String status) {
        List<DepositRequest> requests = status != null && !status.trim().isEmpty()
                ? depositRequestRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase())
                : depositRequestRepository.findAllByOrderByCreatedAtDesc();
        requests.forEach(this::enrichChequeDetails);
        return requests;
    }

    public List<DepositRequest> getByAccount(String accountNumber) {
        List<DepositRequest> requests = depositRequestRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
        requests.forEach(this::enrichChequeDetails);
        return requests;
    }

    public Optional<DepositRequest> getById(Long id) {
        return depositRequestRepository.findById(id);
    }

    public Optional<DepositRequest> getByRequestId(String requestId) {
        return depositRequestRepository.findByRequestId(requestId).map(this::enrichChequeDetails);
    }

    private DepositRequest enrichChequeDetails(DepositRequest request) {
        if (!"CHEQUE".equalsIgnoreCase(request.getMethod()) || request.getReferenceNumber() == null) return request;
        try {
            Map<String, Object> cheque = chequeService.verifyForDeposit(request.getReferenceNumber());
            request.setChequeValid(Boolean.TRUE.equals(cheque.get("valid")) || "APPROVED".equalsIgnoreCase(request.getStatus()));
            request.setChequeAccountNumber(String.valueOf(cheque.get("accountNumber")));
            request.setChequeAccountHolderName(String.valueOf(cheque.get("accountHolderName")));
            request.setChequeStatus(String.valueOf(cheque.get("status")));
            request.setChequeAvailableBalance((Double) cheque.get("availableBalance"));
        } catch (Exception ignored) {
            request.setChequeValid(false);
            request.setChequeStatus("NOT_FOUND");
        }
        return request;
    }

    @Transactional
    public DepositRequest approveRequest(Long id, String processedBy) {
        DepositRequest request = depositRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deposit request not found"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be approved");
        }

        if ("CHEQUE".equalsIgnoreCase(request.getMethod())) {
            if (request.getReferenceNumber() == null || request.getReferenceNumber().isBlank())
                throw new IllegalArgumentException("Cheque number is required for cheque deposits");
            chequeService.markDeposited(request.getReferenceNumber(), request.getRequestId());
        }

        ResolvedAccount account = resolveAnyAccount(request.getAccountNumber());
        if (account == null) {
            throw new IllegalArgumentException("Account not found for number: " + request.getAccountNumber());
        }

        // Check if account is closed
        if ("CLOSED".equalsIgnoreCase(account.status)) {
            throw new IllegalStateException("Cannot approve deposit for a closed account. Account number: " + request.getAccountNumber());
        }

        Double newBalance = creditAnyAccount(request.getAccountNumber(), account.type, request.getAmount());
        if (newBalance == null) {
            throw new IllegalStateException("Unable to credit balance. Please verify account number.");
        }

        // Save transaction
        Transaction transaction = new Transaction();
        transaction.setMerchant("Deposit Request");
        transaction.setAmount(request.getAmount());
        transaction.setType("Deposit");
        transaction.setDescription(request.getNote() != null ? request.getNote() : "Deposit approved");
        transaction.setBalance(newBalance);
        transaction.setStatus("Completed");
        transaction.setUserName(account.name);
        transaction.setAccountNumber(request.getAccountNumber());
        Transaction savedTxn = transactionService.saveTransaction(transaction);

        request.setStatus("APPROVED");
        request.setProcessedBy(processedBy);
        request.setProcessedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        request.setResultingBalance(newBalance);
        request.setTransactionId(savedTxn.getTransactionId());
        return depositRequestRepository.save(request);
    }

    public DepositRequest rejectRequest(Long id, String processedBy, String reason) {
        DepositRequest request = depositRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deposit request not found"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be rejected");
        }

        request.setStatus("REJECTED");
        request.setProcessedBy(processedBy);
        request.setProcessedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        request.setRejectionReason(reason);
        return depositRequestRepository.save(request);
    }
}

