package com.neo.springapp.controller;

import com.neo.springapp.service.Card360Service;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/card360")
@CrossOrigin(origins = "*")
public class Card360Controller {
    private final Card360Service card360Service;

    public Card360Controller(Card360Service card360Service) {
        this.card360Service = card360Service;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(card360Service.login(request.get("cardNumber"), request.get("email")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/admin/accounts/{accountNumber}/passcode")
    public ResponseEntity<?> generatePasscode(@PathVariable String accountNumber, @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(card360Service.generatePasscode(accountNumber, request.get("adminEmail"), request.get("adminPassword")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PutMapping("/admin/accounts/{accountNumber}/enabled")
    public ResponseEntity<?> setEnabled(@PathVariable String accountNumber, @RequestBody Map<String, String> request) {
        String value = request.get("enabled");
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            return ResponseEntity.badRequest().body(Map.of("message", "enabled must be true or false"));
        }
        try {
            boolean enabled = Boolean.parseBoolean(value);
            return ResponseEntity.ok(card360Service.setEnabled(accountNumber, enabled, request.get("adminEmail"), request.get("adminPassword")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/admin/accounts/{accountNumber}/history")
    public ResponseEntity<?> getHistory(@PathVariable String accountNumber, @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(card360Service.getAuditHistory(accountNumber, request.get("adminEmail"), request.get("adminPassword")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/admin/accounts/{accountNumber}/lookup")
    public ResponseEntity<?> getAdminCustomerDetails(@PathVariable String accountNumber,
                                                      @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(card360Service.getAdminCustomerDetails(accountNumber,
                    request.get("adminEmail"), request.get("adminPassword")));
        } catch (IllegalArgumentException exception) {
            HttpStatus status = "Admin authentication failed".equals(exception.getMessage())
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/admin/closure-requests")
    public ResponseEntity<?> getAdminClosureRequests(@RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(card360Service.getAdminClosureRequests(request.get("accountNumber"),
                    request.get("adminEmail"), request.get("adminPassword")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status("Admin authentication failed".equals(exception.getMessage())
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/admin/closure-requests/{id}/review")
    public ResponseEntity<?> reviewClosureRequest(@PathVariable Long id,
                                                   @RequestBody Map<String, String> request) {
        String decision = request.get("decision");
        if (!"approve".equalsIgnoreCase(decision) && !"reject".equalsIgnoreCase(decision)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Decision must be approve or reject"));
        }
        try {
            return ResponseEntity.ok(card360Service.reviewClosureRequest(id,
                    "approve".equalsIgnoreCase(decision), request.get("reviewNote"),
                    request.get("adminEmail"), request.get("adminPassword")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status("Admin authentication failed".equals(exception.getMessage())
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/cards")
    public ResponseEntity<?> cards(Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.getCustomerCards(principal.accountNumber()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/cards/link")
    public ResponseEntity<?> linkCard(@RequestBody Map<String, String> request, Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.linkCard(principal.accountNumber(), request.get("cardNumber")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PutMapping("/cards/{type}/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String type, @PathVariable Long id,
                                          @RequestBody Map<String, Boolean> request, Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (request.get("blocked") == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "blocked is required"));
        }
        try {
            return ResponseEntity.ok(card360Service.updateCardStatus(principal.accountNumber(), type, id, request.get("blocked")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PutMapping("/cards/credit/{id}/limit")
    public ResponseEntity<?> updateLimit(@PathVariable Long id, @RequestBody Map<String, Number> request,
                                         Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Spending limit is required"));
            }
            Number limit = request.get("spendingLimit");
            if (limit == null) return ResponseEntity.badRequest().body(Map.of("message", "Spending limit is required"));
            return ResponseEntity.ok(card360Service.updateCreditLimit(principal.accountNumber(), id, limit.doubleValue()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PutMapping("/cards/credit/{id}/controls")
    public ResponseEntity<?> updateCardControls(@PathVariable Long id,
                                                @RequestBody Map<String, Boolean> request,
                                                Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.updateCreditCardControls(
                    principal.accountNumber(), id, request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/cards/credit/{id}/emergency-lock")
    public ResponseEntity<?> emergencyLock(@PathVariable Long id, Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.emergencyLockCreditCard(principal.accountNumber(), id));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PutMapping("/cards/credit/{id}/pin")
    public ResponseEntity<?> setCardPin(@PathVariable Long id, @RequestBody Map<String, String> request,
                                        Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            if (request == null || request.get("pin") == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "PIN is required"));
            }
            card360Service.setCreditCardPin(principal.accountNumber(), id, request.get("pin"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/emi-settings")
    public ResponseEntity<?> emiSettings(Authentication authentication) {
        if (card360Principal(authentication) == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(card360Service.getEmiSettingsForCustomer());
    }

    @GetMapping("/cards/credit/{id}/security-events")
    public ResponseEntity<?> cardSecurityEvents(@PathVariable Long id, Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.getCardSecurityEvents(principal.accountNumber(), id));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/cards/credit/{id}/emi-eligible-transactions")
    public ResponseEntity<?> eligibleEmiTransactions(@PathVariable Long id, Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.getEmiEligibleTransactions(principal.accountNumber(), id));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/cards/credit/{id}/emi-plans")
    public ResponseEntity<?> emiPlans(@PathVariable Long id, Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.getEmiPlans(principal.accountNumber(), id));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/cards/credit/{id}/emi-conversions")
    public ResponseEntity<?> convertToEmi(@PathVariable Long id, @RequestBody Map<String, Object> request,
                                          Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (request == null) return ResponseEntity.badRequest().body(Map.of("message", "EMI conversion details are required"));
        List<Long> transactionIds = new ArrayList<>();
        Object rawIds = request.get("transactionIds");
        if (rawIds instanceof List<?> ids) {
            for (Object rawId : ids) {
                if (!(rawId instanceof Number number)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Transaction IDs must be numeric"));
                }
                transactionIds.add(number.longValue());
            }
        }
        Object rawTenure = request.get("tenureMonths");
        Integer tenure = rawTenure instanceof Number number ? number.intValue() : null;
        try {
            return ResponseEntity.ok(card360Service.convertTransactionsToEmi(
                    principal.accountNumber(), id, transactionIds, tenure));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/cards/credit/{id}/number")
    public ResponseEntity<?> revealCreditCardNumber(@PathVariable Long id, Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.revealCreditCardNumber(principal.accountNumber(), id));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/cards/credit/{id}/bills")
    public ResponseEntity<?> creditCardBills(@PathVariable Long id, Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.getCreditCardBills(principal.accountNumber(), id));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/cards/credit/{id}/statement")
    public ResponseEntity<?> creditCardStatement(@PathVariable Long id,
                                                  @RequestParam String startDate,
                                                  @RequestParam String endDate,
                                                  Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.getCreditCardStatement(principal.accountNumber(), id,
                    LocalDate.parse(startDate), LocalDate.parse(endDate)));
        } catch (DateTimeParseException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", "Statement dates must use YYYY-MM-DD format"));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/cards/credit/{id}/closure-requests")
    public ResponseEntity<?> requestCreditCardClosure(@PathVariable Long id,
                                                       @RequestBody Map<String, String> request,
                                                       Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.requestCreditCardClosure(principal.accountNumber(), id,
                    request.get("reason")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/payment-account")
    public ResponseEntity<?> paymentAccount(Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.getPaymentAccount(principal.accountNumber()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/credit-card-bill")
    public ResponseEntity<?> payCreditCardBill(@RequestBody Map<String, Number> request,
                                                Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Number cardId = request.get("creditCardId");
        Number amount = request.get("amount");
        if (cardId == null || amount == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Credit card and payment amount are required"));
        }
        try {
            return ResponseEntity.ok(card360Service.payCreditCardBill(principal.accountNumber(),
                    cardId.longValue(), amount.doubleValue()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/cards/{type}/{id}/transactions")
    public ResponseEntity<?> cardTransactions(@PathVariable String type, @PathVariable Long id,
                                              Authentication authentication) {
        SessionPrincipal principal = card360Principal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(card360Service.getCardTransactions(principal.accountNumber(), type, id));
        } catch (IllegalArgumentException exception) {
            HttpStatus status = "Card not found".equals(exception.getMessage())
                    ? HttpStatus.NOT_FOUND : HttpStatus.FORBIDDEN;
            return ResponseEntity.status(status).body(Map.of("message", exception.getMessage()));
        }
    }

    private SessionPrincipal card360Principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionPrincipal principal)
                || !"CARD360".equals(principal.scope())) return null;
        return principal;
    }
}
