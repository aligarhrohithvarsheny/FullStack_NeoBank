package com.neo.springapp.controller;

import com.neo.springapp.model.TermDepositPlan;
import com.neo.springapp.repository.TermDepositPlanRepository;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/term-deposit-plans")
public class TermDepositPlanController {
    private final TermDepositPlanRepository repository;

    public TermDepositPlanController(TermDepositPlanRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<?> activePlans(@RequestParam(defaultValue = "FD") String productType) {
        String normalizedType = normalizeType(productType);
        if (normalizedType == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Product type must be FD or RD."));
        }
        return ResponseEntity.ok(repository.findActiveByProductType(normalizedType));
    }

    @GetMapping("/all")
    public ResponseEntity<?> allPlans(Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TermDepositPlan plan, Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        String productType = normalizeType(plan.getProductType());
        if (!validPlan(plan, productType)) return ResponseEntity.badRequest()
                .body(Map.of("message", "Enter a scheme name, valid duration, annual rate, and amount limits."));
        plan.setId(null);
        plan.setProductType(productType);
        plan.setActive(true);
        plan.setCreatedBy(actor(authentication));
        return ResponseEntity.ok(repository.save(plan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TermDepositPlan updates,
                                    Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        String productType = normalizeType(updates.getProductType());
        if (!validPlan(updates, productType)) return ResponseEntity.badRequest()
                .body(Map.of("message", "Enter a scheme name, valid duration, annual rate, and amount limits."));
        return repository.findById(id).map(plan -> {
            plan.setSchemeName(updates.getSchemeName());
            plan.setProductType(productType);
            plan.setDays(updates.getDays());
            plan.setDurationMonths(updates.getDurationMonths());
            plan.setYears(updates.getYears());
            plan.setMinimumAmount(updates.getMinimumAmount());
            plan.setMaximumAmount(updates.getMaximumAmount());
            plan.setInterestRate(updates.getInterestRate());
            plan.setPayoutDate(updates.getPayoutDate());
            plan.setInterestAmount(updates.getInterestAmount());
            plan.setActive(updates.getActive() == null || updates.getActive());
            return ResponseEntity.ok(repository.save(plan));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivate(@PathVariable Long id, Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        return repository.findById(id).map(plan -> {
            plan.setActive(false);
            repository.save(plan);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean validPlan(TermDepositPlan plan, String productType) {
        if (productType == null || plan.getSchemeName() == null || plan.getSchemeName().isBlank()
                || plan.getInterestRate() == null || plan.getInterestRate() < 0
                || plan.getInterestRate() > 100
                || (plan.getMinimumAmount() != null && plan.getMinimumAmount() < 0)
                || (plan.getMaximumAmount() != null && plan.getMinimumAmount() != null
                    && plan.getMaximumAmount() < plan.getMinimumAmount())) {
            return false;
        }
        return "RD".equals(productType)
                ? plan.getDurationMonths() != null && plan.getDurationMonths() > 0
                : (plan.getDays() != null && plan.getDays() > 0)
                    || (plan.getDurationMonths() != null && plan.getDurationMonths() > 0);
    }

    private String normalizeType(String productType) {
        String normalized = productType == null || productType.isBlank()
                ? "FD" : productType.trim().toUpperCase(Locale.ROOT);
        return "FD".equals(normalized) || "RD".equals(normalized) ? normalized : null;
    }

    private boolean isStaff(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionPrincipal principal)) {
            return false;
        }
        return "ADMIN".equals(principal.scope()) || "MANAGER".equals(principal.scope());
    }

    private String actor(Authentication authentication) {
        SessionPrincipal principal = (SessionPrincipal) authentication.getPrincipal();
        return principal.scope() + ":" + principal.userId();
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Staff sign-in required."));
    }
}
