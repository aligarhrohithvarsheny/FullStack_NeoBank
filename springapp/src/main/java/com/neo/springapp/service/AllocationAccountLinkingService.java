package com.neo.springapp.service;

import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.entity.FundsAllocation;
import com.neo.springapp.repository.AllocationAccountRepository;
import com.neo.springapp.repository.EnhancedFundsAllocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Account Linking Service
 * Handles linking, verification, and management of bank accounts for allocations
 * KEY: Only ONE account can be linked per allocation
 */
@Service
@Transactional
public class AllocationAccountLinkingService {
    
    @Autowired
    private AllocationAccountRepository accountRepository;
    
    @Autowired
    private EnhancedFundsAllocationRepository allocationRepository;
    
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
        
        // Check if account already linked
        if (!allocation.getAccountStatus().equals("NOT_LINKED")) {
            throw new RuntimeException("Account already linked to this allocation");
        }
        
        // Validate account doesn't already exist
        Optional<AllocationAccount> existing = accountRepository.findByAccountNumber(accountNumber);
        if (existing.isPresent()) {
            throw new RuntimeException("Account number already linked to another allocation");
        }
        
        // Create account entity
        AllocationAccount account = new AllocationAccount();
        account.setAllocationId(allocationId);
        account.setAccountNumber(accountNumber);
        account.setIfscCode(ifscCode);
        account.setAccountHolderName(accountHolderName);
        account.setBankName(bankName);
        account.setAccountType(accountType);
        account.setBranchName(branchName);
        account.setCity(city);
        account.setLocation(location);
        account.setState(state);
        account.setVerificationStatus("PENDING");
        account.setAccountStatus("ACTIVE");
        account.setLinkedByAdminId(linkedByAdminId);
        account.setLinkedByAdminName(linkedByAdminName);
        account.setLinkedAt(LocalDateTime.now());
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        
        // Initialize balance tracking
        account.setAccountBalance(BigDecimal.ZERO);
        account.setTotalAllocated(allocation.getAllocatedAmount());
        account.setTotalDebited(BigDecimal.ZERO);
        account.setTotalCredited(BigDecimal.ZERO);
        account.setCurrentBalance(allocation.getAllocatedAmount());
        account.setTotalChargesCollected(BigDecimal.ZERO);
        
        // Save account
        AllocationAccount savedAccount = accountRepository.save(account);
        
        // Update allocation with account link
        allocation.setAllocationAccountId(savedAccount.getId());
        allocation.setLinkedAccountNumber(accountNumber);
        allocation.setLinkedIfscCode(ifscCode);
        allocation.setLinkedAccountHolderName(accountHolderName);
        allocation.setLinkedBankName(bankName);
        allocation.setLinkedAccountType(accountType);
        allocation.setAccountStatus("LINKING_PENDING");  // Waiting for cheque verification
        allocation.setLinkedByAdminId(linkedByAdminId);
        allocation.setLinkedByAdminName(linkedByAdminName);
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
     * Step 3: Verify cheque and account (by admin)
     * This is the FINAL step that enables the account to be used
     */
    public AllocationAccount verifyChequeAndAccount(
            Long allocationId,
            boolean approved,
            String verificationNotes,
            Long verifiedByAdminId,
            String verifiedByAdminName) {
        
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
            allocation.setVerifiedByAdminName(verifiedByAdminName);
            allocation.setVerifiedAt(LocalDateTime.now());
            allocation.setAccountStatus("VERIFIED");  // NOW READY TO USE
            
        } else {
            account.setVerificationStatus("REJECTED");
            account.setChequeStatus("BOUNCED");
            account.setVerificationNotes(verificationNotes);
            
            allocation.setAccountVerificationStatus("REJECTED");
            allocation.setAccountStatus("REJECTED");
            allocation.setChequeVerificationNotes(verificationNotes);
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
        
        return allocation.getAccountStatus().equals("VERIFIED") && 
               allocation.getStatus().equals("ACTIVE");
    }
    
    /**
     * Fetch and verify account details from bank (external API call)
     * This would integrate with bank APIs to validate account
     */
    public boolean verifyAccountWithBank(String accountNumber, String ifscCode) {
        // TODO: Integrate with bank API for account verification
        // Return true if verified, false otherwise
        return true;  // Placeholder
    }
}
