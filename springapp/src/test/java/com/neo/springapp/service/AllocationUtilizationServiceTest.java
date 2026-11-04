package com.neo.springapp.service;

import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.model.AllocationUtilization;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.repository.AllocationAccountRepository;
import com.neo.springapp.repository.AllocationMetricsRepository;
import com.neo.springapp.repository.AllocationUtilizationRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationUtilizationServiceTest {

    @Mock private AllocationUtilizationRepository utilizationRepository;
    @Mock private FundsAllocationRepository allocationRepository;
    @Mock private AllocationAccountRepository allocationAccountRepository;
    @Mock private AllocationMetricsRepository metricsRepository;

    private AllocationUtilizationService service;
    private FundsAllocation allocation;
    private AllocationAccount linkedAccount;

    @BeforeEach
    void setUp() {
        service = new AllocationUtilizationService();
        ReflectionTestUtils.setField(service, "utilizationRepository", utilizationRepository);
        ReflectionTestUtils.setField(service, "allocationRepository", allocationRepository);
        ReflectionTestUtils.setField(service, "allocationAccountRepository", allocationAccountRepository);
        ReflectionTestUtils.setField(service, "metricsRepository", metricsRepository);

        allocation = new FundsAllocation();
        allocation.setId(7L);
        allocation.setAllocationId("FA-7");
        allocation.setAllocationAccountId(9L);
        allocation.setStatus("ACTIVE");
        allocation.setAccountStatus("VERIFIED");
        allocation.setChargeManagementEnabled(true);
        allocation.setValidTill(LocalDate.now().plusDays(30));
        allocation.setCurrentBalance(100.0);
        allocation.setTotalDebited(0.0);
        allocation.setTotalUtilized(0.0);

        linkedAccount = new AllocationAccount();
        linkedAccount.setId(9L);
        linkedAccount.setVerificationStatus("VERIFIED");
        linkedAccount.setAccountStatus("ACTIVE");
        linkedAccount.setCurrentBalance(new BigDecimal("100.00"));
        linkedAccount.setAccountBalance(new BigDecimal("100.00"));
        linkedAccount.setTotalDebited(BigDecimal.ZERO);

        lenient().when(allocationRepository.findById(7L)).thenReturn(Optional.of(allocation));
        lenient().when(allocationAccountRepository.findById(9L)).thenReturn(Optional.of(linkedAccount));
        lenient().when(utilizationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(metricsRepository.findByAllocationId(7L)).thenReturn(Optional.empty());
    }

    @Test
    void debitSynchronizesAllocationAndLinkedAccountBalances() {
        var result = service.debitAllocationFunds(7L, 40.0, "GOLD_LOAN", "CUSTOMER-1", "Customer",
                "GOLD-1", "Gold loan disbursement", 3L);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(allocation.getCurrentBalance()).isEqualTo(60.0);
        assertThat(linkedAccount.getCurrentBalance()).isEqualByComparingTo("60.00");
        assertThat(linkedAccount.getAccountBalance()).isEqualByComparingTo("60.00");
        assertThat(linkedAccount.getTotalDebited()).isEqualByComparingTo("40.00");
    }

    @Test
    void rejectsUnverifiedAllocationAccountWithoutRecordingDebit() {
        allocation.setAccountStatus("PENDING");

        var result = service.debitAllocationFunds(7L, 40.0, "GOLD_LOAN", "CUSTOMER-1", "Customer",
                "GOLD-1", "Gold loan disbursement", 3L);

        assertThat(result.get("success")).isEqualTo(false);
        verify(utilizationRepository, never()).save(any());
    }

    @Test
    void rejectsInsufficientLinkedAccountBalanceWithoutChangingBalances() {
        linkedAccount.setCurrentBalance(new BigDecimal("20.00"));
        linkedAccount.setAccountBalance(new BigDecimal("20.00"));

        var result = service.debitAllocationFunds(7L, 40.0, "GOLD_LOAN", "CUSTOMER-1", "Customer",
                "GOLD-1", "Gold loan disbursement", 3L);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(allocation.getCurrentBalance()).isEqualTo(100.0);
        assertThat(linkedAccount.getCurrentBalance()).isEqualByComparingTo("20.00");
        verify(utilizationRepository, never()).save(any());
    }

    @Test
    void creditSynchronizesAllocationAndLinkedAccountBalances() {
        linkedAccount.setTotalCredited(BigDecimal.ZERO);

        var result = service.creditAllocationFunds(7L, 40.0, "GOLD_LOAN", "CUSTOMER-1", "Customer",
                "GOLD-CLOSURE-1", "Gold loan foreclosure", 3L);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(allocation.getCurrentBalance()).isEqualTo(140.0);
        assertThat(allocation.getTotalCredited()).isEqualTo(40.0);
        assertThat(linkedAccount.getCurrentBalance()).isEqualByComparingTo("140.00");
        assertThat(linkedAccount.getAccountBalance()).isEqualByComparingTo("140.00");
        assertThat(linkedAccount.getTotalCredited()).isEqualByComparingTo("40.00");
    }

    @Test
    void rejectsCreditWhenLinkedAllocationAccountIsInactive() {
        linkedAccount.setAccountStatus("BLOCKED");

        var result = service.creditAllocationFunds(7L, 40.0, "GOLD_LOAN", "CUSTOMER-1", "Customer",
                "GOLD-CLOSURE-1", "Gold loan foreclosure", 3L);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(allocation.getCurrentBalance()).isEqualTo(100.0);
        assertThat(linkedAccount.getCurrentBalance()).isEqualByComparingTo("100.00");
        verify(utilizationRepository, never()).save(any());
    }

    @Test
    void creditsOnlyAnExactCustomerAccountMatchAndRecordsTheSourceDebitStatus() {
        linkedAccount.setAllocationId(7L);
        linkedAccount.setAccountNumber("CUSTOMER-1");
        linkedAccount.setTotalCredited(BigDecimal.ZERO);
        when(allocationAccountRepository.findByAccountNumber("CUSTOMER-1"))
                .thenReturn(Optional.of(linkedAccount));
        when(utilizationRepository.findByLinkedTransactionId("FD-TXN-1")).thenReturn(List.of());

        var result = service.creditPostedDeposit("CUSTOMER-1", "Customer", 40.0, "FD",
                "FD-TXN-1", "Fixed deposit principal", "Completed");

        assertThat(result.get("allocationCreditStatus")).isEqualTo("SUCCESS");
        assertThat(allocation.getCurrentBalance()).isEqualTo(140.0);
        org.mockito.ArgumentCaptor<AllocationUtilization> ledgerEntry =
                org.mockito.ArgumentCaptor.forClass(AllocationUtilization.class);
        verify(utilizationRepository).save(ledgerEntry.capture());
        assertThat(ledgerEntry.getValue().getUserAccountNumber()).isEqualTo("CUSTOMER-1");
        assertThat(ledgerEntry.getValue().getSourceStatus()).isEqualTo("Completed");
    }

    @Test
    void doesNotCreditWhenTheCustomerAccountDoesNotExactlyMatch() {
        when(allocationAccountRepository.findByAccountNumber("CUSTOMER-1"))
                .thenReturn(Optional.empty());

        var result = service.creditPostedDeposit("CUSTOMER-1", "Customer", 40.0, "RD",
                "RD-TXN-1", "Recurring deposit installment", "Completed");

        assertThat(result.get("allocationCreditStatus")).isEqualTo("NOT_MAPPED");
        verify(utilizationRepository, never()).save(any());
    }

    @Test
    void doesNotCreditAnUnpostedCustomerDebit() {
        linkedAccount.setAccountNumber("CUSTOMER-1");
        when(allocationAccountRepository.findByAccountNumber("CUSTOMER-1"))
                .thenReturn(Optional.of(linkedAccount));

        assertThatThrownBy(() -> service.creditPostedDeposit("CUSTOMER-1", "Customer", 40.0, "FD",
                "FD-TXN-PENDING", "Fixed deposit principal", "Pending"))
                .isInstanceOf(AllocationUtilizationService.AllocationDepositCreditException.class);
        verify(utilizationRepository, never()).save(any());
    }

    @Test
    void repeatedPostedDepositReferenceDoesNotCreditTwice() {
        linkedAccount.setAllocationId(7L);
        linkedAccount.setAccountNumber("CUSTOMER-1");
        AllocationUtilization existing = new AllocationUtilization();
        existing.setAllocationId(7L);
        existing.setProductType("FD");
        when(allocationAccountRepository.findByAccountNumber("CUSTOMER-1"))
                .thenReturn(Optional.of(linkedAccount));
        when(utilizationRepository.findByLinkedTransactionId("FD-TXN-1")).thenReturn(List.of(existing));

        var result = service.creditPostedDeposit("CUSTOMER-1", "Customer", 40.0, "FD",
                "FD-TXN-1", "Fixed deposit principal", "Completed");

        assertThat(result.get("allocationCreditStatus")).isEqualTo("SUCCESS");
        assertThat(allocation.getCurrentBalance()).isEqualTo(100.0);
        verify(utilizationRepository, never()).save(any());
    }
}