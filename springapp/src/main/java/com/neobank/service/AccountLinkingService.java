package com.neobank.service;

import com.neobank.entity.Account;
import com.neobank.entity.LinkedAccount;
import com.neobank.entity.LinkedAccountVerification;
import com.neobank.entity.Transaction;
import com.neobank.exception.AccountLinkingException;
import com.neobank.exception.ResourceNotFoundException;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.LinkedAccountRepository;
import com.neobank.repository.LinkedAccountVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class AccountLinkingService {

    private static final Logger logger = LoggerFactory.getLogger(AccountLinkingService.class);
    private static final int MAX_LINKED_ACCOUNTS = 10;
    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_MINUTES = 10;

    @Autowired
    private LinkedAccountRepository linkedAccountRepository;

    @Autowired
    private LinkedAccountVerificationRepository verificationRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    /**
     * Link a new account (initiate linking process)
     */
    public LinkedAccount initiateAccountLinking(Long sourceAccountId, String targetAccountNumber,
                                                 String targetBankCode, String accountAlias) {
        logger.info("Initiating account linking from {} to {}", sourceAccountId, targetAccountNumber);

        // Validate source account
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        // Check if account is active
        if (!sourceAccount.isActive()) {
            throw new AccountLinkingException("Source account is not active");
        }

        // Check maximum linked accounts
        long linkedCount = linkedAccountRepository.countBySourceAccountAndStatus(sourceAccount, "ACTIVE");
        if (linkedCount >= MAX_LINKED_ACCOUNTS) {
            throw new AccountLinkingException("Maximum linked accounts limit reached");
        }

        // Check if account already linked
        Optional<LinkedAccount> existingLink = linkedAccountRepository
                .findBySourceAccountAndTargetAccountNumber(sourceAccount, targetAccountNumber);
        if (existingLink.isPresent() && "ACTIVE".equals(existingLink.get().getStatus())) {
            throw new AccountLinkingException("Account already linked");
        }

        // Create linked account entry
        LinkedAccount linkedAccount = new LinkedAccount();
        linkedAccount.setSourceAccount(sourceAccount);
        linkedAccount.setTargetAccountNumber(targetAccountNumber);
        linkedAccount.setTargetBankCode(targetBankCode);
        linkedAccount.setAccountAlias(accountAlias);
        linkedAccount.setStatus("PENDING");
        linkedAccount.setCreatedAt(LocalDateTime.now());
        linkedAccount.setVerificationAttempts(0);

        LinkedAccount saved = linkedAccountRepository.save(linkedAccount);

        // Generate and send OTP
        sendVerificationOTP(saved);

        // Audit
        auditService.logAccountLinking(sourceAccountId, "LINK_INITIATED", "Account linking initiated", true);

        logger.info("Account linking initiated with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Send OTP for account verification
     */
    public void sendVerificationOTP(LinkedAccount linkedAccount) {
        logger.info("Sending OTP for linked account: {}", linkedAccount.getId());

        String otp = generateOTP();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);

        LinkedAccountVerification verification = new LinkedAccountVerification();
        verification.setLinkedAccount(linkedAccount);
        verification.setOtp(otp);
        verification.setExpiryTime(expiryTime);
        verification.setAttempts(0);
        verification.setCreatedAt(LocalDateTime.now());

        verificationRepository.save(verification);

        // Send OTP via notification service
        try {
            notificationService.sendOTP(
                    linkedAccount.getSourceAccount().getCustomer().getEmail(),
                    otp,
                    "Account Linking Verification",
                    linkedAccount.getTargetAccountNumber()
            );
            logger.info("OTP sent successfully for linked account: {}", linkedAccount.getId());
        } catch (Exception e) {
            logger.error("Failed to send OTP for linked account: {}", linkedAccount.getId(), e);
            throw new AccountLinkingException("Failed to send verification OTP");
        }
    }

    /**
     * Verify account linking with OTP
     */
    public LinkedAccount verifyAccountLinking(Long linkedAccountId, String otp) {
        logger.info("Verifying account linking with ID: {}", linkedAccountId);

        LinkedAccount linkedAccount = linkedAccountRepository.findById(linkedAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked account not found"));

        // Get latest verification
        Optional<LinkedAccountVerification> verification = verificationRepository
                .findLatestByLinkedAccount(linkedAccountId);

        if (verification.isEmpty()) {
            throw new AccountLinkingException("No verification record found");
        }

        LinkedAccountVerification record = verification.get();

        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(record.getExpiryTime())) {
            throw new AccountLinkingException("OTP has expired");
        }

        // Check attempt limit
        if (record.getAttempts() >= 3) {
            linkedAccount.setStatus("BLOCKED");
            linkedAccountRepository.save(linkedAccount);
            throw new AccountLinkingException("Maximum verification attempts exceeded");
        }

        // Verify OTP
        if (!record.getOtp().equals(otp)) {
            record.setAttempts(record.getAttempts() + 1);
            verificationRepository.save(record);
            throw new AccountLinkingException("Invalid OTP");
        }

        // Mark verification as successful
        record.setVerifiedAt(LocalDateTime.now());
        record.setStatus("SUCCESS");
        verificationRepository.save(record);

        // Update linked account status
        linkedAccount.setStatus("ACTIVE");
        linkedAccount.setVerifiedAt(LocalDateTime.now());
        linkedAccount.setVerificationAttempts(0);
        LinkedAccount updated = linkedAccountRepository.save(linkedAccount);

        // Audit
        auditService.logAccountLinking(linkedAccount.getSourceAccount().getId(),
                "LINK_VERIFIED", "Account linking verified", true);

        logger.info("Account linking verified successfully: {}", linkedAccountId);
        return updated;
    }

    /**
     * Get all linked accounts for a source account
     */
    public List<LinkedAccount> getLinkedAccounts(Long sourceAccountId) {
        Account account = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        return linkedAccountRepository.findBySourceAccountAndStatus(account, "ACTIVE");
    }

    /**
     * Get linked account details
     */
    public LinkedAccount getLinkedAccountDetails(Long linkedAccountId, Long sourceAccountId) {
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        LinkedAccount linkedAccount = linkedAccountRepository.findById(linkedAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked account not found"));

        // Verify ownership
        if (!linkedAccount.getSourceAccount().getId().equals(sourceAccountId)) {
            throw new AccountLinkingException("Access denied");
        }

        return linkedAccount;
    }

    /**
     * Unlink account
     */
    public void unlinkAccount(Long linkedAccountId, Long sourceAccountId) {
        logger.info("Unlinking account: {}", linkedAccountId);

        LinkedAccount linkedAccount = linkedAccountRepository.findById(linkedAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked account not found"));

        // Verify ownership
        if (!linkedAccount.getSourceAccount().getId().equals(sourceAccountId)) {
            throw new AccountLinkingException("Access denied");
        }

        // Check if account is not being used in active transactions
        long activeTransactions = 0; // TODO: Implement check for active transactions using this account

        if (activeTransactions > 0) {
            throw new AccountLinkingException("Cannot unlink account with active transactions");
        }

        linkedAccount.setStatus("INACTIVE");
        linkedAccount.setDeactivatedAt(LocalDateTime.now());
        linkedAccountRepository.save(linkedAccount);

        // Audit
        auditService.logAccountLinking(sourceAccountId, "LINK_REMOVED", "Account linking removed", true);

        logger.info("Account unlinked successfully: {}", linkedAccountId);
    }

    /**
     * Resend OTP
     */
    public void resendOTP(Long linkedAccountId) {
        logger.info("Resending OTP for linked account: {}", linkedAccountId);

        LinkedAccount linkedAccount = linkedAccountRepository.findById(linkedAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked account not found"));

        if ("ACTIVE".equals(linkedAccount.getStatus())) {
            throw new AccountLinkingException("Account is already verified");
        }

        sendVerificationOTP(linkedAccount);
    }

    /**
     * Generate random OTP
     */
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Validate linked account before transaction
     */
    public boolean validateLinkedAccount(Long linkedAccountId, Long sourceAccountId) {
        LinkedAccount linkedAccount = linkedAccountRepository.findById(linkedAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked account not found"));

        if (!linkedAccount.getSourceAccount().getId().equals(sourceAccountId)) {
            return false;
        }

        if (!"ACTIVE".equals(linkedAccount.getStatus())) {
            return false;
        }

        return true;
    }

    /**
     * Get verification status
     */
    public String getVerificationStatus(Long linkedAccountId) {
        Optional<LinkedAccountVerification> verification = verificationRepository
                .findLatestByLinkedAccount(linkedAccountId);

        if (verification.isEmpty()) {
            return "PENDING";
        }

        LinkedAccountVerification record = verification.get();
        if ("SUCCESS".equals(record.getStatus())) {
            return "VERIFIED";
        } else if (LocalDateTime.now().isAfter(record.getExpiryTime())) {
            return "EXPIRED";
        } else if (record.getAttempts() >= 3) {
            return "BLOCKED";
        } else {
            return "PENDING";
        }
    }
}
