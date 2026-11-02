package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.RecurringDeposit;
import com.neo.springapp.model.TermDepositPlan;
import com.neo.springapp.repository.RecurringDepositRepository;
import com.neo.springapp.repository.TermDepositPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringDepositServiceTest {

    @Mock private RecurringDepositRepository repository;
    @Mock private TermDepositPlanRepository planRepository;
    @Mock private AccountService accountService;
    @Mock private DepositManagementHistoryService historyService;
    @InjectMocks private RecurringDepositService service;

    @Test
    void createsDetailsOnlyEstimateWithoutMovingFunds() {
        Account account = new Account();
        account.setName("Test Customer");
        TermDepositPlan plan = new TermDepositPlan();
        plan.setId(3L);
        plan.setProductType("RD");
        plan.setSchemeName("Monthly Saver");
        plan.setDurationMonths(12);
        plan.setInterestRate(12.0);
        plan.setMinimumAmount(1000.0);
        plan.setMaximumAmount(10000.0);
        plan.setActive(true);

        when(accountService.getAccountByNumber("123456")).thenReturn(account);
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));
        when(repository.save(any(RecurringDeposit.class))).thenAnswer(invocation -> {
            RecurringDeposit saved = invocation.getArgument(0);
            saved.setId(17L);
            return saved;
        });

        RecurringDeposit result = service.create("123456", 3L, 1000.0,
                LocalDate.of(2026, 1, 1), "metadata only", "ADMIN:1");

        double expectedMaturity = 1000.0 * (Math.pow(1.01, 12) - 1) / 0.01;
        assertEquals("DETAILS_ONLY", result.getStatus());
        assertEquals("Test Customer", result.getCustomerName());
        assertEquals(12000.0, result.getEstimatedPrincipal());
        assertEquals(Math.round(expectedMaturity * 100.0) / 100.0, result.getEstimatedMaturityAmount());
        assertEquals(Math.round((expectedMaturity - 12000.0) * 100.0) / 100.0,
                result.getEstimatedInterest());
        verify(accountService).getAccountByNumber("123456");
        verifyNoMoreInteractions(accountService);
        verify(historyService).record(eq("RD"), eq(17L), eq("123456"), eq("DETAILS_CREATED"),
                eq("ADMIN:1"), eq("Admin-created record; no funds moved."), any());
    }

    @Test
    void issuesReviewLinkBeforeTermsCanBeAcknowledged() {
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:4200");
        RecurringDeposit rd = new RecurringDeposit();
        rd.setId(17L);
        rd.setStatus("DETAILS_ONLY");
        rd.setAccountNumber("123456");
        rd.setMonthlyInstallment(1000.0);
        rd.setTenureMonths(12);
        rd.setAnnualInterestRate(12.0);
        rd.setMaturityDate(LocalDate.of(2027, 1, 1));
        when(repository.findById(17L)).thenReturn(Optional.of(rd));
        when(repository.save(any(RecurringDeposit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.issueShareLink(17L, "ADMIN:1");

        assertEquals("PENDING_SIGNATURE", rd.getStatus());
        assertEquals(64, rd.getShareTokenHash().length());
        assertTrue(result.get("shareUrl").toString().contains("/rd-review/"));
        verify(historyService).record(eq("RD"), eq(17L), eq("123456"), eq("REVIEW_LINK_ISSUED"),
                eq("ADMIN:1"), any(), any());
    }

    @Test
    void adminApprovalRequiresCustomerAcknowledgement() {
        RecurringDeposit rd = new RecurringDeposit();
        rd.setId(17L);
        rd.setStatus("PENDING_SIGNATURE");
        when(repository.findById(17L)).thenReturn(Optional.of(rd));

        assertThrows(IllegalStateException.class, () -> service.approve(17L, "ADMIN:1"));
    }
}
