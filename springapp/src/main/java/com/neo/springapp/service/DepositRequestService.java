package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.DepositRequest;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.DepositRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DepositRequestService {

    private final DepositRequestRepository depositRequestRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public DepositRequestService(DepositRequestRepository depositRequestRepository,
                                 AccountService accountService,
                                 TransactionService transactionService) {
        this.depositRequestRepository = depositRequestRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    public DepositRequest createRequest(DepositRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (request.getAccountNumber() == null || request.getAccountNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Account number is required");
        }

        // Ensure account exists
        Account account = accountService.getAccountByNumber(request.getAccountNumber());
        if (account == null) {
            throw new IllegalArgumentException("Account not found for number: " + request.getAccountNumber());
        }

        // Prefill user friendly fields
        request.setUserName(account.getName());
        request.setStatus("PENDING");
        request.setRequestId(request.getRequestId() != null ? request.getRequestId() : "DEP" + System.currentTimeMillis());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        return depositRequestRepository.save(request);
    }

    public List<DepositRequest> getAll(String status) {
        if (status != null && !status.trim().isEmpty()) {
            return depositRequestRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
        }
        return depositRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<DepositRequest> getByAccount(String accountNumber) {
        return depositRequestRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    public Optional<DepositRequest> getById(Long id) {
        return depositRequestRepository.findById(id);
    }

    public DepositRequest approveRequest(Long id, String processedBy) {
        DepositRequest request = depositRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deposit request not found"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be approved");
        }

        Account account = accountService.getAccountByNumber(request.getAccountNumber());
        if (account == null) {
            throw new IllegalArgumentException("Account not found for number: " + request.getAccountNumber());
        }

        Double newBalance = accountService.creditBalance(request.getAccountNumber(), request.getAmount());
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
        transaction.setUserName(account.getName());
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

