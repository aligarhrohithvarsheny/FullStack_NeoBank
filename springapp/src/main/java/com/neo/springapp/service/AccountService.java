package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

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
        if (account != null && account.getBalance() >= amount) {
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
        if (account != null) {
            System.out.println("✅ Account found: " + account.getAccountNumber());
            System.out.println("Current balance: " + account.getBalance());
            System.out.println("Amount to add: " + amount);
            account.setBalance(account.getBalance() + amount);
            account.setLastUpdated(LocalDateTime.now());
            accountRepository.save(account);
            return account.getBalance();
        } else {
            System.out.println("❌ Account not found: " + accountNumber);
            return null;
        }
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

    // Validation methods
    public boolean isPanUnique(String pan) {
        return accountRepository.findByPan(pan) == null;
    }

    public boolean isAadharUnique(String aadharNumber) {
        return accountRepository.findByAadharNumber(aadharNumber) == null;
    }

    public boolean isAccountNumberUnique(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber) == null;
    }
}
