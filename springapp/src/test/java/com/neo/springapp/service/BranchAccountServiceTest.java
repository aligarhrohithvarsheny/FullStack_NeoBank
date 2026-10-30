package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.Admin;
import com.neo.springapp.model.BranchAccount;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.repository.BranchAccountRepository;
import com.neo.springapp.repository.BranchDailyAllocationRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchAccountServiceTest {

    @Mock private BranchAccountRepository branchAccountRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private AccountService accountService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private BranchDailyAllocationRepository allocationRepository;
    @Mock private CurrentAccountRepository currentAccountRepository;
    @Mock private SalaryAccountRepository salaryAccountRepository;

    private BranchAccountService service;
    private Account neoBankAccount;

    @BeforeEach
    void setUp() {
        service = new BranchAccountService();
        ReflectionTestUtils.setField(service, "branchAccountRepository", branchAccountRepository);
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "adminRepository", adminRepository);
        ReflectionTestUtils.setField(service, "accountService", accountService);
        ReflectionTestUtils.setField(service, "transactionRepository", transactionRepository);
        ReflectionTestUtils.setField(service, "allocationRepository", allocationRepository);
        ReflectionTestUtils.setField(service, "currentAccountRepository", currentAccountRepository);
        ReflectionTestUtils.setField(service, "salaryAccountRepository", salaryAccountRepository);

        neoBankAccount = new Account();
        neoBankAccount.setAccountNumber(BranchAccountService.DEFAULT_NEOBANK_ACCOUNT);
        neoBankAccount.setName("NeoBank Official");
        neoBankAccount.setStatus("ACTIVE");
        neoBankAccount.setBalance(100.0);
    }

    @Test
    void exposesOnlyTheVerifiedNeoBankTreasuryAccount() {
        when(accountRepository.findByAccountNumber(BranchAccountService.DEFAULT_NEOBANK_ACCOUNT))
            .thenReturn(neoBankAccount);

        var accounts = service.getAvailableTreasuryAccounts();

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0))
            .containsEntry("accountNumber", BranchAccountService.DEFAULT_NEOBANK_ACCOUNT)
            .containsEntry("accountName", "NeoBank Official");
    }

    @Test
    void refusesToLinkAnyOtherAccountNumber() {
        var result = service.setBranchAccount("CUSTOMER123", "NeoBank Customer", null, 1L);

        assertThat(result).containsEntry("success", false);
        verify(accountRepository, never()).findByAccountNumber("CUSTOMER123");
        verify(branchAccountRepository, never()).save(any());
    }

    @Test
    void ignoresLegacyMappingsToNonTreasuryAccounts() {
        when(accountRepository.findByAccountNumber(BranchAccountService.DEFAULT_NEOBANK_ACCOUNT)).thenReturn(null);
        when(accountService.getBalanceByAccountNumber(BranchAccountService.DEFAULT_NEOBANK_ACCOUNT)).thenReturn(0.0);
        when(transactionRepository.summarizeCentralLedger(BranchAccountService.DEFAULT_NEOBANK_ACCOUNT, null))
                .thenReturn(new Object[] {0.0, 0.0, 0.0, null});

        var summary = service.getBranchAccountSummary();

        assertThat(summary)
            .containsEntry("accountNumber", BranchAccountService.DEFAULT_NEOBANK_ACCOUNT)
            .containsEntry("isConfigured", false);
    }

    @Test
    void recordsTreasuryDebitsAndCreditsWithUpdatedBalances() {
        when(accountRepository.findByAccountNumber("CUSTOMER123")).thenReturn(null);
        when(accountRepository.findByAccountNumber(BranchAccountService.DEFAULT_NEOBANK_ACCOUNT))
            .thenReturn(neoBankAccount);

        assertThat(service.recordTreasuryMovement(35.0, false, "Loan Sanction", "Loan funded", "CUSTOMER123"))
            .isEqualTo(65.0);
        assertThat(service.recordTreasuryMovement(20.0, true, "Loan EMI", "EMI collected", "CUSTOMER123"))
            .isEqualTo(85.0);

        ArgumentCaptor<Transaction> transactions = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(transactions.capture());
        assertThat(transactions.getAllValues())
            .extracting(Transaction::getType)
            .containsExactly("Debit", "Credit");
        assertThat(transactions.getAllValues())
            .extracting(Transaction::getBalance)
            .containsExactly(65.0, 85.0);
        assertThat(neoBankAccount.getBalance()).isEqualTo(85.0);
    }

    @Test
    void rejectsLoanDisbursementWhenTreasuryFundsAreInsufficient() {
        neoBankAccount.setBalance(50.0);
        when(accountRepository.findByAccountNumber(BranchAccountService.DEFAULT_NEOBANK_ACCOUNT))
            .thenReturn(neoBankAccount);

        assertThatThrownBy(() -> service.recordTreasuryMovement(75.0, false, "Loan", "Sanction", "CUSTOMER123"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Insufficient NeoBank treasury funds");

        assertThat(neoBankAccount.getBalance()).isEqualTo(50.0);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void routesTreasuryMovementsToTheCustomerAssignedBranch() {
        Account customer = new Account();
        customer.setAccountNumber("CUSTOMER123");
        customer.setBranchCity("Tirupati");
        when(accountRepository.findByAccountNumber("CUSTOMER123")).thenReturn(customer);
        when(accountRepository.findByAccountNumber(BranchAccountService.DEFAULT_NEOBANK_ACCOUNT))
            .thenReturn(neoBankAccount);

        assertThat(service.recordTreasuryMovement(35.0, false, "Loan Sanction", "Loan funded", "CUSTOMER123"))
            .isEqualTo(65.0);
        assertThat(service.recordTreasuryMovement(20.0, true, "Loan EMI", "EMI collected", "CUSTOMER123"))
            .isEqualTo(85.0);

        ArgumentCaptor<Transaction> transactions = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(transactions.capture());
        assertThat(transactions.getAllValues())
            .extracting(Transaction::getAccountNumber)
            .containsExactly(BranchAccountService.DEFAULT_NEOBANK_ACCOUNT,
                    BranchAccountService.DEFAULT_NEOBANK_ACCOUNT);
        assertThat(transactions.getAllValues())
            .extracting(Transaction::getBalance)
            .containsExactly(65.0, 85.0);
        assertThat(transactions.getAllValues()).extracting(Transaction::getBranchCity)
                .containsExactly("Tirupati", "Tirupati");
        assertThat(transactions.getAllValues()).extracting(Transaction::getSourceAccountNumber)
                .containsExactly(BranchAccountService.DEFAULT_NEOBANK_ACCOUNT, "CUSTOMER123");
        assertThat(transactions.getAllValues()).extracting(Transaction::getRecipientAccountNumber)
                .containsExactly("CUSTOMER123", BranchAccountService.DEFAULT_NEOBANK_ACCOUNT);
        assertThat(neoBankAccount.getBalance()).isEqualTo(85.0);
    }

    @Test
    void enablesManagerBranchAccessWithoutCreatingOrFundingAnotherAccount() {
        Admin manager = new Admin();
        manager.setId(7L);
        manager.setName("Branch Manager");
        manager.setEmail("manager@neobank.test");
        manager.setRole("MANAGER");
        manager.setAssignedCity("Tirupati");
        when(adminRepository.findById(7L)).thenReturn(Optional.of(manager));
        when(accountRepository.findByAccountNumber(BranchAccountService.DEFAULT_NEOBANK_ACCOUNT))
                .thenReturn(neoBankAccount);

        var result = service.createBranchAccount(7L, 1L, 0.0);

        assertThat(result).containsEntry("success", true);
        @SuppressWarnings("unchecked")
        var branchDetails = (java.util.Map<String, Object>) result.get("branchAccount");
        assertThat(branchDetails)
                .containsEntry("branchCity", "Tirupati")
                .containsEntry("managerAdminId", 7L)
                .containsEntry("bankName", "NeoBank")
                .containsEntry("balance", 100.0)
                .containsEntry("sharedCentralAccount", true);
        assertThat(neoBankAccount.getBalance()).isEqualTo(100.0);
        verify(branchAccountRepository, never()).save(any(BranchAccount.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
