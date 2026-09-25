package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.Cheque;
import com.neo.springapp.model.DemandDraft;
import com.neo.springapp.model.EducationLoanSubsidyClaim;
import com.neo.springapp.model.FixedDeposit;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.ChequeRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.DemandDraftRepository;
import com.neo.springapp.repository.EducationLoanSubsidyClaimRepository;
import com.neo.springapp.repository.FixedDepositRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.LoanRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSearchServiceBarcodeTest {

    @Mock private AccountRepository accountRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private ChequeRepository chequeRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private CardRepository cardRepository;
    @Mock private SalaryAccountRepository salaryAccountRepository;
    @Mock private CurrentAccountRepository currentAccountRepository;
    @Mock private GoldLoanRepository goldLoanRepository;
    @Mock private FixedDepositRepository fixedDepositRepository;
    @Mock private DemandDraftRepository demandDraftRepository;
    @Mock private EducationLoanSubsidyClaimRepository educationLoanSubsidyClaimRepository;

    private AdminSearchService service;

    @BeforeEach
    void setUp() {
        service = new AdminSearchService();
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "loanRepository", loanRepository);
        ReflectionTestUtils.setField(service, "chequeRepository", chequeRepository);
        ReflectionTestUtils.setField(service, "transactionRepository", transactionRepository);
        ReflectionTestUtils.setField(service, "cardRepository", cardRepository);
        ReflectionTestUtils.setField(service, "salaryAccountRepository", salaryAccountRepository);
        ReflectionTestUtils.setField(service, "currentAccountRepository", currentAccountRepository);
        ReflectionTestUtils.setField(service, "goldLoanRepository", goldLoanRepository);
        ReflectionTestUtils.setField(service, "fixedDepositRepository", fixedDepositRepository);
        ReflectionTestUtils.setField(service, "demandDraftRepository", demandDraftRepository);
        ReflectionTestUtils.setField(service, "educationLoanSubsidyClaimRepository", educationLoanSubsidyClaimRepository);
    }

    @Test
    void searchByBarcodeReturnsAllRelatedAccountDetails() {
        Account account = new Account();
        account.setAccountNumber("ACC-1020");
        account.setName("Asha Nair");
        account.setPhone("9000000000");
        account.setBalance(25000.0);
        account.setStatus("ACTIVE");
        account.setBarcodeNumber("8439");
        account.setCustomerId("CUST-1020");

        Loan loan = new Loan();
        loan.setAccountNumber("ACC-1020");

        GoldLoan goldLoan = new GoldLoan();
        goldLoan.setAccountNumber("ACC-1020");

        FixedDeposit fixedDeposit = new FixedDeposit();
        fixedDeposit.setAccountNumber("ACC-1020");

        DemandDraft demandDraft = new DemandDraft();
        demandDraft.setAccountNumber("ACC-1020");

        Cheque cheque = new Cheque();
        cheque.setAccountNumber("ACC-1020");

        Transaction transaction = new Transaction();
        transaction.setAccountNumber("ACC-1020");

        EducationLoanSubsidyClaim claim = new EducationLoanSubsidyClaim();
        claim.setAccountNumber("ACC-1020");

        when(accountRepository.findByBarcodeNumber("8439")).thenReturn(account);
        when(loanRepository.findByAccountNumber("ACC-1020")).thenReturn(List.of(loan));
        when(cardRepository.findByAccountNumber("ACC-1020")).thenReturn(List.of());
        when(chequeRepository.findByAccountNumber("ACC-1020")).thenReturn(List.of(cheque));
        when(transactionRepository.findByAccountNumberOrderByDateDesc(any(), any())).thenReturn(new PageImpl<>(List.of(transaction)));
        when(goldLoanRepository.findByAccountNumber("ACC-1020")).thenReturn(List.of(goldLoan));
        when(fixedDepositRepository.findByAccountNumber("ACC-1020")).thenReturn(List.of(fixedDeposit));
        when(demandDraftRepository.findByAccountNumberOrderByCreatedAtDesc("ACC-1020")).thenReturn(List.of(demandDraft));
        when(educationLoanSubsidyClaimRepository.findByAccountNumber("ACC-1020")).thenReturn(List.of(claim));

        Map<String, Object> result = service.searchByBarcode("8439");

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("count")).isEqualTo(1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches = (List<Map<String, Object>>) result.get("matches");
        Map<String, Object> match = matches.get(0);

        assertThat(match.get("name")).isEqualTo("Asha Nair");
        assertThat(match.get("accountNumber")).isEqualTo("ACC-1020");
        assertThat(((List<?>) match.get("loans")).size()).isEqualTo(1);
        assertThat(((List<?>) match.get("goldLoans")).size()).isEqualTo(1);
        assertThat(((List<?>) match.get("fixedDeposits")).size()).isEqualTo(1);
        assertThat(((List<?>) match.get("demandDrafts")).size()).isEqualTo(1);
        assertThat(((List<?>) match.get("cheques")).size()).isEqualTo(1);
        assertThat(((List<?>) match.get("subsidyClaims")).size()).isEqualTo(1);
        assertThat(match.get("passbookUrl")).isEqualTo("/api/passbook/generate/ACC-1020?accountType=savings");
    }
}
