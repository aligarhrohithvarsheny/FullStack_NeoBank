package com.neo.springapp.service;

import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.model.Admin;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.repository.AllocationAccountRepository;
import com.neo.springapp.repository.AllocationMetricsRepository;
import com.neo.springapp.repository.AllocationUtilizationRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundsAllocationServiceReapprovalTest {

    @Mock private FundsAllocationRepository allocationRepository;
    @Mock private AllocationAccountRepository allocationAccountRepository;
    @Mock private AllocationMetricsRepository metricsRepository;
    @Mock private AllocationUtilizationRepository utilizationRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private AccountService accountService;
    @Mock private TransactionService transactionService;

    private FundsAllocationService service;

    @BeforeEach
    void setUp() {
        service = new FundsAllocationService();
        ReflectionTestUtils.setField(service, "allocationRepository", allocationRepository);
        ReflectionTestUtils.setField(service, "allocationAccountRepository", allocationAccountRepository);
        ReflectionTestUtils.setField(service, "metricsRepository", metricsRepository);
        ReflectionTestUtils.setField(service, "utilizationRepository", utilizationRepository);
        ReflectionTestUtils.setField(service, "adminRepository", adminRepository);
        ReflectionTestUtils.setField(service, "accountService", accountService);
        ReflectionTestUtils.setField(service, "transactionService", transactionService);
    }

    @Test
    void reapprovingExpiredAllocationReactivatesItAndSynchronizesLinkedBalance() {
        Admin hod = new Admin();
        hod.setRole("HOD");
        FundsAllocation allocation = new FundsAllocation();
        allocation.setId(8L);
        allocation.setStatus("COMPLETED");
        allocation.setValidTill(LocalDate.now().minusDays(1));
        allocation.setTotalDebited(500.0);
        allocation.setTotalCredited(25.0);
        AllocationAccount linkedAccount = new AllocationAccount();
        linkedAccount.setTotalDebited(new BigDecimal("500.00"));
        linkedAccount.setTotalCredited(new BigDecimal("25.00"));

        when(adminRepository.findById(1L)).thenReturn(Optional.of(hod));
        when(allocationRepository.findById(8L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(allocation)).thenReturn(allocation);
        when(allocationAccountRepository.findByAllocationId(8L)).thenReturn(Optional.of(linkedAccount));
        when(metricsRepository.findByAllocationId(8L)).thenReturn(Optional.empty());

        var result = service.updateAllocation(
            8L, 1200.0, LocalDate.now().plusMonths(3), "Renewed allocation", 1L
        );

        assertThat(result).containsEntry("success", true);
        assertThat(allocation.getStatus()).isEqualTo("ACTIVE");
        assertThat(allocation.getCurrentBalance()).isEqualTo(725.0);
        assertThat(linkedAccount.getTotalAllocated()).isEqualByComparingTo("1200.00");
        assertThat(linkedAccount.getCurrentBalance()).isEqualByComparingTo("725.00");
        assertThat(linkedAccount.getAccountBalance()).isEqualByComparingTo("725.00");
    }
}
