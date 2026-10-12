package com.neo.springapp.controller;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.Card;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.EmergencyControl;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.service.CardService;
import com.neo.springapp.service.EmergencyControlService;
import com.neo.springapp.service.UserSessionTokenService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/kill-switches")
public class AdminKillSwitchController {
    private final EmergencyControlService controlService;
    private final AccountRepository accountRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final CardService cardService;

    public AdminKillSwitchController(
            EmergencyControlService controlService,
            AccountRepository accountRepository,
            CurrentAccountRepository currentAccountRepository,
            SalaryAccountRepository salaryAccountRepository,
            CardService cardService) {
        this.controlService = controlService;
        this.accountRepository = accountRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.cardService = cardService;
    }

    @GetMapping
    public Map<String, Object> getControls() {
        Map<String, Object> response = new HashMap<>();
        response.put("rails", EmergencyControlService.SUPPORTED_RAILS.stream().map(rail -> {
            EmergencyControl control = controlService.getControl("RAIL", rail);
            return Map.of(
                    "rail", rail,
                    "paused", control != null && control.isActive(),
                    "reason", control == null || control.getReason() == null ? "" : control.getReason(),
                    "changedBy", control == null || control.getChangedBy() == null ? "" : control.getChangedBy(),
                    "updatedAt", control == null ? "" : control.getUpdatedAt().toString());
        }).toList());
        response.put("isolatedAccounts", controlService.getActiveControls("ACCOUNT"));
        response.put("recentAudit", controlService.getRecentAudit());
        return response;
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> searchAccounts(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(Math.max(size, 1), 50);
        String term = search == null ? "" : search.trim();
        Map<String, Map<String, Object>> matches = new LinkedHashMap<>();

        List<Account> savingsAccounts = term.isEmpty()
                ? accountRepository.findAll(PageRequest.of(0, pageSize)).getContent()
                : accountRepository.searchAccounts(term, PageRequest.of(0, pageSize)).getContent();
        savingsAccounts.forEach(account -> addAccount(
                matches, account.getAccountNumber(), account.getName(), account.getAccountType(),
                account.getStatus(), account.getCustomerId()));

        var currentAccounts = term.isEmpty()
                ? currentAccountRepository.findAll(PageRequest.of(0, pageSize)).getContent()
                : currentAccountRepository.searchAccounts(term, PageRequest.of(0, pageSize)).getContent();
        currentAccounts.forEach(account -> addAccount(
                matches, account.getAccountNumber(), account.getOwnerName(), "CURRENT",
                account.getStatus(), account.getCustomerId()));

        List<SalaryAccount> salaryAccounts = term.isEmpty()
                ? salaryAccountRepository.findAll(PageRequest.of(0, pageSize)).getContent()
                : salaryAccountRepository.search(term).stream().limit(pageSize).toList();
        salaryAccounts.forEach(account -> addAccount(
                matches, account.getAccountNumber(), account.getEmployeeName(), "SALARY",
                account.getStatus(), account.getCustomerId()));

        return ResponseEntity.ok(Map.of("accounts", new ArrayList<>(matches.values())));
    }

    @GetMapping("/cards")
    public ResponseEntity<?> getCards(@RequestParam String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Account number is required.");
        }
        List<Map<String, Object>> cards = cardService.getCardsByAccountNumber(accountNumber.trim()).stream()
                .map(card -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", card.getId());
                    String number = card.getCardNumber();
                    result.put("maskedCardNumber", number == null || number.length() < 4
                            ? "****" : "**** **** **** " + number.substring(number.length() - 4));
                    result.put("cardType", card.getCardType());
                    result.put("status", card.getStatus());
                    result.put("blocked", card.isBlocked());
                    result.put("deactivated", card.isDeactivated());
                    EmergencyControl cardControl = controlService.getControl("CARD", card.getId().toString());
                    result.put("emergencyFrozen", cardControl != null && cardControl.isActive());
                    return result;
                }).toList();
        return ResponseEntity.ok(Map.of("cards", cards));
    }

    @PutMapping("/rails/{rail}")
    public ResponseEntity<?> setRail(@PathVariable String rail, @RequestBody ControlRequest request) {
        return updateControl("RAIL", rail, request, null);
    }

    @PutMapping("/accounts/{accountNumber}")
    public ResponseEntity<?> setAccount(
            @PathVariable String accountNumber, @RequestBody ControlRequest request) {
        if (!accountExists(accountNumber)) {
            return error(HttpStatus.NOT_FOUND, "Account not found in savings, current, or salary accounts.");
        }
        return updateControl("ACCOUNT", accountNumber, request, null);
    }

    @PutMapping("/cards/{cardId}")
    public ResponseEntity<?> setCard(
            @PathVariable Long cardId, @RequestBody ControlRequest request) {
        Card card = cardService.getCardById(cardId);
        if (card == null) {
            return error(HttpStatus.NOT_FOUND, "Card not found.");
        }
        String priorState = card.isBlocked() ? "BLOCKED"
                : card.isDeactivated() ? "DEACTIVATED" : "ACTIVE";
        return updateControl("CARD", cardId.toString(), request, priorState);
    }

    private ResponseEntity<?> updateControl(
            String scopeType, String scopeKey, ControlRequest request, String priorState) {
        if (request == null || request.active() == null || request.reason() == null
                || request.reason().isBlank() || request.reason().length() > 500) {
            return error(HttpStatus.BAD_REQUEST, "active and a reason of 1-500 characters are required.");
        }
        try {
            EmergencyControl control = controlService.setControl(
                    scopeType, scopeKey, request.active(), request.reason(), authenticatedAdmin(), priorState);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "scopeType", control.getScopeType(),
                    "scopeKey", control.getScopeKey(),
                    "active", control.isActive(),
                    "reason", control.getReason(),
                    "changedBy", control.getChangedBy(),
                    "updatedAt", control.getUpdatedAt()));
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    private boolean accountExists(String accountNumber) {
        String number = accountNumber == null ? "" : accountNumber.trim();
        return !number.isEmpty()
                && (accountRepository.findByAccountNumber(number) != null
                || currentAccountRepository.findByAccountNumber(number).isPresent()
                || salaryAccountRepository.findByAccountNumber(number) != null);
    }

    private String authenticatedAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserSessionTokenService.SessionPrincipal principal)
                || !"ADMIN".equals(principal.scope())) {
            throw new IllegalStateException("An authenticated admin session is required.");
        }
        return principal.accountNumber();
    }

    private void addAccount(
            Map<String, Map<String, Object>> results,
            String accountNumber, String name, String accountType, String status, String customerId) {
        if (accountNumber == null || accountNumber.isBlank()
                || (name == null && customerId == null)) return;
        results.putIfAbsent(accountNumber.toUpperCase(Locale.ROOT), Map.of(
                "accountNumber", accountNumber,
                "name", name == null ? "" : name,
                "accountType", accountType == null ? "UNKNOWN" : accountType,
                "status", status == null ? "" : status,
                "customerId", customerId == null ? "" : customerId));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }

    public record ControlRequest(Boolean active, String reason) {}
}
