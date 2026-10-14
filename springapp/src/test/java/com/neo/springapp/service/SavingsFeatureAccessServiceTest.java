package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.BankingProductConfig;
import com.neo.springapp.model.EmergencyControl;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.BankingProductConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsFeatureAccessServiceTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private BankingProductConfigRepository productRepository;
    @Mock
    private EmergencyControlService controlService;
    @InjectMocks
    private SavingsFeatureAccessService service;

    @Test
    void assignsApprovedSavingsProductAndRecordsTheChange() {
        Account account = savingsAccount("STANDARD");
        BankingProductConfig premium = savingsProduct("PREMIUM", "accounts");
        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(account);
        when(productRepository
                .findFirstByProductCodeIgnoreCaseAndAccountTypeIgnoreCaseAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(
                        eq("premium"), eq("SAVINGS"), eq("APPROVED"), any()))
                .thenReturn(premium);
        when(productRepository
                .findFirstByProductCodeIgnoreCaseAndAccountTypeIgnoreCaseAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(
                        eq("PREMIUM"), eq("SAVINGS"), eq("APPROVED"), any()))
                .thenReturn(premium);
        when(controlService.getFeatureControls("ACC123:")).thenReturn(List.of());

        var access = service.setSavingsProduct("ACC123", "premium", "Customer upgrade", "admin");

        assertThat(account.getSavingsProductCode()).isEqualTo("PREMIUM");
        assertThat(access.get("productCode")).isEqualTo("PREMIUM");
        verify(accountRepository).save(account);
        verify(controlService).recordSavingsProductChange(
                "ACC123", "STANDARD", "PREMIUM", "Customer upgrade", "admin");
    }

    @Test
    void doesNotAssignAnUnapprovedSavingsProduct() {
        Account account = savingsAccount("STANDARD");
        BankingProductConfig pending = savingsProduct("PREMIUM", "accounts");
        pending.setStatus("PENDING_APPROVAL");
        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(account);
        when(productRepository
                .findFirstByProductCodeIgnoreCaseAndAccountTypeIgnoreCaseAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(
                        eq("PREMIUM"), eq("SAVINGS"), eq("APPROVED"), any()))
                .thenReturn(null);

        assertThatThrownBy(() -> service.setSavingsProduct(
                "ACC123", "PREMIUM", "Customer upgrade", "admin"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(account.getSavingsProductCode()).isEqualTo("STANDARD");
    }

    @Test
    void accessSnapshotAppliesAccountOverridesToProductDefaultsInOneBatch() {
        Account account = savingsAccount("PREMIUM");
        BankingProductConfig product = savingsProduct("PREMIUM", "accounts");
        EmergencyControl disabledAccounts = new EmergencyControl();
        disabledAccounts.setScopeKey("ACC123:ACCOUNTS");
        disabledAccounts.setActive(true);
        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(account);
        when(productRepository
                .findFirstByProductCodeIgnoreCaseAndAccountTypeIgnoreCaseAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescCreatedAtDesc(
                        eq("PREMIUM"), eq("SAVINGS"), eq("APPROVED"), any()))
                .thenReturn(product);
        when(controlService.getFeatureControls("ACC123:")).thenReturn(List.of(disabledAccounts));

        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> features =
                (List<java.util.Map<String, Object>>) service.getFeatureAccess("ACC123").get("features");

        assertThat(features).filteredOn(feature -> "accounts".equals(feature.get("id")))
                .singleElement()
                .satisfies(feature -> {
                    assertThat(feature.get("enabled")).isEqualTo(false);
                    assertThat(feature.get("inherited")).isEqualTo(false);
                });
        assertThat(features).filteredOn(feature -> "ai-assistant".equals(feature.get("id")))
                .singleElement()
                .satisfies(feature -> {
                    assertThat(feature.get("enabled")).isEqualTo(false);
                    assertThat(feature.get("inherited")).isEqualTo(true);
                });
        verify(controlService).getFeatureControls("ACC123:");
    }

    @Test
    void featureCheckUsesTheNormalizedAccountOverride() {
        Account account = savingsAccount("PREMIUM");
        EmergencyControl disabledAccounts = new EmergencyControl();
        disabledAccounts.setActive(true);
        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(account);
        when(controlService.getControl("FEATURE", "ACC123:ACCOUNTS")).thenReturn(disabledAccounts);

        assertThat(service.isFeatureEnabled("ACC123", "accounts")).isFalse();
    }

    @Test
    void featureCheckLeavesNonSavingsAccountsUnaffected() {
        Account account = savingsAccount("PREMIUM");
        account.setAccountType("CURRENT");
        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(account);

        assertThat(service.isFeatureEnabled("ACC123", "accounts")).isTrue();
    }

    private Account savingsAccount(String productCode) {
        Account account = new Account();
        account.setAccountNumber("ACC123");
        account.setAccountType("Savings");
        account.setSavingsProductCode(productCode);
        return account;
    }

    private BankingProductConfig savingsProduct(String productCode, String... features) {
        BankingProductConfig product = new BankingProductConfig();
        product.setProductCode(productCode);
        product.setAccountType("SAVINGS");
        product.setStatus("APPROVED");
        product.setEffectiveFrom(LocalDate.now().minusDays(1));
        product.setEnabledFeatures(List.of(features));
        return product;
    }
}
