package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class Card360Service {
    private final Card360AccessRepository accessRepository;
    private final Card360AuditRepository auditRepository;
    private final CardRepository cardRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardTransactionRepository creditTransactionRepository;
    private final UserRepository userRepository;
    private final AdminService adminService;
    private final UserSessionTokenService tokenService;
    private final CreditCardService creditCardService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    private final SecureRandom random = new SecureRandom();

    public Card360Service(Card360AccessRepository accessRepository, Card360AuditRepository auditRepository,
                          CardRepository cardRepository, CreditCardRepository creditCardRepository,
                          CreditCardTransactionRepository creditTransactionRepository,
                          UserRepository userRepository,
                          AdminService adminService, UserSessionTokenService tokenService,
                          CreditCardService creditCardService) {
        this.accessRepository = accessRepository;
        this.auditRepository = auditRepository;
        this.cardRepository = cardRepository;
        this.creditCardRepository = creditCardRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.userRepository = userRepository;
        this.adminService = adminService;
        this.tokenService = tokenService;
        this.creditCardService = creditCardService;
    }

    private boolean customerCanUnblock(String accountNumber, String type, long id) {
        return auditRepository.findFirstByAccountNumberAndCardTypeAndCardIdOrderByCreatedAtDesc(
                        accountNumber, type.toLowerCase(Locale.ROOT), id)
                .filter(audit -> "CARD_BLOCKED".equals(audit.getAction())
                        && ("CUSTOMER:" + accountNumber).equals(audit.getChangedBy()))
                .isPresent();
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public Map<String, Object> generatePasscode(String accountNumber, String adminEmail, String adminPassword) {
        Admin admin = requireAdmin(adminEmail, adminPassword);
        User user = userRepository.findByAccountNumber(accountNumber)
                .filter(value -> "APPROVED".equalsIgnoreCase(value.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Approved customer account not found"));

        Card360Access access = accessRepository.findByAccountNumber(accountNumber).orElseGet(Card360Access::new);
        access.setAccountNumber(accountNumber);
        String passcode = generatePasscodeValue();
        access.setPasscodeHash(encoder.encode(passcode));
        access.setEnabled(true);
        access.setFailedAttempts(0);
        access.setLockedUntil(null);
        access.setUpdatedBy(admin.getEmail());
        access.setUpdatedAt(LocalDateTime.now());
        accessRepository.save(access);
        record(accountNumber, null, null, "PASSCODE_GENERATED", null, "Enabled", admin.getEmail());

        String customerName = user.getAccount() != null
                ? Objects.toString(user.getAccount().getName(), Objects.toString(user.getUsername(), "Customer"))
                : Objects.toString(user.getUsername(), "Customer");
        return Map.of("success", true, "passcode", passcode, "customerName", customerName, "email", user.getEmail());
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public Map<String, Object> setEnabled(String accountNumber, boolean enabled, String adminEmail, String adminPassword) {
        Admin admin = requireAdmin(adminEmail, adminPassword);
        if (enabled) {
            userRepository.findByAccountNumber(accountNumber)
                    .filter(value -> "APPROVED".equalsIgnoreCase(value.getStatus()))
                    .orElseThrow(() -> new IllegalArgumentException("Approved customer account not found"));
        }
        Card360Access access = accessRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Card360 access has not been set up for this account"));
        boolean previous = access.isEnabled();
        access.setEnabled(enabled);
        access.setUpdatedBy(admin.getEmail());
        access.setUpdatedAt(LocalDateTime.now());
        accessRepository.save(access);
        record(accountNumber, null, null, "LOGIN_" + (enabled ? "ENABLED" : "DISABLED"),
                Boolean.toString(previous), Boolean.toString(enabled), admin.getEmail());
        return Map.of("success", true, "enabled", enabled);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public Map<String, Object> login(String cardNumber, String email) {
        if (isBlank(cardNumber) || isBlank(email)) {
            throw new IllegalArgumentException("Card number and email are required");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedCardNumber = cardNumber.replaceAll("[\\s-]", "");
        String accountNumber = findCardAccount(normalizedCardNumber, normalizedEmail);
        if (accountNumber == null) {
            throw new IllegalArgumentException("Unable to sign in. Check your details or contact the bank.");
        }

        Card360Access access = accessRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Unable to sign in. Check your details or contact the bank."));
        if (!access.isEnabled()) {
            throw new IllegalArgumentException("Unable to sign in. Check your details or contact the bank.");
        }

        access.setFailedAttempts(0);
        access.setLockedUntil(null);
        accessRepository.save(access);
        User user = userRepository.findByAccountNumber(accountNumber)
                .filter(value -> "APPROVED".equalsIgnoreCase(value.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Unable to sign in. Check your details or contact the bank."));
        return Map.of("success", true, "token", tokenService.issueCard360(user.getId(), accountNumber));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerCards(String accountNumber) {
        requireEnabled(accountNumber);
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Card card : cardRepository.findByAccountNumber(accountNumber)) {
            cards.add(debitCardView(card));
        }
        for (CreditCard card : creditCardRepository.findByAccountNumber(accountNumber)) {
            cards.add(creditCardView(card));
        }
        return Map.of("cards", cards);
    }

    @Transactional
    public Map<String, Object> linkCard(String accountNumber, String cardNumber) {
        requireEnabled(accountNumber);
        if (isBlank(cardNumber)) throw new IllegalArgumentException("Card number is required");
        Map<String, Object> card = ownedCard(accountNumber, cardNumber.trim());
        record(accountNumber, (String) card.get("type"), (Long) card.get("id"),
                "CARD_LINKED", null, "****" + cardNumber.trim().substring(Math.max(0, cardNumber.trim().length() - 4)),
                "CUSTOMER:" + accountNumber);
        return Map.of("success", true, "card", card);
    }

    @Transactional
    public Map<String, Object> updateCardStatus(String accountNumber, String type, long id, boolean blocked) {
        requireEnabled(accountNumber);
        if (!blocked && !customerCanUnblock(accountNumber, type, id)) {
            throw new IllegalArgumentException("Contact NeoBank to unblock this card");
        }
        String oldStatus;
        String newStatus = blocked ? "Blocked" : "Active";
        if ("debit".equalsIgnoreCase(type)) {
            Card card = cardRepository.findById(id).filter(c -> accountNumber.equals(c.getAccountNumber()))
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
            oldStatus = card.getStatus();
            card.setBlocked(blocked);
            card.setDeactivated(false);
            card.setStatus(newStatus);
            cardRepository.save(card);
        } else if ("credit".equalsIgnoreCase(type)) {
            CreditCard card = creditCardRepository.findById(id).filter(c -> accountNumber.equals(c.getAccountNumber()))
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
            oldStatus = card.getStatus();
            card.setBlocked(blocked);
            card.setDeactivated(false);
            card.setStatus(newStatus);
            creditCardRepository.save(card);
        } else {
            throw new IllegalArgumentException("Unsupported card type");
        }
        record(accountNumber, type.toLowerCase(Locale.ROOT), id, blocked ? "CARD_BLOCKED" : "CARD_UNBLOCKED",
                oldStatus, newStatus, "CUSTOMER:" + accountNumber);
        return Map.of("success", true, "status", newStatus);
    }

    @Transactional
    public Map<String, Object> updateCreditLimit(String accountNumber, long id, double limit) {
        requireEnabled(accountNumber);
        CreditCard card = creditCardRepository.findById(id).filter(c -> accountNumber.equals(c.getAccountNumber()))
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if (!Double.isFinite(limit) || limit < 0 || (card.getApprovedLimit() != null && limit > card.getApprovedLimit())) {
            throw new IllegalArgumentException("Limit must be between zero and the approved card limit");
        }
        Double oldLimit = card.getUserSetSpendingLimit();
        card.setUserSetSpendingLimit(limit);
        creditCardRepository.save(card);
        record(accountNumber, "credit", id, "SPENDING_LIMIT_CHANGED",
                Objects.toString(oldLimit, "Not set"), Double.toString(limit), "CUSTOMER:" + accountNumber);
        return Map.of("success", true, "spendingLimit", limit);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentAccount(String accountNumber) {
        requireEnabled(accountNumber);
        return creditCardService.getCards360PaymentAccount(accountNumber);
    }

    @Transactional
    public Map<String, Object> payCreditCardBill(String accountNumber, Long creditCardId, double amount) {
        requireEnabled(accountNumber);
        Map<String, Object> result = creditCardService.payCards360Bill(accountNumber, creditCardId, amount);
        record(accountNumber, "credit", creditCardId, "CARD_BILL_PAYMENT", null,
                Objects.toString(result.get("paidAmount"), "0"), "CUSTOMER:" + accountNumber);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCardTransactions(String accountNumber, String type, long cardId) {
        requireEnabled(accountNumber);
        List<Map<String, Object>> items = new ArrayList<>();
        if ("credit".equalsIgnoreCase(type)) {
            CreditCard card = creditCardRepository.findById(cardId)
                    .filter(value -> accountNumber.equals(value.getAccountNumber()))
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
            creditTransactionRepository.findByCreditCardId(card.getId()).stream()
                    .sorted(Comparator.comparing(CreditCardTransaction::getTransactionDate,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(100)
                    .forEach(tx -> items.add(Map.of(
                            "date", Objects.toString(tx.getTransactionDate(), ""),
                            "description", Objects.toString(tx.getDescription(),
                                    Objects.toString(tx.getMerchant(), "Card transaction")),
                            "amount", Objects.toString(tx.getAmount(), "0"),
                            "type", Objects.toString(tx.getTransactionType(), ""),
                            "status", Objects.toString(tx.getStatus(), ""))));
            return items;
        }
        if ("debit".equalsIgnoreCase(type)) {
            cardRepository.findById(cardId)
                    .filter(value -> accountNumber.equals(value.getAccountNumber()))
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
            return items;
        }
        throw new IllegalArgumentException("Unsupported card type");
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public Map<String, Object> getAdminCustomerDetails(String accountNumber, String adminEmail, String adminPassword) {
        requireAdmin(adminEmail, adminPassword);
        if (isBlank(accountNumber)) throw new IllegalArgumentException("Customer account number is required");
        User user = userRepository.findByAccountNumber(accountNumber.trim())
                .filter(value -> "APPROVED".equalsIgnoreCase(value.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Approved customer account not found"));
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Card card : cardRepository.findByAccountNumber(accountNumber.trim())) {
            cards.add(debitCardView(card));
        }
        for (CreditCard card : creditCardRepository.findByAccountNumber(accountNumber.trim())) {
            cards.add(creditCardView(card));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("accountNumber", accountNumber.trim());
        result.put("customerName", Objects.toString(user.getUsername(), "Customer"));
        result.put("email", Objects.toString(user.getEmail(), ""));
        result.put("cards", cards);
        Optional<Card360Access> access = accessRepository.findByAccountNumber(accountNumber.trim());
        result.put("enabled", access.isPresent() && access.get().isEnabled());
        return result;
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public List<Card360Audit> getAuditHistory(String accountNumber, String adminEmail, String adminPassword) {
        requireAdmin(adminEmail, adminPassword);
        return auditRepository.findTop200ByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    private String findCardAccount(String number, String email) {
        Card debit = cardRepository.findByCardNumber(number);
        if (debit != null && accountEmailMatches(debit.getAccountNumber(), email)) return debit.getAccountNumber();
        return creditCardRepository.findByCardNumber(number)
                .filter(card -> accountEmailMatches(card.getAccountNumber(), email))
                .map(card -> card.getAccountNumber()).orElse(null);
    }

    private boolean accountEmailMatches(String accountNumber, String email) {
        if (isBlank(accountNumber)) return false;
        return userRepository.findByAccountNumber(accountNumber)
                .filter(user -> "APPROVED".equalsIgnoreCase(user.getStatus()))
                .map(user -> email.equalsIgnoreCase(Objects.toString(user.getEmail(), "").trim()))
                .orElse(false);
    }

    private Map<String, Object> ownedCard(String accountNumber, String number) {
        Card debit = cardRepository.findByCardNumber(number);
        if (debit != null && accountNumber.equals(debit.getAccountNumber())) return debitCardView(debit);
        return creditCardRepository.findByCardNumber(number)
                .filter(card -> accountNumber.equals(card.getAccountNumber()))
                .map(this::creditCardView)
                .orElseThrow(() -> new IllegalArgumentException("Card does not belong to this customer account"));
    }

    private Map<String, Object> debitCardView(Card card) {
        return Map.of("id", card.getId(), "type", "debit", "cardType", Objects.toString(card.getCardType(), "Debit Card"),
                "maskedNumber", mask(card.getCardNumber()), "status", Objects.toString(card.getStatus(), "Unknown"),
                "expiryDate", Objects.toString(card.getExpiryDate(), ""), "blocked", card.isBlocked(),
                "canUnblock", customerCanUnblock(card.getAccountNumber(), "debit", card.getId()));
    }

    private Map<String, Object> creditCardView(CreditCard card) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", card.getId());
        result.put("type", "credit");
        result.put("cardType", "Credit Card");
        result.put("maskedNumber", mask(card.getCardNumber()));
        result.put("status", Objects.toString(card.getStatus(), "Unknown"));
        result.put("expiryDate", Objects.toString(card.getExpiryDate(), ""));
        result.put("blocked", card.isBlocked());
        result.put("approvedLimit", card.getApprovedLimit());
        result.put("availableLimit", card.getAvailableLimit());
        result.put("spendingLimit", card.getUserSetSpendingLimit());
        result.put("outstandingBalance", card.getCurrentBalance());
        result.put("canUnblock", customerCanUnblock(card.getAccountNumber(), "credit", card.getId()));
        return result;
    }

    private String mask(String number) {
        if (number == null || number.length() < 4) return "****";
        return "•••• •••• •••• " + number.substring(number.length() - 4);
    }

    private void record(String accountNumber, String cardType, Long cardId, String action,
                        String oldValue, String newValue, String changedBy) {
        Card360Audit audit = new Card360Audit();
        audit.setAccountNumber(accountNumber);
        audit.setCardType(cardType);
        audit.setCardId(cardId);
        audit.setAction(action);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setChangedBy(changedBy);
        audit.setCreatedAt(LocalDateTime.now());
        auditRepository.save(audit);
    }

    private Admin requireAdmin(String email, String password) {
        if (isBlank(email) || isBlank(password)) throw new IllegalArgumentException("Admin email and password are required");
        Admin admin = adminService.login(email.trim(), password);
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new IllegalArgumentException("Admin authentication failed");
        }
        return admin;
    }

    private void requireEnabled(String accountNumber) {
        accessRepository.findByAccountNumber(accountNumber)
                .filter(access -> access.isEnabled())
                .orElseThrow(() -> new IllegalArgumentException("Card360 access is disabled"));
        userRepository.findByAccountNumber(accountNumber)
                .filter(user -> "APPROVED".equalsIgnoreCase(user.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Approved customer account not found"));
    }

    private String generatePasscodeValue() {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
