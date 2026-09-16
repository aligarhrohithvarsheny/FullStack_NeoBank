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
                ca.setLastUpdated(LocalDateTime.now());
                currentAccountRepository.save(ca);
                return ca.getBalance();
            }
            case "SALARY": {
                SalaryAccount sal = salaryAccountRepository.findByAccountNumber(accountNumber);
                if (sal == null) throw new IllegalArgumentException("Account not found for number: " + accountNumber);
                sal.setBalance((sal.getBalance() == null ? 0.0 : sal.getBalance()) + amount);
                sal.setUpdatedAt(LocalDateTime.now());
                salaryAccountRepository.save(sal);
                return sal.getBalance();
            }
            default:
                return accountService.creditBalance(accountNumber, amount);
        }
    }

    private Double debitAnyAccount(String accountNumber, String type, Double amount) {
        switch (type) {
            case "CURRENT": {
                CurrentAccount ca = currentAccountRepository.findByAccountNumber(accountNumber)
                        .orElseThrow(() -> new IllegalArgumentException("Source account not found for number: " + accountNumber));
                Double currentBal = ca.getBalance() == null ? 0.0 : ca.getBalance();
                if (currentBal < amount) {
                    throw new IllegalArgumentException("Insufficient balance in source account " + accountNumber + ". Available: ₹" + currentBal + ", Required: ₹" + amount);
                }
                ca.setBalance(currentBal - amount);
                ca.setLastUpdated(LocalDateTime.now());
                currentAccountRepository.save(ca);
                return ca.getBalance();
            }
            case "SALARY": {
                SalaryAccount sal = salaryAccountRepository.findByAccountNumber(accountNumber);
                if (sal == null) throw new IllegalArgumentException("Source account not found for number: " + accountNumber);
                Double currentBal = sal.getBalance() == null ? 0.0 : sal.getBalance();
                if (currentBal < amount) {
                    throw new IllegalArgumentException("Insufficient balance in source account " + accountNumber + ". Available: ₹" + currentBal + ", Required: ₹" + amount);
                }
                sal.setBalance(currentBal - amount);
                sal.setUpdatedAt(LocalDateTime.now());
                salaryAccountRepository.save(sal);
                return sal.getBalance();
            }
            default: {
                Double newBal = accountService.debitBalance(accountNumber, amount);
                if (newBal == null) {
                    throw new IllegalArgumentException("Insufficient balance or inactive source account: " + accountNumber);
                }
                return newBal;
            }
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
        ResolvedAccount account = resolveAnyAccount(request.getAccountNumber().trim());
        if (account == null) {
            throw new IllegalArgumentException("Account not found for number: " + request.getAccountNumber());
        }

        // Prefill user friendly fields
        request.setUserName(account.name);
        if ("CHEQUE".equalsIgnoreCase(request.getMethod())) {
            if (request.getReferenceNumber() == null || request.getReferenceNumber().isBlank()) {
                throw new IllegalArgumentException("Cheque number is required for cheque deposits");
            }
            String chequeNum = request.getReferenceNumber().trim();
            Map<String, Object> cheque = chequeService.verifyForDeposit(chequeNum);
            if (!Boolean.TRUE.equals(cheque.get("valid"))) {
                throw new IllegalArgumentException("Cheque is invalid or already used/drawn/cancelled: " + cheque.get("message"));
            }

            String chqAccNum = String.valueOf(cheque.get("accountNumber"));
            String chqAccHolder = String.valueOf(cheque.get("accountHolderName"));
            String chqStatus = String.valueOf(cheque.get("status"));
            Double chqBal = (Double) cheque.get("availableBalance");

            request.setChequeValid(true);
            request.setChequeAccountNumber(chqAccNum);
            request.setChequeAccountHolderName(chqAccHolder);
            request.setChequeStatus(chqStatus);
            request.setChequeAvailableBalance(chqBal);

            // Determine if SELF or OTHER account cheque transfer
            if (chqAccNum != null && chqAccNum.trim().equalsIgnoreCase(request.getAccountNumber().trim())) {
                request.setTransferType("SELF");
            } else {
                request.setTransferType("OTHER");
                request.setSourceAccountNumber(chqAccNum);
                request.setSourceAccountName(chqAccHolder);
            }
        } else {
            request.setTransferType("SELF");
        }

        request.setStatus("PENDING");
        String requestedSlipId = request.getRequestId() == null ? null : request.getRequestId().trim();
        if (requestedSlipId != null && !requestedSlipId.isEmpty()
            && depositRequestRepository.findByRequestId(requestedSlipId).isPresent()) {
            throw new IllegalArgumentException("Deposit ID is already in use");
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

        ResolvedAccount targetAccount = resolveAnyAccount(request.getAccountNumber());
        if (targetAccount == null) {
            throw new IllegalArgumentException("Account not found for number: " + request.getAccountNumber());
        }

        if ("CLOSED".equalsIgnoreCase(targetAccount.status)) {
            throw new IllegalStateException("Cannot approve deposit for a closed account. Account number: " + request.getAccountNumber());
        }

        // Handle Cheque Deposits & Transfers
        if ("CHEQUE".equalsIgnoreCase(request.getMethod())) {
            if (request.getReferenceNumber() == null || request.getReferenceNumber().isBlank()) {
                throw new IllegalArgumentException("Cheque number is required for cheque deposits");
            }

            // Check if OTHER account cheque transfer
            if ("OTHER".equalsIgnoreCase(request.getTransferType()) && request.getSourceAccountNumber() != null) {
                ResolvedAccount sourceAccount = resolveAnyAccount(request.getSourceAccountNumber());
                if (sourceAccount == null) {
                    throw new IllegalArgumentException("Source cheque account not found: " + request.getSourceAccountNumber());
                }

                // Debit amount from source account
                Double sourceNewBal = debitAnyAccount(request.getSourceAccountNumber(), sourceAccount.type, request.getAmount());

                // Log Debit transaction for source account
                Transaction sourceTxn = new Transaction();
                sourceTxn.setMerchant("Cheque Transfer Debit");
                sourceTxn.setAmount(request.getAmount());
                sourceTxn.setType("Debit");
                sourceTxn.setDescription("Cheque #" + request.getReferenceNumber() + " transferred to " + request.getAccountNumber() + " (Deposit ID: " + request.getRequestId() + ")");
                sourceTxn.setBalance(sourceNewBal);
                sourceTxn.setStatus("Completed");
                sourceTxn.setUserName(sourceAccount.name);
                sourceTxn.setAccountNumber(request.getSourceAccountNumber());
                transactionService.saveTransaction(sourceTxn);
            }

            chequeService.markDeposited(request.getReferenceNumber(), request.getRequestId());
        }

        // Credit target account
        Double newBalance = creditAnyAccount(request.getAccountNumber(), targetAccount.type, request.getAmount());
        if (newBalance == null) {
            throw new IllegalStateException("Unable to credit balance. Please verify account number.");
        }

        // Save Credit transaction
        String desc = request.getNote() != null && !request.getNote().isBlank() ? request.getNote() : "Deposit approved - " + request.getRequestId();
        if ("OTHER".equalsIgnoreCase(request.getTransferType()) && request.getSourceAccountNumber() != null) {
            desc = "Cheque Transfer Credit from " + request.getSourceAccountName() + " (" + request.getSourceAccountNumber() + ") - Cheque #" + request.getReferenceNumber() + " | " + desc;
        }

        Transaction transaction = new Transaction();
        transaction.setMerchant("Deposit Request (" + (request.getMethod() != null ? request.getMethod() : "Cash") + ")");
        transaction.setAmount(request.getAmount());
        transaction.setType("Deposit");
        transaction.setDescription(desc);
        transaction.setBalance(newBalance);
        transaction.setStatus("Completed");
        transaction.setUserName(targetAccount.name);
        transaction.setAccountNumber(request.getAccountNumber());
        Transaction savedTxn = transactionService.saveTransaction(transaction);

        request.setStatus("APPROVED");
        request.setProcessedBy(processedBy != null ? processedBy : "Admin");
        request.setProcessedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        request.setResultingBalance(newBalance);
        request.setTransactionId(savedTxn.getTransactionId());
        return depositRequestRepository.save(request);
    }

    @Transactional
    public DepositRequest createAndApproveDirectDeposit(DepositRequest request, String adminEmail) {
        DepositRequest created = createRequest(request);
        return approveRequest(created.getId(), adminEmail != null ? adminEmail : "Admin");
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

