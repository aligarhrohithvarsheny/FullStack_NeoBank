package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.EmiPaymentRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.HomeLoanRepository;
import com.neo.springapp.repository.LoanRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoanRepaymentServiceTest {
    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final GoldLoanRepository goldLoanRepository = mock(GoldLoanRepository.class);
    private final HomeLoanRepository homeLoanRepository = mock(HomeLoanRepository.class);
    private final EmiPaymentRepository emiPaymentRepository = mock(EmiPaymentRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final SalaryAccountRepository salaryAccountRepository = mock(SalaryAccountRepository.class);
    private final CurrentAccountRepository currentAccountRepository = mock(CurrentAccountRepository.class);
    private final EmiService emiService = mock(EmiService.class);
    private final HomeLoanService homeLoanService = mock(HomeLoanService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final PdfService pdfService = mock(PdfService.class);

    private LoanRepaymentService service;
    private EmiPayment nextEmi;
    private Transaction transaction;

    @BeforeEach
    void setUp() throws Exception {
        service = new LoanRepaymentService(loanRepository, goldLoanRepository, homeLoanRepository,
                emiPaymentRepository, accountRepository, salaryAccountRepository, currentAccountRepository,
                emiService, homeLoanService, accountService, pdfService);

        Loan loan = new Loan();
        loan.setLoanAccountNumber("LN-12345678");
        loan.setAccountNumber("ACC-12345678");
        loan.setUserName("Asha Customer");
        loan.setType("Personal Loan");
        loan.setStatus("Approved");
        when(loanRepository.findByLoanAccountNumber("LN-12345678")).thenReturn(Optional.of(loan));

        Account account = new Account();
        account.setAccountNumber("ACC-12345678");
        account.setDob("1990-01-02");
        account.setName("Asha Customer");
        when(accountRepository.findByAccountNumber("ACC-12345678")).thenReturn(account);

        nextEmi = new EmiPayment();
        nextEmi.setId(9L);
        nextEmi.setLoanAccountNumber("LN-12345678");
        nextEmi.setAccountNumber("ACC-12345678");
        nextEmi.setEmiNumber(1);
        nextEmi.setDueDate(LocalDate.of(2026, 1, 1));
        nextEmi.setTotalAmount(100.0);
        nextEmi.setPrincipalAmount(80.0);
        nextEmi.setRemainingPrincipal(920.0);
        nextEmi.setStatus("Pending");
        EmiPayment laterEmi = new EmiPayment();
        laterEmi.setLoanAccountNumber("LN-12345678");
        laterEmi.setEmiNumber(2);
        laterEmi.setDueDate(LocalDate.of(2026, 2, 1));
        laterEmi.setTotalAmount(100.0);
        laterEmi.setPrincipalAmount(80.0);
        laterEmi.setRemainingPrincipal(840.0);
        laterEmi.setStatus("Pending");
        when(emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc("LN-12345678"))
                .thenReturn(List.of(nextEmi, laterEmi));

        transaction = new Transaction();
        transaction.setTransactionId("TXN-123");
        transaction.setAmount(100.0);
        transaction.setType("Debit");
        transaction.setStatus("Completed");
        transaction.setDate(LocalDateTime.of(2025, 12, 1, 10, 0));
        transaction.setAccountNumber("ACC-12345678");

        when(emiService.payEmi(9L, "ACC-12345678"))
                .thenReturn(Map.of("success", true, "transaction", transaction));
        when(accountService.getBalanceByAccountNumber("ACC-12345678")).thenReturn(900.0);
        when(pdfService.generateLoanPaymentReceipt(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new byte[] { 1, 2 });
    }

    @Test
    void lookupMasksLoanDetailsAndShowsOnlyNextPendingEmi() {
        Map<String, Object> result = service.lookup("LN-12345678", "1990-01-02");

        assertThat(result.get("maskedName")).isEqualTo("A*** C*******");
        assertThat(result.get("maskedLoanAccountNumber")).isEqualTo("*******5678");
        assertThat(result.get("linkedAccountNumber")).isEqualTo("********5678");
        List<?> emis = (List<?>) result.get("emis");
        assertThat(emis).hasSize(1);
        assertThat(((Map<?, ?>) emis.get(0)).get("emiNumber")).isEqualTo(1);
    }

    @Test
    void paymentUsesLoanLinkedAccountAndReturnsMaskedTransactionSummary() throws Exception {
        Map<String, Object> result = service.pay("LN-12345678", "1990-01-02", "EMI", 1, null, null);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("newBalance")).isEqualTo(900.0);
        assertThat(result.get("receiptPdfBase64")).isEqualTo("AQI=");
        Map<?, ?> transactionSummary = (Map<?, ?>) result.get("transaction");
        assertThat(transactionSummary.get("transactionId")).isEqualTo("TXN-123");
        assertThat(transactionSummary.containsKey("accountNumber")).isFalse();
        verify(emiService).payEmi(9L, "ACC-12345678");
        verify(pdfService).generateLoanPaymentReceipt(
                eq("A*** C*******"), eq("*******5678"), eq("Personal Loan"), eq("EMI"),
                eq(100.0), eq(920.0), eq("********5678"), eq("TXN-123"),
                eq(transaction.getDate()), eq(900.0));
    }

    @Test
    void incorrectDateOfBirthDoesNotAttemptPayment() throws Exception {
        assertThatThrownBy(() -> service.pay("LN-12345678", "1990-01-03", "EMI", 1, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Loan details could not be verified.");

        verify(emiService, never()).payEmi(any(), any());
        verify(pdfService, never()).generateLoanPaymentReceipt(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
