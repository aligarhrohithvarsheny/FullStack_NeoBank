package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AtmUpiWithdrawalService {

    private static final String ATM_UPI_ID = "atmwithdrawal@neobank";
    private static final int MAX_PIN_ATTEMPTS = 3;

    @Autowired private AtmUpiWithdrawalRequestRepository requestRepository;
    @Autowired private AtmMachineRepository atmMachineRepository;
    @Autowired private AtmTransactionRepository atmTransactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SalaryAccountRepository salaryAccountRepository;
    @Autowired private SavingsUpiTransactionRepository savingsUpiTransactionRepository;
    @Autowired private SalaryUpiTransactionRepository salaryUpiTransactionRepository;
    @Autowired private AtmManagementService atmManagementService;

    private final BCryptPasswordEncoder pinEncoder = new BCryptPasswordEncoder(10);

    @Transactional
    public Map<String, Object> createRequest(String atmId, String accountNumber, String accountType, Double amount) {
        if (accountNumber == null || accountNumber.isBlank() || amount == null || amount <= 0
                || amount % 100 != 0 || amount > 25_000) {
            throw new IllegalArgumentException("Enter a withdrawal amount in multiples of ₹100, up to ₹25,000.");
        }

        String normalizedType = normalizeAccountType(accountType);
        AccountIdentity account = loadAccount(accountNumber, normalizedType);
        if (!Boolean.TRUE.equals(account.upiEnabled()) || !Boolean.TRUE.equals(account.pinSet())
                || account.upiId() == null || account.upiId().isBlank()) {
            throw new IllegalArgumentException("Enable UPI and set a UPI PIN before using cardless withdrawal.");
        }

        AtmMachine atm = atmMachineRepository.findByAtmId(atmId)
                .orElseThrow(() -> new IllegalArgumentException("ATM not found."));
        if (!"ACTIVE".equals(atm.getStatus())) {
            throw new IllegalArgumentException("This ATM is currently " + atm.getStatus() + ".");
        }
        if (amount > atm.getMaxWithdrawalLimit()) {
            throw new IllegalArgumentException("Amount exceeds this ATM's per-transaction limit.");
        }
        if (atm.getCashAvailable() == null || atm.getCashAvailable() < amount) {
            throw new IllegalArgumentException("This ATM does not have enough cash for that amount.");
        }

        AtmUpiWithdrawalRequest request = new AtmUpiWithdrawalRequest();
        request.setRequestToken(UUID.randomUUID().toString());
        request.setAtmId(atmId);
        request.setAccountNumber(accountNumber);
        request.setAccountType(normalizedType);
        request.setAmount(amount);
        request.setUserName(account.name());
        request.setUserEmail(account.email());
        request.setStatus("PENDING");
        request.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        requestRepository.save(request);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("requestToken", request.getRequestToken());
        result.put("amount", amount);
        result.put("atmId", atm.getAtmId());
        result.put("atmName", atm.getAtmName());
        result.put("location", atm.getLocation());
        result.put("expiresAt", request.getExpiresAt());
        result.put("qrPayload", "upi://pay?pa=" + ATM_UPI_ID
                + "&pn=NeoBank%20ATM&am=" + BigDecimal.valueOf(amount).setScale(2).toPlainString()
                + "&cu=INR&tn=ATMREQ%3A" + request.getRequestToken());
        return result;
    }

    @Transactional
    public Map<String, Object> getRequest(String token, String accountNumber) {
        AtmUpiWithdrawalRequest request = findRequest(token, accountNumber);
        expireIfNeeded(request);
        return requestResponse(request);
    }

    @Transactional
    public Map<String, Object> cancelRequest(String token, String accountNumber) {
        AtmUpiWithdrawalRequest request = findRequest(token, accountNumber);
        expireIfNeeded(request);
        if ("PENDING".equals(request.getStatus())) {
            request.setStatus("CANCELLED");
            requestRepository.save(request);
        }
        return requestResponse(request);
    }

    @Transactional
    public Map<String, Object> confirmRequest(String token, String accountNumber, String pin) {
        AtmUpiWithdrawalRequest request = requestRepository.findByRequestTokenForUpdate(token)
                .orElseThrow(() -> new IllegalArgumentException("ATM withdrawal request not found."));
        if (!request.getAccountNumber().equals(accountNumber)) {
            throw new IllegalArgumentException("This QR code belongs to a different account.");
        }
        expireIfNeeded(request);
        if (!"PENDING".equals(request.getStatus())) {
            return requestResponse(request);
        }
        int pinLength = "SALARY".equals(request.getAccountType()) ? 4 : 6;
        if (pin == null || !pin.matches("[0-9]{" + pinLength + "}")) {
            return failure("Enter your " + pinLength + "-digit UPI PIN.");
        }

        AccountIdentity account = loadAccount(request.getAccountNumber(), request.getAccountType());
        if (!Boolean.TRUE.equals(account.upiEnabled()) || !Boolean.TRUE.equals(account.pinSet())
                || account.pinHash() == null || !pinEncoder.matches(pin, account.pinHash())) {
            int attempts = (request.getPinAttempts() == null ? 0 : request.getPinAttempts()) + 1;
            request.setPinAttempts(attempts);
            if (attempts >= MAX_PIN_ATTEMPTS) {
                request.setStatus("FAILED");
                request.setFailureReason("Too many incorrect UPI PIN attempts.");
            }
            requestRepository.save(request);
            Map<String, Object> result = requestResponse(request);
            result.put("success", false);
            result.put("message", attempts >= MAX_PIN_ATTEMPTS
                    ? "Too many incorrect PIN attempts. Create a new ATM request."
                    : "Incorrect UPI PIN. " + (MAX_PIN_ATTEMPTS - attempts) + " attempt(s) remaining.");
            return result;
        }

        String upiReference = "UPIATM" + UUID.randomUUID().toString().replace("-", "").substring(0, 20)
                .toUpperCase(Locale.ROOT);
        Map<String, Object> withdrawal = atmManagementService.processWithdrawal(
                request.getAtmId(), request.getAccountNumber(), request.getAccountType(),
                null, request.getUserName(), request.getUserEmail(), request.getAmount(),
                "UPI", upiReference);
        if (!Boolean.TRUE.equals(withdrawal.get("success"))) {
            request.setStatus("FAILED");
            request.setFailureReason(String.valueOf(withdrawal.get("message")));
            requestRepository.save(request);
            Map<String, Object> result = requestResponse(request);
            result.put("success", false);
            result.put("message", request.getFailureReason());
            return result;
        }

        String atmReference = String.valueOf(withdrawal.get("transactionRef"));
        saveUpiTransaction(account, request, upiReference);
        request.setStatus("SUCCESS");
        request.setAtmTransactionRef(atmReference);
        request.setUpiTransactionRef(upiReference);
        requestRepository.save(request);
        Map<String, Object> result = requestResponse(request);
        result.put("success", true);
        result.put("message", "UPI authorization successful. Collect your cash at the ATM.");
        result.put("balanceAfter", withdrawal.get("balanceAfter"));
        return result;
    }

    private void saveUpiTransaction(AccountIdentity account, AtmUpiWithdrawalRequest request, String reference) {
        String remark = "ATM cash withdrawal at " + request.getAtmId();
        if ("SALARY".equals(request.getAccountType())) {
            SalaryUpiTransaction transaction = new SalaryUpiTransaction();
            transaction.setSalaryAccountId(account.id());
            transaction.setAccountNumber(request.getAccountNumber());
            transaction.setUpiId(account.upiId());
            transaction.setRecipientUpi(ATM_UPI_ID);
            transaction.setRecipientName("NeoBank ATM");
            transaction.setType("DEBIT");
            transaction.setAmount(request.getAmount());
            transaction.setRemark(remark);
            transaction.setStatus("SUCCESS");
            transaction.setTransactionRef(reference);
            transaction.setCreatedAt(LocalDateTime.now());
            salaryUpiTransactionRepository.save(transaction);
            return;
        }

        SavingsUpiTransaction transaction = new SavingsUpiTransaction();
        transaction.setTransactionRef(reference);
        transaction.setSenderAccount(request.getAccountNumber());
        transaction.setSenderUpiId(account.upiId());
        transaction.setSenderName(account.name());
        transaction.setReceiverUpiId(ATM_UPI_ID);
        transaction.setReceiverName("NeoBank ATM");
        transaction.setAmount(BigDecimal.valueOf(request.getAmount()));
        transaction.setRemark(remark);
        transaction.setStatus("SUCCESS");
        transaction.setPaymentMethod("ATM_WITHDRAWAL");
        transaction.setSenderDebited(true);
        transaction.setReceiverCredited(false);
        transaction.setCreatedAt(LocalDateTime.now());
        savingsUpiTransactionRepository.save(transaction);
    }

    private AtmUpiWithdrawalRequest findRequest(String token, String accountNumber) {
        AtmUpiWithdrawalRequest request = requestRepository.findByRequestToken(token)
                .orElseThrow(() -> new IllegalArgumentException("ATM withdrawal request not found."));
        if (accountNumber == null || !request.getAccountNumber().equals(accountNumber)) {
            throw new IllegalArgumentException("This QR code belongs to a different account.");
        }
        return request;
    }

    private void expireIfNeeded(AtmUpiWithdrawalRequest request) {
        if ("PENDING".equals(request.getStatus()) && request.getExpiresAt().isBefore(LocalDateTime.now())) {
            request.setStatus("EXPIRED");
            requestRepository.save(request);
        }
    }

    private Map<String, Object> requestResponse(AtmUpiWithdrawalRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", !"FAILED".equals(request.getStatus()) && !"EXPIRED".equals(request.getStatus()));
        result.put("status", request.getStatus());
        result.put("amount", request.getAmount());
        result.put("atmId", request.getAtmId());
        result.put("expiresAt", request.getExpiresAt());
        result.put("transactionRef", request.getAtmTransactionRef());
        result.put("upiTransactionRef", request.getUpiTransactionRef());
        if (request.getAtmTransactionRef() != null) {
            atmTransactionRepository.findByTransactionRef(request.getAtmTransactionRef())
                    .ifPresent(transaction -> result.put("balanceAfter", transaction.getBalanceAfter()));
        }
        if (request.getFailureReason() != null) result.put("message", request.getFailureReason());
        return result;
    }

    private Map<String, Object> failure(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("status", "PENDING");
        result.put("message", message);
        return result;
    }

    private String normalizeAccountType(String accountType) {
        String normalized = accountType == null ? ""
                : accountType.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        if (normalized.endsWith("ACCOUNT")) {
            normalized = normalized.substring(0, normalized.length() - "ACCOUNT".length());
        }
        if (normalized.equals("SAVINGS") || normalized.equals("SAVING")) return "SAVINGS";
        if (normalized.equals("SALARY")) return "SALARY";
        throw new IllegalArgumentException("UPI ATM withdrawal is available for savings and salary accounts.");
    }

    private AccountIdentity loadAccount(String accountNumber, String accountType) {
        if ("SALARY".equals(accountType)) {
            SalaryAccount account = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (account == null || !"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                throw new IllegalArgumentException("Active salary account not found.");
            }
            return new AccountIdentity(account.getId(), account.getUpiId(), account.getEmployeeName(),
                    account.getEmail(), account.getTransactionPin(), account.getTransactionPinSet(),
                    account.getUpiEnabled());
        }
        User user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Savings account not found."));
        if (!"APPROVED".equalsIgnoreCase(user.getStatus()) || user.getAccount() == null) {
            throw new IllegalArgumentException("Active savings account not found.");
        }
        String name = user.getAccount().getName() != null
                ? user.getAccount().getName() : user.getUsername();
        return new AccountIdentity(null, user.getUpiId(), name, user.getEmail(), user.getTransactionPin(),
                user.getTransactionPinSet(), user.getUpiEnabled());
    }

    private record AccountIdentity(Long id, String upiId, String name, String email,
                                   String pinHash, Boolean pinSet, Boolean upiEnabled) { }
}
