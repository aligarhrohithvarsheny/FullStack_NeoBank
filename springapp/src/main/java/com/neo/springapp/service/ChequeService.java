package com.neo.springapp.service;

import com.neo.springapp.model.Cheque;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.ChequeBookClosureHistory;
import com.neo.springapp.model.ChequeBankRange;
import com.neo.springapp.model.BusinessChequeBankRange;
import com.neo.springapp.model.ChequeLeaf;
import com.neo.springapp.model.BusinessChequeLeaf;
import com.neo.springapp.repository.ChequeRepository;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.ChequeBankRangeRepository;
import com.neo.springapp.repository.BusinessChequeBankRangeRepository;
import com.neo.springapp.repository.ChequeLeafRepository;
import com.neo.springapp.repository.BusinessChequeLeafRepository;
import com.neo.springapp.repository.ChequeBookClosureHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class ChequeService {

    private static final String OTP_REASON_CHEQUE_DRAW = "Cheque Withdrawal / Draw Request";

    private final ChequeRepository chequeRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final UserService userService;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private ChequeBankRangeRepository chequeBankRangeRepository;

    @Autowired
    private BusinessChequeBankRangeRepository businessChequeBankRangeRepository;

    @Autowired
    private ChequeLeafRepository chequeLeafRepository;

    @Autowired
    private BusinessChequeLeafRepository businessChequeLeafRepository;

    @Autowired
    private ChequeBookClosureHistoryRepository chequeBookClosureHistoryRepository;

    public ChequeService(ChequeRepository chequeRepository, AccountRepository accountRepository, 
                        AccountService accountService, TransactionService transactionService,
                        OtpService otpService, EmailService emailService, UserService userService) {
        this.chequeRepository = chequeRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.userService = userService;
    }

    public Map<String, Object> requestDrawOtp(Long chequeId) {
        Map<String, Object> response = new java.util.HashMap<>();
        try {
            Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new RuntimeException("Cheque not found"));
            String userEmail = userService.getUserByAccountNumber(cheque.getAccountNumber())
                .map(u -> u.getEmail()).orElse(null);
            if (userEmail == null || userEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "User email not found. Cannot send OTP.");
                return response;
            }
            String otp = otpService.generateOtp();
            String key = "CHEQUE_DRAW:" + chequeId;
            otpService.storeOtpForKey(key, otp);
            boolean sent = emailService.sendOtpEmailWithReason(userEmail, otp, OTP_REASON_CHEQUE_DRAW);
            if (!sent) {
                response.put("success", false);
                response.put("message", "Failed to send OTP. Please try again.");
                return response;
            }
            response.put("success", true);
            response.put("message", "OTP sent to your registered email.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to send OTP: " + e.getMessage());
        }
        return response;
    }

    private static class AccountHolderInfo {
        String name;
        String type;
        Double balance;
        Object accountObject;

        AccountHolderInfo(String name, String type, Double balance, Object accountObject) {
            this.name = name;
            this.type = type;
            this.balance = balance != null ? balance : 0.0;
            this.accountObject = accountObject;
        }
    }

    private AccountHolderInfo resolveAccountInfo(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return null;
        String cleanAcc = accountNumber.trim();

        Account savings = accountRepository.findByAccountNumber(cleanAcc);
        if (savings != null) {
            return new AccountHolderInfo(savings.getName(), savings.getAccountType() != null ? savings.getAccountType() : "Savings", savings.getBalance(), savings);
        }

        SalaryAccount salary = salaryAccountRepository.findByAccountNumber(cleanAcc);
        if (salary != null) {
            return new AccountHolderInfo(salary.getEmployeeName(), "Salary", salary.getBalance(), salary);
        }

        Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(cleanAcc);
        if (currentOpt.isPresent()) {
            CurrentAccount current = currentOpt.get();
            return new AccountHolderInfo(current.getOwnerName() != null ? current.getOwnerName() : current.getBusinessName(), "Business", current.getBalance(), current);
        }

        return null;
    }

    // Create new cheque leaves for user
    public Cheque createCheque(String accountNumber, int numberOfCheques) {
        AccountHolderInfo accInfo = resolveAccountInfo(accountNumber);
        if (accInfo == null) {
            throw new RuntimeException("Account not found: " + accountNumber);
        }
        
        // Create cheque leaves
        Cheque cheque = new Cheque();
        cheque.setAccountNumber(accountNumber);
        cheque.setAccountHolderName(accInfo.name);
        cheque.setAccountType(accInfo.type);
        cheque.setStatus("ACTIVE");
        cheque.setCreatedAt(LocalDateTime.now());
        
        return chequeRepository.save(cheque);
    }

    // Create multiple cheque leaves with amount
    public List<Cheque> createChequeLeaves(String accountNumber, int numberOfLeaves, Double amount) {
        AccountHolderInfo accInfo = resolveAccountInfo(accountNumber);
        if (accInfo == null) {
            throw new RuntimeException("Account not found: " + accountNumber);
        }
        
        // Cheque policy: every request always issues a fixed cheque book of CHEQUE_BOOK_SIZE leaves,
        // regardless of the numberOfLeaves the caller requested.
        int leavesToCreate = CHEQUE_BOOK_SIZE;

        // Create the fixed-size batch of cheque leaves with globally-unique cheque numbers
        for (int i = 0; i < leavesToCreate; i++) {
            Cheque cheque = new Cheque();
            cheque.setChequeNumber(generateUniqueChequeNumber());
            cheque.setAccountNumber(accountNumber);
            cheque.setAccountHolderName(accInfo.name);
            cheque.setAccountType(accInfo.type);
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

    // Fixed number of cheque leaves issued per cheque book request
    private static final int CHEQUE_BOOK_SIZE = 30;

    // Generate a cheque number that is guaranteed unique across ALL customers/accounts.
    // Retries with a fresh random/timestamp combination on the rare chance of a collision.
    private String generateUniqueChequeNumber() {
        String candidate;
        int attempts = 0;
        do {
            candidate = "CHQ" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
            attempts++;
        } while (chequeRepository.findByChequeNumber(candidate).isPresent() && attempts < 20);
        return candidate;
    }

    public List<Map<String, Object>> fetchChequeBooksByAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return List.of();
        }

        String cleanAcc = accountNumber.trim();
        List<Map<String, Object>> books = new ArrayList<>();

        SalaryAccount salaryAccount = salaryAccountRepository.findByAccountNumber(cleanAcc);
        if (salaryAccount != null) {
            List<ChequeBankRange> ranges = chequeBankRangeRepository.findBySalaryAccountId(salaryAccount.getId());
            if (!ranges.isEmpty()) {
                for (ChequeBankRange range : ranges) {
                    books.add(buildChequeBookMap(salaryAccount.getAccountNumber(), "Salary", range.getChequeBookNumber(), "SALARY",
                            range.getSerialFrom(), range.getSerialTo(), range.getStatus(), range.getIssuedDate()));
                }
            }
            List<ChequeLeaf> leaves = chequeLeafRepository.findBySalaryAccountIdOrderByLeafNumberAsc(salaryAccount.getId());
            if (ranges.isEmpty() && !leaves.isEmpty()) {
                String first = leaves.get(0).getLeafNumber();
                String last = leaves.get(leaves.size() - 1).getLeafNumber();
                books.add(buildChequeBookMap(salaryAccount.getAccountNumber(), "Salary", "LEGACY-SALARY", "SALARY",
                        first, last, "ACTIVE", salaryAccount.getCreatedAt() != null ? salaryAccount.getCreatedAt().toLocalDate() : LocalDateTime.now().toLocalDate()));
            }
        }

        Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(cleanAcc);
        if (currentOpt.isPresent()) {
            CurrentAccount currentAccount = currentOpt.get();
            List<BusinessChequeBankRange> ranges = businessChequeBankRangeRepository.findByCurrentAccountId(currentAccount.getId());
            if (!ranges.isEmpty()) {
                for (BusinessChequeBankRange range : ranges) {
                    books.add(buildChequeBookMap(currentAccount.getAccountNumber(), "Business", range.getChequeBookNumber(), "CURRENT",
                            range.getSerialFrom(), range.getSerialTo(), range.getStatus(), range.getIssuedDate()));
                }
            }
            List<BusinessChequeLeaf> leaves = businessChequeLeafRepository.findByCurrentAccountIdOrderByLeafNumberAsc(currentAccount.getId());
            if (ranges.isEmpty() && !leaves.isEmpty()) {
                String first = leaves.get(0).getLeafNumber();
                String last = leaves.get(leaves.size() - 1).getLeafNumber();
                books.add(buildChequeBookMap(currentAccount.getAccountNumber(), "Business", "LEGACY-CURRENT", "CURRENT",
                        first, last, "ACTIVE", currentAccount.getCreatedAt() != null ? currentAccount.getCreatedAt().toLocalDate() : LocalDateTime.now().toLocalDate()));
            }
        }

        Account savingsAccount = accountRepository.findByAccountNumber(cleanAcc);
        if (savingsAccount != null) {
            List<Cheque> cheques = chequeRepository.findByAccountNumber(cleanAcc);
            if (!cheques.isEmpty() && books.stream().noneMatch(b -> "SAVINGS".equals(b.get("bookType")))) {
                String first = cheques.stream().map(Cheque::getChequeNumber).min(String::compareTo).orElse(cleanAcc);
                String last = cheques.stream().map(Cheque::getChequeNumber).max(String::compareTo).orElse(cleanAcc);
                books.add(buildChequeBookMap(savingsAccount.getAccountNumber(), savingsAccount.getAccountType() != null ? savingsAccount.getAccountType() : "Savings",
                        "LEGACY-SAVINGS", "SAVINGS", first, last, "ACTIVE", LocalDateTime.now().toLocalDate()));
            }
        }

        return books;
    }

    public Map<String, Object> closeChequeBooksForAccount(String accountNumber, String closedBy, String reason) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }

        String cleanAcc = accountNumber.trim();
        String adminName = closedBy == null || closedBy.isBlank() ? "ADMIN" : closedBy.trim();
        String closureReason = reason == null ? "Chequebook closed by admin" : reason.trim();
        int closedBooks = 0;
        LocalDateTime now = LocalDateTime.now();

        SalaryAccount salaryAccount = salaryAccountRepository.findByAccountNumber(cleanAcc);
        if (salaryAccount != null) {
            List<ChequeBankRange> ranges = chequeBankRangeRepository.findBySalaryAccountId(salaryAccount.getId());
            for (ChequeBankRange range : ranges) {
                if (!"CLOSED".equalsIgnoreCase(range.getStatus())) {
                    range.setStatus("CLOSED");
                    range.setUpdatedAt(now);
                    chequeBankRangeRepository.save(range);
                    closedBooks++;
                    chequeBookClosureHistoryRepository.save(new ChequeBookClosureHistory(cleanAcc, "Salary", range.getChequeBookNumber(), "SALARY",
                            range.getSerialFrom(), range.getSerialTo(), adminName, closureReason, 30));
                }
            }

            List<ChequeLeaf> leaves = chequeLeafRepository.findBySalaryAccountIdOrderByLeafNumberAsc(salaryAccount.getId());
            for (ChequeLeaf leaf : leaves) {
                if (!"CLOSED".equalsIgnoreCase(leaf.getStatus())) {
                    leaf.setStatus("CLOSED");
                    chequeLeafRepository.save(leaf);
                }
            }
        }

        Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(cleanAcc);
        if (currentOpt.isPresent()) {
            CurrentAccount currentAccount = currentOpt.get();
            List<BusinessChequeBankRange> ranges = businessChequeBankRangeRepository.findByCurrentAccountId(currentAccount.getId());
            for (BusinessChequeBankRange range : ranges) {
                if (!"CLOSED".equalsIgnoreCase(range.getStatus())) {
                    range.setStatus("CLOSED");
                    range.setUpdatedAt(now);
                    businessChequeBankRangeRepository.save(range);
                    closedBooks++;
                    chequeBookClosureHistoryRepository.save(new ChequeBookClosureHistory(cleanAcc, "Business", range.getChequeBookNumber(), "CURRENT",
                            range.getSerialFrom(), range.getSerialTo(), adminName, closureReason, 30));
                }
            }

            List<BusinessChequeLeaf> leaves = businessChequeLeafRepository.findByCurrentAccountIdOrderByLeafNumberAsc(currentAccount.getId());
            for (BusinessChequeLeaf leaf : leaves) {
                if (!"CLOSED".equalsIgnoreCase(leaf.getStatus())) {
                    leaf.setStatus("CLOSED");
                    businessChequeLeafRepository.save(leaf);
                }
            }
        }

        Account savingsAccount = accountRepository.findByAccountNumber(cleanAcc);
        if (savingsAccount != null) {
            List<Cheque> accountCheques = chequeRepository.findByAccountNumber(cleanAcc);
            if (!accountCheques.isEmpty()) {
                int activeCheques = 0;
                for (Cheque cheque : accountCheques) {
                    if (cheque.isAvailable()) {
                        cheque.cancel(adminName, closureReason);
                        chequeRepository.save(cheque);
                        activeCheques++;
                    }
                }
                if (activeCheques > 0) {
                    closedBooks++;
                    chequeBookClosureHistoryRepository.save(new ChequeBookClosureHistory(cleanAcc,
                            savingsAccount.getAccountType() != null ? savingsAccount.getAccountType() : "Savings",
                            "LEGACY-SAVINGS", "SAVINGS", null, null, adminName, closureReason, activeCheques));
                }
            }
        }

        if (closedBooks == 0) {
            throw new IllegalArgumentException("No active cheque books were found for this account number");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cheque book(s) closed successfully. Future cheque numbers from this account are now invalid.");
        response.put("accountNumber", cleanAcc);
        response.put("closedBooks", closedBooks);
        response.put("closedBy", adminName);
        response.put("reason", closureReason);
        return response;
    }

    public List<Map<String, Object>> getChequeBookClosureHistory(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return List.of();
        }

        List<Map<String, Object>> history = new ArrayList<>();
        for (ChequeBookClosureHistory entry : chequeBookClosureHistoryRepository.findByAccountNumberOrderByClosedAtDesc(accountNumber.trim())) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", entry.getId());
            item.put("accountNumber", entry.getAccountNumber());
            item.put("accountType", entry.getAccountType());
            item.put("chequeBookNumber", entry.getChequeBookNumber());
            item.put("bookType", entry.getBookType());
            item.put("serialFrom", entry.getSerialFrom());
            item.put("serialTo", entry.getSerialTo());
            item.put("closedBy", entry.getClosedBy());
            item.put("reason", entry.getReason());
            item.put("closedCount", entry.getClosedCount());
            item.put("status", entry.getStatus());
            item.put("closedAt", entry.getClosedAt());
            history.add(item);
        }
        return history;
    }

    private Map<String, Object> buildChequeBookMap(String accountNumber, String accountType, String chequeBookNumber,
                                                  String bookType, String serialFrom, String serialTo,
                                                  String status, java.time.LocalDate issuedDate) {
        Map<String, Object> book = new HashMap<>();
        book.put("accountNumber", accountNumber);
        book.put("accountType", accountType);
        book.put("chequeBookNumber", chequeBookNumber);
        book.put("bookType", bookType);
        book.put("serialFrom", serialFrom);
        book.put("serialTo", serialTo);
        book.put("status", status != null ? status : "ACTIVE");
        book.put("issuedDate", issuedDate);
        return book;
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

    public Map<String, Object> verifyForDeposit(String chequeNumber) {
        Cheque cheque = chequeRepository.findByChequeNumber(chequeNumber == null ? "" : chequeNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Cheque number not found"));
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("valid", cheque.isAvailable());
        result.put("chequeId", cheque.getId());
        result.put("chequeNumber", cheque.getChequeNumber());
        result.put("accountNumber", cheque.getAccountNumber());
        result.put("accountHolderName", cheque.getAccountHolderName());
        result.put("accountType", cheque.getAccountType());
        result.put("amount", cheque.getAmount());
        result.put("status", cheque.getStatus());
        AccountHolderInfo accInfo = resolveAccountInfo(cheque.getAccountNumber());
        result.put("availableBalance", accInfo != null ? accInfo.balance : 0.0);
        result.put("message", cheque.isAvailable() ? "Cheque is valid and unused" : "Cheque is already used or unavailable");
        return result;
    }

    public Map<String, Object> verifyForDeposit(String chequeNumber, String receivingAccountNumber) {
        Map<String, Object> result = verifyForDeposit(chequeNumber);
        boolean ownerMatches = receivingAccountNumber != null
                && receivingAccountNumber.trim().equals(result.get("accountNumber"));
        result.put("valid", Boolean.TRUE.equals(result.get("valid")) && ownerMatches);
        result.put("ownerMatches", ownerMatches);
        result.put("message", ownerMatches
                ? result.get("message")
                : "This cheque belongs to another account and cannot be deposited here");
        return result;
    }

    @Transactional
    public Cheque markDeposited(String chequeNumber, String depositReference) {
        Cheque cheque = chequeRepository.findByChequeNumber(chequeNumber == null ? "" : chequeNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Cheque number not found"));
        if (!cheque.isAvailable()) throw new IllegalArgumentException("Cheque is already used or unavailable");
        cheque.markUsed("CASH_DEPOSIT", depositReference);
        return chequeRepository.save(cheque);
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

    // Count used cheques for an account (consumed by DD or draw)
    public Long countUsedCheques(String accountNumber) {
        return chequeRepository.countByAccountNumberAndStatus(accountNumber, "USED");
    }

    // Request cheque drawing - User. If otp is provided, verifies and sets drawRequestOtpVerified.
    @Transactional
    public Cheque requestChequeDraw(Long chequeId, String requestedBy, String otp) {
        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new RuntimeException("Cheque not found"));
        if (!cheque.canBeRequested()) {
            throw new RuntimeException("Cheque cannot be requested. Status: " + cheque.getStatus() + ", Request Status: " + cheque.getRequestStatus());
        }
        if (cheque.getAmount() == null || cheque.getAmount() <= 0) {
            throw new RuntimeException("Cheque amount is invalid or not set. Please set amount before requesting.");
        }
        cheque.requestDraw(requestedBy);
        cheque.setDrawRequestOtpVerified(false);
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
                .orElseThrow(() -> new RuntimeException("Cheque not found with number: " + chequeNumber));
        
        if (!cheque.canBeDrawn()) {
            throw new RuntimeException("Cheque cannot be drawn. Status: " + cheque.getStatus() + ", Request Status: " + cheque.getRequestStatus());
        }
        
        if (cheque.getAmount() == null || cheque.getAmount() <= 0) {
            throw new RuntimeException("Cheque amount is invalid or not set");
        }
        
        AccountHolderInfo accInfo = resolveAccountInfo(cheque.getAccountNumber());
        if (accInfo == null) {
            throw new RuntimeException("Account not found for account number: " + cheque.getAccountNumber());
        }

        Double currentBalance = accInfo.balance;
        if (currentBalance < cheque.getAmount()) {
            throw new RuntimeException("Insufficient balance. Available: ₹" + currentBalance + ", Required: ₹" + cheque.getAmount());
        }

        Double newBalance = currentBalance - cheque.getAmount();

        if (accInfo.accountObject instanceof Account) {
            Account sa = (Account) accInfo.accountObject;
            Double debited = accountService.debitBalance(sa.getAccountNumber(), cheque.getAmount());
            newBalance = debited != null ? debited : newBalance;
        } else if (accInfo.accountObject instanceof SalaryAccount) {
            SalaryAccount sal = (SalaryAccount) accInfo.accountObject;
            sal.setBalance(newBalance);
            sal.setUpdatedAt(LocalDateTime.now());
            salaryAccountRepository.save(sal);
        } else if (accInfo.accountObject instanceof CurrentAccount) {
            CurrentAccount ca = (CurrentAccount) accInfo.accountObject;
            ca.setBalance(newBalance);
            ca.setLastUpdated(LocalDateTime.now());
            currentAccountRepository.save(ca);
        }

        // Create transaction record
        try {
            transactionService.createTransferTransaction(
                cheque.getAccountNumber(),
                "Cheque drawn - " + cheque.getChequeNumber(),
                cheque.getAmount(),
                "Debit",
                newBalance
            );
        } catch (Exception ignored) {}

        // Update cheque status
        cheque.draw(drawnBy);
        return chequeRepository.save(cheque);
    }

    // Revert cheque (withdraw refund) - Admin only within 24 hours
    @Transactional
    public Cheque revertCheque(String chequeNumber, String revertedBy, String reason) {
        Cheque cheque = chequeRepository.findByChequeNumber(chequeNumber)
                .orElseThrow(() -> new RuntimeException("Cheque not found with number: " + chequeNumber));

        if (!cheque.canBeReverted()) {
            if (!"DRAWN".equals(cheque.getStatus())) {
                throw new RuntimeException("Only DRAWN cheques can be reverted. Current status: " + cheque.getStatus());
            }
            throw new RuntimeException("Cheque drawing can only be reverted within 24 hours of drawing.");
        }

        Double refundAmount = cheque.getAmount() != null ? cheque.getAmount() : 0.0;
        if (refundAmount <= 0) {
            throw new RuntimeException("Invalid cheque amount to refund");
        }

        String accNum = cheque.getAccountNumber();
        AccountHolderInfo accInfo = resolveAccountInfo(accNum);
        if (accInfo == null) {
            throw new RuntimeException("Account not found for account number: " + accNum);
        }

        Double newBalance = accInfo.balance + refundAmount;

        if (accInfo.accountObject instanceof Account) {
            Account sa = (Account) accInfo.accountObject;
            Double credited = accountService.creditBalance(sa.getAccountNumber(), refundAmount);
            newBalance = credited != null ? credited : newBalance;
        } else if (accInfo.accountObject instanceof SalaryAccount) {
            SalaryAccount sal = (SalaryAccount) accInfo.accountObject;
            sal.setBalance(newBalance);
            sal.setUpdatedAt(LocalDateTime.now());
            salaryAccountRepository.save(sal);
        } else if (accInfo.accountObject instanceof CurrentAccount) {
            CurrentAccount ca = (CurrentAccount) accInfo.accountObject;
            ca.setBalance(newBalance);
            ca.setLastUpdated(LocalDateTime.now());
            currentAccountRepository.save(ca);
        }

        // Record refund transaction in history
        try {
            transactionService.createTransferTransaction(
                accNum,
                "Cheque Reverted / Refund - " + cheque.getChequeNumber() + " (By: " + revertedBy + ")",
                refundAmount,
                "Credit",
                newBalance
            );
        } catch (Exception ignored) {}

        cheque.revert(revertedBy, reason != null && !reason.isBlank() ? reason : "Admin Revert within 24h");
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

