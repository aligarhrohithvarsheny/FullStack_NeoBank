package com.neo.springapp.service;

import com.neo.springapp.model.Cheque;
import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.DemandDraft;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.Loan;
import com.neo.springapp.repository.AccountConversionRequestRepository;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AdminAuditLogRepository;
import com.neo.springapp.repository.ChequeRepository;
import com.neo.springapp.repository.CreditCardRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.DemandDraftRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.LoanRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountConversionServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private SalaryAccountRepository salaryAccountRepository;
    @Mock private CurrentAccountRepository currentAccountRepository;
    @Mock private AccountConversionRequestRepository conversionRepository;
    @Mock private AdminAuditLogRepository adminAuditLogRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private GoldLoanRepository goldLoanRepository;
    @Mock private CreditCardRepository creditCardRepository;
    @Mock private DemandDraftRepository demandDraftRepository;
    @Mock private ChequeRepository chequeRepository;

    private AccountConversionService service;

    @BeforeEach
    void setUp() {
        service = new AccountConversionService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "salaryAccountRepository", salaryAccountRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "currentAccountRepository", currentAccountRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "conversionRepository", conversionRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "adminAuditLogRepository", adminAuditLogRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "loanRepository", loanRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "goldLoanRepository", goldLoanRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "creditCardRepository", creditCardRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "demandDraftRepository", demandDraftRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "chequeRepository", chequeRepository);
    }

    @Test
    void supportsSavingsToSalaryConversion() {
        assertThat(AccountConversionService.isSupportedConversion("Savings", "Salary")).isTrue();
    }

    @Test
    void rejectsUnsupportedAccountTypeMappings() {
        assertThat(AccountConversionService.isSupportedConversion("Savings", "Current")).isFalse();
    }

    @Test
    void normalizesAccountTypeNames() {
        assertThat(AccountConversionService.normalizeSourceType("salary")).isEqualTo("Salary");
        assertThat(AccountConversionService.normalizeTargetType("joint")).isEqualTo("Joint");
    }

    @Test
    void blocksSavingsToSalaryConversionWhenDebtProductsAreOpen() {
        Loan loan = new Loan();
        loan.setStatus("Approved");
        loan.setRemainingPrincipal(15000.0);
        GoldLoan goldLoan = new GoldLoan();
        goldLoan.setStatus("Approved");
        goldLoan.setRemainingPrincipal(12000.0);
        CreditCard card = new CreditCard();
        card.setStatus("Active");
        card.setCurrentBalance(5000.0);
        Cheque cheque = new Cheque();
        cheque.setStatus("ACTIVE");
        DemandDraft draft = new DemandDraft();
        draft.setStatus("PENDING");

        when(loanRepository.findByAccountNumber("SAV-100")).thenReturn(List.of(loan));
        when(goldLoanRepository.findByAccountNumber("SAV-100")).thenReturn(List.of(goldLoan));
        when(creditCardRepository.findByAccountNumber("SAV-100")).thenReturn(List.of(card));
        when(chequeRepository.findByAccountNumber("SAV-100")).thenReturn(List.of(cheque));
        when(demandDraftRepository.findByAccountNumberOrderByCreatedAtDesc("SAV-100")).thenReturn(List.of(draft));

        List<String> blockers = service.validateSavingsToSalaryConversion("SAV-100");

        assertThat(blockers).isNotEmpty();
        assertThat(blockers.toString()).contains("Outstanding loan");
        assertThat(blockers.toString()).contains("credit/debit card debt remains");
    }
}
