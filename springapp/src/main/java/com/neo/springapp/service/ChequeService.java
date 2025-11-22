package com.neo.springapp.service;

import com.neo.springapp.model.Cheque;
import com.neo.springapp.model.Account;
import com.neo.springapp.repository.ChequeRepository;
import com.neo.springapp.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChequeService {

    private final ChequeRepository chequeRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public ChequeService(ChequeRepository chequeRepository, AccountRepository accountRepository, 
                        AccountService accountService, TransactionService transactionService) {
        this.chequeRepository = chequeRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    // Create new cheque leaves for user
    public Cheque createCheque(String accountNumber, int numberOfCheques) {
        // Get account details
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new RuntimeException("Account not found");
        }
        
        // Create cheque leaves
        Cheque cheque = new Cheque();
        cheque.setAccountNumber(accountNumber);
        cheque.setAccountHolderName(account.getName());
        cheque.setAccountType(account.getAccountType());
        cheque.setStatus("ACTIVE");
        cheque.setCreatedAt(LocalDateTime.now());
        
        return chequeRepository.save(cheque);
    }

    // Create multiple cheque leaves with amount
    public List<Cheque> createChequeLeaves(String accountNumber, int numberOfLeaves, Double amount) {
        // Get account details
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new RuntimeException("Account not found");
        }
        
        // Create multiple cheque leaves
        for (int i = 0; i < numberOfLeaves; i++) {
            Cheque cheque = new Cheque();
            cheque.setAccountNumber(accountNumber);
            cheque.setAccountHolderName(account.getName());
            cheque.setAccountType(account.getAccountType());
            cheque.setAmount(amount != null ? amount : 0.0);
            cheque.setStatus("ACTIVE");
            cheque.setCreatedAt(LocalDateTime.now());
            chequeRepository.save(cheque);
        }
        
        return chequeRepository.findByAccountNumber(accountNumber);
    }

    // Create multiple cheque leaves (without amount for backward compatibility)
    public List<Cheque> createChequeLeaves(String accountNumber, int numberOfLeaves) {
        return createChequeLeaves(accountNumber, numberOfLeaves, null);
    }

    // Get all cheques for an account
    public List<Cheque> getChequesByAccountNumber(String accountNumber) {
        return chequeRepository.findByAccountNumber(accountNumber);
    }

    // Get cheques by account number with pagination
    public Page<Cheque> getChequesByAccountNumber(String accountNumber, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return chequeRepository.findByAccountNumber(accountNumber, pageable);
    }

    // Get active cheques for an account
    public List<Cheque> getActiveChequesByAccountNumber(String accountNumber) {
        return chequeRepository.findActiveChequesByAccountNumber(accountNumber);
    }

    // Get cancelled cheques for an account
    public List<Cheque> getCancelledChequesByAccountNumber(String accountNumber) {
        return chequeRepository.findCancelledChequesByAccountNumber(accountNumber);
    }

    // Get cheque by ID
    public Optional<Cheque> getChequeById(Long id) {
        return chequeRepository.findById(id);
    }

    // Get cheque by cheque number
    public Optional<Cheque> getChequeByChequeNumber(String chequeNumber) {
        return chequeRepository.findByChequeNumber(chequeNumber);
    }

    // Cancel cheque
    public Cheque cancelCheque(Long id, String cancelledBy, String reason) {
        Cheque cheque = chequeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cheque not found"));
        
        if (!cheque.canBeCancelled()) {
            throw new RuntimeException("Cheque cannot be cancelled. Status: " + cheque.getStatus());
        }
        
        cheque.cancel(cancelledBy, reason);
        return chequeRepository.save(cheque);
    }

    // Get all cheques with pagination
    public Page<Cheque> getAllCheques(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return chequeRepository.findAll(pageable);
    }

    // Get cheques by status
    public Page<Cheque> getChequesByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return chequeRepository.findByStatus(status, pageable);
    }

    // Get cheques by account number and status
    public Page<Cheque> getChequesByAccountNumberAndStatus(String accountNumber, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return chequeRepository.findByAccountNumberAndStatus(accountNumber, status, pageable);
    }

    // Count active cheques for an account
    public Long countActiveCheques(String accountNumber) {
        return chequeRepository.countByAccountNumberAndStatus(accountNumber, "ACTIVE");
    }

    // Count cancelled cheques for an account
    public Long countCancelledCheques(String accountNumber) {
        return chequeRepository.countByAccountNumberAndStatus(accountNumber, "CANCELLED");
    }

    // Request cheque drawing - User
    @Transactional
    public Cheque requestChequeDraw(Long chequeId, String requestedBy) {
        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new RuntimeException("Cheque not found"));
        
        if (!cheque.canBeRequested()) {
            throw new RuntimeException("Cheque cannot be requested. Status: " + cheque.getStatus() + ", Request Status: " + cheque.getRequestStatus());
        }
        
        if (cheque.getAmount() == null || cheque.getAmount() <= 0) {
            throw new RuntimeException("Cheque amount is invalid or not set. Please set amount before requesting.");
        }
        
        cheque.requestDraw(requestedBy);
        return chequeRepository.save(cheque);
    }

    // Approve cheque request and draw - Admin only
    @Transactional
    public Cheque approveChequeRequest(Long chequeId, String approvedBy) {
        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new RuntimeException("Cheque not found"));
        
        if (!"PENDING".equals(cheque.getRequestStatus())) {
            throw new RuntimeException("Cheque request is not pending. Current status: " + cheque.getRequestStatus());
        }
        
        // Approve the request
        cheque.approveRequest(approvedBy);
        cheque = chequeRepository.save(cheque);
        
        // Automatically draw the cheque after approval
        return drawCheque(cheque.getChequeNumber(), approvedBy);
    }

    // Reject cheque request - Admin only
    @Transactional
    public Cheque rejectChequeRequest(Long chequeId, String rejectedBy, String reason) {
        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new RuntimeException("Cheque not found"));
        
        if (!"PENDING".equals(cheque.getRequestStatus())) {
            throw new RuntimeException("Cheque request is not pending. Current status: " + cheque.getRequestStatus());
        }
        
        cheque.rejectRequest(rejectedBy, reason);
        return chequeRepository.save(cheque);
    }

    // Draw cheque (withdraw amount from account) - Admin only (after approval)
    @Transactional
    public Cheque drawCheque(String chequeNumber, String drawnBy) {
        Cheque cheque = chequeRepository.findByChequeNumber(chequeNumber)
                .orElseThrow(() -> new RuntimeException("Cheque not found"));
        
        if (!cheque.canBeDrawn()) {
            throw new RuntimeException("Cheque cannot be drawn. Status: " + cheque.getStatus() + ", Request Status: " + cheque.getRequestStatus());
        }
        
        if (cheque.getAmount() == null || cheque.getAmount() <= 0) {
            throw new RuntimeException("Cheque amount is invalid or not set");
        }
        
        // Check account balance
        Account account = accountRepository.findByAccountNumber(cheque.getAccountNumber());
        if (account == null) {
            throw new RuntimeException("Account not found");
        }
        
        Double currentBalance = account.getBalance() != null ? account.getBalance() : 0.0;
        if (currentBalance < cheque.getAmount()) {
            throw new RuntimeException("Insufficient balance. Available: ₹" + currentBalance + ", Required: ₹" + cheque.getAmount());
        }
        
        // Debit amount from account (real-time update)
        Double newBalance = accountService.debitBalance(cheque.getAccountNumber(), cheque.getAmount());
        
        // Create transaction record
        transactionService.createTransferTransaction(
            cheque.getAccountNumber(),
            "Cheque drawn - " + cheque.getChequeNumber(),
            cheque.getAmount(),
            "Debit",
            newBalance
        );
        
        // Update cheque status
        cheque.draw(drawnBy);
        return chequeRepository.save(cheque);
    }

    // Bounce cheque - Admin only
    @Transactional
    public Cheque bounceCheque(String chequeNumber, String bouncedBy, String reason) {
        Cheque cheque = chequeRepository.findByChequeNumber(chequeNumber)
                .orElseThrow(() -> new RuntimeException("Cheque not found"));
        
        if (!cheque.canBeBounced()) {
            throw new RuntimeException("Cheque cannot be bounced. Status: " + cheque.getStatus());
        }
        
        // Bounce the cheque
        cheque.bounce(bouncedBy, reason);
        return chequeRepository.save(cheque);
    }

    // Get all drawn cheques
    public Page<Cheque> getDrawnCheques(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("drawnDate").descending());
        return chequeRepository.findByStatus("DRAWN", pageable);
    }

    // Get all bounced cheques
    public Page<Cheque> getBouncedCheques(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("bouncedDate").descending());
        return chequeRepository.findByStatus("BOUNCED", pageable);
    }

    // Search cheques by cheque number (for admin)
    public Page<Cheque> searchChequesByChequeNumber(String chequeNumber, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return chequeRepository.findByChequeNumberContaining(chequeNumber, pageable);
    }

    // Get pending cheque requests - Admin only
    public Page<Cheque> getPendingRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestDate").ascending());
        return chequeRepository.findPendingRequests(pageable);
    }

    // Get approved cheque requests - Admin only
    public Page<Cheque> getApprovedRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("approvedDate").descending());
        return chequeRepository.findApprovedRequests(pageable);
    }

    // Get cheque requests by request status
    public Page<Cheque> getChequeRequestsByStatus(String requestStatus, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestDate").descending());
        return chequeRepository.findByRequestStatus(requestStatus, pageable);
    }

    // Get cheque statistics (all statuses)
    public Map<String, Object> getChequeStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalCheques", chequeRepository.count());
        
        Long activeCount = chequeRepository.countByStatus("ACTIVE");
        Long drawnCount = chequeRepository.countByStatus("DRAWN");
        Long bouncedCount = chequeRepository.countByStatus("BOUNCED");
        Long cancelledCount = chequeRepository.countByStatus("CANCELLED");
        
        stats.put("activeCheques", activeCount != null ? activeCount : 0L);
        stats.put("drawnCheques", drawnCount != null ? drawnCount : 0L);
        stats.put("bouncedCheques", bouncedCount != null ? bouncedCount : 0L);
        stats.put("cancelledCheques", cancelledCount != null ? cancelledCount : 0L);
        
        // Request statistics
        Long pendingCount = chequeRepository.countByRequestStatus("PENDING");
        Long approvedCount = chequeRepository.countByRequestStatus("APPROVED");
        Long rejectedCount = chequeRepository.countByRequestStatus("REJECTED");
        
        stats.put("pendingRequests", pendingCount != null ? pendingCount : 0L);
        stats.put("approvedRequests", approvedCount != null ? approvedCount : 0L);
        stats.put("rejectedRequests", rejectedCount != null ? rejectedCount : 0L);
        
        return stats;
    }
}

