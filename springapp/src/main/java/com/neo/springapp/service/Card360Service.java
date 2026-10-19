package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class Card360Service {
    private final Card360AccessRepository accessRepository;
    private final Card360AuditRepository auditRepository;
    private final CardRepository cardRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardTransactionRepository creditTransactionRepository;
    private final Card360ClosureRequestRepository closureRequestRepository;
    private final UserRepository userRepository;
    private final AdminService adminService;
    private final UserSessionTokenService tokenService;
    private final CreditCardService creditCardService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    private final SecureRandom random = new SecureRandom();

    public Card360Service(Card360AccessRepository accessRepository, Card360AuditRepository auditRepository,
                          CardRepository cardRepository, CreditCardRepository creditCardRepository,
                          CreditCardTransactionRepository creditTransactionRepository,
                          Card360ClosureRequestRepository closureRequestRepository,
                          UserRepository userRepository,
                          AdminService adminService, UserSessionTokenService tokenService,
                          CreditCardService creditCardService) {
        this.accessRepository = accessRepository;
        this.auditRepository = auditRepository;
        this.cardRepository = cardRepository;
        this.creditCardRepository = creditCardRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.closureRequestRepository = closureRequestRepository;
        this.userRepository = userRepository;
        this.adminService = adminService;
        this.tokenService = tokenService;
        this.creditCardService = creditCardService;
    }

    private boolean customerCanUnblock(String accountNumber, String type, long id) {
        return auditRepository.findFirstByAccountNumberAndCardTypeAndCardIdOrderByCreatedAtDesc(
                        accountNumber, type.toLowerCase(Locale.ROOT), id)
                .filter(audit -> "CARD_BLOCKED".equals(audit.getAction())
                        || "EMERGENCY_CARD_LOCK".equals(audit.getAction()))
                .filter(audit -> ("CUSTOMER:" + accountNumber).equals(audit.getChangedBy()))
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
            if (!blocked && card.isDeactivated()) {
                throw new IllegalArgumentException("Contact NeoBank to unblock a deactivated card");
            }
            oldStatus = card.getStatus();
            card.setBlocked(blocked);
            card.setDeactivated(false);
            card.setStatus(newStatus);
            cardRepository.save(card);
        } else if ("credit".equalsIgnoreCase(type)) {
            CreditCard card = creditCardRepository.findById(id).filter(c -> accountNumber.equals(c.getAccountNumber()))
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
            if (!blocked && card.isDeactivated()) {
                throw new IllegalArgumentException("Contact NeoBank to unblock a deactivated card");
            }
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

    @Transactional
    public Map<String, Boolean> updateCreditCardControls(String accountNumber, long id,
                                                          Map<String, Boolean> updates) {
        requireEnabled(accountNumber);
        CreditCard card = ownedCreditCard(accountNumber, id);
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("Select at least one card control to update");
        }
        Map<String, Boolean> controls = new HashMap<>();
        setControl(updates, controls, "onlineTransactionsEnabled",
                card::setOnlineTransactionsEnabled);
        setControl(updates, controls, "posTransactionsEnabled",
                card::setPosTransactionsEnabled);
        setControl(updates, controls, "contactlessEnabled",
                card::setContactlessEnabled);
        setControl(updates, controls, "atmWithdrawalsEnabled",
                card::setAtmWithdrawalsEnabled);
        setControl(updates, controls, "internationalTransactionsEnabled",
                card::setInternationalTransactionsEnabled);
        if (controls.isEmpty()) {
            throw new IllegalArgumentException("No supported card control was supplied");
        }
        creditCardRepository.save(card);
        record(accountNumber, "credit", id, "CARD_CONTROLS_CHANGED", null,
                controls.toString(), "CUSTOMER:" + accountNumber);
        return controls;
    }

    @Transactional
    public void setCreditCardPin(String accountNumber, long id, String pin) {
        requireEnabled(accountNumber);
        ownedCreditCard(accountNumber, id);
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must contain exactly 4 digits");
        }
        if (!creditCardService.setPin(id, pin)) {
            throw new IllegalArgumentException("Card not found");
        }
        record(accountNumber, "credit", id, "CARD_PIN_CHANGED", null,
                "PIN updated", "CUSTOMER:" + accountNumber);
    }

    @Transactional
    public Map<String, Object> emergencyLockCreditCard(String accountNumber, long id) {
        requireEnabled(accountNumber);
        CreditCard card = ownedCreditCard(accountNumber, id);
        Map<String, Boolean> controls = Map.of(
                "onlineTransactionsEnabled", false,
                "posTransactionsEnabled", false,
                "contactlessEnabled", false,
                "atmWithdrawalsEnabled", false,
                "internationalTransactionsEnabled", false);
        card.setOnlineTransactionsEnabled(false);
        card.setPosTransactionsEnabled(false);
        card.setContactlessEnabled(false);
        card.setAtmWithdrawalsEnabled(false);
        card.setInternationalTransactionsEnabled(false);
        card.setBlocked(true);
        card.setDeactivated(false);
        card.setStatus("Blocked");
        creditCardRepository.save(card);
        record(accountNumber, "credit", id, "EMERGENCY_CARD_LOCK", null,
                controls.toString(), "CUSTOMER:" + accountNumber);
        return Map.of("status", card.getStatus(), "controls", controls);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEmiSettingsForCustomer() {
        return creditCardService.getEmiSettings();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCardSecurityEvents(String accountNumber, long id) {
        requireEnabled(accountNumber);
        ownedCreditCard(accountNumber, id);
        return auditRepository.findTop200ByAccountNumberOrderByCreatedAtDesc(accountNumber).stream()
                .filter(event -> "credit".equalsIgnoreCase(event.getCardType())
                        && Objects.equals(event.getCardId(), id))
                .map(event -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("action", event.getAction());
                    result.put("previousValue", Objects.toString(event.getOldValue(), ""));
                    result.put("newValue", Objects.toString(event.getNewValue(), ""));
                    result.put("createdAt", event.getCreatedAt());
                    return result;
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEmiEligibleTransactions(String accountNumber, long id) {
        requireEnabled(accountNumber);
        ownedCreditCard(accountNumber, id);
        return creditCardService.getEmiEligibleTransactions(id).stream().map(transaction -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", transaction.getId());
            result.put("merchant", Objects.toString(transaction.getMerchant(), ""));
            result.put("description", Objects.toString(transaction.getDescription(), ""));
            result.put("amount", transaction.getAmount());
            result.put("transactionDate", transaction.getTransactionDate());
            result.put("status", transaction.getStatus());
            return result;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEmiPlans(String accountNumber, long id) {
        requireEnabled(accountNumber);
        ownedCreditCard(accountNumber, id);
        return creditCardService.getEmiPlans(id);
    }

    @Transactional
    public Map<String, Object> convertTransactionsToEmi(String accountNumber, long id,
                                                         List<Long> transactionIds, Integer tenureMonths) {
        requireEnabled(accountNumber);
        ownedCreditCard(accountNumber, id);
        Map<String, Object> result = creditCardService.convertTransactionsToEmi(id, transactionIds, tenureMonths);
        record(accountNumber, "credit", id, "EMI_PLAN_CREATED", null,
                Objects.toString(result.get("id"), "Created"), "CUSTOMER:" + accountNumber);
        return result;
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
                    .forEach(tx -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", tx.getId());
                        item.put("date", tx.getTransactionDate());
                        item.put("description", Objects.toString(tx.getDescription(),
                                Objects.toString(tx.getMerchant(), "Card transaction")));
                        item.put("merchant", Objects.toString(tx.getMerchant(), ""));
                        item.put("amount", tx.getAmount());
                        item.put("type", Objects.toString(tx.getTransactionType(), ""));
                        item.put("status", Objects.toString(tx.getStatus(), ""));
                        item.put("receipt", Objects.toString(tx.getGlobalTransactionSequence(),
                                tx.getBillId() == null ? "—" : "Bill #" + tx.getBillId()));
                        item.put("billId", tx.getBillId());
                        item.put("emiPlanId", tx.getEmiPlanId());
                        item.put("balanceAfter", tx.getBalanceAfter());
                        item.put("paymentMethod", tx.getPaymentMethod());
                        items.add(item);
                    });
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

    @Transactional(readOnly = true)
    public Map<String, Object> revealCreditCardNumber(String accountNumber, long cardId) {
        requireEnabled(accountNumber);
        CreditCard card = creditCardRepository.findById(cardId)
                .filter(value -> accountNumber.equals(value.getAccountNumber()))
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        return Map.of("fullCardNumber", Objects.toString(card.getCardNumber(), ""));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCreditCardBills(String accountNumber, long cardId) {
        requireEnabled(accountNumber);
        CreditCard card = creditCardRepository.findById(cardId)
                .filter(value -> accountNumber.equals(value.getAccountNumber()))
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        return creditCardService.getBillsByCardId(card.getId()).stream()
                .sorted(Comparator.comparing(CreditCardBill::getBillGenerationDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::billStatementView)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCreditCardStatement(String accountNumber, long cardId,
                                                       LocalDate startDate, LocalDate endDate) {
        requireEnabled(accountNumber);
        CreditCard card = creditCardRepository.findById(cardId)
                .filter(value -> accountNumber.equals(value.getAccountNumber()))
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Enter a valid statement date range");
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        List<Map<String, Object>> transactions = creditTransactionRepository.findByCreditCardId(card.getId())
                .stream()
                .filter(tx -> tx.getTransactionDate() != null
                        && !tx.getTransactionDate().isBefore(start)
                        && tx.getTransactionDate().isBefore(endExclusive))
                .sorted(Comparator.comparing(CreditCardTransaction::getTransactionDate))
                .map(tx -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", tx.getTransactionDate());
                    item.put("description", Objects.toString(tx.getDescription(),
                            Objects.toString(tx.getMerchant(), "Card transaction")));
                    item.put("type", Objects.toString(tx.getTransactionType(), ""));
                    item.put("amount", tx.getAmount());
                    item.put("balanceAfter", tx.getBalanceAfter());
                    item.put("billId", tx.getBillId());
                    return item;
                }).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("cardType", "Credit Card");
        result.put("maskedCardNumber", mask(card.getCardNumber()));
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("transactions", transactions);
        return result;
    }

    @Transactional
    public Map<String, Object> requestCreditCardClosure(String accountNumber, long cardId, String reason) {
        requireEnabled(accountNumber);
        CreditCard card = creditCardRepository.findById(cardId)
                .filter(value -> accountNumber.equals(value.getAccountNumber()))
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if ("Closed".equalsIgnoreCase(card.getStatus())) {
            throw new IllegalArgumentException("This credit card is already closed");
        }
        if (closureRequestRepository.findFirstByCreditCardIdAndStatusOrderByRequestedAtDesc(cardId, "Pending").isPresent()) {
            throw new IllegalArgumentException("A closure request is already pending for this card");
        }
        if (isBlank(reason)) throw new IllegalArgumentException("Enter a reason for closing this card");
        Card360ClosureRequest request = new Card360ClosureRequest();
        request.setCreditCardId(card.getId());
        request.setAccountNumber(accountNumber);
        request.setCustomerName(card.getUserName());
        request.setMaskedCardNumber(mask(card.getCardNumber()));
        request.setReason(reason.trim());
        request.setStatus("Pending");
        Card360ClosureRequest saved = closureRequestRepository.save(request);
        record(accountNumber, "credit", cardId, "CARD_CLOSURE_REQUESTED", null,
                "Request #" + saved.getId() + ": " + reason.trim(), "CUSTOMER:" + accountNumber);
        return closureRequestView(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAdminClosureRequests(String accountNumber, String adminEmail,
                                                              String adminPassword) {
        requireAdmin(adminEmail, adminPassword);
        if (isBlank(accountNumber)) throw new IllegalArgumentException("Customer account number is required");
        return closureRequestRepository.findByAccountNumberOrderByRequestedAtDesc(accountNumber.trim())
                .stream().map(this::closureRequestView).toList();
    }

    @Transactional
    public Map<String, Object> reviewClosureRequest(Long requestId, boolean approve, String reviewNote,
                                                     String adminEmail, String adminPassword) {
        Admin admin = requireAdmin(adminEmail, adminPassword);
        Card360ClosureRequest request = closureRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Card closure request not found"));
        if (!"Pending".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalArgumentException("This closure request has already been reviewed");
        }
        if (approve) {
            CreditCard card = creditCardRepository.findById(request.getCreditCardId())
                    .filter(value -> request.getAccountNumber().equals(value.getAccountNumber()))
                    .orElseThrow(() -> new IllegalArgumentException("Requested credit card not found"));
            if (!creditCardService.closeCreditCard(card.getId())) {
                throw new IllegalArgumentException("Card cannot be closed while it has an outstanding balance");
            }
            request.setStatus("Approved");
        } else {
            if (isBlank(reviewNote)) throw new IllegalArgumentException("Enter a reason when rejecting the request");
            request.setStatus("Rejected");
        }
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(admin.getEmail());
        request.setReviewNote(isBlank(reviewNote) ? null : reviewNote.trim());
        Card360ClosureRequest saved = closureRequestRepository.save(request);
        record(request.getAccountNumber(), "credit", request.getCreditCardId(),
                approve ? "CARD_CLOSURE_REQUEST_APPROVED" : "CARD_CLOSURE_REQUEST_REJECTED",
                "Pending", saved.getStatus(), admin.getEmail());
        return closureRequestView(saved);
    }

    private Map<String, Object> billStatementView(CreditCardBill bill) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", bill.getId());
        result.put("billGenerationDate", bill.getBillGenerationDate());
        result.put("dueDate", bill.getDueDate());
        result.put("totalAmount", bill.getTotalAmount());
        result.put("regularAmount", bill.getRegularAmount());
        result.put("emiAmount", bill.getEmiAmount());
        result.put("minimumDue", bill.getMinimumDue());
        result.put("paidAmount", bill.getPaidAmount());
        result.put("fine", bill.getFine());
        result.put("penalty", bill.getPenalty());
        result.put("status", bill.getStatus());
        result.put("billingPeriod", bill.getBillingPeriod());
        return result;
    }

    private Map<String, Object> closureRequestView(Card360ClosureRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", request.getId());
        result.put("creditCardId", request.getCreditCardId());
        result.put("accountNumber", request.getAccountNumber());
        result.put("customerName", request.getCustomerName());
        result.put("maskedCardNumber", request.getMaskedCardNumber());
        result.put("reason", request.getReason());
        result.put("status", request.getStatus());
        result.put("requestedAt", request.getRequestedAt());
        result.put("reviewedAt", request.getReviewedAt());
        result.put("reviewedBy", request.getReviewedBy());
        result.put("reviewNote", request.getReviewNote());
        return result;
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
        result.put("cardholderName", Objects.toString(card.getUserName(), ""));
        result.put("maskedNumber", mask(card.getCardNumber()));
        result.put("status", Objects.toString(card.getStatus(), "Unknown"));
        result.put("pinSet", card.isPinSet());
        result.put("expiryDate", Objects.toString(card.getExpiryDate(), ""));
        result.put("blocked", card.isBlocked());
        result.put("approvedLimit", card.getApprovedLimit());
        result.put("availableLimit", card.getAvailableLimit());
        result.put("spendingLimit", card.getUserSetSpendingLimit());
        result.put("outstandingBalance", card.getCurrentBalance());
        result.put("canUnblock", customerCanUnblock(card.getAccountNumber(), "credit", card.getId()));
        result.put("onlineTransactionsEnabled", card.isOnlineTransactionsEnabled());
        result.put("posTransactionsEnabled", card.isPosTransactionsEnabled());
        result.put("contactlessEnabled", card.isContactlessEnabled());
        result.put("atmWithdrawalsEnabled", card.isAtmWithdrawalsEnabled());
        result.put("internationalTransactionsEnabled", card.isInternationalTransactionsEnabled());
        result.put("fine", card.getFine());
        result.put("penalty", card.getPenalty());
        closureRequestRepository.findFirstByCreditCardIdAndStatusOrderByRequestedAtDesc(card.getId(), "Pending")
                .ifPresent(request -> result.put("closureRequestStatus", request.getStatus()));
        creditCardService.getBillsByCardId(card.getId()).stream()
                .max(Comparator.comparing(CreditCardBill::getBillGenerationDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .ifPresent(bill -> {
                    result.put("billGenerationDate", bill.getBillGenerationDate());
                    result.put("billDueDate", bill.getDueDate());
                    result.put("billTotalAmount", bill.getTotalAmount());
                    result.put("billPaidAmount", bill.getPaidAmount());
                    result.put("billMinimumDue", bill.getMinimumDue());
                    result.put("billFine", bill.getFine());
                    result.put("billPenalty", bill.getPenalty());
                    result.put("billEmiAmount", bill.getEmiAmount());
                    result.put("billStatus", bill.getStatus());
                });
        return result;
    }

    private CreditCard ownedCreditCard(String accountNumber, long id) {
        return creditCardRepository.findById(id)
                .filter(card -> accountNumber.equals(card.getAccountNumber()))
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
    }

    private void setControl(Map<String, Boolean> updates, Map<String, Boolean> controls,
                            String key, java.util.function.Consumer<Boolean> setter) {
        if (!updates.containsKey(key)) return;
        Boolean enabled = updates.get(key);
        if (enabled == null) throw new IllegalArgumentException(key + " must be true or false");
        setter.accept(enabled);
        controls.put(key, enabled);
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
