package com.neo.springapp.controller;

import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.model.AllocationUtilization;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.repository.AllocationAccountRepository;
import com.neo.springapp.repository.AllocationUtilizationRepository;
import com.neo.springapp.repository.ChargeTransactionRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import com.neo.springapp.service.PasswordService;
import com.neo.springapp.service.UserSessionTokenService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AllocationAccountLoginControllerTest {
    @Mock private FundsAllocationRepository allocationRepository;
    @Mock private AllocationAccountRepository linkedAccountRepository;
    @Mock private AllocationUtilizationRepository utilizationRepository;
    @Mock private ChargeTransactionRepository chargeTransactionRepository;
    @Mock private PasswordService passwordService;
    @Mock private UserSessionTokenService tokenService;

    private AllocationAccountLoginController controller;
    private FundsAllocation allocation;
    private AllocationAccount linkedAccount;

    @BeforeEach
    void setUp() {
        controller = new AllocationAccountLoginController(
            allocationRepository, linkedAccountRepository, utilizationRepository,
            chargeTransactionRepository, passwordService, tokenService);
        allocation = new FundsAllocation();
        allocation.setId(12L);
        allocation.setAllocationId("FA-2026-00012");
        allocation.setAllocatedByAdminId(7L);
        allocation.setAllocatedAmount(5000.0);
        allocation.setCurrentBalance(5000.0);
        allocation.setStatus("ACTIVE");
        allocation.setValidTill(LocalDate.now().plusDays(10));
        linkedAccount = new AllocationAccount();
        linkedAccount.setAllocationId(12L);
        linkedAccount.setAccountNumber("1234567890");
        linkedAccount.setAccountHolderName("Branch Funds");
        linkedAccount.setVerificationStatus("VERIFIED");
        linkedAccount.setAccountStatus("ACTIVE");
    }

    @Test
    void provisioningRequiresHodSession() {
        ResponseEntity<Map<String, Object>> response = controller.provisionLogin(
                12L, new SessionPrincipal(7L, "hod@example.com", "MANAGER"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void hodCanProvisionOnlyOwnedVerifiedAllocation() {
        when(allocationRepository.findById(12L)).thenReturn(Optional.of(allocation));
        when(linkedAccountRepository.findByAllocationId(12L)).thenReturn(Optional.of(linkedAccount));
        when(passwordService.encryptPassword(org.mockito.ArgumentMatchers.anyString())).thenReturn("bcrypt-hash");

        ResponseEntity<Map<String, Object>> response = controller.provisionLogin(
                12L, new SessionPrincipal(7L, "hod@example.com", "HOD"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("username", "fund-12");
        assertThat(response.getBody()).containsKey("password");
        assertThat(allocation.getAllocationLoginPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(response.getBody()).doesNotContainValue("bcrypt-hash");
        verify(allocationRepository).save(allocation);
    }

    @Test
    void loginIssuesAllocationScopedSession() {
        allocation.setAllocationLoginUsername("fund-12");
        allocation.setAllocationLoginPasswordHash("bcrypt-hash");
        when(allocationRepository.findByAllocationLoginUsername("fund-12")).thenReturn(Optional.of(allocation));
        when(linkedAccountRepository.findByAllocationId(12L)).thenReturn(Optional.of(linkedAccount));
        when(passwordService.verifyPassword("temporary-password", "bcrypt-hash")).thenReturn(true);
        when(tokenService.issueAllocationAccount(12L, "FA-2026-00012")).thenReturn("signed-allocation-token");

        ResponseEntity<Map<String, Object>> response = controller.login(
                new AllocationAccountLoginController.LoginRequest("FUND-12", "temporary-password"));

        assertThat(response.getBody()).containsEntry("role", "ALLOCATION_ACCOUNT");
        assertThat(response.getBody()).containsEntry("sessionToken", "signed-allocation-token");
        verify(tokenService).issueAllocationAccount(12L, "FA-2026-00012");
    }

    @Test
    void fifthInvalidPasswordLocksAllocationLogin() {
        allocation.setAllocationLoginUsername("fund-12");
        allocation.setAllocationLoginPasswordHash("bcrypt-hash");
        allocation.setAllocationLoginFailedAttempts(4);
        when(allocationRepository.findByAllocationLoginUsername("fund-12")).thenReturn(Optional.of(allocation));
        when(passwordService.verifyPassword("wrong", "bcrypt-hash")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.login(
                new AllocationAccountLoginController.LoginRequest("fund-12", "wrong"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(allocation.getAllocationLoginLocked()).isTrue();
        verify(allocationRepository).save(allocation);
    }

    @Test
    void dashboardReadsOnlyAllocationBoundToSignedSession() {
        when(allocationRepository.findById(12L)).thenReturn(Optional.of(allocation));
        when(linkedAccountRepository.findByAllocationId(12L)).thenReturn(Optional.of(linkedAccount));

        ResponseEntity<Map<String, Object>> response = controller.getMyAllocation(
            new SessionPrincipal(-12L, "ALLOCATION:FA-2026-00012", "ALLOCATION_ACCOUNT"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("allocation");
        assertThat(response.getBody()).containsKey("account");
        assertThat(((Map<?, ?>) response.getBody().get("allocation")).get("currentBalance")).isEqualTo(5000.0);
        assertThat(((Map<?, ?>) response.getBody().get("account")).get("accountNumber")).isEqualTo("1234567890");
    }

    @Test
    void allocationSessionTokenUsesNamespacedPrincipal() {
        UserSessionTokenService service = new UserSessionTokenService("local-test-secret-with-at-least-32-characters");

        SessionPrincipal principal = service.verify(service.issueAllocationAccount(12L, "FA-2026-00012"));

        assertThat(principal).isNotNull();
        assertThat(principal.scope()).isEqualTo("ALLOCATION_ACCOUNT");
        assertThat(principal.userId()).isEqualTo(-12L);
        assertThat(principal.accountNumber()).isEqualTo("ALLOCATION:FA-2026-00012");
    }

    @Test
    void dashboardReturnsLiveTotalsAndRecentLedgerRowsForSignedAllocation() {
        allocation.setAllocationId("FA-2026-00012");
        linkedAccount.setCurrentBalance(new BigDecimal("4750.00"));
        linkedAccount.setTotalDebited(new BigDecimal("250.00"));
        linkedAccount.setTotalCredited(new BigDecimal("0.00"));
        AllocationUtilization loanDebit = new AllocationUtilization();
        loanDebit.setId(21L);
        loanDebit.setUtilizationId("AU-21");
        loanDebit.setAllocationId(12L);
        loanDebit.setTransactionType("DEBIT");
        loanDebit.setAmount(250.0);
        loanDebit.setRemainingBalance(4750.0);
        loanDebit.setProductType("GOLD_LOAN");
        loanDebit.setTransactionDate(LocalDateTime.now());

        when(allocationRepository.findById(12L)).thenReturn(Optional.of(allocation));
        when(linkedAccountRepository.findByAllocationId(12L)).thenReturn(Optional.of(linkedAccount));
        when(utilizationRepository.findByAllocationIdOrderByTransactionDateDesc(eq(12L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loanDebit), PageRequest.of(0, 25), 1));
        when(utilizationRepository.countByAllocationId(12L)).thenReturn(1L);
        when(utilizationRepository.getDebitCount(12L)).thenReturn(1L);
        when(utilizationRepository.getCreditCount(12L)).thenReturn(0L);
        when(utilizationRepository.sumByAllocationAndProductTypes(eq(12L), eq("DEBIT"), any())).thenReturn(250.0);
        when(utilizationRepository.countByAllocationAndProductTypes(eq(12L), eq("DEBIT"), any())).thenReturn(1L);
        when(chargeTransactionRepository.findByAllocationIdOrderByCreatedAtDesc(eq(12L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 25), 0));
        when(chargeTransactionRepository.sumSuccessfulInterestCharges(12L)).thenReturn(new BigDecimal("0.00"));
        when(chargeTransactionRepository.sumTotalCharges(12L)).thenReturn(new BigDecimal("0.00"));
        when(chargeTransactionRepository.sumCreditedCharges(12L)).thenReturn(new BigDecimal("0.00"));
        when(chargeTransactionRepository.sumPendingChargeCredits(12L)).thenReturn(new BigDecimal("0.00"));
        when(chargeTransactionRepository.sumSuccessfulFineCharges(12L)).thenReturn(new BigDecimal("0.00"));
        when(chargeTransactionRepository.sumSuccessfulChequeCharges(12L)).thenReturn(new BigDecimal("0.00"));
        when(chargeTransactionRepository.countCollectedCharges(12L)).thenReturn(0L);
        when(chargeTransactionRepository.countByCreditStatus(12L, "CREDITED")).thenReturn(0L);
        when(chargeTransactionRepository.countByCreditStatus(12L, "PENDING_CREDIT")).thenReturn(0L);

        ResponseEntity<Map<String, Object>> response = controller.getMyAllocationDashboard(
                new SessionPrincipal(-12L, "ALLOCATION:FA-2026-00012", "ALLOCATION_ACCOUNT"), 0, 25);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("summary");
        assertThat(response.getBody().get("transactions")).asList().hasSize(1);
        assertThat(((Map<?, ?>) response.getBody().get("summary")).get("loansOpened")).isEqualTo(1L);
        assertThat(((Map<?, ?>) response.getBody().get("summary")).get("loanDebits")).isEqualTo(250.0);
    }
}
