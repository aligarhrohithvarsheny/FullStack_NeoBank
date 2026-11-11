package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.ChequeBookClosureHistory;
import com.neo.springapp.model.CibilReport;
import com.neo.springapp.model.SavingsChequeLeaf;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.BusinessChequeBankRangeRepository;
import com.neo.springapp.repository.BusinessChequeLeafRepository;
import com.neo.springapp.repository.ChequeBookClosureHistoryRepository;
import com.neo.springapp.repository.ChequeBankRangeRepository;
import com.neo.springapp.repository.ChequeLeafRepository;
import com.neo.springapp.repository.CibilReportRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SavingsChequeBankRangeRepository;
import com.neo.springapp.repository.SavingsChequeLeafRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChequeBookIssuanceServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private SalaryAccountRepository salaryAccountRepository;
    @Mock private CurrentAccountRepository currentAccountRepository;
    @Mock private ChequeLeafRepository chequeLeafRepository;
    @Mock private BusinessChequeLeafRepository businessChequeLeafRepository;
    @Mock private SavingsChequeLeafRepository savingsChequeLeafRepository;
    @Mock private ChequeBankRangeRepository chequeBankRangeRepository;
    @Mock private BusinessChequeBankRangeRepository businessChequeBankRangeRepository;
    @Mock private SavingsChequeBankRangeRepository savingsChequeBankRangeRepository;
    @Mock private ChequeBookClosureHistoryRepository closureHistoryRepository;
    @Mock private CibilReportRepository cibilReportRepository;
    @Mock private AccountService accountService;
    @Mock private TransactionService transactionService;

    private ChequeBookIssuanceService service;

    @BeforeEach
    void setUp() {
        service = new ChequeBookIssuanceService();
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "salaryAccountRepository", salaryAccountRepository);
        ReflectionTestUtils.setField(service, "currentAccountRepository", currentAccountRepository);
        ReflectionTestUtils.setField(service, "chequeLeafRepository", chequeLeafRepository);
        ReflectionTestUtils.setField(service, "businessChequeLeafRepository", businessChequeLeafRepository);
        ReflectionTestUtils.setField(service, "savingsChequeLeafRepository", savingsChequeLeafRepository);
        ReflectionTestUtils.setField(service, "chequeBankRangeRepository", chequeBankRangeRepository);
        ReflectionTestUtils.setField(service, "businessChequeBankRangeRepository", businessChequeBankRangeRepository);
        ReflectionTestUtils.setField(service, "savingsChequeBankRangeRepository", savingsChequeBankRangeRepository);
        ReflectionTestUtils.setField(service, "closureHistoryRepository", closureHistoryRepository);
        ReflectionTestUtils.setField(service, "cibilReportRepository", cibilReportRepository);
        ReflectionTestUtils.setField(service, "accountService", accountService);
        ReflectionTestUtils.setField(service, "transactionService", transactionService);
    }

    @Test
    void chargesOneHundredNinetyNineAndIssuesThirtyNumbers() {
        Account account = new Account();
        account.setId(8L);
        account.setAccountNumber("SAV-800");
        when(accountRepository.findById(8L)).thenReturn(Optional.of(account));
        when(savingsChequeLeafRepository.findByAccountIdOrderByLeafNumberAsc(8L)).thenReturn(List.of());
        when(savingsChequeLeafRepository.findByAccountIdAndStatusOrderByLeafNumberAsc(8L, "AVAILABLE"))
                .thenReturn(List.of());
        when(chequeLeafRepository.findAll()).thenReturn(List.of());
        when(businessChequeLeafRepository.findAll()).thenReturn(List.of());
        when(savingsChequeLeafRepository.findAll()).thenReturn(List.of());
        when(accountService.debitBalance("SAV-800", 199.0, "SAVINGS")).thenReturn(801.0);

        var response = service.requestBook(8L, "SAVINGS", "SAV-800");

        assertThat(response.get("charge")).isEqualTo(199.0);
        assertThat(response.get("balanceAfterCharge")).isEqualTo(801.0);
        assertThat(response.get("serialFrom")).isEqualTo("000001");
        assertThat(response.get("serialTo")).isEqualTo("000030");
        ArgumentCaptor<List<SavingsChequeLeaf>> leavesCaptor = ArgumentCaptor.forClass(List.class);
        verify(savingsChequeLeafRepository).saveAll(leavesCaptor.capture());
        assertThat(leavesCaptor.getValue()).hasSize(30);
        assertThat(leavesCaptor.getValue().get(0).getLeafNumber()).isEqualTo("000001");
        assertThat(leavesCaptor.getValue().get(29).getLeafNumber()).isEqualTo("000030");
        verify(savingsChequeBankRangeRepository).save(any());
        verify(transactionService).createTransferTransaction(
                "SAV-800", "Cheque book issuance fee (" + response.get("chequeBookNumber") + ")",
                199.0, "Debit", 801.0);
    }

    @Test
    void doesNotDebitWhenAccountAlreadyHasAvailableNumbers() {
        Account account = new Account();
        account.setId(9L);
        account.setAccountNumber("SAV-900");
        SavingsChequeLeaf available = new SavingsChequeLeaf(9L, 9L, "000001");
        when(accountRepository.findById(9L)).thenReturn(Optional.of(account));
        when(savingsChequeLeafRepository.findByAccountIdOrderByLeafNumberAsc(9L)).thenReturn(List.of(available));
        when(savingsChequeLeafRepository.findByAccountIdAndStatusOrderByLeafNumberAsc(9L, "AVAILABLE"))
                .thenReturn(List.of(available));

        assertThatThrownBy(() -> service.requestBook(9L, "SAVINGS", "SAV-900"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Use the available cheque numbers");
    }

    @Test
    void doesNotDebitAnAccountThatDoesNotMatchTheSignedInUser() {
        Account account = new Account();
        account.setId(10L);
        account.setAccountNumber("SAV-1000");
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.requestBook(10L, "SAVINGS", "SAV-OTHER"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void adminIssuesRequestedLeafCountWithStoredCibilAndAuditsTheCharge() {
        Account account = new Account();
        account.setId(11L);
        account.setAccountNumber("SAV-1100");
        CibilReport report = new CibilReport();
        report.setCibilScore(650);
        when(accountRepository.findByAccountNumber("SAV-1100")).thenReturn(account);
        when(cibilReportRepository.findByLinkedAccountNumber("SAV-1100")).thenReturn(List.of(report));
        when(savingsChequeLeafRepository.findByAccountIdAndStatusOrderByLeafNumberAsc(11L, "AVAILABLE"))
                .thenReturn(List.of());
        when(chequeLeafRepository.findAll()).thenReturn(List.of());
        when(businessChequeLeafRepository.findAll()).thenReturn(List.of());
        when(savingsChequeLeafRepository.findAll()).thenReturn(List.of());
        when(accountService.debitBalance("SAV-1100", 125.5, "SAVINGS")).thenReturn(874.5);

        var response = service.issueBookForAdmin("SAV-1100", 5, 125.5, "admin@example.com");

        assertThat(response.get("leafCount")).isEqualTo(5);
        assertThat(response.get("cibilScore")).isEqualTo(650);
        assertThat(response.get("charge")).isEqualTo(125.5);
        assertThat(response.get("serialFrom")).isEqualTo("000001");
        assertThat(response.get("serialTo")).isEqualTo("000005");
        ArgumentCaptor<List<SavingsChequeLeaf>> leavesCaptor = ArgumentCaptor.forClass(List.class);
        verify(savingsChequeLeafRepository).saveAll(leavesCaptor.capture());
        assertThat(leavesCaptor.getValue()).hasSize(5);
        assertThat(leavesCaptor.getValue()).allMatch(leaf -> "AVAILABLE".equals(leaf.getStatus()));
        ArgumentCaptor<ChequeBookClosureHistory> historyCaptor = ArgumentCaptor.forClass(ChequeBookClosureHistory.class);
        verify(closureHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo("ISSUED");
        assertThat(historyCaptor.getValue().getChargeAmount()).isEqualTo(125.5);
        assertThat(historyCaptor.getValue().getCibilScore()).isEqualTo(650);
        verify(transactionService).createTransferTransaction(
                "SAV-1100", "Admin cheque book issuance fee (" + response.get("chequeBookNumber") + ")",
                125.5, "Debit", 874.5);
    }

    @Test
    void adminRejectsChequeBookWhenStoredCibilIsBelowMinimum() {
        Account account = new Account();
        account.setId(12L);
        account.setAccountNumber("SAV-1200");
        CibilReport report = new CibilReport();
        report.setCibilScore(299);
        when(accountRepository.findByAccountNumber("SAV-1200")).thenReturn(account);
        when(cibilReportRepository.findByLinkedAccountNumber("SAV-1200")).thenReturn(List.of(report));

        assertThatThrownBy(() -> service.issueBookForAdmin("SAV-1200", 10, 199.0, "admin@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 300");
        verify(accountService, org.mockito.Mockito.never()).debitBalance(any(), any(), any());
    }
}
