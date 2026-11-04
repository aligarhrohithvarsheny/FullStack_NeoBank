package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SavingsUpiService {

    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private SavingsUpiTransactionRepository savingsUpiTxnRepo;
    @Autowired private SalaryAccountRepository salaryAccountRepository;
    @Autowired private CurrentAccountRepository currentAccountRepository;
    @Autowired private EmergencyControlService emergencyControlService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    // ── Generate default UPI ID from account number ──────────────────────────
    private String generateDefaultUpiId(User user) {
        String base = user.getAccountNumber() != null
                ? user.getAccountNumber()
                : (user.getAccount() != null && user.getAccount().getPhone() != null
                        ? user.getAccount().getPhone().replaceAll("[^0-9]", "")
                        : String.valueOf(user.getId()));
        return base.toLowerCase() + "@neobank";
    }

    // ── Setup / Enable UPI ────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> setupUpi(String accountNumber) {
        Map<String, Object> res = new HashMap<>();
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) { res.put("success", false); res.put("error", "User not found"); return res; }
        if (!"APPROVED".equalsIgnoreCase(user.getStatus())) {
            res.put("success", false); res.put("error", "Account not approved"); return res;
        }
        if (user.getUpiId() == null) {
            String generated = generateDefaultUpiId(user);
            // ensure uniqueness
            if (userRepository.findByUpiId(generated).isPresent()) {
                generated = accountNumber.toLowerCase() + System.currentTimeMillis() % 1000 + "@neobank";
            }
            user.setUpiId(generated);
        }
        user.setUpiEnabled(true);
        userRepository.save(user);
        res.put("success", true);
        res.put("upiId", user.getUpiId());
        res.put("upiEnabled", true);
        return res;
    }

    // ── Toggle UPI on/off ─────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> toggleUpi(String accountNumber) {
        Map<String, Object> res = new HashMap<>();
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) { res.put("success", false); res.put("error", "User not found"); return res; }
        boolean newState = !Boolean.TRUE.equals(user.getUpiEnabled());
        user.setUpiEnabled(newState);
        userRepository.save(user);
        res.put("success", true);
        res.put("upiEnabled", newState);
        res.put("message", newState ? "UPI enabled" : "UPI disabled");
        return res;
    }

    // ── Set UPI Transaction PIN (6 digits) ────────────────────────────────────
    @Transactional
    public Map<String, Object> setTransactionPin(String accountNumber, String newPin) {
        Map<String, Object> res = new HashMap<>();
        if (newPin == null || !newPin.matches("[0-9]{6}")) {
            res.put("success", false); res.put("error", "PIN must be exactly 6 digits"); return res;
        }
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) { res.put("success", false); res.put("error", "User not found"); return res; }
        user.setTransactionPin(encoder.encode(newPin));
        user.setTransactionPinSet(true);
        userRepository.save(user);
        res.put("success", true);
        res.put("message", "UPI PIN set successfully");
        return res;
    }

    // ── Forgot / Reset UPI PIN (verify via registered phone) ──────────────────
    @Transactional
    public Map<String, Object> forgotPin(String accountNumber, String registeredPhone, String newPin) {
        Map<String, Object> res = new HashMap<>();
        if (newPin == null || !newPin.matches("[0-9]{6}")) {
            res.put("success", false); res.put("error", "New PIN must be exactly 6 digits"); return res;
        }
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) { res.put("success", false); res.put("error", "Account not found"); return res; }
        // Verify registered phone matches
        String storedPhone = user.getAccount() != null ? user.getAccount().getPhone() : null;
        if (storedPhone == null || !storedPhone.replaceAll("[^0-9]", "").endsWith(
                registeredPhone.replaceAll("[^0-9]", ""))) {
            res.put("success", false); res.put("error", "Phone number does not match our records"); return res;
        }
        user.setTransactionPin(encoder.encode(newPin));
        user.setTransactionPinSet(true);
        userRepository.save(user);
        res.put("success", true);
        res.put("message", "UPI PIN reset successfully");
        return res;
    }

    // ── Verify UPI ID (any account type: savings/salary/current) ─────────────
    public Map<String, Object> verifyUpiId(String rawUpiId) {
        Map<String, Object> res = new HashMap<>();
        if (rawUpiId == null || rawUpiId.trim().isEmpty()) {
            res.put("verified", false); res.put("error", "UPI ID cannot be empty"); return res;
        }
        String upiId = rawUpiId.trim().toLowerCase();

        // Savings accounts
        Optional<User> userOpt = userRepository.findByUpiId(upiId);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            if (!Boolean.TRUE.equals(u.getUpiEnabled())) {
                res.put("verified", false); res.put("error", "UPI is disabled for this account"); return res;
            }
            String name = (u.getAccount() != null && u.getAccount().getName() != null)
                    ? u.getAccount().getName() : u.getUsername();
            res.put("verified", true);
            res.put("name", name);
            res.put("accountNumber", u.getAccountNumber());
            res.put("accountType", "SAVINGS");
            return res;
        }

        // Salary accounts
        SalaryAccount sa = salaryAccountRepository.findByUpiId(upiId);
        if (sa != null) {
            if (!Boolean.TRUE.equals(sa.getUpiEnabled())) {
                res.put("verified", false); res.put("error", "UPI not enabled for this account"); return res;
            }
            res.put("verified", true);
            res.put("name", sa.getEmployeeName());
            res.put("accountNumber", sa.getAccountNumber());
            res.put("accountType", "SALARY");
            return res;
        }

        // Current accounts
        Optional<CurrentAccount> caOpt = currentAccountRepository.findByUpiId(upiId);
        if (caOpt.isPresent()) {
            CurrentAccount ca = caOpt.get();
            if (!"ACTIVE".equals(ca.getStatus()) || !Boolean.TRUE.equals(ca.getUpiEnabled())) {
                res.put("verified", false); res.put("error", "UPI not active for this account"); return res;
            }
            String name = ca.getBusinessName() != null ? ca.getBusinessName() : ca.getOwnerName();
            res.put("verified", true);
            res.put("name", name);
            res.put("accountNumber", ca.getAccountNumber());
            res.put("accountType", "CURRENT");
            return res;
        }

        res.put("verified", false);
        res.put("error", "No account found with this UPI ID");
        return res;
    }

    // ── Send Money via UPI ────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> sendMoney(String senderAccountNumber, String receiverUpiId,
                                         BigDecimal amount, String pin, String remark) {
        return sendMoney(senderAccountNumber, receiverUpiId, amount, pin, remark, null, null);
    }

    @Transactional
    public Map<String, Object> sendMoney(String senderAccountNumber, String receiverUpiId,
                                         BigDecimal amount, String pin, String remark,
                                         String sourceIp, String userAgent) {
        return sendMoney(senderAccountNumber, receiverUpiId, amount, pin, remark, sourceIp, userAgent,
                null, null, null);
    }

    @Transactional
    public Map<String, Object> sendMoney(String senderAccountNumber, String receiverUpiId,
                                         BigDecimal amount, String pin, String remark,
                                         String sourceIp, String userAgent,
                                         Double locationLatitude, Double locationLongitude,
                                         Double locationAccuracyMeters) {
        Map<String, Object> res = new HashMap<>();

        // Validate inputs
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            recordFailedAttempt(senderAccountNumber, receiverUpiId, amount, "Invalid amount", null, sourceIp, userAgent);
            res.put("success", false); res.put("error", "Invalid amount"); return res;
        }
        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            recordFailedAttempt(senderAccountNumber, receiverUpiId, amount, "Amount exceeds UPI limit of ₹1,00,000", null, sourceIp, userAgent);
            res.put("success", false); res.put("error", "Amount exceeds UPI limit of ₹1,00,000"); return res;
        }

        // Load sender
        User sender = userRepository.findByAccountNumber(senderAccountNumber).orElse(null);
        if (sender == null) { res.put("success", false); res.put("error", "Sender not found"); return res; }
        if (!Boolean.TRUE.equals(sender.getUpiEnabled())) {
            recordFailedAttempt(senderAccountNumber, receiverUpiId, amount, "UPI is not enabled for this account", sender, sourceIp, userAgent);
            res.put("success", false); res.put("error", "UPI is not enabled for your account"); return res;
        }
        if (!Boolean.TRUE.equals(sender.getTransactionPinSet()) || sender.getTransactionPin() == null) {
            recordFailedAttempt(senderAccountNumber, receiverUpiId, amount, "UPI PIN is not set", sender, sourceIp, userAgent);
            res.put("success", false); res.put("error", "Please set your UPI PIN first"); return res;
        }
        if (!encoder.matches(pin, sender.getTransactionPin())) {
            recordFailedAttempt(senderAccountNumber, receiverUpiId, amount, "Incorrect UPI PIN", sender, sourceIp, userAgent);
            res.put("success", false); res.put("error", "Incorrect UPI PIN"); return res;
        }

        Account senderAcc = sender.getAccount();
        if (senderAcc == null) {
            recordFailedAttempt(senderAccountNumber, receiverUpiId, amount, "Account not found", sender, sourceIp, userAgent);
            res.put("success", false); res.put("error", "Account not found"); return res;
        }
        emergencyControlService.assertAccountNotIsolated(senderAccountNumber);
        if (senderAcc.getBalance() == null || senderAcc.getBalance() < amount.doubleValue()) {
            recordFailedAttempt(senderAccountNumber, receiverUpiId, amount, "Insufficient balance", sender, sourceIp, userAgent);
            res.put("success", false); res.put("error", "Insufficient balance"); return res;
        }

        // Fraud check: flag if > ₹50,000 or multiple txns in 5 min
        int riskScore = 0;
        if (amount.compareTo(new BigDecimal("50000")) > 0) riskScore += 40;
        if (amount.compareTo(new BigDecimal("10000")) > 0) riskScore += 20;
        LocalDateTime fiveMinAgo = LocalDateTime.now().minusMinutes(5);
        long recentCount = savingsUpiTxnRepo
                .findBySenderAccountOrderByCreatedAtDesc(senderAccountNumber)
                .stream().filter(t -> t.getCreatedAt().isAfter(fiveMinAgo)).count();
        if (recentCount >= 3) riskScore += 30;

        // Verify receiver UPI ID
        if (receiverUpiId == null || receiverUpiId.isBlank()) {
            recordFailedAttempt(senderAccountNumber, receiverUpiId, amount, "Receiver UPI ID is required", sender, sourceIp, userAgent);
            res.put("success", false); res.put("error", "Receiver UPI ID is required"); return res;
        }
        String rUpi = receiverUpiId.trim().toLowerCase();
        if (rUpi.equals(sender.getUpiId())) {
            recordFailedAttempt(senderAccountNumber, receiverUpiId, amount, "Cannot send money to your own UPI ID", sender, sourceIp, userAgent);
            res.put("success", false); res.put("error", "Cannot send money to yourself"); return res;
        }
        Map<String, Object> verify = verifyUpiId(rUpi);
        if (!Boolean.TRUE.equals(verify.get("verified"))) {
            String error = verify.getOrDefault("error", "Invalid receiver UPI ID").toString();
            recordFailedAttempt(senderAccountNumber, receiverUpiId, amount, error, sender, sourceIp, userAgent);
            res.put("success", false); res.put("error", error); return res;
        }
        String receiverName = (String) verify.get("name");
        String receiverAccount = (String) verify.get("accountNumber");
        String receiverType = (String) verify.get("accountType");

        // Build transaction record
        String txnRef = "SUPI" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(9999));
        SavingsUpiTransaction txn = new SavingsUpiTransaction();
        txn.setTransactionRef(txnRef);
        txn.setSenderAccount(senderAccountNumber);
        txn.setSenderUpiId(sender.getUpiId());
        txn.setSenderName(senderAcc.getName());
        txn.setReceiverUpiId(rUpi);
        txn.setReceiverName(receiverName);
        txn.setReceiverAccount(receiverAccount);
        txn.setAmount(amount);
        txn.setRemark(remark);
        txn.setPaymentMethod("UPI");
        txn.setRiskScore(riskScore);
        txn.setSourceIp(truncate(sourceIp, 64));
        txn.setUserAgent(truncate(userAgent, 512));
        Map<String, Object> senderProfile = participantProfile(senderAccountNumber, sender.getUpiId());
        Map<String, Object> receiverProfile = participantProfile(receiverAccount, rUpi);
        txn.setSenderPhone((String) senderProfile.get("phone"));
        txn.setSenderAddress((String) senderProfile.get("address"));
        txn.setReceiverPhone((String) receiverProfile.get("phone"));
        txn.setReceiverAddress((String) receiverProfile.get("address"));
        if (isValidLocation(locationLatitude, locationLongitude, locationAccuracyMeters)) {
            txn.setLocationLatitude(locationLatitude);
            txn.setLocationLongitude(locationLongitude);
            txn.setLocationAccuracyMeters(locationAccuracyMeters);
        }

        boolean flagged = riskScore >= 70;
        txn.setFraudFlagged(flagged);

        if (flagged) {
            txn.setStatus("FLAGGED");
            savingsUpiTxnRepo.save(txn);
            res.put("success", false);
            res.put("error", "Transaction flagged for review (risk score: " + riskScore + "). Contact support.");
            return res;
        }

        // Credit must be confirmed before this transaction is reported as successful.
        if (!creditReceiver(receiverType, receiverAccount, rUpi, amount)) {
            recordFailedAttempt(senderAccountNumber, receiverUpiId, amount,
                    "Receiver account could not be credited", sender, sourceIp, userAgent);
            res.put("success", false);
            res.put("error", "Receiver account could not be credited. No money was debited.");
            return res;
        }
        txn.setReceiverCredited(true);

        // Both account updates run in the same transaction, so a failed debit rolls back the credit.
        senderAcc.setBalance(senderAcc.getBalance() - amount.doubleValue());
        accountRepository.save(senderAcc);
        txn.setSenderDebited(true);

        txn.setStatus("SUCCESS");
        savingsUpiTxnRepo.save(txn);

        res.put("success", true);
        res.put("transactionRef", txnRef);
        res.put("receiverName", receiverName);
        res.put("amount", amount);
        res.put("message", "₹" + amount + " sent successfully to " + receiverName);
        res.put("transaction", txn);
        return res;
    }

    private void recordFailedAttempt(String senderAccountNumber, String receiverUpiId, BigDecimal amount,
                                     String failureReason, User knownSender, String sourceIp, String userAgent) {
        User sender = knownSender != null
                ? knownSender
                : userRepository.findByAccountNumber(senderAccountNumber).orElse(null);
        if (sender == null) return;
        if (amount != null && amount.precision() > 16) return;

        SavingsUpiTransaction attempt = new SavingsUpiTransaction();
        attempt.setTransactionRef("SUPI" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(9999)));
        attempt.setSenderAccount(senderAccountNumber);
        attempt.setSenderUpiId(sender.getUpiId());
        attempt.setSenderName(sender.getAccount() != null ? sender.getAccount().getName() : sender.getUsername());
        String recipient = receiverUpiId == null || receiverUpiId.isBlank()
                ? "Not provided" : receiverUpiId.trim().toLowerCase();
        attempt.setReceiverUpiId(recipient.length() > 255 ? recipient.substring(0, 255) : recipient);
        attempt.setAmount(amount == null ? BigDecimal.ZERO : amount);
        String reason = "Failed: " + failureReason;
        attempt.setRemark(reason.length() > 255 ? reason.substring(0, 255) : reason);
        attempt.setPaymentMethod("UPI");
        attempt.setStatus("FAILED");
        attempt.setFraudFlagged(false);
        attempt.setSenderDebited(false);
        attempt.setReceiverCredited(false);
        attempt.setSourceIp(truncate(sourceIp, 64));
        attempt.setUserAgent(truncate(userAgent, 512));
        attempt.setCreatedAt(LocalDateTime.now());
        savingsUpiTxnRepo.save(attempt);
    }

    private boolean creditReceiver(String accountType, String accountNumber, String upiId, BigDecimal amount) {
        switch (accountType) {
            case "SAVINGS" -> {
                User receiver = userRepository.findByAccountNumber(accountNumber).orElse(null);
                if (receiver == null || receiver.getAccount() == null) return false;
                Account acc = receiver.getAccount();
                acc.setBalance((acc.getBalance() == null ? 0 : acc.getBalance()) + amount.doubleValue());
                accountRepository.save(acc);
                return true;
            }
            case "SALARY" -> {
                SalaryAccount sa = salaryAccountRepository.findByUpiId(upiId);
                if (sa == null) return false;
                sa.setBalance((sa.getBalance() == null ? 0 : sa.getBalance()) + amount.doubleValue());
                salaryAccountRepository.save(sa);
                return true;
            }
            case "CURRENT" -> {
                CurrentAccount ca = currentAccountRepository.findByUpiId(upiId).orElse(null);
                if (ca == null) return false;
                ca.setBalance((ca.getBalance() == null ? 0 : ca.getBalance()) + amount.doubleValue());
                currentAccountRepository.save(ca);
                return true;
            }
            default -> { return false; }
        }
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    // ── Transaction History ───────────────────────────────────────────────────
    public List<SavingsUpiTransaction> getTransactions(String accountNumber) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null || user.getUpiId() == null) return List.of();
        return savingsUpiTxnRepo.findBySenderUpiIdOrReceiverUpiIdOrderByCreatedAtDesc(
                user.getUpiId(), user.getUpiId());
    }

    // ── UPI Status & Info ─────────────────────────────────────────────────────
    public Map<String, Object> getUpiStatus(String accountNumber) {
        Map<String, Object> res = new HashMap<>();
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) { res.put("found", false); return res; }
        res.put("found", true);
        res.put("upiId", user.getUpiId());
        res.put("upiEnabled", Boolean.TRUE.equals(user.getUpiEnabled()));
        res.put("pinSet", Boolean.TRUE.equals(user.getTransactionPinSet()));
        res.put("accountNumber", user.getAccountNumber());
        if (user.getAccount() != null) {
            res.put("name", user.getAccount().getName());
            res.put("balance", user.getAccount().getBalance());
        }
        return res;
    }

    // ── Admin: all UPI accounts (savings only) ────────────────────────────────
    public List<Map<String, Object>> adminListAllUpiAccounts() {
        List<Map<String, Object>> result = new ArrayList<>();
        userRepository.findAll().forEach(u -> {
            if (u.getUpiId() != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("accountNumber", u.getAccountNumber());
                m.put("upiId", u.getUpiId());
                m.put("upiEnabled", Boolean.TRUE.equals(u.getUpiEnabled()));
                m.put("pinSet", Boolean.TRUE.equals(u.getTransactionPinSet()));
                m.put("status", u.getStatus());
                if (u.getAccount() != null) {
                    m.put("name", u.getAccount().getName());
                    m.put("balance", u.getAccount().getBalance());
                }
                result.add(m);
            }
        });
        return result;
    }

    // ── Admin: block / unblock UPI ────────────────────────────────────────────
    @Transactional
    public Map<String, Object> adminToggleUpi(String accountNumber, boolean enable) {
        Map<String, Object> res = new HashMap<>();
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) { res.put("success", false); res.put("error", "User not found"); return res; }
        user.setUpiEnabled(enable);
        userRepository.save(user);
        res.put("success", true);
        res.put("upiEnabled", enable);
        res.put("message", "UPI " + (enable ? "enabled" : "blocked") + " for " + accountNumber);
        return res;
    }

    // ── Latest transactions after a timestamp (for real-time polling) ──────────
    public List<SavingsUpiTransaction> getLatestTransactions(String accountNumber, LocalDateTime after) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null || user.getUpiId() == null) return List.of();
        return savingsUpiTxnRepo.findRecentAfter(user.getUpiId(), after);
    }

    // ── Admin: all savings UPI transactions ───────────────────────────────────
    public List<Map<String, Object>> adminListAllTransactions() {
        return savingsUpiTxnRepo.findAllOrderByCreatedAtDesc().stream().map(this::toAdminTransaction).toList();
    }

    public List<Map<String, Object>> adminListFlaggedTransactions() {
        return savingsUpiTxnRepo.findFlagged().stream().map(this::toAdminTransaction).toList();
    }

    private Map<String, Object> toAdminTransaction(SavingsUpiTransaction txn) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("id", txn.getId());
        details.put("transactionRef", txn.getTransactionRef());
        details.put("senderAccount", txn.getSenderAccount());
        details.put("senderUpiId", txn.getSenderUpiId());
        details.put("senderName", txn.getSenderName());
        details.put("receiverAccount", txn.getReceiverAccount());
        details.put("receiverUpiId", txn.getReceiverUpiId());
        details.put("receiverName", txn.getReceiverName());
        details.put("amount", txn.getAmount());
        details.put("remark", txn.getRemark());
        details.put("status", txn.getStatus());
        details.put("paymentMethod", txn.getPaymentMethod());
        details.put("fraudFlagged", txn.getFraudFlagged());
        details.put("riskScore", txn.getRiskScore());
        details.put("createdAt", txn.getCreatedAt());
        details.put("senderDebited", txn.getSenderDebited());
        details.put("receiverCredited", txn.getReceiverCredited());
        details.put("sourceIp", txn.getSourceIp());
        details.put("userAgent", txn.getUserAgent());
        details.put("location", formatLocation(txn));
        details.put("locationLatitude", txn.getLocationLatitude());
        details.put("locationLongitude", txn.getLocationLongitude());
        details.put("locationAccuracyMeters", txn.getLocationAccuracyMeters());
        details.put("senderProfile", transactionProfile(txn.getSenderAccount(), txn.getSenderUpiId(),
                txn.getSenderPhone(), txn.getSenderAddress()));
        details.put("receiverProfile", transactionProfile(txn.getReceiverAccount(), txn.getReceiverUpiId(),
                txn.getReceiverPhone(), txn.getReceiverAddress()));
        return details;
    }

    private Map<String, Object> transactionProfile(String accountNumber, String upiId,
                                                  String phone, String address) {
        if (available(phone).equals("Not available") || available(address).equals("Not available")) {
            Map<String, Object> currentProfile = participantProfile(accountNumber, upiId);
            if (available(phone).equals("Not available")) phone = (String) currentProfile.get("phone");
            if (available(address).equals("Not available")) address = (String) currentProfile.get("address");
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("phone", available(phone));
        profile.put("address", available(address));
        return profile;
    }

    private Map<String, Object> participantProfile(String accountNumber, String upiId) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("phone", "Not available");
        profile.put("address", "Not available");

        User user = upiId == null ? null : userRepository.findByUpiId(upiId).orElse(null);
        if (user == null && accountNumber != null) {
            user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        }
        Account savings = user == null ? null : user.getAccount();
        if (savings != null) {
            profile.put("phone", available(savings.getPhone()));
            profile.put("address", joinAddress(savings.getAddress(), savings.getBranchCity()));
            return profile;
        }

        SalaryAccount salary = accountNumber == null ? null : salaryAccountRepository.findByAccountNumber(accountNumber);
        if (salary == null && upiId != null) salary = salaryAccountRepository.findByUpiId(upiId);
        if (salary != null) {
            profile.put("phone", available(salary.getMobileNumber()));
            profile.put("address", available(salary.getAddress()));
            return profile;
        }

        CurrentAccount current = accountNumber == null ? null
                : currentAccountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (current == null && upiId != null) {
            current = currentAccountRepository.findByUpiId(upiId).orElse(null);
        }
        if (current != null) {
            profile.put("phone", available(current.getMobile()));
            profile.put("address", joinAddress(current.getShopAddress(), current.getCity(), current.getState()));
        }
        return profile;
    }

    private boolean isValidLocation(Double latitude, Double longitude, Double accuracy) {
        return latitude != null && longitude != null
                && Double.isFinite(latitude) && latitude >= -90 && latitude <= 90
                && Double.isFinite(longitude) && longitude >= -180 && longitude <= 180
                && (accuracy == null || (Double.isFinite(accuracy) && accuracy >= 0));
    }

    private String formatLocation(SavingsUpiTransaction txn) {
        if (txn.getLocationLatitude() == null || txn.getLocationLongitude() == null) return "Not recorded";
        return String.format(Locale.ROOT, "%.5f, %.5f",
                txn.getLocationLatitude(), txn.getLocationLongitude());
    }

    private String available(String value) {
        return value == null || value.isBlank() ? "Not available" : value;
    }

    private String joinAddress(String... parts) {
        return Arrays.stream(parts).filter(part -> part != null && !part.isBlank())
                .reduce((left, right) -> left + ", " + right).orElse("Not available");
    }
}
