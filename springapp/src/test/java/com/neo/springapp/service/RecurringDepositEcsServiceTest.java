package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.EcsMandate;
import com.neo.springapp.model.RecurringDeposit;
import com.neo.springapp.repository.EcsMandateEventRepository;
import com.neo.springapp.repository.EcsMandateRepository;
import com.neo.springapp.repository.RecurringDepositContributionRepository;
import com.neo.springapp.repository.RecurringDepositRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringDepositEcsServiceTest {

    @Mock private RecurringDepositRepository recurringDepositRepository;
    @Mock private RecurringDepositContributionRepository contributionRepository;
    @Mock private EcsMandateRepository mandateRepository;
    @Mock private EcsMandateEventRepository eventRepository;
    @Mock private AccountService accountService;
    @Mock private TransactionService transactionService;
    @Mock private DepositManagementHistoryService historyService;
    @InjectMocks private RecurringDepositEcsService service;

    @Test
    void doesNotDebitUntilCustomerHasSeparatelyConsented() {
        LocalDate today = LocalDate.now();
        EcsMandate mandate = mandate(today);
        mandate.setCustomerConsentAt(null);
        RecurringDeposit deposit = deposit(today);
        when(recurringDepositRepository.findByRdAccountNumberForUpdate("RD00000017"))
                .thenReturn(Optional.of(deposit));

        assertFalse(service.debitContribution(mandate, today));
        verify(accountService, never()).debitBalance(any(), any(), any());
    }

    @Test
    void postsOneAuthorizedInstallmentToContributionLedger() {
        LocalDate today = LocalDate.now();
        EcsMandate mandate = mandate(today);
        RecurringDeposit deposit = deposit(today);
        Account account = new Account();
        account.setAccountNumber("123456");
        account.setName("Test Customer");
        account.setAccountType("Savings");
        when(recurringDepositRepository.findByRdAccountNumberForUpdate("RD00000017"))
                .thenReturn(Optional.of(deposit));
        when(contributionRepository.existsByIdempotencyKey("RD17-" + today.getYear() + "-"
                + String.format("%02d", today.getMonthValue()))).thenReturn(false);
        when(accountService.getAccountByNumber("123456")).thenReturn(account);
        when(accountService.debitBalance("123456", 1000.0, "Savings")).thenReturn(4000.0);
        when(contributionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(service.debitContribution(mandate, today));

        assertEquals(1, deposit.getPaidInstallments());
        assertEquals(1000.0, deposit.getPaidPrincipal());
        assertEquals("SUCCESS", mandate.getLastDebitStatus());
        verify(transactionService).saveTransaction(any());
        verify(contributionRepository).save(any());
        verify(historyService).record(any(), any(), any(), any(), any(), any(), any());
    }

    private EcsMandate mandate(LocalDate today) {
        EcsMandate mandate = new EcsMandate();
        mandate.setId(8L);
        mandate.setMandateId("ECS-8");
        mandate.setLoanAccountNumber("RD00000017");
        mandate.setSavingsAccountNumber("123456");
        mandate.setDebitDay(1);
        mandate.setAmountLimit(1000.0);
        mandate.setEmiAmount(1000.0);
        mandate.setStatus("ACTIVE");
        mandate.setCustomerConsentAt(LocalDateTime.of(today.minusMonths(1).getYear(),
                today.minusMonths(1).getMonth(), 1, 10, 0));
        return mandate;
    }

    private RecurringDeposit deposit(LocalDate today) {
        RecurringDeposit deposit = new RecurringDeposit();
        deposit.setId(17L);
        deposit.setRdAccountNumber("RD00000017");
        deposit.setAccountNumber("123456");
        deposit.setMonthlyInstallment(1000.0);
        deposit.setTenureMonths(12);
        deposit.setPaidInstallments(0);
        deposit.setPaidPrincipal(0.0);
        deposit.setStatus("ACTIVE");
        deposit.setStartDate(today.minusMonths(1));
        deposit.setMaturityDate(today.plusMonths(11));
        return deposit;
    }
}
