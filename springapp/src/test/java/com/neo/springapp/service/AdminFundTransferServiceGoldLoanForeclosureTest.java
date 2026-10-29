package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.AdminFundTransfer;
import com.neo.springapp.model.AllocationUtilization;
import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.GoldLoanHistory;
import com.neo.springapp.model.SavingsChequeAuditLog;
import com.neo.springapp.model.SavingsChequeRequest;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AdminFundTransferRepository;
import com.neo.springapp.repository.AllocationUtilizationRepository;
import com.neo.springapp.repository.BusinessChequeRequestRepository;
import com.neo.springapp.repository.ChequeRepository;
import com.neo.springapp.repository.ChequeRequestRepository;
import com.neo.springapp.repository.EmiPaymentRepository;
import com.neo.springapp.repository.GoldLoanHistoryRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.PositivePayRequestRepository;
import com.neo.springapp.repository.SavingsChequeAuditLogRepository;
import com.neo.springapp.repository.SavingsChequeRequestRepository;
import com.neo.springapp.repository.SalaryNormalTransactionRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFundTransferServiceGoldLoanForeclosureTest {
    @Mock private AdminFundTransferRepository transferRepository;
    @Mock private AllocationUtilizationRepository allocationUtilizationRepository;
    @Mock private AllocationUtilizationService allocationUtilizationService;
    @Mock private AccountService accountService;
    @Mock private AccountRepository accountRepository;
    @Mock private GoldLoanRepository goldLoanRepository;
    @Mock private EmiPaymentRepository emiPaymentRepository;
    @Mock private GoldLoanHistoryRepository goldLoanHistoryRepository;
    @Mock private GoldLoanService goldLoanService;
    @Mock private TransactionService transactionService;
    @Mock private SalaryNormalTransactionRepository salaryNormalTransactionRepository;
    @Mock private ChequeRepository chequeRepository;
    @Mock private ChequeRequestRepository chequeRequestRepository;
    @Mock private BusinessChequeRequestRepository businessChequeRequestRepository;
    @Mock private SavingsChequeRequestRepository savingsChequeRequestRepository;
    @Mock private SavingsChequeAuditLogRepository savingsChequeAuditLogRepository;
    @Mock private PositivePayRequestRepository positivePayRequestRepository;

    @InjectMocks private AdminFundTransferService service;

    @Test
    void foreclosesOnlyFromTheLoanLinkedAccountAndRecordsTheDebit() {
        GoldLoan loan = new GoldLoan();
        loan.setId(8L);
        loan.setLoanAccountNumber("GL-1008");
        loan.setAccountNumber("LINKED-2001");
        loan.setUserName("Customer");
        loan.setStatus("Approved");
        Account account = new Account();
        account.setAccountNumber("LINKED-2001");
        account.setName("Customer");
        account.setStatus("ACTIVE");
        account.setBalance(5000.0);
        when(goldLoanRepository.findByLoanAccountNumber("GL-1008")).thenReturn(Optional.of(loan));
        when(accountService.verifyAccountByNumber("LINKED-2001")).thenReturn(Map.of(
                "found", true, "status", "ACTIVE", "accountType", "Savings Account", "name", "Customer"));
        when(accountRepository.findByAccountNumber("LINKED-2001")).thenReturn(account);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc("GL-1008")).thenReturn(List.of());
        when(goldLoanService.calculateForeclosure("GL-1008")).thenReturn(Map.of(
                "success", true, "totalForeclosureAmount", 1000.0, "remainingPrincipal", 800.0,
                "remainingInterest", 100.0, "foreclosureCharges", 50.0, "gst", 9.0));
        when(goldLoanService.processForeclosureWithAmount("GL-1008", "Admin", 1000.0)).thenReturn(loan);
        AllocationUtilization originalDebit = new AllocationUtilization();
        originalDebit.setAllocationId(6L);
        originalDebit.setProductType("GOLD_LOAN");
        originalDebit.setTransactionType("DEBIT");
        originalDebit.setPerformedByAdminId(4L);
        when(allocationUtilizationRepository.findByLinkedTransactionId("GOLD-LOAN-GL-1008"))
                .thenReturn(List.of(originalDebit));
        when(allocationUtilizationService.creditAllocationFunds(6L, 1000.0, "GOLD_LOAN", "LINKED-2001",
                "Customer", "GOLD-CLOSURE-GL-1008", "Gold loan foreclosure repayment GL-1008", 4L))
                .thenReturn(Map.of("success", true));
        when(transferRepository.save(any(AdminFundTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = service.processGoldLoanForeclosure("GL-1008", null, null, "Admin");

        assertEquals(true, result.get("success"));
        assertEquals(4000.0, result.get("newBalance"));
        assertEquals(4000.0, account.getBalance());
        ArgumentCaptor<AdminFundTransfer> transfer = ArgumentCaptor.forClass(AdminFundTransfer.class);
        verify(transferRepository).save(transfer.capture());
        assertEquals("LINKED-2001", transfer.getValue().getSenderAccountNumber());
        assertEquals("GL-1008", transfer.getValue().getLoanAccountNumber());
        assertEquals("FORECLOSURE", transfer.getValue().getLoanPaymentType());
        verify(allocationUtilizationService).creditAllocationFunds(6L, 1000.0, "GOLD_LOAN", "LINKED-2001",
                "Customer", "GOLD-CLOSURE-GL-1008", "Gold loan foreclosure repayment GL-1008", 4L);
        ArgumentCaptor<Transaction> statement = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionService).saveTransaction(statement.capture());
        assertEquals("LINKED-2001", statement.getValue().getAccountNumber());
        assertEquals("Debit", statement.getValue().getType());
        assertEquals(1000.0, statement.getValue().getAmount());
    }

    @Test
    void chequePrepaymentUpdatesAccountStatementAndConsumesReservedCheque() {
        ReflectionTestUtils.setField(service, "positivePayMinimumAmount", new java.math.BigDecimal("10000"));
        GoldLoan loan = new GoldLoan();
        loan.setId(8L);
        loan.setLoanAccountNumber("GL-1008");
        loan.setAccountNumber("LINKED-2001");
        loan.setUserName("Customer");
        loan.setInterestRate(12.0);
        loan.setStatus("Approved");
        Account account = new Account();
        account.setId(81L);
        account.setAccountNumber("LINKED-2001");
        account.setName("Customer");
        account.setStatus("ACTIVE");
        account.setBalance(5000.0);
        SavingsChequeRequest cheque = new SavingsChequeRequest();
        cheque.setId(91L);
        cheque.setAccountId(account.getId());
        cheque.setChequeNumber("GL-CHEQUE-91");
        cheque.setChequePurpose("GOLD_LOAN_PREPAYMENT");
        cheque.setGoldLoanAccountNumber("GL-1008");
        cheque.setAmount(new java.math.BigDecimal("100.00"));
        cheque.setStatus("PENDING");
        EmiPayment firstEmi = pendingEmi(1, 500.0, 50.0);
        EmiPayment secondEmi = pendingEmi(2, 500.0, 50.0);
        List<EmiPayment> schedule = List.of(firstEmi, secondEmi);
        when(goldLoanRepository.findByLoanAccountNumber("GL-1008")).thenReturn(Optional.of(loan));
        when(goldLoanRepository.save(any(GoldLoan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountService.verifyAccountByNumber("LINKED-2001")).thenReturn(Map.of(
                "found", true, "status", "ACTIVE", "accountType", "Savings Account", "name", "Customer"));
        when(accountRepository.findByAccountNumber("LINKED-2001")).thenReturn(account);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(savingsChequeRequestRepository.findAllByChequeNumber("GL-CHEQUE-91")).thenReturn(List.of(cheque));
        when(savingsChequeRequestRepository.save(any(SavingsChequeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc("GL-1008")).thenReturn(schedule);
        when(emiPaymentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(goldLoanService.calculateForeclosure("GL-1008")).thenReturn(Map.of("success", true));
        when(transferRepository.save(any(AdminFundTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = service.processTransfer("LINKED-2001", "GL-CHEQUE-91", "GL-1008",
                100.0, "Gold loan prepayment by cheque", "Admin", "GOLD_LOAN", "PREPAYMENT",
                "GL-1008", null, "REDUCE_EMI");

        assertEquals(true, result.get("success"));
        assertEquals(4900.0, result.get("newBalance"));
        assertEquals(4900.0, account.getBalance());
        assertEquals("COMPLETED", cheque.getStatus());
        assertEquals("LINKED-2001", cheque.getDebitedFromAccount());
        ArgumentCaptor<Transaction> statement = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionService).saveTransaction(statement.capture());
        assertEquals("LINKED-2001", statement.getValue().getAccountNumber());
        assertEquals("Debit", statement.getValue().getType());
        assertEquals(100.0, statement.getValue().getAmount());
        verify(savingsChequeAuditLogRepository).save(any(SavingsChequeAuditLog.class));
        verify(goldLoanHistoryRepository).save(any(GoldLoanHistory.class));
    }

    private EmiPayment pendingEmi(int number, double principal, double interest) {
        EmiPayment emi = new EmiPayment();
        emi.setLoanId(8L);
        emi.setLoanAccountNumber("GL-1008");
        emi.setAccountNumber("LINKED-2001");
        emi.setEmiNumber(number);
        emi.setPrincipalAmount(principal);
        emi.setInterestAmount(interest);
        emi.setTotalAmount(principal + interest);
        emi.setRemainingPrincipal(principal);
        emi.setStatus("Pending");
        return emi;
    }
}
