package com.neo.springapp.service;

import com.neo.springapp.model.ChequeBankRange;
import com.neo.springapp.model.ChequeLeaf;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.BusinessChequeBankRangeRepository;
import com.neo.springapp.repository.BusinessChequeLeafRepository;
import com.neo.springapp.repository.ChequeBankRangeRepository;
import com.neo.springapp.repository.ChequeBookClosureHistoryRepository;
import com.neo.springapp.repository.ChequeLeafRepository;
import com.neo.springapp.repository.ChequeRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChequeServiceClosureTest {

    @Mock private ChequeRepository chequeRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private SalaryAccountRepository salaryAccountRepository;
    @Mock private CurrentAccountRepository currentAccountRepository;
    @Mock private ChequeBankRangeRepository chequeBankRangeRepository;
    @Mock private BusinessChequeBankRangeRepository businessChequeBankRangeRepository;
    @Mock private ChequeLeafRepository chequeLeafRepository;
    @Mock private BusinessChequeLeafRepository businessChequeLeafRepository;
    @Mock private ChequeBookClosureHistoryRepository closureHistoryRepository;

    private ChequeService service;

    @BeforeEach
    void setUp() {
        service = new ChequeService(
                chequeRepository,
                accountRepository,
                null,
                null,
                null,
                null,
                null
        );

        org.springframework.test.util.ReflectionTestUtils.setField(service, "salaryAccountRepository", salaryAccountRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "currentAccountRepository", currentAccountRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "chequeBankRangeRepository", chequeBankRangeRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "businessChequeBankRangeRepository", businessChequeBankRangeRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "chequeLeafRepository", chequeLeafRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "businessChequeLeafRepository", businessChequeLeafRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "chequeBookClosureHistoryRepository", closureHistoryRepository);
    }

    @Test
    void closesSalaryChequeBookAndRecordsHistory() {
        SalaryAccount salaryAccount = new SalaryAccount();
        salaryAccount.setId(10L);
        salaryAccount.setAccountNumber("SAL-900");

        ChequeBankRange range = new ChequeBankRange();
        range.setId(1L);
        range.setSalaryAccountId(10L);
        range.setChequeBookNumber("BK-100");
        range.setSerialFrom("000001");
        range.setSerialTo("000030");
        range.setStatus("ACTIVE");

        ChequeLeaf leaf = new ChequeLeaf();
        leaf.setId(1L);
        leaf.setSalaryAccountId(10L);
        leaf.setUserId(90L);
        leaf.setLeafNumber("000001");
        leaf.setStatus("AVAILABLE");

        when(salaryAccountRepository.findByAccountNumber("SAL-900")).thenReturn(salaryAccount);
        when(chequeBankRangeRepository.findBySalaryAccountId(10L)).thenReturn(List.of(range));
        when(chequeLeafRepository.findBySalaryAccountIdOrderByLeafNumberAsc(10L)).thenReturn(List.of(leaf));

        Map<String, Object> result = service.closeChequeBooksForAccount("SAL-900", "admin@neobank.com", "Manual closure");

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(range.getStatus()).isEqualTo("CLOSED");
        assertThat(leaf.getStatus()).isEqualTo("CLOSED");
        assertThat(result.get("closedBooks")).isEqualTo(1);
    }
}
