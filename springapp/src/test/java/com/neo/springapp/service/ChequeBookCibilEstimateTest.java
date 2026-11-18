package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CibilReportRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChequeBookCibilEstimateTest {

    @Test
    void generatesClearlyLabeledEstimateWithoutPersistingIt() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        SalaryAccountRepository salaryAccountRepository = mock(SalaryAccountRepository.class);
        CurrentAccountRepository currentAccountRepository = mock(CurrentAccountRepository.class);
        CibilReportRepository cibilReportRepository = mock(CibilReportRepository.class);
        CreditScorePredictorService predictor = mock(CreditScorePredictorService.class);
        ChequeBookIssuanceService service = new ChequeBookIssuanceService();

        Account account = new Account();
        account.setPan("abcde1234f");
        account.setIncome(240_000d);
        when(accountRepository.findByAccountNumber("ACC-1")).thenReturn(account);
        when(predictor.predictCibilFromPanAndIncome("ABCDE1234F", 240_000d)).thenReturn(701);
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "salaryAccountRepository", salaryAccountRepository);
        ReflectionTestUtils.setField(service, "currentAccountRepository", currentAccountRepository);
        ReflectionTestUtils.setField(service, "cibilReportRepository", cibilReportRepository);
        ReflectionTestUtils.setField(service, "creditScorePredictorService", predictor);

        Map<String, Object> estimate = service.generateCibilEstimate(" ACC-1 ");

        assertEquals(701, estimate.get("estimatedScore"));
        assertEquals("SIMULATED_PAN_INCOME_ESTIMATE", estimate.get("scoreSource"));
        assertTrue(((String) estimate.get("scoreNotice")).contains("display only"));
        verify(predictor).predictCibilFromPanAndIncome("ABCDE1234F", 240_000d);
        verifyNoInteractions(cibilReportRepository);
    }

    @Test
    void requiresValidPanForTheEstimate() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        ChequeBookIssuanceService service = new ChequeBookIssuanceService();
        when(accountRepository.findByAccountNumber("ACC-1")).thenReturn(new Account());
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);

        assertThrows(IllegalStateException.class, () -> service.generateCibilEstimate("ACC-1"));
    }
}
