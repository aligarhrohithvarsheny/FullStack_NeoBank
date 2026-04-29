package com.neo.springapp.service;

import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.CurrentAccountUpiPayment;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.CurrentAccountUpiPaymentRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CurrentAccountUpiService {

    private final CurrentAccountRepository accountRepository;
    private final CurrentAccountUpiPaymentRepository upiPaymentRepository;
    private final QrCodeService qrCodeService;
    private final UserRepository userRepository;
    private final SalaryAccountRepository salaryAccountRepository;

    public CurrentAccountUpiService(CurrentAccountRepository accountRepository,
                                     CurrentAccountUpiPaymentRepository upiPaymentRepository,
                                     QrCodeService qrCodeService,
                                     UserRepository userRepository,
                                     SalaryAccountRepository salaryAccountRepository) {
        this.accountRepository = accountRepository;
        this.upiPaymentRepository = upiPaymentRepository;
        this.qrCodeService = qrCodeService;
        this.userRepository = userRepository;
        this.salaryAccountRepository = salaryAccountRepository;
    }

    // ==================== UPI ID Management ====================

    public String generateDefaultUpiId(CurrentAccount account) {
        if (account.getMobile() != null && !account.getMobile().isEmpty()) {
            String phone = account.getMobile().replaceAll("[^0-9]", "");
            if (phone.length() >= 10) {
                return phone + "@neobank";
            }
        }
        return account.getAccountNumber() + "@neobank";
    }

    @Transactional
    public Map<String, Object> setupUpiId(String accountNumber, String upiId) {
        Map<String, Object> result = new HashMap<>();
        CurrentAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

        if (upiId == null || upiId.trim().isEmpty()) {
            upiId = generateDefaultUpiId(account);
        }
        // Enforce uniqueness across all account types
        String finalUpiId = upiId.trim().toLowerCase();
        if (accountRepository.findByUpiId(finalUpiId).isPresent() &&
                !accountNumber.equals(accountRepository.findByUpiId(finalUpiId).get().getAccountNumber())) {
            result.put("success", false);
            result.put("error", "UPI ID " + finalUpiId + " is already taken by another current account");
            return result;
        }
        if (userRepository.findByUpiId(finalUpiId).isPresent()) {
            result.put("success", false);
            result.put("error", "UPI ID " + finalUpiId + " is already taken by a savings account");
            return result;
        }
        if (salaryAccountRepository.findByUpiId(finalUpiId) != null) {
            result.put("success", false);
            result.put("error", "UPI ID " + finalUpiId + " is already taken by a salary account");
            return result;
        }
        account.setUpiId(finalUpiId);
        account.setUpiEnabled(true);
        accountRepository.save(account);
        result.put("success", true);
        result.put("upiId", finalUpiId);
        return result;
    }

    @Transactional
    public Map<String, Object> updateUpiId(String accountNumber, String newUpiId) {
        Map<String, Object> result = new HashMap<>();
        CurrentAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
        String finalUpiId = newUpiId.trim().toLowerCase();
        // Uniqueness check across all types
        if (accountRepository.findByUpiId(finalUpiId).isPresent() &&
                !accountNumber.equals(accountRepository.findByUpiId(finalUpiId).get().getAccountNumber())) {
            result.put("success", false);
            result.put("error", "UPI ID " + finalUpiId + " is already taken");
            return result;
        }
        if (userRepository.findByUpiId(finalUpiId).isPresent()) {
            result.put("success", false);
            result.put("error", "UPI ID " + finalUpiId + " is already taken");
            return result;
        }
        if (salaryAccountRepository.findByUpiId(finalUpiId) != null) {
            result.put("success", false);
            result.put("error", "UPI ID " + finalUpiId + " is already taken");
            return result;
        }
        account.setUpiId(finalUpiId);
        accountRepository.save(account);
        result.put("success", true);
        result.put("upiId", finalUpiId);
        return result;
    }

    @Transactional
    public CurrentAccount toggleUpi(String accountNumber) {
        CurrentAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
        account.setUpiEnabled(!Boolean.TRUE.equals(account.getUpiEnabled()));
        return accountRepository.save(account);
    }

    // ==================== QR Code Generation ====================

    public Map<String, Object> generateQrCode(String accountNumber, Double amount) {
        CurrentAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

        String upiId = account.getUpiId();
        if (upiId == null || upiId.isEmpty()) {
            upiId = generateDefaultUpiId(account);
            account.setUpiId(upiId);
            accountRepository.save(account);
        }

        String name = account.getBusinessName() != null ? account.getBusinessName() : account.getOwnerName();
        String note = "Payment to " + name;

        String qrCodeImage = qrCodeService.generateUpiQrCode(upiId, name, amount, note);

        Map<String, Object> result = new HashMap<>();
        result.put("qrCode", qrCodeImage);
        result.put("upiId", upiId);
        result.put("businessName", name);
        result.put("accountNumber", accountNumber);
        if (amount != null) result.put("amount", amount);
        return result;
    }

    // ==================== Payment Processing ====================

    @Transactional
    public Map<String, Object> processUpiPayment(CurrentAccountUpiPayment payment) {
        CurrentAccount account = accountRepository.findByAccountNumber(payment.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new RuntimeException("Account is not active");
        }
        if (!Boolean.TRUE.equals(account.getUpiEnabled())) {
            throw new RuntimeException("UPI is not enabled for this account");
        }

        // Credit the amount
        account.setBalance(account.getBalance() + payment.getAmount());
        accountRepository.save(account);

        // Save UPI payment
        payment.setBusinessName(account.getBusinessName());
        if (payment.getUpiId() == null) {
            payment.setUpiId(account.getUpiId() != null ? account.getUpiId() : generateDefaultUpiId(account));
        }
        payment.setStatus("SUCCESS");
        payment.setQrGenerated(true);
        CurrentAccountUpiPayment saved = upiPaymentRepository.save(payment);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("payment", saved);
        result.put("newBalance", account.getBalance());
        result.put("message", "Payment of ₹" + String.format("%.2f", payment.getAmount()) + " received successfully");
        return result;
    }

    // ==================== Transaction History ====================

    public List<CurrentAccountUpiPayment> getPaymentsByAccount(String accountNumber) {
        return upiPaymentRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    public Page<CurrentAccountUpiPayment> getPaymentsPaginated(String accountNumber, int page, int size) {
        return upiPaymentRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber, PageRequest.of(page, size));
    }

    // ==================== Stats ====================

    public Map<String, Object> getUserUpiStats(String accountNumber) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        CurrentAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("upiId", account.getUpiId());
        stats.put("upiEnabled", Boolean.TRUE.equals(account.getUpiEnabled()));
        stats.put("totalReceived", upiPaymentRepository.getTotalReceivedByAccount(accountNumber));
        stats.put("todayReceived", upiPaymentRepository.getTodayReceivedByAccount(accountNumber, startOfDay));
        stats.put("todayTransactions", upiPaymentRepository.getTodayTxnCountByAccount(accountNumber, startOfDay));
        stats.put("balance", account.getBalance());
        return stats;
    }

    public Map<String, Object> getAdminUpiStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPayments", upiPaymentRepository.getTotalSuccessfulPayments());
        stats.put("totalVolume", upiPaymentRepository.getTotalUpiVolume());
        stats.put("activeAccounts", upiPaymentRepository.getActiveUpiAccounts());
        stats.put("todayVolume", upiPaymentRepository.getTodayTotalVolume(startOfDay));
        return stats;
    }

    public List<CurrentAccountUpiPayment> getRecentPayments() {
        return upiPaymentRepository.findTop50ByOrderByCreatedAtDesc();
    }

    // ==================== UPI ID Verification ====================

    public Map<String, Object> verifyUpiId(String upiId) {
        Map<String, Object> result = new HashMap<>();
        if (upiId == null || upiId.trim().isEmpty()) {
            result.put("verified", false);
            result.put("error", "UPI ID cannot be empty");
            return result;
        }

        var accountOpt = accountRepository.findByUpiId(upiId.trim());
        if (accountOpt.isPresent()) {
            CurrentAccount account = accountOpt.get();
            if (!"ACTIVE".equals(account.getStatus())) {
                result.put("verified", false);
                result.put("error", "Account linked to this UPI ID is not active");
                return result;
            }
            if (!Boolean.TRUE.equals(account.getUpiEnabled())) {
                result.put("verified", false);
                result.put("error", "UPI is disabled for this account");
                return result;
            }
            String name = account.getBusinessName() != null ? account.getBusinessName() : account.getOwnerName();
            result.put("verified", true);
            result.put("accountName", name);
            result.put("ownerName", account.getOwnerName());
            result.put("businessName", account.getBusinessName());
            result.put("accountNumber", account.getAccountNumber());
            result.put("upiId", upiId.trim());
        } else {
            result.put("verified", false);
            result.put("error", "No account found with this UPI ID");
        }
        return result;
    }

    public Map<String, Object> generateQrCodeForUpi(String upiId, Double amount, String accountNumber) {
        // Verify UPI belongs to the specified account
        CurrentAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

        String accountUpiId = account.getUpiId();
        if (accountUpiId == null || accountUpiId.isEmpty()) {
            throw new RuntimeException("No UPI ID configured for this account. Please set up UPI first.");
        }

        // Also check linked UPIs in soundbox if upiId is different from account's main UPI
        if (!accountUpiId.equalsIgnoreCase(upiId.trim())) {
            // Check if the upiId belongs to another valid account
            var targetAccount = accountRepository.findByUpiId(upiId.trim());
            if (targetAccount.isEmpty()) {
                throw new RuntimeException("UPI ID not found in system: " + upiId);
            }
            CurrentAccount target = targetAccount.get();
            if (!target.getAccountNumber().equals(accountNumber)) {
                throw new RuntimeException("UPI ID does not belong to this account");
            }
        }

        String name = account.getBusinessName() != null ? account.getBusinessName() : account.getOwnerName();
        String note = "Payment to " + name;

        String qrCodeImage = qrCodeService.generateUpiQrCode(upiId.trim(), name, amount, note);

        Map<String, Object> result = new HashMap<>();
        result.put("qrCode", qrCodeImage);
        result.put("upiId", upiId.trim());
        result.put("businessName", name);
        result.put("accountNumber", accountNumber);
        if (amount != null) result.put("amount", amount);
        return result;
    }

    // ==================== Search by Transaction ID ====================

    public Map<String, Object> searchByTxnId(String txnId) {
        Map<String, Object> result = new HashMap<>();
        var paymentOpt = upiPaymentRepository.findByTxnId(txnId.trim());
        if (paymentOpt.isEmpty()) {
            // Try partial match
            var partialMatches = upiPaymentRepository.findByTxnIdContainingIgnoreCaseOrderByCreatedAtDesc(txnId.trim());
            if (partialMatches.isEmpty()) {
                result.put("found", false);
                result.put("error", "No transaction found with ID: " + txnId);
                return result;
            }
            // Return first match from partial
            var payment = partialMatches.get(0);
            return buildTxnSearchResult(payment, partialMatches);
        }
        var payment = paymentOpt.get();
        return buildTxnSearchResult(payment, List.of(payment));
    }

    private Map<String, Object> buildTxnSearchResult(CurrentAccountUpiPayment payment, List<CurrentAccountUpiPayment> matches) {
        Map<String, Object> result = new HashMap<>();
        result.put("found", true);
        result.put("payment", payment);
        result.put("matches", matches);

        // Get receiver (merchant) details
        var receiverOpt = accountRepository.findByAccountNumber(payment.getAccountNumber());
        if (receiverOpt.isPresent()) {
            CurrentAccount receiver = receiverOpt.get();
            Map<String, Object> receiverDetails = new HashMap<>();
            receiverDetails.put("accountNumber", receiver.getAccountNumber());
            receiverDetails.put("ownerName", receiver.getOwnerName());
            receiverDetails.put("businessName", receiver.getBusinessName());
            receiverDetails.put("mobile", receiver.getMobile());
            receiverDetails.put("upiId", receiver.getUpiId());
            receiverDetails.put("status", receiver.getStatus());
            result.put("receiverDetails", receiverDetails);
        }

        // Get payer details if payer UPI exists
        if (payment.getPayerUpi() != null && !payment.getPayerUpi().isEmpty()) {
            var payerOpt = accountRepository.findByUpiId(payment.getPayerUpi());
            if (payerOpt.isPresent()) {
                CurrentAccount payer = payerOpt.get();
                Map<String, Object> payerDetails = new HashMap<>();
                payerDetails.put("accountNumber", payer.getAccountNumber());
                payerDetails.put("ownerName", payer.getOwnerName());
                payerDetails.put("businessName", payer.getBusinessName());
                payerDetails.put("mobile", payer.getMobile());
                payerDetails.put("upiId", payer.getUpiId());
                payerDetails.put("status", payer.getStatus());
                result.put("payerDetails", payerDetails);
            }
        }
        return result;
    }

    // ==================== Cross-Customer UPI Payment ====================

    @Transactional
    public Map<String, Object> processCrossCustomerPayment(String senderAccountNumber, String receiverUpiId, Double amount, String note) {
        // Validate sender
        CurrentAccount sender = accountRepository.findByAccountNumber(senderAccountNumber)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));
        if (!"ACTIVE".equals(sender.getStatus())) {
            throw new RuntimeException("Sender account is not active");
        }
        if (sender.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance. Available: ₹" + String.format("%.2f", sender.getBalance()));
        }

        // Validate receiver by UPI ID
        CurrentAccount receiver = accountRepository.findByUpiId(receiverUpiId.trim())
                .orElseThrow(() -> new RuntimeException("No NeoBank account found with UPI ID: " + receiverUpiId));
        if (!"ACTIVE".equals(receiver.getStatus())) {
            throw new RuntimeException("Receiver account is not active");
        }
        if (!Boolean.TRUE.equals(receiver.getUpiEnabled())) {
            throw new RuntimeException("Receiver UPI is disabled");
        }

        // Debit sender
        sender.setBalance(sender.getBalance() - amount);
        accountRepository.save(sender);

        // Credit receiver
        receiver.setBalance(receiver.getBalance() + amount);
        accountRepository.save(receiver);

        // Save payment record (receiver side)
        CurrentAccountUpiPayment payment = new CurrentAccountUpiPayment();
        payment.setAccountNumber(receiver.getAccountNumber());
        payment.setBusinessName(receiver.getBusinessName() != null ? receiver.getBusinessName() : receiver.getOwnerName());
        payment.setUpiId(receiverUpiId.trim());
        payment.setAmount(amount);
        payment.setPayerName(sender.getOwnerName());
        payment.setPayerUpi(sender.getUpiId() != null ? sender.getUpiId() : senderAccountNumber + "@neobank");
        payment.setPaymentMethod("UPI");
        payment.setTxnType("CREDIT");
        payment.setStatus("SUCCESS");
        payment.setNote(note != null ? note : "UPI Transfer from " + sender.getOwnerName());
        payment.setQrGenerated(false);
        CurrentAccountUpiPayment saved = upiPaymentRepository.save(payment);

        String receiverName = receiver.getBusinessName() != null ? receiver.getBusinessName() : receiver.getOwnerName();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("payment", saved);
        result.put("senderNewBalance", sender.getBalance());
        result.put("receiverName", receiverName);
        result.put("receiverAccountNumber", receiver.getAccountNumber());
        result.put("message", "₹" + String.format("%.2f", amount) + " sent to " + receiverName + " (" + receiverUpiId + ")");
        return result;
    }
}
