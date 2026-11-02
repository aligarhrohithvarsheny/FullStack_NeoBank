package com.neo.springapp.controller;

import com.neo.springapp.model.RecurringDeposit;
import com.neo.springapp.service.DepositManagementHistoryService;
import com.neo.springapp.service.RecurringDepositService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recurring-deposits")
public class RecurringDepositController {

    private final RecurringDepositService service;
    private final DepositManagementHistoryService historyService;

    public RecurringDepositController(RecurringDepositService service,
                                     DepositManagementHistoryService historyService) {
        this.service = service;
        this.historyService = historyService;
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(Authentication authentication) {
        SessionPrincipal principal = principal(authentication);
        if (principal == null || !"USER".equals(principal.scope())) return unauthorized();
        return ResponseEntity.ok(service.customerViews(principal.accountNumber()));
    }

    @GetMapping
    public ResponseEntity<?> all(Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        return ResponseEntity.ok(service.adminViews());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequest request, Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        try {
            return ResponseEntity.ok(service.create(request.accountNumber(), request.planId(),
                    request.monthlyInstallment(), request.startDate(), request.remarks(), actor(authentication)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", safeMessage(e)));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateRequest request,
                                    Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        try {
            RecurringDeposit updated = service.update(id, request.monthlyInstallment(),
                    request.tenureMonths(), request.annualInterestRate(), request.startDate(),
                    request.maturityDate(), request.remarks(), request.reason(), actor(authentication));
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", safeMessage(e)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", safeMessage(e)));
        }
    }

    @PostMapping("/{id}/share-link")
    public ResponseEntity<?> issueShareLink(@PathVariable Long id, Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        try {
            return ResponseEntity.ok(service.issueShareLink(id, actor(authentication)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", safeMessage(e)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", safeMessage(e)));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        try {
            return ResponseEntity.ok(service.approve(id, actor(authentication)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", safeMessage(e)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", safeMessage(e)));
        }
    }

    @GetMapping("/share/{token}")
    public ResponseEntity<?> shareDetails(@PathVariable String token) {
        try {
            return ResponseEntity.ok(service.shareDetails(token));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("message", safeMessage(e)));
        }
    }

    @GetMapping("/share/{token}/captcha")
    public ResponseEntity<?> captcha(@PathVariable String token) {
        try {
            return ResponseEntity.ok(service.createCaptcha(token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", safeMessage(e)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("message", safeMessage(e)));
        }
    }

    @PostMapping("/share/{token}/sign")
    public ResponseEntity<?> sign(@PathVariable String token, @RequestBody SignRequest request,
                                  HttpServletRequest servletRequest) {
        try {
            Map<String, Object> response = service.sign(token, request.challengeId(), request.captchaAnswer(),
                    request.signedName(), request.consent(), servletRequest.getRemoteAddr());
            return Boolean.TRUE.equals(response.get("success"))
                    ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", safeMessage(e)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("message", safeMessage(e)));
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> history(@PathVariable Long id, Authentication authentication) {
        RecurringDeposit deposit = service.getById(id);
        if (deposit == null) return ResponseEntity.notFound().build();
        SessionPrincipal principal = principal(authentication);
        if (!isStaff(authentication)
                && (principal == null || !"USER".equals(principal.scope())
                    || !deposit.getAccountNumber().equalsIgnoreCase(principal.accountNumber()))) {
            return forbidden();
        }
        return ResponseEntity.ok(historyService.getHistory("RD", id));
    }

    @GetMapping("/{id}/contributions")
    public ResponseEntity<?> contributions(@PathVariable Long id, Authentication authentication) {
        RecurringDeposit deposit = service.getById(id);
        if (deposit == null) return ResponseEntity.notFound().build();
        SessionPrincipal principal = principal(authentication);
        if (!isStaff(authentication)
                && (principal == null || !"USER".equals(principal.scope())
                    || !deposit.getAccountNumber().equalsIgnoreCase(principal.accountNumber()))) {
            return forbidden();
        }
        return ResponseEntity.ok(service.contributions(id));
    }

    private SessionPrincipal principal(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal
                ? principal : null;
    }

    private boolean isStaff(Authentication authentication) {
        SessionPrincipal principal = principal(authentication);
        return principal != null
                && ("ADMIN".equals(principal.scope()) || "MANAGER".equals(principal.scope()));
    }

    private String actor(Authentication authentication) {
        SessionPrincipal principal = principal(authentication);
        return principal.scope() + ":" + principal.userId();
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "The recurring-deposit operation could not be completed."
                : exception.getMessage();
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Staff sign-in required."));
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Customer sign-in required."));
    }

    public record CreateRequest(String accountNumber, Long planId, Double monthlyInstallment,
                                LocalDate startDate, String remarks) {}

    public record UpdateRequest(Double monthlyInstallment, Integer tenureMonths,
                                Double annualInterestRate, LocalDate startDate,
                                LocalDate maturityDate, String remarks, String reason) {}

    public record SignRequest(String challengeId, String captchaAnswer, String signedName, boolean consent) {}
}
