package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.BranchAccount;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.repository.BranchAccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.AllocationAccountRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationAccountLinkingServiceTest {

    @Mock private AllocationAccountRepository allocationAccountRepository;
    @Mock private FundsAllocationRepository fundsAllocationRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private AccountRepository savingsAccountRepository;
    @Mock private CurrentAccountRepository currentAccountRepository;
    @Mock private BranchAccountRepository branchAccountRepository;

    private AllocationAccountLinkingService service;

    @BeforeEach
    void setUp() {
        service = new AllocationAccountLinkingService();
        ReflectionTestUtils.setField(service, "accountRepository", allocationAccountRepository);
        ReflectionTestUtils.setField(service, "allocationRepository", fundsAllocationRepository);
        ReflectionTestUtils.setField(service, "adminRepository", adminRepository);
        ReflectionTestUtils.setField(service, "savingsAccountRepository", savingsAccountRepository);
        ReflectionTestUtils.setField(service, "currentAccountRepository", currentAccountRepository);
        ReflectionTestUtils.setField(service, "branchAccountRepository", branchAccountRepository);
    }

    @Test
    void verifiesOnlySupportedIfscCodes() {
        assertThat(service.verifyIfscCode("neob0000001").get("bankName")).isEqualTo("NeoBank");
        assertThat(service.verifyIfscCode("EZYV000123").get("bankName")).isEqualTo("ExyVault");
        assertThatThrownBy(() -> service.verifyIfscCode("SBIN0001234"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only NeoBank or ExyVault");
    }

    @Test
    void verifiesNeoBankInternalAccountAndReturnsCanonicalDetails() {
        Account bankAccount = new Account();
        bankAccount.setAccountNumber("NEOBANK000001");
        bankAccount.setName("NeoBank Official");
        bankAccount.setStatus("ACTIVE");
        when(savingsAccountRepository.findByAccountNumber("NEOBANK000001")).thenReturn(bankAccount);

        Map<String, Object> result = service.verifyInternalAccount("neobank000001", "neob0000001");

        assertThat(result).containsEntry("success", true);
        assertThat(result).containsEntry("accountHolderName", "NeoBank Official");
        assertThat(result).containsEntry("accountType", "CURRENT");
    }

    @Test
    void rejectsAnyAccountNumberOtherThanRegisteredNeoBankAccount() {
        assertThatThrownBy(() -> service.verifyInternalAccount("OTHER12345", "NEOB0000001"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("NeoBank's registered internal or configured branch account");
    }

    @Test
    void verifiesConfiguredNeoBankBranchAccount() {
        BranchAccount branchAccount = new BranchAccount();
        branchAccount.setAccountNumber("ACC1787636100964194");
        branchAccount.setAccountName("NeoBank Official Branch");
        when(branchAccountRepository.findAll()).thenReturn(java.util.List.of(branchAccount));

        Account internalAccount = new Account();
        internalAccount.setAccountNumber("ACC1787636100964194");
        internalAccount.setName("NeoBank Official Branch");
        internalAccount.setStatus("ACTIVE");
        when(savingsAccountRepository.findByAccountNumber("ACC1787636100964194")).thenReturn(internalAccount);

        Map<String, Object> result = service.verifyInternalAccount("ACC1787636100964194", "NEOB0000001");

        assertThat(result).containsEntry("success", true);
        assertThat(result).containsEntry("accountHolderName", "NeoBank Official Branch");
    }

    @Test
    void verifiesOnlyActiveExyVaultCurrentAccounts() {
        CurrentAccount exyVaultAccount = new CurrentAccount();
        exyVaultAccount.setAccountNumber("EXYV000456789");
        exyVaultAccount.setIfscCode("EZYV000123");
        exyVaultAccount.setOwnerName("ExyVault Treasury");
        exyVaultAccount.setStatus("ACTIVE");
        when(currentAccountRepository.findByAccountNumber("EXYV000456789"))
            .thenReturn(Optional.of(exyVaultAccount));

        Map<String, Object> result = service.verifyInternalAccount("EXYV000456789", "EZYV000123");

        assertThat(result).containsEntry("success", true);
        assertThat(result).containsEntry("bankName", "ExyVault");
        assertThat(result).containsEntry("accountHolderName", "ExyVault Treasury");
    }
}