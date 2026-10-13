package com.neo.springapp.controller;

import com.neo.springapp.dto.BankingProductConfigRequest;
import com.neo.springapp.dto.BankingProductRejectionRequest;
import com.neo.springapp.model.BankingProductConfig;
import com.neo.springapp.model.SavingsFeature;
import com.neo.springapp.repository.BankingProductConfigRepository;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/banking-products")
public class BankingProductConfigController {

    private final BankingProductConfigRepository repository;

    public BankingProductConfigController(BankingProductConfigRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<BankingProductConfig> getAllVersions() {
        return repository.findAllByOrderByProductCodeAscEffectiveFromDescCreatedAtDesc();
    }

    @GetMapping("/pending")
    public List<BankingProductConfig> getPendingApprovals() {
        return repository.findByStatusOrderByCreatedAtDesc("PENDING_APPROVAL");
    }

    @GetMapping("/active")
    public List<BankingProductConfig> getActiveProducts() {
        LocalDate today = LocalDate.now();
        Map<String, BankingProductConfig> latestEffectiveVersions = repository
                .findAllByOrderByProductCodeAscEffectiveFromDescCreatedAtDesc()
                .stream()
                .filter(product -> "APPROVED".equals(product.getStatus()))
                .filter(product -> !product.getEffectiveFrom().isAfter(today))
                .collect(Collectors.toMap(
                        product -> product.getProductCode().toUpperCase(Locale.ROOT),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        return List.copyOf(latestEffectiveVersions.values());
    }

    @PostMapping
    public ResponseEntity<BankingProductConfig> propose(@RequestBody BankingProductConfigRequest request) {
        validate(request);
        if (repository.existsByProductCodeIgnoreCaseAndEffectiveFromAndStatus(
                request.productCode().trim(), request.effectiveFrom(), "APPROVED")
                || repository.existsByProductCodeIgnoreCaseAndEffectiveFromAndStatus(
                request.productCode().trim(), request.effectiveFrom(), "PENDING_APPROVAL")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A product version already exists for this code and effective date.");
        }

        BankingProductConfig product = new BankingProductConfig();
        product.setProductCode(request.productCode().trim().toUpperCase(Locale.ROOT));
        product.setProductName(request.productName().trim());
        product.setAccountType(request.accountType().trim().toUpperCase(Locale.ROOT));
        product.setInterestRate(request.interestRate());
        product.setMinimumBalance(request.minimumBalance());
        product.setDailyTransactionLimit(request.dailyTransactionLimit());
        product.setMonthlyTransactionLimit(request.monthlyTransactionLimit());
        product.setServiceCharge(request.serviceCharge());
        product.setEligibility(request.eligibility().trim());
        List<String> enabledFeatures = request.enabledFeatures() == null
                ? SavingsFeature.IDS
                : request.enabledFeatures().stream()
                        .map(feature -> feature == null ? "" : feature.trim().toLowerCase(Locale.ROOT))
                        .distinct()
                        .toList();
        if (!SavingsFeature.ID_SET.containsAll(enabledFeatures)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more savings features are invalid.");
        }
        product.setEnabledFeatures(enabledFeatures);
        product.setEffectiveFrom(request.effectiveFrom());
        product.setRequestedBy(request.requestedBy().trim());
        product.setStatus("PENDING_APPROVAL");
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(product));
    }

    @PutMapping("/{id}/approve")
    @Transactional
    public BankingProductConfig approve(@PathVariable @NonNull Long id, @RequestParam String approvedBy) {
        BankingProductConfig product = findPending(id);
        if (approvedBy == null || approvedBy.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approver name is required.");
        }
        product.setStatus("APPROVED");
        product.setApprovedBy(approvedBy.trim());
        product.setApprovedAt(LocalDateTime.now());
        return repository.save(product);
    }

    @PutMapping("/{id}/reject")
    @Transactional
    public BankingProductConfig reject(
            @PathVariable @NonNull Long id, @RequestBody BankingProductRejectionRequest request) {
        BankingProductConfig product = findPending(id);
        if (request == null || request.rejectedBy() == null || request.rejectedBy().isBlank()
                || request.reason() == null || request.reason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Rejecting admin and rejection reason are required.");
        }
        product.setStatus("REJECTED");
        product.setApprovedBy(request.rejectedBy().trim());
        product.setRejectionReason(request.reason().trim());
        product.setApprovedAt(LocalDateTime.now());
        return repository.save(product);
    }

    private BankingProductConfig findPending(@NonNull Long id) {
        BankingProductConfig product = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product version not found."));
        if (!"PENDING_APPROVAL".equals(product.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending product versions can be reviewed.");
        }
        return product;
    }

    private void validate(BankingProductConfigRequest request) {
        if (request == null
                || blank(request.productCode()) || blank(request.productName())
                || blank(request.accountType()) || blank(request.eligibility())
                || request.effectiveFrom() == null || blank(request.requestedBy())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product code, name, account type, eligibility, effective date, and requesting admin are required.");
        }
        String accountType = request.accountType().trim().toUpperCase(Locale.ROOT);
        if (!"SAVINGS".equals(accountType) && !"CURRENT".equals(accountType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Account type must be SAVINGS or CURRENT.");
        }
        if (!validAmount(request.interestRate()) || !validAmount(request.minimumBalance())
                || !validAmount(request.dailyTransactionLimit())
                || !validAmount(request.monthlyTransactionLimit()) || !validAmount(request.serviceCharge())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Rates, balances, limits, and charges must be finite non-negative numbers.");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean validAmount(Double value) {
        return value != null && Double.isFinite(value) && value >= 0;
    }
}
