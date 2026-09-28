package com.neo.springapp.service;

import com.neo.springapp.model.Admin;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationValidatorServiceTest {

    @Mock private FundsAllocationRepository allocationRepository;
    @Mock private AdminRepository adminRepository;

    private AllocationValidatorService service;

    @BeforeEach
    void setUp() {
        service = new AllocationValidatorService();
        ReflectionTestUtils.setField(service, "allocationRepository", allocationRepository);
        ReflectionTestUtils.setField(service, "adminRepository", adminRepository);
    }

    @Test
    void allowsAssignedManagerWithoutSalaryAccountToReceiveAllocation() {
        Admin hod = new Admin();
        hod.setRole("HOD");
        Admin manager = new Admin();
        manager.setRole("MANAGER");
        manager.setSalaryAccountNumber(null);

        when(adminRepository.findById(1L)).thenReturn(Optional.of(hod));
        when(adminRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(allocationRepository.findActiveAllocationsForManager(2L)).thenReturn(List.of());

        var result = service.validateAllocationRequest(
            100_000.0,
            2L,
            "GENERAL",
            LocalDate.now().plusMonths(1),
            1L
        );

        assertThat(result.get("isValid")).isEqualTo(true);
        assertThat(result).doesNotContainKey("errors");
    }
}