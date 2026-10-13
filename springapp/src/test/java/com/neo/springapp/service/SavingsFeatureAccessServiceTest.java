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
import static org.mockito.ArgumentMatchers.anyString;
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
        when(productRepository.findAllByOrderByProductCodeAscEffectiveFromDescCreatedAtDesc())
                .thenReturn(List.of(premium));
        when(controlService.getControl(anyString(), anyString())).thenReturn(null);

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
        when(productRepository.findAllByOrderByProductCodeAscEffectiveFromDescCreatedAtDesc())
                .thenReturn(List.of(pending));

        assertThatThrownBy(() -> service.setSavingsProduct(
                "ACC123", "PREMIUM", "Customer upgrade", "admin"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(account.getSavingsProductCode()).isEqualTo("STANDARD");
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
