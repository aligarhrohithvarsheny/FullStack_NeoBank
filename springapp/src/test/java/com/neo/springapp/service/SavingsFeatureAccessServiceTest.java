package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.BankingProductConfig;
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
    void accessSnapshotKeepsEveryFeatureEnabledDespiteProductDefaultsAndOldOverrides() {
        Account account = savingsAccount("PREMIUM");
        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(account);

        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> features =
                (List<java.util.Map<String, Object>>) service.getFeatureAccess("ACC123").get("features");

        assertThat(features).hasSize(com.neo.springapp.model.SavingsFeature.IDS.size())
                .allSatisfy(feature -> {
                    assertThat(feature.get("enabled")).isEqualTo(true);
                    assertThat(feature.get("inherited")).isEqualTo(true);
                });
    }

    @Test
    void featureCheckIgnoresHistoricalDisabledOverrides() {
        assertThat(service.isFeatureEnabled("ACC123", "accounts")).isTrue();
    }

    @Test
    void adminsCannotDisableSavingsFeatures() {
        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(savingsAccount("PREMIUM"));

        assertThatThrownBy(() -> service.setFeatureEnabled(
                "ACC123", "accounts", false, "No reason", "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Savings account features cannot be disabled.");
    }

    @Test
    void featureCheckLeavesNonSavingsAccountsUnaffected() {
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
