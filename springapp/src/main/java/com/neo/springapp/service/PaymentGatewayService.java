package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@SuppressWarnings("null")
public class PaymentGatewayService {

    private final PgMerchantRepository merchantRepository;
    private final PgOrderRepository orderRepository;
    private final PgTransactionRepository transactionRepository;
    private final PgRefundRepository refundRepository;
    private final AccountRepository accountRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PgPaymentSessionRepository paymentSessionRepository;
    private final PgSettlementLedgerRepository settlementLedgerRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final TransactionService transactionService;
    private final PgPaymentLinkRepository paymentLinkRepository;
    private final UserRepository userRepository;

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.02"); // 2%
    private static final BigDecimal GST_RATE = new BigDecimal("0.18"); // 18% on fee
    private static final String HMAC_ALGO = "HmacSHA256";

    public PaymentGatewayService(
            PgMerchantRepository merchantRepository,
            PgOrderRepository orderRepository,
            PgTransactionRepository transactionRepository,
            PgRefundRepository refundRepository,
            AccountRepository accountRepository,
            OtpService otpService,
            EmailService emailService,
            PgPaymentSessionRepository paymentSessionRepository,
            PgSettlementLedgerRepository settlementLedgerRepository,
            CurrentAccountRepository currentAccountRepository,
            SalaryAccountRepository salaryAccountRepository,
            TransactionService transactionService,
            PgPaymentLinkRepository paymentLinkRepository,
            UserRepository userRepository) {
        this.merchantRepository = merchantRepository;
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.refundRepository = refundRepository;
        this.accountRepository = accountRepository;
        this.otpService = otpService;
        this.emailService = emailService;
        this.paymentSessionRepository = paymentSessionRepository;
        this.settlementLedgerRepository = settlementLedgerRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.transactionService = transactionService;
        this.paymentLinkRepository = paymentLinkRepository;
        this.userRepository = userRepository;
    }

    // ==================== MERCHANT OPERATIONS ====================

    public PgMerchant registerMerchant(Map<String, String> request) {
        String email = request.get("businessEmail");
        if (merchantRepository.existsByBusinessEmail(email)) {
            throw new RuntimeException("Merchant with this email already exists");
        }

        PgMerchant merchant = new PgMerchant();
        merchant.setBusinessName(request.get("businessName"));
        merchant.setBusinessEmail(email);
        merchant.setBusinessPhone(request.get("businessPhone"));
        merchant.setBusinessType(request.getOrDefault("businessType", "ONLINE"));
        merchant.setWebhookUrl(request.get("webhookUrl"));
        merchant.setCallbackUrl(request.get("callbackUrl"));
        merchant.setAccountNumber(request.get("accountNumber"));
        merchant.setSettlementAccount(request.get("settlementAccount"));
        merchant.setApiKey(generateApiKey());
        merchant.setSecretKey(generateSecretKey());
        merchant.setLoginEnabled(true);

        return merchantRepository.save(merchant);
    }

    public Optional<PgMerchant> getMerchantById(String merchantId) {
        return merchantRepository.findByMerchantId(merchantId);
    }

    public Optional<PgMerchant> getMerchantByApiKey(String apiKey) {
        return merchantRepository.findByApiKey(apiKey);
    }

    public List<PgMerchant> getAllMerchants() {
        return merchantRepository.findAll();
    }

    // ==================== ORDER OPERATIONS ====================

    @Transactional
    public PgOrder createOrder(Map<String, Object> request) {
        String merchantId = (String) request.get("merchantId");
        PgMerchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        if (!merchant.getIsActive()) {
            throw new RuntimeException("Merchant is not active");
        }

        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        PgOrder order = new PgOrder();
        order.setMerchantId(merchantId);
        order.setAmount(amount);
        order.setCurrency((String) request.getOrDefault("currency", "INR"));
        order.setDescription((String) request.get("description"));
        order.setCustomerEmail((String) request.get("customerEmail"));
        order.setCustomerPhone((String) request.get("customerPhone"));
        order.setCustomerName((String) request.get("customerName"));
        order.setReceipt((String) request.get("receipt"));
        order.setStatus("CREATED");

        return orderRepository.save(order);
    }

    public Optional<PgOrder> getOrderById(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    public List<PgOrder> getOrdersByMerchant(String merchantId) {
        return orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    // ==================== PAYMENT PROCESSING ====================

    @Transactional
    public PgTransaction processPayment(Map<String, String> request) {
        String orderId = request.get("orderId");
        String paymentMethod = request.get("paymentMethod");
        String payerAccount = request.get("payerAccount");
        String payerName = request.get("payerName");

        // 1. Validate order
        PgOrder order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"CREATED".equals(order.getStatus()) && !"ATTEMPTED".equals(order.getStatus())) {
            throw new RuntimeException("Order is not in a payable state. Current status: " + order.getStatus());
        }

        if (order.getExpiresAt() != null && order.getExpiresAt().isBefore(LocalDateTime.now())) {
            order.setStatus("EXPIRED");
            orderRepository.save(order);
            throw new RuntimeException("Order has expired");
        }

        // 2. Validate merchant
        PgMerchant merchant = merchantRepository.findByMerchantId(order.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        // 3. Calculate fees
        BigDecimal amount = order.getAmount();
        BigDecimal fee = amount.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = fee.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(fee).subtract(tax);

        // 4. Fraud check
        int riskScore = calculateRiskScore(payerAccount, amount);
        boolean fraudFlagged = riskScore > 70;

        // 5. Create transaction
        PgTransaction txn = new PgTransaction();
        txn.setOrderId(orderId);
        txn.setMerchantId(order.getMerchantId());
        txn.setPayerAccount(payerAccount);
        txn.setPayerName(payerName);
        txn.setAmount(amount);
        txn.setFee(fee);
        txn.setTax(tax);
        txn.setNetAmount(netAmount);
        txn.setPaymentMethod(paymentMethod);
        txn.setRiskScore(riskScore);
        txn.setFraudFlagged(fraudFlagged);
        txn.setIpAddress(request.get("ipAddress"));
        txn.setDeviceInfo(request.get("deviceInfo"));

        if (fraudFlagged) {
            txn.setStatus("FLAGGED");
            order.setStatus("ATTEMPTED");
            orderRepository.save(order);
            transactionRepository.save(txn);
            throw new RuntimeException("Transaction flagged for review. Risk score: " + riskScore);
        }

        // 6. Validate payer balance (for account-based payments) - supports ALL account types
        if (payerAccount != null && !payerAccount.isEmpty()) {
            Double payerBalance = null;
            String accountType = "UNKNOWN";

            // Check regular (savings) accounts
            Account account = accountRepository.findByAccountNumber(payerAccount);
            if (account != null) {
                payerBalance = account.getBalance();
                accountType = "SAVINGS";
            }

            // Check current accounts
            if (payerBalance == null) {
                Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(payerAccount);
                if (currentOpt.isPresent()) {
                    payerBalance = currentOpt.get().getBalance();
                    accountType = "CURRENT";
                }
            }

            // Check salary accounts
            if (payerBalance == null) {
                SalaryAccount salAcc = salaryAccountRepository.findByAccountNumber(payerAccount);
                if (salAcc != null) {
                    payerBalance = salAcc.getBalance();
                    accountType = "SALARY";
                }
            }

            if (payerBalance == null) {
                txn.setStatus("FAILED");
                txn.setErrorCode("INVALID_ACCOUNT");
                txn.setErrorDescription("Payer account not found");
                transactionRepository.save(txn);
                order.setStatus("ATTEMPTED");
                orderRepository.save(order);
                throw new RuntimeException("Payer account not found");
            }
            if (payerBalance < amount.doubleValue()) {
                txn.setStatus("FAILED");
                txn.setErrorCode("INSUFFICIENT_FUNDS");
                txn.setErrorDescription("Insufficient balance");
                transactionRepository.save(txn);
                order.setStatus("ATTEMPTED");
                orderRepository.save(order);
                throw new RuntimeException("Insufficient balance");
            }

            // 7. Deduct from payer - from the correct account type
            Double newBalance = null;
            String userName = payerName;
            switch (accountType) {
                case "SAVINGS":
                    Account acc = accountRepository.findByAccountNumber(payerAccount);
                    acc.setBalance(acc.getBalance() - amount.doubleValue());
                    accountRepository.save(acc);
                    newBalance = acc.getBalance();
                    if (userName == null) userName = acc.getName();
                    break;
                case "CURRENT":
                    CurrentAccount ca = currentAccountRepository.findByAccountNumber(payerAccount).get();
                    ca.setBalance(ca.getBalance() - amount.doubleValue());
                    currentAccountRepository.save(ca);
                    newBalance = ca.getBalance();
                    if (userName == null) userName = ca.getOwnerName();
                    break;
                case "SALARY":
                    SalaryAccount sa = salaryAccountRepository.findByAccountNumber(payerAccount);
                    sa.setBalance(sa.getBalance() - amount.doubleValue());
                    salaryAccountRepository.save(sa);
                    newBalance = sa.getBalance();
                    if (userName == null) userName = sa.getEmployeeName();
                    break;
            }

            // 7a. Record transaction in user's transaction history
            if (newBalance != null) {
                Transaction userTxn = new Transaction();
                userTxn.setAccountNumber(payerAccount);
                userTxn.setUserName(userName);
                userTxn.setAmount(amount.doubleValue());
                userTxn.setType("Debit");
                userTxn.setDescription("PG Payment - " + paymentMethod + " - Order: " + orderId + " - Merchant: " + order.getMerchantId());
                userTxn.setBalance(newBalance);
                userTxn.setDate(LocalDateTime.now());
                userTxn.setStatus("Completed");
                userTxn.setMerchant(order.getMerchantId());
                transactionService.saveTransaction(userTxn);
            }
        }

        // 8. Generate signature
        String signature = generateSignature(orderId, txn.getTransactionId(), merchant.getSecretKey());
        txn.setSignature(signature);
        txn.setSignatureVerified(true);
        txn.setStatus("SUCCESS");

        PgTransaction savedTxn = transactionRepository.save(txn);

        // 9. Update order status
        order.setStatus("PAID");
        order.setPaymentMethod(paymentMethod);
        orderRepository.save(order);

        // 10. Update merchant volume
        merchant.setTotalVolume(merchant.getTotalVolume().add(amount));
        merchantRepository.save(merchant);

        // 11. Auto-settle: credit merchant account & record transaction in real-time
        String creditAccount = merchant.getLinkedAccountNumber();
        if (creditAccount == null || creditAccount.isEmpty()) {
            creditAccount = merchant.getAccountNumber();
        }
        if (creditAccount != null && !creditAccount.isEmpty()) {
            Double merchantBalanceBefore = null;
            Double merchantBalanceAfter = null;
            String merchantAccName = merchant.getBusinessName();
            boolean credited = false;

            Optional<CurrentAccount> merchantCaOpt = currentAccountRepository.findByAccountNumber(creditAccount);
            if (merchantCaOpt.isPresent()) {
                CurrentAccount mca = merchantCaOpt.get();
                merchantBalanceBefore = mca.getBalance();
                mca.setBalance(mca.getBalance() + netAmount.doubleValue());
                currentAccountRepository.save(mca);
                merchantBalanceAfter = mca.getBalance();
                merchantAccName = mca.getOwnerName();
                credited = true;
            }
            if (!credited) {
                Account merchantAcc = accountRepository.findByAccountNumber(creditAccount);
                if (merchantAcc != null) {
                    merchantBalanceBefore = merchantAcc.getBalance();
                    merchantAcc.setBalance(merchantAcc.getBalance() + netAmount.doubleValue());
                    accountRepository.save(merchantAcc);
                    merchantBalanceAfter = merchantAcc.getBalance();
                    merchantAccName = merchantAcc.getName();
                    credited = true;
                }
            }
            if (!credited) {
                SalaryAccount merchantSa = salaryAccountRepository.findByAccountNumber(creditAccount);
                if (merchantSa != null) {
                    merchantBalanceBefore = merchantSa.getBalance();
                    merchantSa.setBalance(merchantSa.getBalance() + netAmount.doubleValue());
                    salaryAccountRepository.save(merchantSa);
                    merchantBalanceAfter = merchantSa.getBalance();
                    merchantAccName = merchantSa.getEmployeeName();
                    credited = true;
                }
            }

            if (credited) {
                // Record credit transaction in merchant's transaction history
                Transaction merchantTxn = new Transaction();
                merchantTxn.setAccountNumber(creditAccount);
                merchantTxn.setUserName(merchantAccName);
                merchantTxn.setAmount(netAmount.doubleValue());
                merchantTxn.setType("Credit");
                merchantTxn.setDescription("PG Settlement - " + paymentMethod + " - Order: " + orderId);
                merchantTxn.setBalance(merchantBalanceAfter);
                merchantTxn.setDate(LocalDateTime.now());
                merchantTxn.setStatus("Completed");
                merchantTxn.setMerchant(order.getMerchantId());
                transactionService.saveTransaction(merchantTxn);

                // Create settlement ledger entry
                PgSettlementLedger ledger = new PgSettlementLedger();
                ledger.setMerchantId(order.getMerchantId());
                ledger.setTransactionId(savedTxn.getTransactionId());
                ledger.setOrderId(orderId);
                ledger.setGrossAmount(amount);
                ledger.setFeeAmount(fee);
                ledger.setTaxAmount(tax);
                ledger.setNetAmount(netAmount);
                ledger.setCreditAccount(creditAccount);
                ledger.setCreditStatus("CREDITED");
                ledger.setCreditedAt(LocalDateTime.now());
                ledger.setBalanceBefore(BigDecimal.valueOf(merchantBalanceBefore));
                ledger.setBalanceAfter(BigDecimal.valueOf(merchantBalanceAfter));
                ledger.setReferenceNote("PG Auto-Settlement for Order: " + orderId + " | TXN: " + savedTxn.getTransactionId());
                settlementLedgerRepository.save(ledger);

                // Mark PG transaction as settled
                savedTxn.setSettled(true);
                savedTxn.setSettledAt(LocalDateTime.now());
                transactionRepository.save(savedTxn);
            }
        }

        return savedTxn;
    }

    // ==================== REFUND OPERATIONS ====================

    @Transactional
    public PgRefund processRefund(Map<String, Object> request) {
        String transactionId = (String) request.get("transactionId");
        BigDecimal refundAmount = new BigDecimal(request.get("amount").toString());
        String reason = (String) request.get("reason");

        PgTransaction txn = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!"SUCCESS".equals(txn.getStatus())) {
            throw new RuntimeException("Can only refund successful transactions");
        }

        // Check already refunded amount
        BigDecimal alreadyRefunded = refundRepository.getTotalRefundedForTransaction(transactionId);
        BigDecimal maxRefundable = txn.getAmount().subtract(alreadyRefunded);

        if (refundAmount.compareTo(maxRefundable) > 0) {
            throw new RuntimeException("Refund amount exceeds refundable amount. Max: " + maxRefundable);
        }

        String refundType = refundAmount.compareTo(txn.getAmount()) == 0 ? "FULL" : "PARTIAL";

        PgRefund refund = new PgRefund();
        refund.setTransactionId(transactionId);
        refund.setOrderId(txn.getOrderId());
        refund.setMerchantId(txn.getMerchantId());
        refund.setAmount(refundAmount);
        refund.setReason(reason);
        refund.setRefundType(refundType);
        refund.setStatus("PROCESSED");
        refund.setProcessedAt(LocalDateTime.now());

        PgRefund savedRefund = refundRepository.save(refund);

        // Credit back to payer account - supports ALL account types
        Double refundNewBalance = null;
        String refundUserName = txn.getPayerName();
        if (txn.getPayerAccount() != null) {
            boolean credited = false;
            Account account = accountRepository.findByAccountNumber(txn.getPayerAccount());
            if (account != null) {
                account.setBalance(account.getBalance() + refundAmount.doubleValue());
                accountRepository.save(account);
                refundNewBalance = account.getBalance();
                if (refundUserName == null) refundUserName = account.getName();
                credited = true;
            }
            if (!credited) {
                Optional<CurrentAccount> caOpt = currentAccountRepository.findByAccountNumber(txn.getPayerAccount());
                if (caOpt.isPresent()) {
                    CurrentAccount ca = caOpt.get();
                    ca.setBalance(ca.getBalance() + refundAmount.doubleValue());
                    currentAccountRepository.save(ca);
                    refundNewBalance = ca.getBalance();
                    if (refundUserName == null) refundUserName = ca.getOwnerName();
                    credited = true;
                }
            }
            if (!credited) {
                SalaryAccount sa = salaryAccountRepository.findByAccountNumber(txn.getPayerAccount());
                if (sa != null) {
                    sa.setBalance(sa.getBalance() + refundAmount.doubleValue());
                    salaryAccountRepository.save(sa);
                    refundNewBalance = sa.getBalance();
                    if (refundUserName == null) refundUserName = sa.getEmployeeName();
                }
            }
        }

        // Record refund transaction in user's transaction history
        if (refundNewBalance != null && txn.getPayerAccount() != null) {
            Transaction refundTxn = new Transaction();
            refundTxn.setAccountNumber(txn.getPayerAccount());
            refundTxn.setUserName(refundUserName);
            refundTxn.setAmount(refundAmount.doubleValue());
            refundTxn.setType("Credit");
            refundTxn.setDescription("PG Refund - " + refundType + " - Order: " + txn.getOrderId() + " - Reason: " + reason);
            refundTxn.setBalance(refundNewBalance);
            refundTxn.setDate(LocalDateTime.now());
            refundTxn.setStatus("Completed");
            refundTxn.setMerchant(txn.getMerchantId());
            transactionService.saveTransaction(refundTxn);
        }

        // Update transaction refund status
        BigDecimal totalRefunded = alreadyRefunded.add(refundAmount);
        txn.setRefundedAmount(totalRefunded);
        txn.setRefundStatus(totalRefunded.compareTo(txn.getAmount()) >= 0 ? "FULL" : "PARTIAL");
        transactionRepository.save(txn);

        return savedRefund;
    }

    public List<PgRefund> getRefundsByMerchant(String merchantId) {
        return refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public List<PgRefund> getRefundsByTransaction(String transactionId) {
        return refundRepository.findByTransactionId(transactionId);
    }

    // ==================== ANALYTICS ====================

    public Map<String, Object> getMerchantAnalytics(String merchantId) {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalVolume", transactionRepository.getTotalVolumeByMerchantId(merchantId));
        analytics.put("successfulTransactions", transactionRepository.countSuccessfulByMerchantId(merchantId));
        analytics.put("failedTransactions", transactionRepository.countFailedByMerchantId(merchantId));
        analytics.put("totalFees", transactionRepository.getTotalFeesByMerchantId(merchantId));
        analytics.put("recentTransactions", transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId));
        analytics.put("fraudAlerts", transactionRepository.findByFraudFlaggedTrue());
        return analytics;
    }

    public List<PgTransaction> getTransactionsByMerchant(String merchantId) {
        return transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public Optional<PgTransaction> getTransactionById(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    // ==================== SIGNATURE VERIFICATION ====================

    public boolean verifySignature(String orderId, String transactionId, String signature, String secretKey) {
        String expectedSignature = generateSignature(orderId, transactionId, secretKey);
        return expectedSignature.equals(signature);
    }

    private String generateSignature(String orderId, String transactionId, String secretKey) {
        try {
            String data = orderId + "|" + transactionId;
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }

    // ==================== FRAUD DETECTION ====================

    private int calculateRiskScore(String payerAccount, BigDecimal amount) {
        int score = 0;

        // Rule 1: High amount
        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            score += 30;
        }

        // Rule 2: Velocity check (5+ transactions in 1 minute)
        if (payerAccount != null) {
            long recentCount = transactionRepository.countRecentByPayer(
                    payerAccount, LocalDateTime.now().minusMinutes(1));
            if (recentCount >= 5) {
                score += 40;
            } else if (recentCount >= 3) {
                score += 20;
            }

            // Rule 3: Daily volume check
            BigDecimal dailyVolume = transactionRepository.getDailyVolumeByPayer(
                    payerAccount, LocalDateTime.now().minusHours(24));
            if (dailyVolume.compareTo(new BigDecimal("500000")) > 0) {
                score += 25;
            }
        }

        // Rule 4: Suspicious round amounts
        if (amount.remainder(new BigDecimal("10000")).compareTo(BigDecimal.ZERO) == 0
                && amount.compareTo(new BigDecimal("50000")) > 0) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    // ==================== KEY GENERATION ====================

    private String generateApiKey() {
        return "ezpay_key_" + generateRandomHex(24);
    }

    private String generateSecretKey() {
        return "ezpay_secret_" + generateRandomHex(32);
    }

    private String generateRandomHex(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ==================== OTP LOGIN ====================

    public Map<String, Object> sendLoginOtp(String email) {
        Map<String, Object> result = new HashMap<>();
        if (email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Email is required");
            return result;
        }
        String normalizedEmail = email.trim().toLowerCase();
        Optional<PgMerchant> merchantOpt = merchantRepository.findByBusinessEmail(normalizedEmail);
        if (merchantOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "No payment gateway merchant found with this email");
            return result;
        }

        PgMerchant merchant = merchantOpt.get();
        if (!Boolean.TRUE.equals(merchant.getLoginEnabled())) {
            result.put("success", false);
            result.put("message", "Login not enabled. Please wait for admin approval.");
            return result;
        }

        String otp = otpService.generateOtp();
        otpService.storeOtp(normalizedEmail, otp);

        try {
            emailService.sendOtpEmail(normalizedEmail, otp);
        } catch (Exception e) {
            // OTP still stored, log the error
        }

        String maskedEmail = maskEmail(normalizedEmail);
        result.put("success", true);
        result.put("maskedEmail", maskedEmail);
        result.put("businessName", merchant.getBusinessName());
        result.put("merchantId", merchant.getMerchantId());
        result.put("message", "OTP sent successfully");
        return result;
    }

    public Map<String, Object> verifyLoginOtp(String email, String otp) {
        Map<String, Object> result = new HashMap<>();
        if (email == null || otp == null || email.trim().isEmpty() || otp.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Email and OTP are required");
            return result;
        }
        String normalizedEmail = email.trim().toLowerCase();
        boolean verified = otpService.verifyOtp(normalizedEmail, otp.trim());
        if (!verified) {
            result.put("success", false);
            result.put("message", "Invalid or expired OTP");
            return result;
        }

        PgMerchant merchant = merchantRepository.findByBusinessEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        Map<String, Object> merchantData = new HashMap<>();
        merchantData.put("merchantId", merchant.getMerchantId());
        merchantData.put("businessName", merchant.getBusinessName());
        merchantData.put("businessEmail", merchant.getBusinessEmail());
        merchantData.put("businessPhone", merchant.getBusinessPhone());
        merchantData.put("apiKey", merchant.getApiKey());
        merchantData.put("isVerified", merchant.getIsVerified());
        merchantData.put("linkedAccountNumber", merchant.getLinkedAccountNumber());
        merchantData.put("linkedAccountHolderName", merchant.getLinkedAccountHolderName());
        merchantData.put("totalVolume", merchant.getTotalVolume());

        result.put("success", true);
        result.put("merchant", merchantData);
        result.put("message", "Login successful");
        return result;
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    // ==================== UPI QR CODE PAYMENT SESSION ====================

    @Transactional
    public Map<String, Object> createPaymentSession(Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        String merchantId = (String) request.get("merchantId");

        PgOrder order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        PgMerchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        PgPaymentSession session = new PgPaymentSession();
        session.setOrderId(orderId);
        session.setMerchantId(merchantId);
        session.setAmount(order.getAmount());
        session.setCustomerName(order.getCustomerName());
        session.setCustomerEmail(order.getCustomerEmail());
        session.setCustomerPhone(order.getCustomerPhone());
        session.setPaymentMethod((String) request.getOrDefault("paymentMethod", "UPI"));

        // Generate UPI QR data
        String upiId = merchant.getLinkedAccountNumber() != null
                ? merchant.getLinkedAccountNumber() + "@ezyvault"
                : merchant.getMerchantId() + "@ezyvault";
        session.setUpiId(upiId);

        String qrData = "upi://pay?pa=" + upiId
                + "&pn=" + encodeURIComponent(merchant.getBusinessName())
                + "&am=" + order.getAmount()
                + "&cu=INR"
                + "&tn=" + encodeURIComponent("Payment for " + orderId);
        session.setQrData(qrData);
        session.setStatus("PENDING");

        PgPaymentSession saved = paymentSessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("session", saved);
        result.put("qrData", qrData);
        result.put("upiId", upiId);
        result.put("amount", order.getAmount());
        result.put("merchantName", merchant.getBusinessName());
        return result;
    }

    private String encodeURIComponent(String s) {
        if (s == null) return "";
        return s.replaceAll(" ", "%20").replaceAll("&", "%26");
    }

    // ==================== NAME VERIFICATION ====================

    public Map<String, Object> verifyPayerName(String orderId, String payerName, String payerAccount) {
        Map<String, Object> result = new HashMap<>();

        PgOrder order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Get the account holder name - check all account types
        String accountHolderName = null;
        Account accountObj = accountRepository.findByAccountNumber(payerAccount);
        if (accountObj != null) {
            accountHolderName = accountObj.getName();
        } else {
            // Check current accounts
            Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(payerAccount);
            if (currentOpt.isPresent()) {
                accountHolderName = currentOpt.get().getOwnerName();
            } else {
                // Check salary accounts
                SalaryAccount salAcc = salaryAccountRepository.findByAccountNumber(payerAccount);
                if (salAcc != null) {
                    accountHolderName = salAcc.getEmployeeName();
                }
            }
        }

        if (accountHolderName == null) {
            result.put("success", false);
            result.put("verified", false);
            result.put("message", "Account not found");
            return result;
        }

        // Name matching (fuzzy)
        double matchScore = calculateNameMatchScore(payerName.trim().toLowerCase(), accountHolderName.trim().toLowerCase());
        boolean verified = matchScore >= 0.7;

        result.put("success", true);
        result.put("verified", verified);
        result.put("accountHolderName", accountHolderName);
        result.put("matchScore", matchScore);
        result.put("message", verified ? "Name verified successfully" : "Name does not match account holder");
        return result;
    }

    private double calculateNameMatchScore(String name1, String name2) {
        if (name1.equals(name2)) return 1.0;
        // Levenshtein-based similarity
        String[] parts1 = name1.split("\\s+");
        String[] parts2 = name2.split("\\s+");
        int matches = 0;
        for (String p1 : parts1) {
            for (String p2 : parts2) {
                if (p1.equals(p2) || (p1.length() > 2 && p2.contains(p1)) || (p2.length() > 2 && p1.contains(p2))) {
                    matches++;
                    break;
                }
            }
        }
        return Math.max(parts1.length, parts2.length) > 0
                ? (double) matches / Math.max(parts1.length, parts2.length) : 0;
    }

    // ==================== ADMIN PG MANAGEMENT ====================

    public List<PgMerchant> getPendingMerchants() {
        return merchantRepository.findByRegistrationStatus("PENDING");
    }

    public List<PgMerchant> getApprovedMerchants() {
        return merchantRepository.findByAdminApproved(true);
    }

    @Transactional
    public Map<String, Object> adminApproveMerchant(String merchantId, String adminName, Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        PgMerchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        String linkedAccount = request.get("linkedAccountNumber");
        if (linkedAccount != null && !linkedAccount.isEmpty()) {
            // Verify the linked account exists
            boolean accountExists = currentAccountRepository.findByAccountNumber(linkedAccount).isPresent()
                    || accountRepository.findByAccountNumber(linkedAccount) != null;
            if (!accountExists) {
                result.put("success", false);
                result.put("message", "Linked account number not found in the system");
                return result;
            }

            // Get account holder name
            String holderName = "";
            Optional<CurrentAccount> currOpt = currentAccountRepository.findByAccountNumber(linkedAccount);
            if (currOpt.isPresent()) {
                holderName = currOpt.get().getOwnerName();
            } else {
                Account accObj = accountRepository.findByAccountNumber(linkedAccount);
                if (accObj != null) {
                    holderName = accObj.getName();
                }
            }

            merchant.setLinkedAccountNumber(linkedAccount);
            merchant.setLinkedAccountHolderName(holderName);
            merchant.setLinkedAccountVerified(true);
            merchant.setLinkedAccountType(request.getOrDefault("accountType", "CURRENT"));
        }

        merchant.setAdminApproved(true);
        merchant.setLoginEnabled(true);
        merchant.setIsActive(true);
        merchant.setIsVerified(true);
        merchant.setRegistrationStatus("APPROVED");
        merchant.setApprovedBy(adminName);
        merchant.setApprovedAt(LocalDateTime.now());
        merchantRepository.save(merchant);

        result.put("success", true);
        result.put("merchant", merchant);
        result.put("message", "Merchant approved and login enabled");
        return result;
    }

    @Transactional
    public Map<String, Object> adminRejectMerchant(String merchantId, String reason) {
        Map<String, Object> result = new HashMap<>();
        PgMerchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        merchant.setRegistrationStatus("REJECTED");
        merchant.setLoginEnabled(false);
        merchant.setAdminApproved(false);
        merchant.setRejectionReason(reason);
        merchantRepository.save(merchant);

        result.put("success", true);
        result.put("message", "Merchant rejected");
        return result;
    }

    @Transactional
    public Map<String, Object> adminToggleLogin(String merchantId, boolean enable) {
        Map<String, Object> result = new HashMap<>();
        PgMerchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        merchant.setLoginEnabled(enable);
        merchantRepository.save(merchant);

        result.put("success", true);
        result.put("loginEnabled", enable);
        result.put("message", enable ? "Login enabled" : "Login disabled");
        return result;
    }

    // ==================== REAL-TIME CREDIT TO BUSINESS ACCOUNT ====================

    @Transactional
    public Map<String, Object> creditMerchantAccount(String transactionId) {
        Map<String, Object> result = new HashMap<>();

        PgTransaction txn = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!"SUCCESS".equals(txn.getStatus())) {
            result.put("success", false);
            result.put("message", "Only successful transactions can be settled");
            return result;
        }

        if (Boolean.TRUE.equals(txn.getSettled())) {
            result.put("success", false);
            result.put("message", "Transaction already settled");
            return result;
        }

        PgMerchant merchant = merchantRepository.findByMerchantId(txn.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        String creditAccount = merchant.getLinkedAccountNumber();
        if (creditAccount == null || creditAccount.isEmpty()) {
            creditAccount = merchant.getAccountNumber();
        }
        if (creditAccount == null || creditAccount.isEmpty()) {
            result.put("success", false);
            result.put("message", "No linked account found for merchant");
            return result;
        }

        BigDecimal netAmount = txn.getNetAmount();

        // Try current account first, then regular account, then salary account
        Double balanceBefore = null;
        Double balanceAfter = null;
        boolean credited = false;

        Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(creditAccount);
        if (currentOpt.isPresent()) {
            CurrentAccount ca = currentOpt.get();
            balanceBefore = ca.getBalance();
            ca.setBalance(ca.getBalance() + netAmount.doubleValue());
            currentAccountRepository.save(ca);
            balanceAfter = ca.getBalance();
            credited = true;
        } else {
            Account acc = accountRepository.findByAccountNumber(creditAccount);
            if (acc != null) {
                balanceBefore = acc.getBalance();
                acc.setBalance(acc.getBalance() + netAmount.doubleValue());
                accountRepository.save(acc);
                balanceAfter = acc.getBalance();
                credited = true;
            } else {
                SalaryAccount sa = salaryAccountRepository.findByAccountNumber(creditAccount);
                if (sa != null) {
                    balanceBefore = sa.getBalance();
                    sa.setBalance(sa.getBalance() + netAmount.doubleValue());
                    salaryAccountRepository.save(sa);
                    balanceAfter = sa.getBalance();
                    credited = true;
                }
            }
        }

        if (!credited) {
            result.put("success", false);
            result.put("message", "Credit account not found");
            return result;
        }

        // Create settlement ledger entry
        PgSettlementLedger ledger = new PgSettlementLedger();
        ledger.setMerchantId(txn.getMerchantId());
        ledger.setTransactionId(transactionId);
        ledger.setOrderId(txn.getOrderId());
        ledger.setGrossAmount(txn.getAmount());
        ledger.setFeeAmount(txn.getFee());
        ledger.setTaxAmount(txn.getTax());
        ledger.setNetAmount(netAmount);
        ledger.setCreditAccount(creditAccount);
        ledger.setCreditStatus("CREDITED");
        ledger.setCreditedAt(LocalDateTime.now());
        ledger.setBalanceBefore(BigDecimal.valueOf(balanceBefore));
        ledger.setBalanceAfter(BigDecimal.valueOf(balanceAfter));
        ledger.setReferenceNote("PG Settlement for " + txn.getOrderId() + " | TXN: " + transactionId);
        settlementLedgerRepository.save(ledger);

        // Mark transaction as settled
        txn.setSettled(true);
        txn.setSettledAt(LocalDateTime.now());
        transactionRepository.save(txn);

        result.put("success", true);
        result.put("ledger", ledger);
        result.put("balanceBefore", balanceBefore);
        result.put("balanceAfter", balanceAfter);
        result.put("netAmount", netAmount);
        result.put("message", "₹" + netAmount + " credited to " + creditAccount);
        return result;
    }

    // ==================== SETTLEMENT LEDGER ====================

    public List<PgSettlementLedger> getLedgerByMerchant(String merchantId) {
        return settlementLedgerRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public List<PgSettlementLedger> getLedgerByAccount(String accountNumber) {
        return settlementLedgerRepository.findByCreditAccount(accountNumber);
    }

    // ==================== COMPLETE ORDER FROM EXTERNAL QR PAYMENT ====================
    // Called after a salary/business-account UPI payment to an @ezyvault UPI ID.
    // The debit already happened via the UPI payment path; this just records the PG
    // transaction and marks the order PAID — prevents double-debit.
    @Transactional
    public Map<String, Object> completeOrderFromQrPayment(String orderId, Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        PgOrder order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            result.put("success", false);
            result.put("error", "Order not found: " + orderId);
            return result;
        }
        if ("PAID".equals(order.getStatus())) {
            // Already paid — return success so the UI can update gracefully
            result.put("success", true);
            result.put("alreadyPaid", true);
            result.put("order", order);
            return result;
        }
        if ("EXPIRED".equals(order.getStatus())) {
            result.put("success", false);
            result.put("error", "Order has expired");
            return result;
        }

        PgMerchant merchant = merchantRepository.findByMerchantId(order.getMerchantId()).orElse(null);

        String payerAccount = (String) request.getOrDefault("payerAccount", "");
        String payerName    = (String) request.getOrDefault("payerName", "");
        String paymentMethod = (String) request.getOrDefault("paymentMethod", "UPI_QR");
        String txnRef       = (String) request.getOrDefault("txnRef", "");

        BigDecimal amount = order.getAmount();
        BigDecimal fee    = amount.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax    = fee.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(fee).subtract(tax);

        // Create PG transaction record (no balance debit — already done by UPI payment)
        PgTransaction txn = new PgTransaction();
        txn.setOrderId(orderId);
        txn.setMerchantId(order.getMerchantId());
        txn.setPayerAccount(payerAccount);
        txn.setPayerName(payerName);
        txn.setAmount(amount);
        txn.setFee(fee);
        txn.setTax(tax);
        txn.setNetAmount(netAmount);
        txn.setPaymentMethod(paymentMethod);
        txn.setStatus("SUCCESS");
        txn.setRiskScore(0);
        txn.setFraudFlagged(false);
        if (merchant != null) {
            String sig = generateSignature(orderId, txn.getTransactionId(), merchant.getSecretKey());
            txn.setSignature(sig);
            txn.setSignatureVerified(true);
        }
        PgTransaction savedTxn = transactionRepository.save(txn);

        // Mark order PAID
        order.setStatus("PAID");
        order.setPaymentMethod(paymentMethod);
        orderRepository.save(order);

        // Update merchant volume
        if (merchant != null) {
            merchant.setTotalVolume(merchant.getTotalVolume().add(amount));
            merchantRepository.save(merchant);
        }

        // Invalidate QR session so the QR code cannot be scanned again
        paymentSessionRepository.findByOrderId(orderId).ifPresent(session -> {
            session.setStatus("PAID");
            session.setCompletedAt(LocalDateTime.now());
            paymentSessionRepository.save(session);
        });

        result.put("success", true);
        result.put("order", order);
        result.put("transaction", savedTxn);
        result.put("message", "Order marked as PAID");
        return result;
    }

    // ==================== VERIFY LINKED ACCOUNT ====================

    public Map<String, Object> verifyLinkedAccount(String accountNumber) {
        Map<String, Object> result = new HashMap<>();

        // Check current accounts first
        Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(accountNumber);
        if (currentOpt.isPresent()) {
            CurrentAccount ca = currentOpt.get();
            result.put("success", true);
            result.put("verified", true);
            result.put("accountNumber", ca.getAccountNumber());
            result.put("holderName", ca.getOwnerName());
            result.put("businessName", ca.getBusinessName());
            result.put("accountType", "CURRENT");
            result.put("balance", ca.getBalance());
            return result;
        }

        // Check regular accounts
        Account acc = accountRepository.findByAccountNumber(accountNumber);
        if (acc != null) {
            result.put("success", true);
            result.put("verified", true);
            result.put("accountNumber", acc.getAccountNumber());
            result.put("holderName", acc.getName());
            result.put("accountType", "SAVINGS");
            result.put("balance", acc.getBalance());
            return result;
        }

        // Check salary accounts
        SalaryAccount salAcc = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (salAcc != null) {
            result.put("success", true);
            result.put("verified", true);
            result.put("accountNumber", salAcc.getAccountNumber());
            result.put("holderName", salAcc.getEmployeeName());
            result.put("accountType", "SALARY");
            result.put("balance", salAcc.getBalance());
            return result;
        }

        result.put("success", false);
        result.put("verified", false);
        result.put("message", "Account not found");
        return result;
    }

    // ==================== CARD PAYMENT WITH OTP ====================

    @Transactional
    public Map<String, Object> verifyCardAndSendOtp(Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        String cardNumber = request.get("cardNumber");
        String cardExpiry = request.get("cardExpiry");
        String cardCvv = request.get("cardCvv");
        String cardHolderName = request.get("cardHolderName");
        String cardType = request.getOrDefault("cardType", "DEBIT");
        String orderId = request.get("orderId");
        String merchantId = request.get("merchantId");

        if (cardNumber == null || cardNumber.length() < 16) {
            result.put("success", false);
            result.put("message", "Invalid card number");
            return result;
        }

        // Validate order exists
        PgOrder order = orderRepository.findByOrderId(orderId)
                .orElse(null);
        if (order == null) {
            result.put("success", false);
            result.put("message", "Order not found");
            return result;
        }

        // Try to find the card across all account types
        String accountNumber = null;
        String email = null;
        String holderName = null;

        // 1. Check Salary Accounts (they have card numbers)
        SalaryAccount salAcc = salaryAccountRepository.findByDebitCardNumber(cardNumber);
        if (salAcc != null) {
            // Verify CVV and expiry
            if (salAcc.getDebitCardCvv() != null && !salAcc.getDebitCardCvv().equals(cardCvv)) {
                result.put("success", false);
                result.put("message", "Invalid CVV");
                return result;
            }
            if (salAcc.getDebitCardExpiry() != null && !salAcc.getDebitCardExpiry().equals(cardExpiry)) {
                result.put("success", false);
                result.put("message", "Invalid card expiry");
                return result;
            }
            accountNumber = salAcc.getAccountNumber();
            email = salAcc.getEmail();
            holderName = salAcc.getEmployeeName();
        }

        // 2. If not found in salary, search by card holder name across all accounts
        if (accountNumber == null) {
            // Try to find by matching card holder name in regular accounts
            Account acc = accountRepository.findByName(cardHolderName);
            if (acc != null) {
                accountNumber = acc.getAccountNumber();
                email = acc.getPhone() + "@notify"; // fallback - use phone as identifier
                holderName = acc.getName();
            }
        }

        // 3. Check current accounts by owner name
        if (accountNumber == null) {
            List<CurrentAccount> currentAccts = currentAccountRepository.findByOwnerName(cardHolderName);
            if (currentAccts != null && !currentAccts.isEmpty()) {
                CurrentAccount ca = currentAccts.get(0);
                accountNumber = ca.getAccountNumber();
                email = ca.getEmail();
                holderName = ca.getOwnerName();
            }
        }

        if (accountNumber == null) {
            result.put("success", false);
            result.put("message", "Card not linked to any account. Please ensure cardholder name matches your account.");
            return result;
        }

        // Generate and send OTP
        String otpKey = "card_" + orderId + "_" + cardNumber.substring(cardNumber.length() - 4);
        String otp = otpService.generateOtp();
        otpService.storeOtp(otpKey, otp);

        // Also store the account mapping for verification step
        otpService.storeOtp("card_acc_" + orderId, accountNumber);

        if (email != null && !email.contains("@notify")) {
            try {
                emailService.sendOtpEmail(email, otp);
            } catch (Exception e) {
                // OTP still stored
            }
        }

        String maskedEmail = email != null && !email.contains("@notify") ? maskEmail(email) : "registered contact";

        result.put("success", true);
        result.put("maskedEmail", maskedEmail);
        result.put("accountNumber", accountNumber);
        result.put("holderName", holderName);
        result.put("last4", cardNumber.substring(cardNumber.length() - 4));
        result.put("cardType", cardType);
        result.put("message", "OTP sent for card verification");
        return result;
    }

    @Transactional
    public Map<String, Object> verifyCardOtpAndProcessPayment(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String otp = (String) request.get("otp");
        String orderId = (String) request.get("orderId");
        String cardNumber = (String) request.get("cardNumber");
        String cardHolderName = (String) request.get("cardHolderName");
        String cardType = (String) request.getOrDefault("cardType", "DEBIT");
        String payerAccount = (String) request.get("payerAccount");
        String merchantId = (String) request.get("merchantId");

        // Verify OTP
        String otpKey = "card_" + orderId + "_" + cardNumber.substring(cardNumber.length() - 4);
        boolean verified = otpService.verifyOtp(otpKey, otp);
        if (!verified) {
            result.put("success", false);
            result.put("message", "Invalid or expired OTP");
            return result;
        }

        // Process as regular payment with CARD method
        Map<String, String> payRequest = new HashMap<>();
        payRequest.put("orderId", orderId);
        payRequest.put("paymentMethod", cardType.equals("CREDIT") ? "CREDIT_CARD" : "DEBIT_CARD");
        payRequest.put("payerAccount", payerAccount);
        payRequest.put("payerName", cardHolderName);

        try {
            PgTransaction txn = processPayment(payRequest);
            result.put("success", true);
            result.put("transaction", txn);
            result.put("message", "Card payment processed successfully");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    // ==================== PAYMENT LINKS ====================

    /**
     * Verify any UPI ID across CurrentAccount, SalaryAccount, and Savings (User) tables.
     */
    public Map<String, Object> verifyAnyUpiId(String upiId) {
        Map<String, Object> result = new HashMap<>();
        if (upiId == null || upiId.trim().isEmpty()) {
            result.put("verified", false);
            result.put("error", "UPI ID cannot be empty");
            return result;
        }
        String id = upiId.trim().toLowerCase();

        // Check savings accounts (User table)
        Optional<com.neo.springapp.model.User> userOpt = userRepository.findByUpiId(id);
        if (userOpt.isPresent()) {
            com.neo.springapp.model.User u = userOpt.get();
            if (!Boolean.TRUE.equals(u.getUpiEnabled())) {
                result.put("verified", false);
                result.put("error", "UPI is disabled for this account");
                return result;
            }
            String name = (u.getAccount() != null && u.getAccount().getName() != null)
                    ? u.getAccount().getName() : u.getUsername();
            result.put("verified", true);
            result.put("valid", true);
            result.put("name", name);
            result.put("accountHolderName", name);
            result.put("accountNumber", u.getAccountNumber());
            result.put("accountType", "SAVINGS");
            return result;
        }

        // Check current accounts
        Optional<CurrentAccount> caOpt = currentAccountRepository.findByUpiId(id);
        if (caOpt.isPresent()) {
            CurrentAccount ca = caOpt.get();
            if (!"ACTIVE".equals(ca.getStatus()) || !Boolean.TRUE.equals(ca.getUpiEnabled())) {
                result.put("verified", false);
                result.put("error", "UPI not active for this account");
                return result;
            }
            String name = ca.getBusinessName() != null ? ca.getBusinessName() : ca.getOwnerName();
            result.put("verified", true);
            result.put("name", name);
            result.put("accountNumber", ca.getAccountNumber());
            result.put("accountType", "CURRENT");
            return result;
        }

        // Check salary accounts
        SalaryAccount sa = salaryAccountRepository.findByUpiId(id);
        if (sa != null) {
            if (!Boolean.TRUE.equals(sa.getUpiEnabled())) {
                result.put("verified", false);
                result.put("error", "UPI not enabled for this account");
                return result;
            }
            result.put("verified", true);
            result.put("name", sa.getEmployeeName());
            result.put("accountNumber", sa.getAccountNumber());
            result.put("accountType", "SALARY");
            return result;
        }

        result.put("verified", false);
        result.put("error", "No account found with this UPI ID");
        return result;
    }

    @Transactional
    public Map<String, Object> sendPaymentLink(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String merchantId   = (String) request.get("merchantId");
        String recipientUpi = (String) request.get("recipientUpiId");
        String description  = (String) request.getOrDefault("description", "Payment Request");
        BigDecimal amount   = new BigDecimal(request.get("amount").toString());

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            result.put("success", false);
            result.put("error", "Amount must be greater than zero");
            return result;
        }

        PgMerchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        // Verify recipient UPI ID
        Map<String, Object> upiVerify = verifyAnyUpiId(recipientUpi);
        if (!Boolean.TRUE.equals(upiVerify.get("verified"))) {
            result.put("success", false);
            result.put("error", "Invalid recipient UPI ID: " + upiVerify.get("error"));
            return result;
        }

        String recipientName = (String) upiVerify.get("name");

        // Build unique link token
        String token = generateRandomHex(16);

        PgPaymentLink link = new PgPaymentLink();
        link.setMerchantId(merchantId);
        link.setMerchantName(merchant.getBusinessName());
        link.setMerchantUpiId(merchant.getLinkedAccountNumber() + "@ezyvault");
        link.setRecipientUpiId(recipientUpi.trim().toLowerCase());
        link.setRecipientName(recipientName);
        link.setAmount(amount);
        link.setDescription(description);
        link.setLinkToken(token);
        link.setStatus("PENDING");
        PgPaymentLink saved = paymentLinkRepository.save(link);

        result.put("success", true);
        result.put("link", saved);
        result.put("linkToken", token);
        result.put("recipientName", recipientName);
        result.put("message", "Payment link sent to " + recipientName);
        return result;
    }

    public List<PgPaymentLink> getMerchantPaymentLinks(String merchantId) {
        return paymentLinkRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public List<PgPaymentLink> getCustomerPaymentLinks(String upiId) {
        return paymentLinkRepository.findByRecipientUpiIdOrderByCreatedAtDesc(upiId.trim().toLowerCase());
    }

    public List<PgPaymentLink> getPendingCustomerPaymentLinks(String upiId) {
        return paymentLinkRepository.findByRecipientUpiIdAndStatusOrderByCreatedAtDesc(
                upiId.trim().toLowerCase(), "PENDING");
    }

    @Transactional
    public Map<String, Object> payPaymentLink(String linkToken, Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        PgPaymentLink link = paymentLinkRepository.findByLinkToken(linkToken)
                .orElseThrow(() -> new RuntimeException("Payment link not found"));

        if ("PAID".equals(link.getStatus())) {
            result.put("success", false);
            result.put("error", "This payment link has already been paid");
            return result;
        }
        if ("CANCELLED".equals(link.getStatus())) {
            result.put("success", false);
            result.put("error", "This payment link has been cancelled");
            return result;
        }
        if ("EXPIRED".equals(link.getStatus()) ||
                (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now()))) {
            link.setStatus("EXPIRED");
            paymentLinkRepository.save(link);
            result.put("success", false);
            result.put("error", "This payment link has expired");
            return result;
        }

        String payerAccountNumber = (String) request.get("payerAccountNumber");
        String transactionPin     = (String) request.get("transactionPin");
        BigDecimal amount         = link.getAmount();

        if (payerAccountNumber == null || payerAccountNumber.isBlank()) {
            result.put("success", false);
            result.put("error", "Account number is required");
            return result;
        }

        // Locate payer account and verify PIN
        Double payerBalance = null;
        String payerName    = null;
        String accountType  = null;
        String payerEmail   = null;
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        // Check savings account (User)
        com.neo.springapp.model.User savingsUser = userRepository.findByAccountNumber(payerAccountNumber).orElse(null);
        if (savingsUser != null) {
            if (transactionPin == null || transactionPin.isBlank()) {
                result.put("success", false);
                result.put("error", "UPI PIN is required");
                return result;
            }
            if (!Boolean.TRUE.equals(savingsUser.getTransactionPinSet()) || savingsUser.getTransactionPin() == null) {
                result.put("success", false);
                result.put("error", "UPI PIN not set. Please set your UPI PIN in your dashboard first.");
                return result;
            }
            if (!encoder.matches(transactionPin, savingsUser.getTransactionPin())) {
                result.put("success", false);
                result.put("error", "Invalid UPI PIN");
                return result;
            }
            com.neo.springapp.model.Account savingsAcc = savingsUser.getAccount();
            if (savingsAcc == null) {
                result.put("success", false);
                result.put("error", "Savings account not linked");
                return result;
            }
            payerBalance = savingsAcc.getBalance();
            payerName    = savingsAcc.getName();
            accountType  = "SAVINGS";
            payerEmail   = savingsUser.getEmail();
        }

        // Check salary account
        SalaryAccount salAcc = accountType == null ? salaryAccountRepository.findByAccountNumber(payerAccountNumber) : null;
        if (salAcc != null) {
            if (transactionPin == null || transactionPin.isBlank()) {
                result.put("success", false);
                result.put("error", "UPI PIN is required");
                return result;
            }
            if (!Boolean.TRUE.equals(salAcc.getTransactionPinSet()) || salAcc.getTransactionPin() == null) {
                result.put("success", false);
                result.put("error", "UPI PIN not set. Please set your 4-digit UPI PIN first.");
                return result;
            }
            if (!encoder.matches(transactionPin, salAcc.getTransactionPin())) {
                result.put("success", false);
                result.put("error", "Invalid UPI PIN");
                return result;
            }
            payerBalance = salAcc.getBalance();
            payerName    = salAcc.getEmployeeName();
            accountType  = "SALARY";
            payerEmail   = salAcc.getEmail();
        }

        // Check current account
        if (accountType == null) {
            Optional<CurrentAccount> caOpt = currentAccountRepository.findByAccountNumber(payerAccountNumber);
            if (caOpt.isPresent()) {
                CurrentAccount ca = caOpt.get();
                if (transactionPin == null || transactionPin.isBlank()) {
                    result.put("success", false);
                    result.put("error", "UPI PIN is required");
                    return result;
                }
                if (!Boolean.TRUE.equals(ca.getPasswordSet()) || ca.getPassword() == null) {
                    result.put("success", false);
                    result.put("error", "UPI PIN not set. Please set your 4-digit UPI PIN first.");
                    return result;
                }
                if (!encoder.matches(transactionPin, ca.getPassword())) {
                    result.put("success", false);
                    result.put("error", "Invalid UPI PIN");
                    return result;
                }
                payerBalance = ca.getBalance();
                payerName    = ca.getOwnerName();
                accountType  = "CURRENT";
                payerEmail   = ca.getEmail();
            }
        }

        if (accountType == null) {
            result.put("success", false);
            result.put("error", "Payer account not found");
            return result;
        }

        if (payerBalance == null || payerBalance < amount.doubleValue()) {
            result.put("success", false);
            result.put("error", "Insufficient balance");
            return result;
        }

        // Deduct balance
        Double newBalance = null;
        if ("SAVINGS".equals(accountType)) {
            com.neo.springapp.model.Account savingsAcc = savingsUser.getAccount();
            savingsAcc.setBalance(savingsAcc.getBalance() - amount.doubleValue());
            accountRepository.save(savingsAcc);
            newBalance = savingsAcc.getBalance();
        } else if ("SALARY".equals(accountType)) {
            salAcc.setBalance(salAcc.getBalance() - amount.doubleValue());
            salaryAccountRepository.save(salAcc);
            newBalance = salAcc.getBalance();
        } else {
            CurrentAccount ca = currentAccountRepository.findByAccountNumber(payerAccountNumber).get();
            ca.setBalance(ca.getBalance() - amount.doubleValue());
            currentAccountRepository.save(ca);
            newBalance = ca.getBalance();
        }

        // Record in user transaction history
        String txnRef = "PLK" + System.currentTimeMillis();
        Transaction userTxn = new Transaction();
        userTxn.setAccountNumber(payerAccountNumber);
        userTxn.setUserName(payerName);
        userTxn.setAmount(amount.doubleValue());
        userTxn.setType("Debit");
        userTxn.setDescription("Payment Link - " + link.getMerchantName() + " - " + link.getDescription());
        userTxn.setBalance(newBalance);
        userTxn.setDate(LocalDateTime.now());
        userTxn.setStatus("Completed");
        userTxn.setMerchant(link.getMerchantId());
        transactionService.saveTransaction(userTxn);

        // Create PG transaction
        BigDecimal fee = amount.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = fee.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(fee).subtract(tax);

        PgTransaction pgTxn = new PgTransaction();
        pgTxn.setOrderId(link.getOrderId() != null ? link.getOrderId() : link.getLinkId());
        pgTxn.setMerchantId(link.getMerchantId());
        pgTxn.setPayerAccount(payerAccountNumber);
        pgTxn.setPayerName(payerName);
        pgTxn.setAmount(amount);
        pgTxn.setFee(fee);
        pgTxn.setTax(tax);
        pgTxn.setNetAmount(netAmount);
        pgTxn.setPaymentMethod("UPI_LINK");
        pgTxn.setStatus("SUCCESS");
        pgTxn.setRiskScore(0);
        pgTxn.setFraudFlagged(false);
        PgTransaction savedTxn = transactionRepository.save(pgTxn);

        // Update merchant volume & credit merchant settlement account
        PgMerchant merchant = merchantRepository.findByMerchantId(link.getMerchantId()).orElse(null);
        if (merchant != null) {
            merchant.setTotalVolume(merchant.getTotalVolume().add(amount));
            merchantRepository.save(merchant);

            // Credit merchant's linked account with net amount
            String creditAccount = merchant.getLinkedAccountNumber();
            if (creditAccount == null || creditAccount.isEmpty()) {
                creditAccount = merchant.getAccountNumber();
            }
            if (creditAccount != null && !creditAccount.isEmpty()) {
                Double merchantBalanceBefore = null;
                Double merchantBalanceAfter = null;
                String merchantAccName = merchant.getBusinessName();
                boolean credited = false;

                Optional<CurrentAccount> merchantCaOpt = currentAccountRepository.findByAccountNumber(creditAccount);
                if (merchantCaOpt.isPresent()) {
                    CurrentAccount mca = merchantCaOpt.get();
                    merchantBalanceBefore = mca.getBalance();
                    mca.setBalance(mca.getBalance() + netAmount.doubleValue());
                    currentAccountRepository.save(mca);
                    merchantBalanceAfter = mca.getBalance();
                    merchantAccName = mca.getOwnerName();
                    credited = true;
                }
                if (!credited) {
                    Account merchantAcc = accountRepository.findByAccountNumber(creditAccount);
                    if (merchantAcc != null) {
                        merchantBalanceBefore = merchantAcc.getBalance();
                        merchantAcc.setBalance(merchantAcc.getBalance() + netAmount.doubleValue());
                        accountRepository.save(merchantAcc);
                        merchantBalanceAfter = merchantAcc.getBalance();
                        merchantAccName = merchantAcc.getName();
                        credited = true;
                    }
                }
                if (!credited) {
                    SalaryAccount merchantSa = salaryAccountRepository.findByAccountNumber(creditAccount);
                    if (merchantSa != null) {
                        merchantBalanceBefore = merchantSa.getBalance();
                        merchantSa.setBalance(merchantSa.getBalance() + netAmount.doubleValue());
                        salaryAccountRepository.save(merchantSa);
                        merchantBalanceAfter = merchantSa.getBalance();
                        merchantAccName = merchantSa.getEmployeeName();
                        credited = true;
                    }
                }

                if (credited) {
                    // Record credit transaction in merchant's transaction history
                    Transaction merchantTxn = new Transaction();
                    merchantTxn.setAccountNumber(creditAccount);
                    merchantTxn.setUserName(merchantAccName);
                    merchantTxn.setAmount(netAmount.doubleValue());
                    merchantTxn.setType("Credit");
                    merchantTxn.setDescription("PG Settlement - Payment from " + payerName + " - " + link.getDescription());
                    merchantTxn.setBalance(merchantBalanceAfter);
                    merchantTxn.setDate(LocalDateTime.now());
                    merchantTxn.setStatus("Completed");
                    merchantTxn.setMerchant(link.getMerchantId());
                    transactionService.saveTransaction(merchantTxn);

                    // Create settlement ledger entry
                    PgSettlementLedger ledger = new PgSettlementLedger();
                    ledger.setMerchantId(link.getMerchantId());
                    ledger.setTransactionId(savedTxn.getTransactionId());
                    ledger.setOrderId(link.getOrderId() != null ? link.getOrderId() : link.getLinkId());
                    ledger.setGrossAmount(amount);
                    ledger.setFeeAmount(fee);
                    ledger.setTaxAmount(tax);
                    ledger.setNetAmount(netAmount);
                    ledger.setCreditAccount(creditAccount);
                    ledger.setCreditStatus("CREDITED");
                    ledger.setCreditedAt(LocalDateTime.now());
                    ledger.setBalanceBefore(BigDecimal.valueOf(merchantBalanceBefore));
                    ledger.setBalanceAfter(BigDecimal.valueOf(merchantBalanceAfter));
                    ledger.setReferenceNote("PG Settlement for " + link.getDescription() + " | TXN: " + savedTxn.getTransactionId());
                    settlementLedgerRepository.save(ledger);

                    // Mark PG transaction as settled
                    savedTxn.setSettled(true);
                    savedTxn.setSettledAt(LocalDateTime.now());
                    transactionRepository.save(savedTxn);
                }
            }
        }

        // Mark link as PAID
        link.setStatus("PAID");
        link.setPaidAt(LocalDateTime.now());
        link.setPayerAccountNumber(payerAccountNumber);
        link.setPayerName(payerName);
        link.setTxnRef(txnRef);
        paymentLinkRepository.save(link);

        // If this payment link is tied to a PG order, mark the order as PAID
        if (link.getOrderId() != null && !link.getOrderId().isEmpty()) {
            orderRepository.findByOrderId(link.getOrderId()).ifPresent(order -> {
                order.setStatus("PAID");
                order.setPaymentMethod("UPI_LINK");
                orderRepository.save(order);

                // Invalidate any pending QR sessions for this order
                paymentSessionRepository.findByOrderId(link.getOrderId()).ifPresent(session -> {
                    session.setStatus("PAID");
                    session.setCompletedAt(LocalDateTime.now());
                    paymentSessionRepository.save(session);
                });
            });
        }

        result.put("success", true);
        result.put("txnRef", txnRef);
        result.put("txnId", savedTxn.getTransactionId());
        result.put("amount", amount);
        result.put("newBalance", newBalance);
        result.put("merchantName", link.getMerchantName());
        result.put("message", "Payment successful");

        // Invoice data
        Map<String, Object> invoice = new HashMap<>();
        invoice.put("invoiceNumber", "INV-" + savedTxn.getTransactionId());
        invoice.put("transactionId", savedTxn.getTransactionId());
        invoice.put("txnRef", txnRef);
        invoice.put("date", LocalDateTime.now().toString());
        invoice.put("paymentMethod", "UPI_LINK");

        // Merchant details
        invoice.put("merchantName", link.getMerchantName());
        invoice.put("merchantUpiId", link.getMerchantUpiId());
        invoice.put("merchantId", link.getMerchantId());
        if (merchant != null) {
            invoice.put("businessEmail", merchant.getBusinessEmail());
            invoice.put("businessPhone", merchant.getBusinessPhone());
            invoice.put("businessType", merchant.getBusinessType());
        }

        // Payer details
        invoice.put("payerName", payerName);
        invoice.put("payerAccount", payerAccountNumber);
        invoice.put("payerEmail", payerEmail);
        invoice.put("accountType", accountType);

        // Item & amount breakdown
        invoice.put("description", link.getDescription());
        invoice.put("grossAmount", amount);
        invoice.put("platformFee", fee);
        invoice.put("gst", tax);
        invoice.put("netAmount", netAmount);
        invoice.put("currency", "INR");
        invoice.put("status", "PAID");

        result.put("invoice", invoice);

        return result;
    }

    @Transactional
    public Map<String, Object> cancelPaymentLink(String linkToken) {
        Map<String, Object> result = new HashMap<>();
        PgPaymentLink link = paymentLinkRepository.findByLinkToken(linkToken).orElse(null);
        if (link == null) {
            result.put("success", false);
            result.put("error", "Link not found");
            return result;
        }
        if ("PAID".equals(link.getStatus())) {
            result.put("success", false);
            result.put("error", "Cannot cancel a paid link");
            return result;
        }
        link.setStatus("CANCELLED");
        paymentLinkRepository.save(link);
        result.put("success", true);
        result.put("message", "Payment link cancelled");
        return result;
    }

    // ==================== DELETE ORDER (CREATED ONLY) ====================

    @Transactional
    public Map<String, Object> deleteOrder(String orderId) {
        Map<String, Object> result = new HashMap<>();
        PgOrder order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            result.put("success", false);
            result.put("error", "Order not found");
            return result;
        }
        if (!"CREATED".equals(order.getStatus())) {
            result.put("success", false);
            result.put("error", "Only CREATED orders can be deleted. Current status: " + order.getStatus());
            return result;
        }
        // Invalidate any associated QR sessions
        paymentSessionRepository.findByOrderId(orderId).ifPresent(session -> {
            session.setStatus("INVALID");
            paymentSessionRepository.save(session);
        });
        orderRepository.delete(order);
        result.put("success", true);
        result.put("message", "Order deleted successfully");
        return result;
    }

    // ==================== PAY ORDER WITH UPI ID (sends payment request) ====================

    @Transactional
    public Map<String, Object> payOrderWithUpiId(String orderId, Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        PgOrder order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            result.put("success", false);
            result.put("error", "Order not found");
            return result;
        }
        if (!"CREATED".equals(order.getStatus()) && !"ATTEMPTED".equals(order.getStatus())) {
            result.put("success", false);
            result.put("error", "Order is not payable. Status: " + order.getStatus());
            return result;
        }
        if (order.getExpiresAt() != null && order.getExpiresAt().isBefore(LocalDateTime.now())) {
            order.setStatus("EXPIRED");
            orderRepository.save(order);
            result.put("success", false);
            result.put("error", "Order has expired");
            return result;
        }

        String payerUpiId = (String) request.get("upiId");
        if (payerUpiId == null || payerUpiId.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "UPI ID is required");
            return result;
        }
        String upiId = payerUpiId.trim().toLowerCase();

        // Verify recipient UPI ID exists
        Map<String, Object> upiVerify = verifyAnyUpiId(upiId);
        if (!Boolean.TRUE.equals(upiVerify.get("verified"))) {
            result.put("success", false);
            result.put("error", "UPI ID not found: " + upiId);
            return result;
        }
        String recipientName = (String) upiVerify.get("name");

        // Get merchant info
        PgMerchant merchant = merchantRepository.findByMerchantId(order.getMerchantId()).orElse(null);
        String merchantName = merchant != null ? merchant.getBusinessName() : order.getMerchantId();

        // Create a payment link (request) tied to this order
        String token = generateRandomHex(16);
        PgPaymentLink link = new PgPaymentLink();
        link.setMerchantId(order.getMerchantId());
        link.setMerchantName(merchantName);
        link.setRecipientUpiId(upiId);
        link.setRecipientName(recipientName);
        link.setAmount(order.getAmount());
        link.setDescription("Payment for Order: " + orderId);
        link.setLinkToken(token);
        link.setStatus("PENDING");
        link.setOrderId(orderId);
        PgPaymentLink saved = paymentLinkRepository.save(link);

        // Mark order as ATTEMPTED (waiting for user to pay)
        order.setStatus("ATTEMPTED");
        orderRepository.save(order);

        result.put("success", true);
        result.put("linkSent", true);
        result.put("link", saved);
        result.put("recipientName", recipientName);
        result.put("recipientUpiId", upiId);
        result.put("message", "Payment request of ₹" + order.getAmount() + " sent to " + recipientName + " (" + upiId + "). They will authorize from their dashboard.");
        return result;
    }
}
