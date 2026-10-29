package com.neo.springapp.service;

import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.model.Admin;
import com.neo.springapp.model.BranchAccount;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AllocationAccountRepository;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.BranchAccountRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Account Linking Service
 * Handles linking, verification, and management of bank accounts for allocations
 * KEY: Only ONE account can be linked per allocation
 */
@Service
@Transactional
public class AllocationAccountLinkingService {

    private static final String NEOBANK_ACCOUNT_NUMBER = "NEOBANK000001";
    private static final String NEOBANK_IFSC = "NEOB0000001";
    private static final String EXYVAULT_IFSC = "EZYV000123";
    
    @Autowired
    private AllocationAccountRepository accountRepository;
    
    @Autowired
    private FundsAllocationRepository allocationRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AccountRepository savingsAccountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private BranchAccountRepository branchAccountRepository;

    public Map<String, Object> verifyIfscCode(String ifscCode) {
        String normalizedIfsc = normalize(ifscCode);
        Map<String, Object> result = new HashMap<>();
        if (NEOBANK_IFSC.equals(normalizedIfsc)) {
            result.put("success", true);
            result.put("bankName", "NeoBank");
            result.put("branchName", "NeoBank Main Branch");
            result.put("ifscCode", NEOBANK_IFSC);
            return result;
        }
        if (EXYVAULT_IFSC.equals(normalizedIfsc)) {
            result.put("success", true);
            result.put("bankName", "ExyVault");
            result.put("branchName", "ExyVault Main Branch");
            result.put("ifscCode", EXYVAULT_IFSC);
            return result;
        }
        throw new IllegalArgumentException("Only NeoBank or ExyVault IFSC codes are accepted.");
    }

    public Map<String, Object> verifyInternalAccount(String accountNumber, String ifscCode) {
        String normalizedAccountNumber = normalize(accountNumber);
        String normalizedIfsc = normalize(ifscCode);
        verifyIfscCode(normalizedIfsc);

        Map<String, Object> result = new HashMap<>();
        if (NEOBANK_IFSC.equals(normalizedIfsc)) {
            BranchAccount configuredBranchAccount = branchAccountRepository.findAll().stream()
                .filter(branch -> normalizedAccountNumber.equals(normalize(branch.getAccountNumber())))
                .filter(branch -> branch.getIfscCode() == null || branch.getIfscCode().isBlank() ||
                    normalizedIfsc.equals(normalize(branch.getIfscCode())))
                .filter(branch -> branch.getAccountName() != null && branch.getAccountName().toLowerCase(java.util.Locale.ROOT).contains("neo"))
                .findFirst()
                .orElse(null);

            if (!NEOBANK_ACCOUNT_NUMBER.equals(normalizedAccountNumber) && configuredBranchAccount == null) {
                throw new IllegalArgumentException("Account is not NeoBank's registered internal or configured branch account.");
            }
            var account = savingsAccountRepository.findByAccountNumber(normalizedAccountNumber);
            if (account == null || !"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                throw new IllegalArgumentException("NeoBank's internal account is unavailable or inactive.");
            }
            result.put("accountHolderName", account.getName());
            result.put("bankName", "NeoBank");
            result.put("branchName", "NeoBank Main Branch");
            result.put("accountType", "CURRENT");
        } else {
            Optional<CurrentAccount> currentAccount = currentAccountRepository.findByAccountNumber(normalizedAccountNumber);
            CurrentAccount account = currentAccount.orElseThrow(() ->
                new IllegalArgumentException("The ExyVault account number was not found."));
            if (!EXYVAULT_IFSC.equals(normalize(account.getIfscCode())) ||
                    !("ACTIVE".equalsIgnoreCase(account.getStatus()) || "APPROVED".equalsIgnoreCase(account.getStatus()))) {
                throw new IllegalArgumentException("Only an active ExyVault account can be linked.");
            }
            result.put("accountHolderName", account.getOwnerName());
            result.put("bankName", "ExyVault");
            result.put("branchName", account.getBranchName() != null ? account.getBranchName() : "ExyVault Main Branch");
            result.put("city", account.getCity());
            result.put("state", account.getState());
            result.put("accountType", "CURRENT");
        }
        result.put("success", true);
        result.put("accountNumber", normalizedAccountNumber);
        result.put("ifscCode", normalizedIfsc);
        return result;
    }
    
    /**
     * Step 1: Create and link account to allocation
     * Called from HOD Dashboard when adding account details
     */
    public AllocationAccount createAndLinkAccount(
            Long allocationId, 
            String accountNumber,
            String ifscCode,
            String accountHolderName,
            String bankName,
            String accountType,
            String branchName,
            String city,
            String location,
            String state,
            Long linkedByAdminId,
            String linkedByAdminName) {
        
        // Fetch allocation
        FundsAllocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new RuntimeException("Allocation not found: " + allocationId));

        Admin linkingAdmin = adminRepository.findById(linkedByAdminId)
            .orElseThrow(() -> new IllegalArgumentException("HOD account was not found."));
        if (!"HOD".equalsIgnoreCase(linkingAdmin.getRole())) {
            throw new IllegalArgumentException("Only a HOD can link an account to an allocation.");
        }

        if (allocation.getBranchName() == null || allocation.getBranchName().isBlank()) {
            throw new IllegalArgumentException("The selected allocation has no branch assigned.");
        }
        if (branchName == null || !allocation.getBranchName().trim().equalsIgnoreCase(branchName.trim())) {
            throw new IllegalArgumentException("Branch must match the selected allocation.");
        }
        Admin branchManager = allocation.getManagerId() == null
            ? null : adminRepository.findById(allocation.getManagerId()).orElse(null);
        String allocationCity = branchManager != null ? branchManager.getAssignedCity() : null;
        if (allocationCity != null && !allocationCity.isBlank() &&
                (city == null || !allocationCity.trim().equalsIgnoreCase(city.trim()))) {
            throw new IllegalArgumentException("City must match the selected allocation's branch.");
        }

        Map<String, Object> verifiedAccount = verifyInternalAccount(accountNumber, ifscCode);
        String verifiedAccountNumber = (String) verifiedAccount.get("accountNumber");
        String verifiedIfscCode = (String) verifiedAccount.get("ifscCode");
        String verifiedBankName = (String) verifiedAccount.get("bankName");
        String verifiedHolderName = (String) verifiedAccount.get("accountHolderName");
        if (!verifiedBankName.equalsIgnoreCase(bankName == null ? "" : bankName.trim())) {
            throw new IllegalArgumentException("Selected bank does not match the verified IFSC code.");
        }
        if (verifiedHolderName == null || accountHolderName == null ||
            !verifiedHolderName.equalsIgnoreCase(accountHolderName.trim())) {
            throw new IllegalArgumentException("Account holder name does not match the verified account.");
        }
        
        // Check if account already linked
        if (allocation.getAccountStatus() != null && !"NOT_LINKED".equals(allocation.getAccountStatus())) {
            throw new RuntimeException("Account already linked to this allocation");
        }
        
        // Validate account doesn't already exist
        Optional<AllocationAccount> existing = accountRepository.findByAccountNumber(verifiedAccountNumber);
        if (existing.isPresent()) {
            throw new RuntimeException("Account number already linked to another allocation");
        }
        
        // Create account entity
        AllocationAccount account = new AllocationAccount();
        account.setAllocationId(allocationId);
        account.setAccountNumber(verifiedAccountNumber);
        account.setIfscCode(verifiedIfscCode);
        account.setAccountHolderName(verifiedHolderName);
        account.setBankName(verifiedBankName);
        account.setAccountType("CURRENT");
        account.setBranchName(allocation.getBranchName());
        account.setCity(city);
        account.setLocation(location);
        account.setState(state);
        account.setVerificationStatus("PENDING");
        account.setAccountStatus("ACTIVE");
        account.setLinkedByAdminId(linkedByAdminId);
        account.setLinkedByAdminName(linkingAdmin.getName());
        account.setLinkedAt(LocalDateTime.now());
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        
        // Initialize balance tracking
        account.setAccountBalance(BigDecimal.ZERO);
        account.setTotalAllocated(BigDecimal.valueOf(allocation.getAllocatedAmount() == null ? 0.0 : allocation.getAllocatedAmount()));
        account.setTotalDebited(BigDecimal.ZERO);
        account.setTotalCredited(BigDecimal.ZERO);
        account.setCurrentBalance(BigDecimal.valueOf(allocation.getAllocatedAmount() == null ? 0.0 : allocation.getAllocatedAmount()));
        account.setTotalChargesCollected(BigDecimal.ZERO);
        
        // Save account
        AllocationAccount savedAccount = accountRepository.save(account);
        
        // Update allocation with account link
        allocation.setAllocationAccountId(savedAccount.getId());
        allocation.setLinkedAccountNumber(verifiedAccountNumber);
        allocation.setLinkedIfscCode(verifiedIfscCode);
        allocation.setLinkedAccountHolderName(verifiedHolderName);
        allocation.setLinkedBankName(verifiedBankName);
        allocation.setLinkedAccountType("CURRENT");
        allocation.setAccountStatus("LINKING_PENDING");  // Waiting for cheque verification
        allocation.setLinkedByAdminId(linkedByAdminId);
        allocation.setLinkedByAdminName(linkingAdmin.getName());
        allocation.setLinkedAt(LocalDateTime.now());
        allocationRepository.save(allocation);
        
        return savedAccount;
    }
    
    /**
     * Step 2: Add cheque for verification
     * Called when HOD provides cheque details
     */
    public AllocationAccount updateChequeDetails(
            Long allocationId,
            String chequeNumber,
            String chequeHolderName,
            java.time.LocalDate chequeDate,
            String chequeBank,
            String chequeImageUrl) {
        
        AllocationAccount account = accountRepository.findByAllocationId(allocationId)
            .orElseThrow(() -> new RuntimeException("No account linked to this allocation"));
        
        account.setChequeNumber(chequeNumber);
        account.setChequeHolderName(chequeHolderName);
        account.setChequeStatus("PENDING");
        
        // Update allocation
        FundsAllocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new RuntimeException("Allocation not found"));
        
        allocation.setLinkedChequeNumber(chequeNumber);
        allocation.setChequeHolderName(chequeHolderName);
        allocation.setChequeDate(chequeDate);
        allocation.setChequeBank(chequeBank);
        allocation.setChequeImageUrl(chequeImageUrl);
        allocation.setChequeVerificationStatus("PENDING");
        
        accountRepository.save(account);
        allocationRepository.save(allocation);
        
        return account;
    }

    /**
     * Update a linked account and resubmit it for the existing admin verification step.
     */
    public AllocationAccount updateLinkedAccountDetails(
            Long allocationId,
            String accountNumber,
            String ifscCode,
            String accountHolderName,
            String bankName,
            String accountType,
            String branchName,
            String city,
            String location,
            String state,
            String chequeNumber,
            String chequeHolderName,
            LocalDate chequeDate,
            String chequeBank,
            String chequeImageUrl,
            Long hodAdminId) {
        Admin hod = adminRepository.findById(hodAdminId)
            .orElseThrow(() -> new IllegalArgumentException("HOD account was not found."));
        if (!"HOD".equalsIgnoreCase(hod.getRole())) {
            throw new IllegalArgumentException("Only a HOD can edit a linked allocation account.");
        }

        FundsAllocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new IllegalArgumentException("Allocation not found."));
        if (!"ACTIVE".equalsIgnoreCase(allocation.getStatus())) {
            throw new IllegalArgumentException("Only active allocations can be edited.");
        }
        AllocationAccount account = accountRepository.findByAllocationId(allocationId)
            .orElseThrow(() -> new IllegalArgumentException("No account linked to this allocation."));

        if (allocation.getBranchName() == null ||
                !allocation.getBranchName().trim().equalsIgnoreCase(valueOrEmpty(branchName))) {
            throw new IllegalArgumentException("Branch must match the selected allocation.");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required.");
        }
        Admin manager = allocation.getManagerId() == null
            ? null : adminRepository.findById(allocation.getManagerId()).orElse(null);
        String allocationCity = manager == null ? null : manager.getAssignedCity();
        if (allocationCity != null && !allocationCity.isBlank() &&
                !allocationCity.trim().equalsIgnoreCase(valueOrEmpty(city))) {
            throw new IllegalArgumentException("City must match the selected allocation's branch.");
        }

        Map<String, Object> verified = verifyInternalAccount(accountNumber, ifscCode);
        if (!String.valueOf(verified.get("bankName")).equalsIgnoreCase(valueOrEmpty(bankName)) ||
                !String.valueOf(verified.get("accountHolderName")).equalsIgnoreCase(valueOrEmpty(accountHolderName)) ||
                !String.valueOf(verified.get("accountType")).equalsIgnoreCase(valueOrEmpty(accountType))) {
            throw new IllegalArgumentException("Bank and account holder must match the verified account.");
        }
        String verifiedAccountNumber = String.valueOf(verified.get("accountNumber"));
        Optional<AllocationAccount> existingAccount = accountRepository.findByAccountNumber(verifiedAccountNumber);
        if (existingAccount.isPresent() && !existingAccount.get().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Account number is already linked to another allocation.");
        }
        if (chequeNumber == null || !chequeNumber.matches("\\d{6,10}")) {
            throw new IllegalArgumentException("Cheque number must contain 6 to 10 digits.");
        }
        if (chequeDate == null || chequeDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Enter a valid cheque date that is not in the future.");
        }
        if (chequeHolderName == null || chequeHolderName.isBlank() ||
                chequeBank == null || chequeBank.isBlank()) {
            throw new IllegalArgumentException("Cheque holder and cheque bank are required.");
        }

        account.setAccountNumber(verifiedAccountNumber);
        account.setIfscCode(String.valueOf(verified.get("ifscCode")));
        account.setAccountHolderName(String.valueOf(verified.get("accountHolderName")));
        account.setBankName(String.valueOf(verified.get("bankName")));
        account.setAccountType(String.valueOf(verified.get("accountType")));
        account.setBranchName(allocation.getBranchName());
        account.setCity(city.trim());
        account.setLocation(location == null ? "" : location.trim());
        account.setState(state == null ? "" : state.trim());
        account.setChequeNumber(chequeNumber.trim());
        account.setChequeHolderName(chequeHolderName.trim());
        account.setChequeStatus("PENDING");
        account.setVerificationStatus("PENDING");
        account.setVerificationNotes(null);
        account.setVerifiedAt(null);
        account.setVerifiedByAdminId(null);
        account.setUpdatedAt(LocalDateTime.now());

        allocation.setLinkedAccountNumber(verifiedAccountNumber);
        allocation.setLinkedIfscCode(String.valueOf(verified.get("ifscCode")));
        allocation.setLinkedAccountHolderName(String.valueOf(verified.get("accountHolderName")));
        allocation.setLinkedBankName(String.valueOf(verified.get("bankName")));
        allocation.setLinkedAccountType(String.valueOf(verified.get("accountType")));
        allocation.setLinkedChequeNumber(chequeNumber.trim());
        allocation.setChequeHolderName(chequeHolderName.trim());
        allocation.setChequeDate(chequeDate);
        allocation.setChequeBank(chequeBank.trim());
        allocation.setChequeImageUrl(chequeImageUrl);
        allocation.setAccountVerificationStatus("PENDING");
        allocation.setChequeVerificationStatus("PENDING");
        allocation.setAccountStatus("LINKING_PENDING");
        allocation.setAccountVerifiedAt(null);
        allocation.setVerifiedByAdminId(null);
        allocation.setVerifiedByAdminName(null);
        allocation.setVerifiedAt(null);
        allocation.setChequeVerificationNotes(null);
        allocation.setChargeManagementEnabled(false);

        accountRepository.save(account);
        allocationRepository.save(allocation);
        return account;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
    
    /**
     * Step 3: Verify cheque and account (by admin)
     * This is the FINAL step that enables the account to be used
     */
    public AllocationAccount verifyChequeAndAccount(
            Long allocationId,
            boolean approved,
            String verificationNotes,
            Long verifiedByAdminId,
            String verifiedByAdminName) {

        Admin verifyingAdmin = adminRepository.findById(verifiedByAdminId)
            .orElseThrow(() -> new IllegalArgumentException("Admin account was not found."));
        if (!"ADMIN".equalsIgnoreCase(verifyingAdmin.getRole())) {
            throw new IllegalArgumentException("Only an admin can verify an allocation account.");
        }
        
        AllocationAccount account = accountRepository.findByAllocationId(allocationId)
            .orElseThrow(() -> new RuntimeException("No account linked to this allocation"));
        
        FundsAllocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new RuntimeException("Allocation not found"));
        
        if (approved) {
            account.setVerificationStatus("VERIFIED");
            account.setChequeStatus("CLEARED");
            account.setVerifiedAt(LocalDateTime.now());
            account.setVerifiedByAdminId(verifiedByAdminId);
            
            allocation.setAccountVerificationStatus("VERIFIED");
            allocation.setAccountVerifiedAt(LocalDateTime.now());
            allocation.setVerifiedByAdminId(verifiedByAdminId);
            allocation.setVerifiedByAdminName(verifyingAdmin.getName());
            allocation.setVerifiedAt(LocalDateTime.now());
            allocation.setAccountStatus("VERIFIED");  // NOW READY TO USE
            allocation.setChargeManagementEnabled(true);
            
        } else {
            account.setVerificationStatus("REJECTED");
            account.setChequeStatus("BOUNCED");
            account.setVerificationNotes(verificationNotes);
            
            allocation.setAccountVerificationStatus("REJECTED");
            allocation.setAccountStatus("REJECTED");
            allocation.setChequeVerificationNotes(verificationNotes);
            allocation.setChargeManagementEnabled(false);
        }
        
        accountRepository.save(account);
        allocationRepository.save(allocation);
        
        return account;
    }
    
    /**
     * Get linked account details for manager/admin dashboard
     */
    public AllocationAccount getAccountDetails(Long allocationId) {
        return accountRepository.findByAllocationId(allocationId)
            .orElseThrow(() -> new RuntimeException("No account linked to this allocation"));
    }
    
    /**
     * Get all accounts in a city/branch (for HOD dashboard)
     */
    public List<AllocationAccount> getAccountsByLocationAndBranch(String city, String branch) {
        return accountRepository.findByLocationAndBranch(city, branch);
    }
    
    /**
     * Get pending verification accounts (for HOD review)
     */
    public List<AllocationAccount> getPendingVerificationAccounts() {
        return accountRepository.findPendingVerificationAccounts();
    }
    
    /**
     * Get all verified accounts
     */
    public List<AllocationAccount> getVerifiedAccounts() {
        return accountRepository.findVerifiedAccounts();
    }
    
    /**
     * Update account status (e.g., BLOCK account if issues)
     */
    public AllocationAccount updateAccountStatus(Long accountId, String newStatus, String reason) {
        AllocationAccount account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        account.setAccountStatus(newStatus);
        account.setVerificationNotes(reason);
        account.setUpdatedAt(LocalDateTime.now());
        
        return accountRepository.save(account);
    }
    
    /**
     * Check if account is verified and ready to use
     */
    public boolean isAccountVerifiedAndActive(Long allocationId) {
        FundsAllocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new RuntimeException("Allocation not found"));
        
         return "VERIFIED".equals(allocation.getAccountStatus()) &&
             "ACTIVE".equals(allocation.getStatus());
    }
    
    /**
     * Fetch and verify account details from bank (external API call)
     * This would integrate with bank APIs to validate account
     */
    public boolean verifyAccountWithBank(String accountNumber, String ifscCode) {
        try {
            verifyInternalAccount(accountNumber, ifscCode);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
