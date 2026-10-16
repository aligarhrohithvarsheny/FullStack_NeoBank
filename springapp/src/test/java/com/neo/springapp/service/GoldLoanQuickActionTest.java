package com.neo.springapp.service;

import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.repository.EmiPaymentRepository;
import com.neo.springapp.repository.GoldLoanHistoryRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SalaryNormalTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoldLoanQuickActionTest {

    private GoldLoanRepository goldLoanRepository;
    private EmiPaymentRepository emiPaymentRepository;
    private GoldLoanHistoryRepository goldLoanHistoryRepository;
    private GoldLoan loan;
    private List<EmiPayment> schedule;
    private GoldLoanService service;

    @BeforeEach
    void setUp() {
        goldLoanRepository = mock(GoldLoanRepository.class);
        emiPaymentRepository = mock(EmiPaymentRepository.class);
        goldLoanHistoryRepository = mock(GoldLoanHistoryRepository.class);
        loan = new GoldLoan();
        loan.setId(1L);
        loan.setLoanAccountNumber("GL123");
        loan.setAccountNumber("AC123");
        loan.setStatus("Approved");
        loan.setLoanAmount(11_000.0);
        loan.setRemainingPrincipal(11_000.0);
        loan.setInterestRate(12.0);
        loan.setTenure(12);
        loan.setRemainingTenure(2);

        EmiPayment firstEmi = emi(1, 5_500.0);
        EmiPayment secondEmi = emi(2, 5_500.0);
        schedule = List.of(firstEmi, secondEmi);

        when(goldLoanRepository.findByLoanAccountNumber("GL123")).thenReturn(Optional.of(loan));
        when(goldLoanRepository.save(any(GoldLoan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc("GL123")).thenReturn(schedule);

        service = new GoldLoanService(
                goldLoanRepository,
                mock(GoldRateService.class),
                mock(AccountService.class),
                emiPaymentRepository,
                mock(OtpService.class),
                mock(EmailService.class),
                mock(UserService.class),
                mock(SalaryAccountRepository.class),
                mock(SalaryNormalTransactionRepository.class),
                goldLoanHistoryRepository,
                mock(AllocationUtilizationService.class),
                mock(ChargeManagementService.class),
                mock(TransactionService.class));
    }

    @Test
    void depositReducesOutstandingAndRecalculatesEmis() {
        Map<String, Double> balances = service.processQuickAction("GL123", "DEPOSIT", 1_000.0, "Admin");

        assertThat(balances).containsEntry("balanceBefore", 11_000.0).containsEntry("balanceAfter", 10_000.0);
        assertThat(loan.getRemainingPrincipal()).isEqualTo(10_000.0);
        assertThat(loan.getCurrentEmi()).isLessThan(loan.getLoanAmount() / 2);
        assertThat(schedule.get(0).getPrincipalAmount()).isPositive();
        verify(emiPaymentRepository).saveAll(schedule);
        verify(goldLoanHistoryRepository).save(any());
    }

    @Test
    void withdrawalIncreasesOutstandingAndRecalculatesEmis() {
        Map<String, Double> balances = service.processQuickAction("GL123", "WITHDRAWAL", 1_000.0, "Admin");

        assertThat(balances).containsEntry("balanceBefore", 11_000.0).containsEntry("balanceAfter", 12_000.0);
        assertThat(loan.getRemainingPrincipal()).isEqualTo(12_000.0);
        assertThat(loan.getCurrentEmi()).isGreaterThan(loan.getLoanAmount() / 2);
        verify(emiPaymentRepository).saveAll(schedule);
    }

    @Test
    void depositCannotExceedOutstandingPrincipal() {
        assertThrows(IllegalArgumentException.class,
                () -> service.processQuickAction("GL123", "DEPOSIT", 11_001.0, "Admin"));
    }

    @Test
    void revertingDepositRestoresPrincipalAndRecalculatesEmis() {
        service.processQuickAction("GL123", "DEPOSIT", 1_000.0, "Admin");

        service.restoreQuickActionBalance("GL123", 1_000.0, "DEPOSIT", "Admin");

        assertThat(loan.getRemainingPrincipal()).isEqualTo(11_000.0);
        assertThat(loan.getPrincipalPaid()).isEqualTo(0.0);
        verify(emiPaymentRepository, org.mockito.Mockito.times(2)).saveAll(schedule);
    }

    private EmiPayment emi(int number, double principal) {
        EmiPayment emi = new EmiPayment();
        emi.setLoanId(loan.getId());
        emi.setLoanAccountNumber(loan.getLoanAccountNumber());
        emi.setAccountNumber(loan.getAccountNumber());
        emi.setEmiNumber(number);
        emi.setDueDate(LocalDate.of(2026, number, 1));
        emi.setPrincipalAmount(principal);
        emi.setInterestAmount(100.0);
        emi.setTotalAmount(principal + 100.0);
        emi.setStatus("Pending");
        return emi;
    }
}
