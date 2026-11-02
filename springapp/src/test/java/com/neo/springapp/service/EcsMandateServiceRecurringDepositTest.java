package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.EcsMandate;
import com.neo.springapp.model.RecurringDeposit;
import com.neo.springapp.repository.EcsMandateEventRepository;
import com.neo.springapp.repository.EcsMandateRepository;
import com.neo.springapp.repository.RecurringDepositRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcsMandateServiceRecurringDepositTest {

    @Mock private EcsMandateRepository mandateRepository;
    @Mock private EcsMandateEventRepository eventRepository;
    @Mock private RecurringDepositRepository recurringDepositRepository;
    @Mock private AccountService accountService;
    @Mock private RecurringDepositEcsService recurringDepositEcsService;
    @Mock private EmiService emiService;
    @InjectMocks private EcsMandateService service;

    @Test
    void approvedRdCreatesPendingConsentWithoutImmediateDebit() {
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:4200");
        RecurringDeposit rd = new RecurringDeposit();
        rd.setId(17L);
        rd.setRdAccountNumber("RD00000017");
        rd.setAccountNumber("123456");
        rd.setCustomerName("Test Customer");
        rd.setMonthlyInstallment(1000.0);
        rd.setTenureMonths(12);
        rd.setPaidInstallments(0);
        rd.setEstimatedInterest(660.0);
        rd.setEstimatedMaturityAmount(12660.0);
        rd.setStatus("APPROVED");
        Account account = new Account();
        account.setAccountNumber("123456");
        account.setName("Test Customer");
        account.setDob("1990-01-01");
        when(recurringDepositRepository.findByRdAccountNumber("RD00000017")).thenReturn(Optional.of(rd));
        when(mandateRepository.existsByLoanAccountNumberAndStatusIn(eq("RD00000017"), anyList())).thenReturn(false);
        when(mandateRepository.save(any(EcsMandate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountService.getAccountByNumber("123456")).thenReturn(account);

        Map<String, Object> response = service.link(Map.of(
                "loanAccountNumber", "RD00000017",
                "savingsAccountNumber", "123456",
                "dob", "1990-01-01",
                "debitDay", 5,
                "amountLimit", 1000), "ADMIN:1");

        assertTrue(Boolean.TRUE.equals(response.get("success")));
        assertTrue(response.get("consentUrl").toString().contains("/ecs-consent/"));
        assertEquals("PENDING_CONSENT", ((EcsMandate) response.get("mandate")).getStatus());
        verify(emiService, never()).payEmi(any(), any());
        verify(recurringDepositEcsService, never()).debitContribution(any(), any());
        verify(eventRepository).save(any());
    }
}
