package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.BankingProductConfig;
import com.neo.springapp.model.SavingsFeature;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.BankingProductConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        List<String> defaultFeatures = getDefaultFeatures(account.getSavingsProductCode());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accountNumber", account.getAccountNumber());
        response.put("productCode", account.getSavingsProductCode());
        response.put("features", SavingsFeature.IDS.stream().map(feature -> {
            var override = controlService.getControl("FEATURE", featureKey(accountNumber, feature));
            boolean inherited = override == null;
            boolean enabled = inherited
                    ? defaultFeatures.contains(feature)
                    : !override.isActive();
            return Map.of("id", feature, "enabled", enabled, "inherited", inherited);
        }).toList());
        return response;
    }

    public boolean isFeatureEnabled(String accountNumber, String feature) {
        String featureId = normalizeFeature(feature);
        Account account = requireSavingsAccount(accountNumber);
        var override = controlService.getControl("FEATURE", featureKey(accountNumber, featureId));
        if (override != null) return !override.isActive();
        return getDefaultFeatures(account.getSavingsProductCode()).contains(featureId);
    }

    public boolean isSavingsAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber == null ? "" : accountNumber.trim());
        return account != null && "SAVINGS".equalsIgnoreCase(account.getAccountType());
    }

    public void setFeatureEnabled(String accountNumber, String feature, boolean enabled, String reason, String changedBy) {
        requireSavingsAccount(accountNumber);
        String featureId = normalizeFeature(feature);
        controlService.setControl("FEATURE", featureKey(accountNumber, featureId), !enabled, reason, changedBy);
    }

    private List<String> getDefaultFeatures(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return SavingsFeature.IDS;
        }
        LocalDate today = LocalDate.now();
        return productRepository.findAllByOrderByProductCodeAscEffectiveFromDescCreatedAtDesc().stream()
                .filter(product -> product.getProductCode().equalsIgnoreCase(productCode))
                .filter(product -> "SAVINGS".equalsIgnoreCase(product.getAccountType()))
                .filter(product -> "APPROVED".equalsIgnoreCase(product.getStatus()))
                .filter(product -> product.getEffectiveFrom() != null && !product.getEffectiveFrom().isAfter(today))
                .findFirst()
                .map(BankingProductConfig::getEnabledFeatures)
                .map(features -> features == null ? SavingsFeature.IDS : features)
                .orElse(List.of());
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
        return accountNumber.trim().toUpperCase(Locale.ROOT) + ":" + feature;
    }

    private String normalizeFeature(String feature) {
        String normalized = feature == null ? "" : feature.trim().toLowerCase(Locale.ROOT);
        if (!SavingsFeature.ID_SET.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown savings account feature.");
        }
        return normalized;
    }
}
