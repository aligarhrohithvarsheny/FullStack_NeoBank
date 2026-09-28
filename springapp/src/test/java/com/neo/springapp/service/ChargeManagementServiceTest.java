package com.neo.springapp.service;

import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.entity.ChargeTransaction;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AllocationAccountRepository;
import com.neo.springapp.repository.BusinessTransactionRepository;
import com.neo.springapp.repository.ChargeTransactionRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SalaryNormalTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeManagementServiceTest {

    @Mock private ChargeTransactionRepository chargeRepository;
    @Mock private FundsAllocationRepository allocationRepository;
    @Mock private AllocationAccountRepository allocationAccountRepository;
    @Mock private AccountService accountService;
    @Mock private TransactionService transactionService;
    @Mock private AccountRepository savingsAccountRepository;
    @Mock private CurrentAccountRepository currentAccountRepository;
    @Mock private SalaryAccountRepository salaryAccountRepository;
    @Mock private BusinessTransactionRepository businessTransactionRepository;
    @Mock private SalaryNormalTransactionRepository salaryTransactionRepository;

    private ChargeManagementService service;
    private FundsAllocation allocation;
    private AllocationAccount destination;

    @BeforeEach
    void setUp() {
        service = new ChargeManagementService();
        ReflectionTestUtils.setField(service, "chargeRepository", chargeRepository);
        ReflectionTestUtils.setField(service, "allocationRepository", allocationRepository);
        ReflectionTestUtils.setField(service, "accountRepository", allocationAccountRepository);
        ReflectionTestUtils.setField(service, "accountService", accountService);
        ReflectionTestUtils.setField(service, "transactionService", transactionService);
        ReflectionTestUtils.setField(service, "savingsAccountRepository", savingsAccountRepository);
        ReflectionTestUtils.setField(service, "currentAccountRepository", currentAccountRepository);
        ReflectionTestUtils.setField(service, "salaryAccountRepository", salaryAccountRepository);
        ReflectionTestUtils.setField(service, "businessTransactionRepository", businessTransactionRepository);
        ReflectionTestUtils.setField(service, "salaryTransactionRepository", salaryTransactionRepository);

        allocation = new FundsAllocation();
        allocation.setId(10L);
        allocation.setAllocationAccountId(20L);
        allocation.setChargeManagementEnabled(true);
        allocation.setAccountStatus("VERIFIED");

        destination = new AllocationAccount();
        destination.setId(20L);
        destination.setAllocationId(10L);
        destination.setAccountStatus("ACTIVE");
        destination.setVerificationStatus("VERIFIED");
        destination.setCurrentBalance(new BigDecimal("100.00"));
        destination.setTotalCredited(BigDecimal.ZERO);
        destination.setTotalChargesCollected(BigDecimal.ZERO);

        when(allocationRepository.findById(10L)).thenReturn(Optional.of(allocation));
        when(allocationAccountRepository.findById(20L)).thenReturn(Optional.of(destination));
        lenient().when(chargeRepository.save(any(ChargeTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void debitsSavingsAccountAndCreditsOnlyLinkedAllocationAccount() {
        Account source = new Account();
        source.setAccountNumber("SAV123456");
        source.setName("Customer");
        when(savingsAccountRepository.findByAccountNumber("SAV123456")).thenReturn(source);
        when(accountService.debitBalance("SAV123456", 25.0)).thenReturn(75.0);

        ChargeTransaction charge = process("SAV123456");

        assertThat(charge.getStatus()).isEqualTo("SUCCESS");
        assertThat(destination.getCurrentBalance()).isEqualByComparingTo("125.00");
        assertThat(destination.getTotalCredited()).isEqualByComparingTo("25.00");
        assertThat(destination.getTotalChargesCollected()).isEqualByComparingTo("25.00");
        verify(transactionService).saveTransaction(any());
        verify(allocationAccountRepository).save(destination);
    }

    @Test
    void debitsCurrentAccountAndRecordsBusinessTransaction() {
        CurrentAccount source = new CurrentAccount();
        source.setAccountNumber("CUR123456");
        source.setStatus("ACTIVE");
        source.setBalance(100.0);
        when(savingsAccountRepository.findByAccountNumber("CUR123456")).thenReturn(null);
        when(currentAccountRepository.findByAccountNumber("CUR123456")).thenReturn(Optional.of(source));

        process("CUR123456");

        assertThat(source.getBalance()).isEqualTo(75.0);
        verify(businessTransactionRepository).save(any());
    }

    @Test
    void debitsSalaryAccountAndRecordsSalaryTransaction() {
        SalaryAccount source = new SalaryAccount();
        source.setAccountNumber("SAL123456");
        source.setStatus("Active");
        source.setBalance(100.0);
        when(savingsAccountRepository.findByAccountNumber("SAL123456")).thenReturn(null);
        when(currentAccountRepository.findByAccountNumber("SAL123456")).thenReturn(Optional.empty());
        when(salaryAccountRepository.findByAccountNumber("SAL123456")).thenReturn(source);

        process("SAL123456");

        assertThat(source.getBalance()).isEqualTo(75.0);
        verify(salaryTransactionRepository).save(any());
    }

    @Test
    void doesNotCreditAllocationWhenUserAccountCannotBeDebited() {
        when(savingsAccountRepository.findByAccountNumber("MISSING123")).thenReturn(null);
        when(currentAccountRepository.findByAccountNumber("MISSING123")).thenReturn(Optional.empty());
        when(salaryAccountRepository.findByAccountNumber("MISSING123")).thenReturn(null);

        assertThatThrownBy(() -> process("MISSING123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User account was not found");

        assertThat(destination.getCurrentBalance()).isEqualByComparingTo("100.00");
        verify(chargeRepository, never()).save(any(ChargeTransaction.class));
    }

    private ChargeTransaction process(String userAccountNumber) {
        return service.processCharge(
            10L,
            "UPI",
            "UPI fee",
            new BigDecimal("25.00"),
            userAccountNumber,
            "Customer",
            "UPI",
            null,
            null,
            null
        );
    }
}