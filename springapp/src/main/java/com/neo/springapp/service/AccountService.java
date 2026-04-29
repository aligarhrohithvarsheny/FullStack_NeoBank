package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@SuppressWarnings("null")
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    // Basic CRUD operations
    public Account saveAccount(Account account) {
        if (account.getAccountNumber() == null) {
            account.setAccountNumber(generateAccountNumber());
        }
        return accountRepository.save(account);
    }

    public Account getAccountByPan(String pan) {
        return accountRepository.findByPan(pan);
    }

    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public Account getAccountByAadhar(String aadharNumber) {
        return accountRepository.findByAadharNumber(aadharNumber);
    }

    public Account getAccountByPhone(String phone) {
        List<Account> accounts = accountRepository.findByPhone(phone);
        return accounts != null && !accounts.isEmpty() ? accounts.get(0) : null;
    }

    public Account getAccountByVerificationReference(String verificationReference) {
        return accountRepository.findByAadharVerificationReference(verificationReference);
    }

    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Page<Account> getAllAccountsWithPagination(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return accountRepository.findAll(pageable);
    }

    // Status-based operations
    public List<Account> getAccountsByStatus(String status) {
        return accountRepository.findByStatus(status);
    }

    public Page<Account> getAccountsByStatusWithPagination(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return accountRepository.findByStatus(status, pageable);
    }

    // Account type operations
    public List<Account> getAccountsByType(String accountType) {
        return accountRepository.findByAccountType(accountType);
    }

    public Page<Account> getAccountsByTypeWithPagination(String accountType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return accountRepository.findByAccountType(accountType, pageable);
    }

    // KYC verification operations
    public List<Account> getKycVerifiedAccounts() {
        return accountRepository.findByKycVerified(true);
    }

    public List<Account> getVerifiedMatrixAccounts() {
        return accountRepository.findByVerifiedMatrix(true);
    }

    // Search operations
    public Page<Account> searchAccounts(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return accountRepository.searchAccounts(searchTerm, pageable);
    }

    // Filter operations
    public List<Account> getAccountsByIncomeRange(Double minIncome, Double maxIncome) {
        return accountRepository.findByIncomeRange(minIncome, maxIncome);
    }

    public List<Account> getAccountsByBalanceRange(Double minBalance, Double maxBalance) {
        return accountRepository.findByBalanceRange(minBalance, maxBalance);
    }

    public List<Account> getAccountsByOccupation(String occupation) {
        return accountRepository.findByOccupation(occupation);
    }

    public List<Account> getAccountsByCreatedDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return accountRepository.findByCreatedDateRange(startDate, endDate);
    }

    // Balance operations
    public Account updateBalance(String accountNumber, Double amount) {
        Account account = getAccountByNumber(accountNumber);
        if (account != null) {
            account.setBalance(account.getBalance() + amount);
            account.setLastUpdated(LocalDateTime.now());
            return accountRepository.save(account);
        }
        return null;
    }

    public Double getBalanceByAccountNumber(String accountNumber) {
        Account account = getAccountByNumber(accountNumber);
        return account != null ? account.getBalance() : null;
    }

    public Double debitBalance(String accountNumber, Double amount) {
        Account account = getAccountByNumber(accountNumber);
        if (account == null) {
            return null;
        }
        // Check if account is active
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new RuntimeException("Cannot perform withdrawal: account is not active. Account number: " + accountNumber + " | status=" + account.getStatus());
        }
        
        if (account.getBalance() >= amount) {
            account.setBalance(account.getBalance() - amount);
            account.setLastUpdated(LocalDateTime.now());
            accountRepository.save(account);
            return account.getBalance();
        }
        return null;
    }

    public Double creditBalance(String accountNumber, Double amount) {
        System.out.println("=== ACCOUNT SERVICE - CREDIT BALANCE ===");
        System.out.println("Looking for account: " + accountNumber);
        
        Account account = getAccountByNumber(accountNumber);
        if (account == null) {
            System.out.println("❌ Account not found: " + accountNumber);
            return null;
        }
        // Check if account is active
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            System.out.println("❌ Cannot deposit to inactive account: " + accountNumber + " | status=" + account.getStatus());
            throw new RuntimeException("Cannot perform deposit: account is not active. Account number: " + accountNumber + " | status=" + account.getStatus());
        }
        
        System.out.println("✅ Account found: " + account.getAccountNumber());
        System.out.println("Current balance: " + account.getBalance());
        System.out.println("Amount to add: " + amount);
        account.setBalance(account.getBalance() + amount);
        account.setLastUpdated(LocalDateTime.now());
        accountRepository.save(account);
        return account.getBalance();
    }

    // Statistics operations
    public Long getTotalAccountsCount() {
        return accountRepository.count();
    }

    public Long getAccountsCountByStatus(String status) {
        return accountRepository.countByStatus(status);
    }

    public Long getKycVerifiedAccountsCount() {
        return accountRepository.countKycVerifiedAccounts();
    }

    public Long getVerifiedMatrixAccountsCount() {
        return accountRepository.countVerifiedMatrixAccounts();
    }

    public Double getAverageBalanceByStatus(String status) {
        return accountRepository.getAverageBalanceByStatus(status);
    }

    public Double getTotalBalanceByStatus(String status) {
        return accountRepository.getTotalBalanceByStatus(status);
    }

    public Double getAverageIncomeByStatus(String status) {
        return accountRepository.getAverageIncomeByStatus(status);
    }

    public List<Account> getRecentAccounts(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return accountRepository.findRecentAccounts(pageable);
    }

    // Update operations
    public Account updateAccount(Long id, Account accountDetails) {
        Optional<Account> accountOpt = accountRepository.findById(id);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            // If account is not ACTIVE, allow only status updates (e.g., admin activating the account)
            if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                if (accountDetails.getStatus() == null || accountDetails.getStatus().equalsIgnoreCase(account.getStatus())) {
                    throw new RuntimeException("Cannot update account details until account is active. Account number: " + account.getAccountNumber());
                }
            }
            
            // Update only non-null fields
            if (accountDetails.getName() != null) account.setName(accountDetails.getName());
            if (accountDetails.getDob() != null) account.setDob(accountDetails.getDob());
            if (accountDetails.getAge() != 0) account.setAge(accountDetails.getAge());
            if (accountDetails.getOccupation() != null) account.setOccupation(accountDetails.getOccupation());
            if (accountDetails.getAccountType() != null) account.setAccountType(accountDetails.getAccountType());
            if (accountDetails.getIncome() != null) account.setIncome(accountDetails.getIncome());
            if (accountDetails.getPhone() != null) account.setPhone(accountDetails.getPhone());
            if (accountDetails.getAddress() != null) account.setAddress(accountDetails.getAddress());
            if (accountDetails.getStatus() != null) account.setStatus(accountDetails.getStatus());
            if (accountDetails.isVerifiedMatrix() != account.isVerifiedMatrix()) {
                account.setVerifiedMatrix(accountDetails.isVerifiedMatrix());
            }
            if (accountDetails.isKycVerified() != account.isKycVerified()) {
                account.setKycVerified(accountDetails.isKycVerified());
            }
            
            account.setLastUpdated(LocalDateTime.now());
            return accountRepository.save(account);
        }
        return null;
    }

    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    // Utility methods
    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    /**
     * Public helper to generate a unique account number for new accounts.
     * Tries until a unique value is found (with a safety max attempts).
     */
    public String generateUniqueAccountNumberForNewAccount() {
        int attempts = 0;
        while (attempts < 50) {
            String candidate = generateAccountNumber();
            if (isAccountNumberUnique(candidate)) {
                return candidate;
            }
            attempts++;
        }
        // Fallback: return timestamp-based value
        return "ACC" + System.currentTimeMillis();
    }

    /**
     * Return account by customerId
     */
    public Account getAccountByCustomerId(String customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    /**
     * Return list of accounts missing a customer ID (for migration)
     */
    public List<Account> getAccountsWithoutCustomerId() {
        return accountRepository.findAccountsWithoutCustomerId();
    }

    /**
     * Assign customer IDs to existing accounts that lack one.
     * Returns the number of accounts updated.
     */
    public int assignCustomerIdsToExistingAccounts() {
        List<Account> accounts = getAccountsWithoutCustomerId();
        int updated = 0;
        for (Account acc : accounts) {
            try {
                String cid = generateCustomerIdForAccount(acc);
                if (cid != null && !cid.isEmpty()) {
                    acc.setCustomerId(cid);
                    accountRepository.save(acc);
                    updated++;
                }
            } catch (Exception e) {
                // Log and continue with next
                System.err.println("Error assigning customerId for account id=" + acc.getId() + ": " + e.getMessage());
            }
        }
        return updated;
    }

    // Helper to create a 9-digit numeric customer ID based on PAN + DOB (best-effort)
    public String generateCustomerIdForAccount(Account acc) {
        String pan = acc.getPan() != null ? acc.getPan().replaceAll("\\s+", "") : "";
        String dob = acc.getDob() != null ? acc.getDob().trim() : "";

        // PAN part: take last 4 NUMERIC digits of PAN (pad with zeros if needed)
        String panDigits = pan.replaceAll("\\D", ""); // keep digits only
        String panPart = "0000";
        if (panDigits.length() >= 4) {
            panPart = panDigits.substring(panDigits.length() - 4);
        } else if (panDigits.length() > 0) {
            panPart = String.format("%4s", panDigits).replace(' ', '0');
        }

        // DOB part: produce DDMMY (day 2, month 2, last digit of year)
        String day = "01", month = "01", yearLast = "0";
        try {
            String[] parts = dob.split("[-/]");
            if (parts.length == 3) {
                if (parts[0].length() == 4) { // yyyy-mm-dd
                    yearLast = parts[0].substring(parts[0].length() - 1);
                    month = parts[1];
                    day = parts[2];
                } else { // dd-mm-yyyy or dd/mm/yyyy
                    day = parts[0];
                    month = parts[1];
                    yearLast = parts[2].substring(parts[2].length() - 1);
                }
            }
        } catch (Exception ignored) {}

        day = day.length() == 1 ? "0" + day : day;
        month = month.length() == 1 ? "0" + month : month;

        String dobPart = (day + month + yearLast);

        // Combine and ensure we only have digits
        String candidate = (panPart + dobPart).replaceAll("\\D", "");

        // Ensure 9 characters: if shorter, pad with numeric randoms; if longer, truncate
        if (candidate.length() < 9) {
            StringBuilder sb = new StringBuilder(candidate);
            while (sb.length() < 9) {
                sb.append((int) (Math.random() * 10));
            }
            candidate = sb.toString();
        } else if (candidate.length() > 9) {
            candidate = candidate.substring(0, 9);
        }

        // Ensure uniqueness; if already exists, try small variations
        int attempts = 0;
        while (attempts < 20 && accountRepository.findByCustomerId(candidate) != null) {
            // modify first 4 digits randomly
            String randomPrefix = String.format("%04d", (int) (Math.random() * 10000));
            candidate = randomPrefix + candidate.substring(4);
            attempts++;
        }

        return candidate;
    }

    // Universal account verification across all account types
    public Map<String, Object> verifyAccountByNumber(String accountNumber) {
        Map<String, Object> result = new HashMap<>();

        // Check savings accounts
        Account savingsAccount = accountRepository.findByAccountNumber(accountNumber);
        if (savingsAccount != null) {
            result.put("found", true);
            result.put("name", savingsAccount.getName());
            result.put("accountType", "Savings Account");
            result.put("bankName", "NEO BANK");
            result.put("ifscCode", "NEOB0001234");
            result.put("phone", savingsAccount.getPhone());
            result.put("accountNumber", savingsAccount.getAccountNumber());
            result.put("status", savingsAccount.getStatus());
            return result;
        }

        // Check current accounts
        Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(accountNumber);
        if (currentOpt.isPresent()) {
            CurrentAccount currentAccount = currentOpt.get();
            result.put("found", true);
            result.put("name", currentAccount.getOwnerName());
            result.put("accountType", "Current Account");
            result.put("bankName", "NEO BANK");
            result.put("ifscCode", currentAccount.getIfscCode() != null ? currentAccount.getIfscCode() : "NEOB0001234");
            result.put("phone", currentAccount.getMobile());
            result.put("accountNumber", currentAccount.getAccountNumber());
            result.put("businessName", currentAccount.getBusinessName());
            result.put("status", currentAccount.getStatus());
            return result;
        }

        // Check salary accounts
        SalaryAccount salaryAccount = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (salaryAccount != null) {
            result.put("found", true);
            result.put("name", salaryAccount.getEmployeeName());
            result.put("accountType", "Salary Account");
            result.put("bankName", "NEO BANK");
            result.put("ifscCode", salaryAccount.getIfscCode() != null ? salaryAccount.getIfscCode() : "NEOB0001234");
            result.put("phone", salaryAccount.getMobileNumber());
            result.put("accountNumber", salaryAccount.getAccountNumber());
            result.put("companyName", salaryAccount.getCompanyName());
            result.put("status", salaryAccount.getStatus());
            return result;
        }

        result.put("found", false);
        result.put("message", "Account not found in NEO BANK. Please verify the account number.");
        return result;
    }

    // Validation methods
    public boolean isPanUnique(String pan) {
        return accountRepository.findByPan(pan) == null;
    }

    public boolean isAadharUnique(String aadharNumber) {
        return accountRepository.findByAadharNumber(aadharNumber) == null;
    }

    public boolean isPhoneUnique(String phone) {
        List<Account> accounts = accountRepository.findByPhone(phone);
        return accounts == null || accounts.isEmpty();
    }

    public boolean isAccountNumberUnique(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber) == null;
    }

    /**
     * Verify Aadhar for an account
     */
    public Account verifyAadhar(String aadharNumber, String verificationReference, String verifiedBy) {
        Account account = getAccountByAadhar(aadharNumber);
        if (account != null) {
            account.setAadharVerified(true);
            account.setAadharVerifiedDate(LocalDateTime.now());
            account.setAadharVerificationReference(verificationReference != null ? verificationReference : "ADMIN_" + System.currentTimeMillis());
            account.setAadharVerificationStatus("VERIFIED");
            account.setLastUpdated(LocalDateTime.now());
            return accountRepository.save(account);
        }
        return null;
    }

    /**
     * Verify Aadhar by account number
     */
    public Account verifyAadharByAccountNumber(String accountNumber, String verificationReference, String verifiedBy) {
        Account account = getAccountByNumber(accountNumber);
        if (account != null) {
            account.setAadharVerified(true);
            account.setAadharVerifiedDate(LocalDateTime.now());
            account.setAadharVerificationReference(verificationReference != null ? verificationReference : "ADMIN_" + System.currentTimeMillis());
            account.setAadharVerificationStatus("VERIFIED");
            account.setLastUpdated(LocalDateTime.now());
            return accountRepository.save(account);
        }
        return null;
    }

    /**
     * Get accounts pending Aadhar verification
     */
    public List<Account> getAccountsPendingAadharVerification() {
        return accountRepository.findByAadharVerifiedFalseOrAadharVerificationStatus("PENDING");
    }

    /**
     * Get accounts by Aadhar verification status
     */
    public List<Account> getAccountsByAadharVerificationStatus(String status) {
        return accountRepository.findByAadharVerificationStatus(status);
    }
}
