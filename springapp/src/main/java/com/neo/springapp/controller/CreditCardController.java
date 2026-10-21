package com.neo.springapp.controller;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.AdminAuditLogRepository;
import com.neo.springapp.repository.CreditCardAuditLogRepository;
import com.neo.springapp.repository.CreditCardBillRepository;
import com.neo.springapp.repository.CreditCardSettingsRepository;
import com.neo.springapp.service.CreditCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import com.neo.springapp.service.UserSessionTokenService;

@RestController
@RequestMapping("/api/credit-cards")
@CrossOrigin(origins = "*")
public class CreditCardController {

    @Autowired
    private CreditCardService creditCardService;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    @Autowired
    private CreditCardAuditLogRepository creditCardAuditLogRepository;

    @Autowired
    private CreditCardBillRepository creditCardBillRepository;

    @Autowired
    private CreditCardSettingsRepository creditCardSettingsRepository;

    private Optional<CreditCard> getOwnedCard(Long id, Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserSessionTokenService.SessionPrincipal principal)
                || !"USER".equals(principal.scope())) {
            return Optional.empty();
        }
        return creditCardService.getCreditCardById(id)
                .filter(card -> principal.accountNumber().equals(card.getAccountNumber()));
    }

    private boolean isStaff(Authentication authentication) {
        return authentication != null
                && authentication.getPrincipal() instanceof UserSessionTokenService.SessionPrincipal principal
                && ("ADMIN".equals(principal.scope()) || "MANAGER".equals(principal.scope()));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getPrincipal() instanceof UserSessionTokenService.SessionPrincipal principal
                && "ADMIN".equals(principal.scope());
    }

    private boolean canAccessCard(Long id, Authentication authentication) {
        return isStaff(authentication) || getOwnedCard(id, authentication).isPresent();
    }

    private void addAuditLog(CreditCard card, String action, String previousValue, String newValue) {
        CreditCardAuditLog entry = new CreditCardAuditLog();
        entry.setCreditCardId(card.getId());
        entry.setAccountNumber(card.getAccountNumber());
        entry.setAction(action);
        entry.setPreviousValue(previousValue);
        entry.setNewValue(newValue);
        creditCardAuditLogRepository.save(entry);
    }

    private Map<String, Boolean> cardControls(CreditCard card) {
        Map<String, Boolean> controls = new HashMap<>();
        controls.put("onlineTransactionsEnabled", card.isOnlineTransactionsEnabled());
        controls.put("posTransactionsEnabled", card.isPosTransactionsEnabled());
        controls.put("contactlessEnabled", card.isContactlessEnabled());
        controls.put("atmWithdrawalsEnabled", card.isAtmWithdrawalsEnabled());
        controls.put("internationalTransactionsEnabled", card.isInternationalTransactionsEnabled());
        return controls;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) return null;
        return "**** **** **** " + cardNumber.substring(Math.max(0, cardNumber.length() - 4));
    }

    private Map<String, Object> customerCardSummary(CreditCard card) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", card.getId());
        summary.put("cardNumber", maskCardNumber(card.getCardNumber()));
        summary.put("expiryDate", card.getExpiryDate());
        summary.put("pinSet", card.isPinSet());
        summary.put("status", card.getStatus());
        summary.put("accountNumber", card.getAccountNumber());
        summary.put("userName", card.getUserName());
        summary.put("userEmail", card.getUserEmail());
        summary.put("approvedLimit", card.getApprovedLimit());
        summary.put("currentBalance", card.getCurrentBalance());
        summary.put("availableLimit", card.getAvailableLimit());
        summary.put("usageLimit", card.getUsageLimit());
        summary.put("userSetSpendingLimit", card.getUserSetSpendingLimit());
        summary.put("overdueAmount", card.getOverdueAmount());
        summary.put("fine", card.getFine());
        summary.put("penalty", card.getPenalty());
        summary.put("appliedDate", card.getAppliedDate());
        summary.put("approvalDate", card.getApprovalDate());
        summary.put("closureDate", card.getClosureDate());
        summary.put("lastPaidDate", card.getLastPaidDate());
        return summary;
    }

    private Map<String, Object> adminCardSummary(CreditCard card) {
        Map<String, Object> summary = customerCardSummary(card);
        summary.put("maskedCardNumber", maskCardNumber(card.getCardNumber()));
        summary.put("expiryDateTime", card.getExpiryDateTime());
        summary.put("issueDate", card.getIssueDate());
        summary.put("lastUsed", card.getLastUsed());
        summary.put("nextBillingDate", card.getNextBillingDate());
        summary.put("blocked", card.isBlocked());
        summary.put("deactivated", card.isDeactivated());
        summary.put("onlineTransactionsEnabled", card.isOnlineTransactionsEnabled());
        summary.put("posTransactionsEnabled", card.isPosTransactionsEnabled());
        summary.put("contactlessEnabled", card.isContactlessEnabled());
        summary.put("atmWithdrawalsEnabled", card.isAtmWithdrawalsEnabled());
        summary.put("internationalTransactionsEnabled", card.isInternationalTransactionsEnabled());
        return summary;
    }

    private Map<String, Object> customerTransactionSummary(CreditCardTransaction transaction) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", transaction.getId());
        summary.put("globalTransactionSequence", transaction.getGlobalTransactionSequence());
        summary.put("cardNumber", maskCardNumber(transaction.getCardNumber()));
        summary.put("billId", transaction.getBillId());
        summary.put("emiPlanId", transaction.getEmiPlanId());
        summary.put("transactionType", transaction.getTransactionType());
        summary.put("amount", transaction.getAmount());
        summary.put("merchant", transaction.getMerchant());
        summary.put("description", transaction.getDescription());
        summary.put("transactionDate", transaction.getTransactionDate());
        summary.put("balanceAfter", transaction.getBalanceAfter());
        summary.put("status", transaction.getStatus());
        summary.put("paymentMethod", transaction.getPaymentMethod());
        return summary;
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return null;
        int visibleDigits = Math.min(4, accountNumber.length());
        return "****" + accountNumber.substring(accountNumber.length() - visibleDigits);
    }

    private Map<String, Object> adminTransactionSummary(CreditCardTransaction transaction) {
        Map<String, Object> summary = customerTransactionSummary(transaction);
        summary.put("globalTransactionSequence", transaction.getGlobalTransactionSequence());
        summary.put("cardReference", transaction.getCreditCardId());
        summary.put("customerReference", maskAccountNumber(transaction.getAccountNumber()));
        summary.put("paymentMethod", transaction.getPaymentMethod());
        summary.put("chequeNumber", transaction.getChequeNumber());
        summary.put("debitAccountNumber", maskAccountNumber(transaction.getDebitAccountNumber()));
        summary.put("processedBy", transaction.getProcessedBy());
        summary.put("chequeImage", transaction.getChequeImage() != null && transaction.getChequeImage().length > 0);
        summary.put("chequeImageName", transaction.getChequeImageName());
        return summary;
    }

    private Map<String, Object> customerBillSummary(CreditCardBill bill) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", bill.getId());
        summary.put("cardNumber", maskCardNumber(bill.getCardNumber()));
        summary.put("billGenerationDate", bill.getBillGenerationDate());
        summary.put("dueDate", bill.getDueDate());
        summary.put("paidDate", bill.getPaidDate());
        summary.put("totalAmount", bill.getTotalAmount());
        summary.put("regularAmount", bill.getRegularAmount());
        summary.put("emiAmount", bill.getEmiAmount());
        summary.put("minimumDue", bill.getMinimumDue());
        summary.put("paidAmount", bill.getPaidAmount());
        summary.put("overdueAmount", bill.getOverdueAmount());
        summary.put("fine", bill.getFine());
        summary.put("penalty", bill.getPenalty());
        summary.put("status", bill.getStatus());
        summary.put("billingPeriod", bill.getBillingPeriod());
        return summary;
    }

    @GetMapping("/{id}/card-number")
    public ResponseEntity<?> getCardNumber(@PathVariable Long id, Authentication authentication) {
        Optional<CreditCard> card = getOwnedCard(id, authentication);
        if (card.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(Map.of("cardNumber", card.get().getCardNumber()));
    }

    @GetMapping("/{id}/admin/card-number")
    @Transactional
    public ResponseEntity<?> revealAdminCardNumber(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Only authorized administrators may reveal card numbers"));
        }

        Optional<CreditCard> card = creditCardService.getCreditCardById(id);
        if (card.isEmpty() || card.get().getCardNumber() == null || card.get().getCardNumber().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        UserSessionTokenService.SessionPrincipal principal =
                (UserSessionTokenService.SessionPrincipal) authentication.getPrincipal();
        AdminAuditLog audit = new AdminAuditLog(
                principal.userId(), principal.accountNumber(), "SENSITIVE_CARD_PAN_REVEAL_ADMIN", "CREDIT_CARD", id);
        audit.setEntityName("Credit card PAN");
        audit.setReasonForChange("ADMIN requested card-number reveal");
        audit.setStatus("COMPLETED");
        adminAuditLogRepository.save(audit);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(Map.of("cardNumber", card.get().getCardNumber()));
    }

    @GetMapping("/{id}/controls")
    public ResponseEntity<?> getCardControls(@PathVariable Long id, Authentication authentication) {
        Optional<CreditCard> card = getOwnedCard(id, authentication);
        if (card.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(cardControls(card.get()));
    }

    @GetMapping("/{id}/security-events")
    public ResponseEntity<?> getCardSecurityEvents(@PathVariable Long id, Authentication authentication) {
        Optional<CreditCard> card = getOwnedCard(id, authentication);
        if (card.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(creditCardAuditLogRepository
                .findByCreditCardIdOrderByCreatedAtDesc(id).stream()
                .map(event -> Map.of(
                        "action", event.getAction() == null ? "" : event.getAction(),
                        "previousValue", event.getPreviousValue() == null ? "" : event.getPreviousValue(),
                        "newValue", event.getNewValue() == null ? "" : event.getNewValue(),
                        "createdAt", event.getCreatedAt()))
                .toList());
    }

    @PutMapping("/{id}/controls")
    @Transactional
    public ResponseEntity<?> updateCardControls(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> updates,
            Authentication authentication) {
        Optional<CreditCard> cardOpt = getOwnedCard(id, authentication);
        if (cardOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        CreditCard card = cardOpt.get();
        Map<String, Boolean> previous = cardControls(card);
        Map<String, Boolean> supported = new HashMap<>();
        supported.put("onlineTransactionsEnabled", updates.get("onlineTransactionsEnabled"));
        supported.put("posTransactionsEnabled", updates.get("posTransactionsEnabled"));
        supported.put("contactlessEnabled", updates.get("contactlessEnabled"));
        supported.put("atmWithdrawalsEnabled", updates.get("atmWithdrawalsEnabled"));
        supported.put("internationalTransactionsEnabled", updates.get("internationalTransactionsEnabled"));
        if (supported.values().stream().allMatch(value -> value == null)) {
            return ResponseEntity.badRequest().body(Map.of("message", "At least one card control must be provided"));
        }
        if (!"Active".equalsIgnoreCase(card.getStatus())
                && supported.values().stream().anyMatch(Boolean.TRUE::equals)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unlock the card before enabling transaction controls"));
        }

        if (updates.get("onlineTransactionsEnabled") != null) card.setOnlineTransactionsEnabled(updates.get("onlineTransactionsEnabled"));
        if (updates.get("posTransactionsEnabled") != null) card.setPosTransactionsEnabled(updates.get("posTransactionsEnabled"));
        if (updates.get("contactlessEnabled") != null) card.setContactlessEnabled(updates.get("contactlessEnabled"));
        if (updates.get("atmWithdrawalsEnabled") != null) card.setAtmWithdrawalsEnabled(updates.get("atmWithdrawalsEnabled"));
        if (updates.get("internationalTransactionsEnabled") != null) card.setInternationalTransactionsEnabled(updates.get("internationalTransactionsEnabled"));

        creditCardService.updateCreditCard(card);
        for (String control : supported.keySet()) {
            Boolean next = supported.get(control);
            if (next != null && !next.equals(previous.get(control))) {
                addAuditLog(card, "CARD_CONTROL_UPDATED:" + control, String.valueOf(previous.get(control)), String.valueOf(next));
            }
        }
        return ResponseEntity.ok(cardControls(card));
    }

    @PostMapping("/{id}/emergency-lock")
    @Transactional
    public ResponseEntity<?> emergencyLock(@PathVariable Long id, Authentication authentication) {
        Optional<CreditCard> cardOpt = isStaff(authentication)
                ? creditCardService.getCreditCardById(id)
                : getOwnedCard(id, authentication);
        if (cardOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        CreditCard card = cardOpt.get();
        card.setStatus("Blocked");
        card.setBlocked(true);
        card.setDeactivated(true);
        card.setOnlineTransactionsEnabled(false);
        card.setPosTransactionsEnabled(false);
        card.setContactlessEnabled(false);
        card.setAtmWithdrawalsEnabled(false);
        card.setInternationalTransactionsEnabled(false);
        creditCardService.updateCreditCard(card);
        addAuditLog(card, "EMERGENCY_CARD_LOCK", "UNLOCKED", "LOCKED_ALL_CHANNELS");
        return ResponseEntity.ok(Map.of("controls", cardControls(card), "status", card.getStatus()));
    }

    @PostMapping("/{id}/unlock")
    @Transactional
    public ResponseEntity<?> unlockCard(@PathVariable Long id, Authentication authentication) {
        Optional<CreditCard> cardOpt = getOwnedCard(id, authentication);
        if (cardOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        CreditCard card = cardOpt.get();
        if ("Closed".equalsIgnoreCase(card.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "A closed card cannot be unlocked"));
        }
        card.setStatus("Active");
        card.setBlocked(false);
        card.setDeactivated(false);
        creditCardService.updateCreditCard(card);
        addAuditLog(card, "CARD_UNLOCKED", "BLOCKED", "ACTIVE");
        return ResponseEntity.ok(customerCardSummary(card));
    }

    @PutMapping("/{id}/spending-limit")
    @Transactional
    public ResponseEntity<?> updateSpendingLimit(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Optional<CreditCard> cardOpt = getOwnedCard(id, authentication);
        if (cardOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Object rawLimit = request.get("spendingLimit");
        if (!(rawLimit instanceof Number number)) {
            return ResponseEntity.badRequest().body(Map.of("message", "A numeric spendingLimit is required"));
        }
        double limit = number.doubleValue();
        CreditCard card = cardOpt.get();
        if (!Double.isFinite(limit) || limit <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Spending limit must be greater than zero"));
        }
        if (card.getApprovedLimit() == null || limit > card.getApprovedLimit()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Spending limit cannot exceed the bank-approved limit",
                    "approvedLimit", card.getApprovedLimit() == null ? 0 : card.getApprovedLimit()));
        }
        Double previousLimit = card.getUserSetSpendingLimit();
        card.setUserSetSpendingLimit(limit);
        CreditCard updatedCard = creditCardService.updateCreditCard(card);
        addAuditLog(card, "SPENDING_LIMIT_UPDATED",
                String.valueOf(previousLimit == null ? card.getApprovedLimit() : previousLimit),
                String.valueOf(limit));
        return ResponseEntity.ok(customerCardSummary(updatedCard));
    }

    @GetMapping("/bill-pay/balance/{accountNumber}")
    public ResponseEntity<?> getPayerBalance(@PathVariable String accountNumber) {
        Double balance = creditCardService.getAccountBalance(accountNumber);
        Map<String, Object> body = new HashMap<>();
        body.put("balance", balance);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/bill-pay/lookup")
    public ResponseEntity<?> lookupBillPay(@RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(creditCardService.lookupCardForBillPay(
                    request.get("last4"), request.get("mobile"), request.get("payerAccountNumber")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/bill-pay/pay")
    public ResponseEntity<?> payBillFromAccount(@RequestBody Map<String, Object> request) {
        try {
            Object amt = request.get("amount");
            Double amount = amt == null ? null : Double.valueOf(amt.toString());
            return ResponseEntity.ok(creditCardService.payBillFromAccount(
                    String.valueOf(request.get("last4")), String.valueOf(request.get("mobile")),
                    String.valueOf(request.get("payerAccountNumber")), amount));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Admin: Get all credit cards
    @GetMapping("/all")
    public ResponseEntity<?> getAllCreditCards(Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(creditCardService.getAllCreditCards().stream()
                .map(this::adminCardSummary)
                .toList());
    }

    // User: Get credit cards by account number
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<?> getCreditCardsByAccount(
            @PathVariable String accountNumber,
            Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserSessionTokenService.SessionPrincipal principal)
                || !"USER".equals(principal.scope())
                || !principal.accountNumber().equals(accountNumber)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(creditCardService.getCreditCardsByAccount(accountNumber).stream()
                .map(this::customerCardSummary)
                .toList());
    }

    // Get credit card by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCreditCardById(@PathVariable Long id, Authentication authentication) {
        Optional<CreditCard> card = getOwnedCard(id, authentication);
        return card.<ResponseEntity<?>>map(value -> ResponseEntity.ok(customerCardSummary(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Create credit card from approved request
    @PostMapping("/create")
    public ResponseEntity<?> createCreditCard(
            @RequestBody CreditCardRequest request,
            Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        // Generate card details
        String cardNumber = generateCardNumber();
        String cvv = String.format("%03d", (int)(Math.random() * 1000));
        String expiryMonth = String.format("%02d", (int)(Math.random() * 12) + 1);
        String expiryYear = String.valueOf(LocalDateTime.now().getYear() + 3);
        String expiryDate = expiryMonth + "/" + expiryYear.substring(2);
        
        CreditCard creditCard = creditCardService.createCreditCardFromRequest(request, cardNumber, cvv, expiryDate);
        return ResponseEntity.ok(creditCard);
    }

    // Update credit card
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCreditCard(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updateData,
            Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Optional<CreditCard> existingCardOpt = creditCardService.getCreditCardById(id);
        if (existingCardOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CreditCard existingCard = existingCardOpt.get();
        
        // Update approved limit if provided
        if (updateData.containsKey("approvedLimit")) {
            Double newLimit = ((Number) updateData.get("approvedLimit")).doubleValue();
            existingCard.setApprovedLimit(newLimit);
            // Recalculate available limit and usage
            existingCard.calculateAvailableLimit();
            existingCard.calculateUsageLimit();
        }
        
        // Update other fields if provided
        if (updateData.containsKey("status")) {
            existingCard.setStatus((String) updateData.get("status"));
        }

        // Update user-set spending limit if provided (must be within approved limit)
        if (updateData.containsKey("userSetSpendingLimit")) {
            Double newSpendingLimit = ((Number) updateData.get("userSetSpendingLimit")).doubleValue();
            if (newSpendingLimit < 0) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Spending limit cannot be negative");
                return ResponseEntity.badRequest().body(err);
            }
            if (existingCard.getApprovedLimit() != null && newSpendingLimit > existingCard.getApprovedLimit()) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Spending limit cannot exceed approved limit");
                err.put("approvedLimit", existingCard.getApprovedLimit());
                return ResponseEntity.badRequest().body(err);
            }
            existingCard.setUserSetSpendingLimit(newSpendingLimit);
        }
        
        return ResponseEntity.ok(creditCardService.updateCreditCard(existingCard));
    }

    // Set PIN
    @PutMapping("/{id}/set-pin")
    public ResponseEntity<?> setPin(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        if (getOwnedCard(id, authentication).isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        String pin = request.get("pin");
        if (pin == null || !pin.matches("\\d{4}")) {
            return ResponseEntity.badRequest().body("PIN must contain exactly 4 digits");
        }
        boolean success = creditCardService.setPin(id, pin);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // Get transactions
    @GetMapping("/{id}/transactions")
    public ResponseEntity<?> getTransactions(@PathVariable Long id, Authentication authentication) {
        if (!canAccessCard(id, authentication)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if (isStaff(authentication)) {
            return ResponseEntity.ok(creditCardService.getTransactionsByCardId(id).stream()
                    .map(this::adminTransactionSummary)
                    .toList());
        }
        return ResponseEntity.ok(creditCardService.getTransactionsByCardId(id).stream()
                .map(this::customerTransactionSummary)
                .toList());
    }

    @GetMapping("/transactions/{transactionId}/cheque-image")
    public ResponseEntity<byte[]> downloadChequeImage(@PathVariable Long transactionId) {
        CreditCardTransaction transaction = creditCardService.getChequeImageTransaction(transactionId);
        if (transaction == null) return ResponseEntity.notFound().build();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (transaction.getChequeImageType() != null) mediaType = MediaType.parseMediaType(transaction.getChequeImageType());
        } catch (Exception ignored) { }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                        + (transaction.getChequeImageName() == null ? "cheque-image" : transaction.getChequeImageName()) + "\"")
                .contentType(mediaType)
                .body(transaction.getChequeImage());
    }

    @GetMapping("/account/{accountNumber}/transactions")
    public ResponseEntity<List<CreditCardTransaction>> getTransactionsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(creditCardService.getTransactionsByAccount(accountNumber));
    }

    // Add transaction
    @PostMapping("/transactions")
    public ResponseEntity<CreditCardTransaction> addTransaction(@RequestBody CreditCardTransaction transaction) {
        return ResponseEntity.ok(creditCardService.addTransaction(transaction));
    }

    // Generate bill
    @PostMapping("/{id}/generate-bill")
    public ResponseEntity<CreditCardBill> generateBill(@PathVariable Long id) {
        CreditCardBill bill = creditCardService.generateBill(id);
        return bill != null ? ResponseEntity.ok(bill) : ResponseEntity.notFound().build();
    }

    // Get bills
    @GetMapping("/{id}/bills")
    public ResponseEntity<?> getBills(@PathVariable Long id, Authentication authentication) {
        if (!canAccessCard(id, authentication)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(creditCardService.getBillsByCardId(id).stream()
                .map(this::customerBillSummary)
                .toList());
    }

    @GetMapping("/emi-settings")
    public ResponseEntity<?> getEmiSettings() {
        return ResponseEntity.ok(creditCardService.getEmiSettings());
    }

    @GetMapping("/credit-limit-increase-ceiling")
    public ResponseEntity<?> getCreditLimitIncreaseCeiling(Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        BigDecimal ceiling = creditCardSettingsRepository.findAll().stream().findFirst()
                .map(CreditCardSettings::getCreditLimitIncreaseCeiling).orElse(null);
        return ResponseEntity.ok(Map.of("ceiling", ceiling == null ? 0 : ceiling));
    }

    @PutMapping("/credit-limit-increase-ceiling")
    @Transactional
    public ResponseEntity<?> updateCreditLimitIncreaseCeiling(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Object rawCeiling = request.get("ceiling");
        if (!(rawCeiling instanceof Number number) || !Double.isFinite(number.doubleValue())
                || number.doubleValue() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "The credit-limit ceiling must be greater than zero"));
        }
        CreditCardSettings settings = creditCardSettingsRepository.findAll().stream().findFirst()
                .orElseGet(CreditCardSettings::new);
        settings.setCreditLimitIncreaseCeiling(new BigDecimal(number.toString()));
        settings.setUpdatedBy(authentication.getName());
        settings.setUpdatedAt(LocalDateTime.now());
        creditCardSettingsRepository.save(settings);
        return ResponseEntity.ok(Map.of("ceiling", settings.getCreditLimitIncreaseCeiling()));
    }

    @PutMapping("/emi-settings")
    public ResponseEntity<?> updateEmiSettings(@RequestBody Map<String, Object> request) {
        try {
            Number apr = request.get("annualInterestPercent") instanceof Number value ? value : null;
            Number fee = request.get("processingFeePercent") instanceof Number value ? value : null;
            return ResponseEntity.ok(creditCardService.updateEmiSettings(
                    apr == null ? null : apr.doubleValue(),
                    fee == null ? null : fee.doubleValue(),
                    request.get("adminName") == null ? null : String.valueOf(request.get("adminName"))));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/{id}/emi-eligible-transactions")
    public ResponseEntity<?> getEmiEligibleTransactions(@PathVariable Long id, Authentication authentication) {
        if (getOwnedCard(id, authentication).isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        try {
            return ResponseEntity.ok(creditCardService.getEmiEligibleTransactions(id).stream()
                    .map(this::customerTransactionSummary)
                    .toList());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/{id}/emi-conversions")
    public ResponseEntity<?> convertToEmi(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        if (getOwnedCard(id, authentication).isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        try {
            Object rawIds = request.get("transactionIds");
            List<Long> transactionIds = new ArrayList<>();
            if (rawIds instanceof List<?> ids) {
                for (Object transactionId : ids) {
                    if (!(transactionId instanceof Number number)) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Transaction IDs must be numeric"));
                    }
                    transactionIds.add(number.longValue());
                }
            }
            Object rawTenure = request.get("tenureMonths");
            Integer tenure = rawTenure instanceof Number number ? number.intValue() : null;
            return ResponseEntity.ok(creditCardService.convertTransactionsToEmi(id, transactionIds, tenure));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/{id}/emi-plans")
    public ResponseEntity<?> getEmiPlans(@PathVariable Long id, Authentication authentication) {
        if (getOwnedCard(id, authentication).isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        try {
            return ResponseEntity.ok(creditCardService.getEmiPlans(id));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/emi-plans")
    public ResponseEntity<?> getAllEmiPlans() {
        return ResponseEntity.ok(creditCardService.getAllEmiPlans());
    }

    @GetMapping("/account/{accountNumber}/bills")
    public ResponseEntity<List<CreditCardBill>> getBillsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(creditCardService.getBillsByAccount(accountNumber));
    }

    // Pay bill
    @PostMapping("/bills/{billId}/pay")
    public ResponseEntity<?> payBill(
            @PathVariable Long billId,
            @RequestParam Double amount,
            Authentication authentication) {
        CreditCardBill existingBill = creditCardBillRepository.findById(billId).orElse(null);
        if (existingBill == null || getOwnedCard(existingBill.getCreditCardId(), authentication).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        try {
            CreditCardBill bill = creditCardService.payBill(billId, amount);
            return bill != null ? ResponseEntity.ok(customerBillSummary(bill)) : ResponseEntity.notFound().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    // Admin: pay a bill by cash, user account debit, or cheque
    @PostMapping("/bills/{billId}/admin-pay")
    public ResponseEntity<?> adminPayBill(@PathVariable Long billId,
                                          @RequestBody AdminCreditCardPaymentRequest request) {
        try {
            return ResponseEntity.ok(creditCardService.payBillAsAdmin(billId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Admin: verify a cheque number before using it to pay a credit card bill
    @GetMapping("/verify-cheque")
    public ResponseEntity<?> verifyChequeForCardPayment(@RequestParam String chequeNumber,
                                                        @RequestParam String debitAccountNumber) {
        return ResponseEntity.ok(creditCardService.verifyChequeForCardPayment(chequeNumber, debitAccountNumber));
    }

    // Admin: transfer amount from credit card's available limit to any account (savings/current/salary)
    @PostMapping("/{id}/transfer-to-account")
    public ResponseEntity<?> transferToAccount(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String destinationAccountNumber = String.valueOf(request.get("destinationAccountNumber"));
            Double amount = request.get("amount") == null ? null : ((Number) request.get("amount")).doubleValue();
            String adminName = request.get("adminName") == null ? null : String.valueOf(request.get("adminName"));
            return ResponseEntity.ok(creditCardService.transferToAccount(id, destinationAccountNumber, amount, adminName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Get current admin-configurable transfer fee percent
    @GetMapping("/transfer-fee-percent")
    public ResponseEntity<?> getTransferFeePercent() {
        return ResponseEntity.ok(Map.of("transferFeePercent", creditCardService.getTransferFeePercent()));
    }

    // Admin: update the transfer fee percent
    @PutMapping("/transfer-fee-percent")
    public ResponseEntity<?> updateTransferFeePercent(@RequestBody Map<String, Object> request) {
        try {
            double feePercent = ((Number) request.get("transferFeePercent")).doubleValue();
            String updatedBy = request.get("adminName") == null ? null : String.valueOf(request.get("adminName"));
            return ResponseEntity.ok(creditCardService.updateTransferFeePercent(feePercent, updatedBy));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Get statement
    @GetMapping("/{id}/statement")
    public ResponseEntity<?> getStatement(
            @PathVariable Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        if (!canAccessCard(id, authentication)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : LocalDateTime.now();
        return ResponseEntity.ok(creditCardService.getStatement(id, start, end).stream()
                .map(this::customerTransactionSummary)
                .toList());
    }

    // Close credit card
    @PutMapping("/{id}/close")
    public ResponseEntity<?> closeCreditCard(@PathVariable Long id, Authentication authentication) {
        if (!canAccessCard(id, authentication)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        boolean success = creditCardService.closeCreditCard(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("Cannot close credit card with outstanding balance");
    }

    // Calculate overdue and penalties (admin cron job)
    @PostMapping("/calculate-overdue")
    public ResponseEntity<?> calculateOverdue() {
        creditCardService.calculateOverdueAndPenalties();
        return ResponseEntity.ok().build();
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append((int)(Math.random() * 10));
        }
        return sb.toString();
    }
}
