package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.BankingProductConfig;
import com.neo.springapp.model.EmergencyControl;
import com.neo.springapp.model.SavingsFeature;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.BankingProductConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SavingsFeatureAccessService {
    private final AccountRepository accountRepository;
    private final BankingProductConfigRepository productRepository;
    private final EmergencyControlService controlService;

    public SavingsFeatureAccessService(
            AccountRepository accountRepository,
            BankingProductConfigRepository productRepository,
            EmergencyControlService controlService) {
        this.accountRepository = accountRepository;
        this.productRepository = productRepository;
        this.controlService = controlService;
    }

    public Map<String, Object> getFeatureAccess(String accountNumber) {
        Account account = requireSavingsAccount(accountNumber);
        Map<String, Boolean> defaultFeatures = getDefaultFeatures(account.getSavingsProductCode());
        Map<String, EmergencyControl> overrides = controlService
                .getFeatureControls(featureKeyPrefix(account.getAccountNumber())).stream()
                .collect(Collectors.toMap(
                        control -> control.getScopeKey().toUpperCase(Locale.ROOT),
                        Function.identity(),
                        (first, ignored) -> first));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accountNumber", account.getAccountNumber());
        response.put("productCode", account.getSavingsProductCode());
        response.put("features", SavingsFeature.IDS.stream().map(feature -> {
            var override = overrides.get(featureKey(account.getAccountNumber(), feature));
            boolean inherited = override == null;
            boolean enabled = inherited
                    ? defaultFeatures.getOrDefault(feature, false)
                    : !override.isActive();
            Map<String, Object> featureAccess = new LinkedHashMap<>();
            featureAccess.put("id", feature);
            featureAccess.put("enabled", enabled);
            featureAccess.put("inherited", inherited);
            featureAccess.put("reason", inherited
                    ? "Using product defaults"
                    : override.getReason() == null || override.getReason().isBlank()
                            ? "No reason recorded"
                            : override.getReason());
            return featureAccess;
        }).toList());
        return response;
    }

    public boolean isFeatureEnabled(String accountNumber, String feature) {
        String featureId = normalizeFeature(feature);
        Account account = accountRepository.findByAccountNumber(accountNumber == null ? "" : accountNumber.trim());
        if (account == null || !"SAVINGS".equalsIgnoreCase(account.getAccountType())) {
            return true;
        }
        var override = controlService.getControl("FEATURE", featureKey(account.getAccountNumber(), featureId));
        if (override != null) return !override.isActive();
        return getDefaultFeatures(account.getSavingsProductCode()).getOrDefault(featureId, false);
    }

    public boolean isSavingsAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber == null ? "" : accountNumber.trim());
        return account != null && "SAVINGS".equalsIgnoreCase(account.getAccountType());
    }

    @Transactional
    public Map<String, Object> setSavingsProduct(
            String accountNumber, String productCode, String reason, String changedBy) {
        Account account = requireSavingsAccount(accountNumber);
        String normalizedCode = productCode == null ? "" : productCode.trim();
        if (normalizedCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A savings product must be selected.");
        }
        LocalDate today = LocalDate.now();
        BankingProductConfig product = productRepository
                .findFirstByProductCodeIgnoreCaseAndAccountTypeIgnoreCaseAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(
                        normalizedCode, "SAVINGS", "APPROVED", today);
        if (product == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The selected savings product is not active.");
        }

        String previousProductCode = account.getSavingsProductCode();
        account.setSavingsProductCode(product.getProductCode());
        accountRepository.save(account);
        controlService.recordSavingsProductChange(
                account.getAccountNumber(), previousProductCode, product.getProductCode(), reason, changedBy);
        return getFeatureAccess(account.getAccountNumber());
    }

    public void setFeatureEnabled(String accountNumber, String feature, boolean enabled, String reason, String changedBy) {
        requireSavingsAccount(accountNumber);
        String featureId = normalizeFeature(feature);
        controlService.setControl("FEATURE", featureKey(accountNumber, featureId), !enabled, reason, changedBy);
    }

    private Map<String, Boolean> getDefaultFeatures(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return SavingsFeature.IDS.stream().collect(Collectors.toMap(
                    Function.identity(), ignored -> true, (first, ignored) -> first, LinkedHashMap::new));
        }
        LocalDate today = LocalDate.now();
        BankingProductConfig product = productRepository
                .findFirstByProductCodeIgnoreCaseAndAccountTypeIgnoreCaseAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(
                        productCode, "SAVINGS", "APPROVED", today);
        List<String> enabledFeatures = product == null || product.getEnabledFeatures() == null
                ? List.of() : product.getEnabledFeatures();
        return SavingsFeature.IDS.stream().collect(Collectors.toMap(
                Function.identity(), enabledFeatures::contains, (first, ignored) -> first, LinkedHashMap::new));
    }

    private Account requireSavingsAccount(String accountNumber) {
        String normalized = accountNumber == null ? "" : accountNumber.trim();
        Account account = accountRepository.findByAccountNumber(normalized);
        if (account == null || !"SAVINGS".equalsIgnoreCase(account.getAccountType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings account not found.");
        }
        return account;
    }

    private String featureKey(String accountNumber, String feature) {
        return accountNumber.trim().toUpperCase(Locale.ROOT) + ":"
                + feature.toUpperCase(Locale.ROOT);
    }

    private String featureKeyPrefix(String accountNumber) {
        return accountNumber.trim().toUpperCase(Locale.ROOT) + ":";
    }

    private String normalizeFeature(String feature) {
        String normalized = feature == null ? "" : feature.trim().toLowerCase(Locale.ROOT);
        if (!SavingsFeature.ID_SET.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown savings account feature.");
        }
        return normalized;
    }
}
