package com.neo.springapp.service;

import com.neo.springapp.entity.ChargeTransaction;
import com.neo.springapp.model.Admin;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.model.SoundboxDevice;
import com.neo.springapp.model.SoundboxLinkedAccount;
import com.neo.springapp.model.SoundboxRequest;
import com.neo.springapp.model.SoundboxTransaction;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import com.neo.springapp.repository.SoundboxDeviceRepository;
import com.neo.springapp.repository.SoundboxRequestRepository;
import com.neo.springapp.repository.SoundboxTransactionRepository;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SoundboxLinkedAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoundboxServiceAllocationChargeTest {

    @Mock private SoundboxDeviceRepository deviceRepository;
    @Mock private SoundboxRequestRepository requestRepository;
    @Mock private SoundboxTransactionRepository soundboxTransactionRepository;
    @Mock private CurrentAccountRepository currentAccountRepository;
    @Mock private FundsAllocationRepository allocationRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private ChargeManagementService chargeManagementService;
    @Mock private AccountRepository accountRepository;
    @Mock private SalaryAccountRepository salaryAccountRepository;
    @Mock private SoundboxLinkedAccountRepository linkedAccountRepository;

    private SoundboxService service;
    private SoundboxRequest request;
    private FundsAllocation allocation;
    private Admin manager;

    @BeforeEach
    void setUp() {
        service = new SoundboxService(
            deviceRepository,
            requestRepository,
            soundboxTransactionRepository,
            currentAccountRepository,
            allocationRepository,
            adminRepository,
            chargeManagementService,
            accountRepository,
            salaryAccountRepository,
            linkedAccountRepository
        );

        request = new SoundboxRequest();
        request.setId(7L);
        request.setRequestId("SBREQ-7");
        request.setAccountNumber("CA-100");
        request.setOwnerName("Merchant Owner");
        request.setCity("Tirupati");
        request.setStatus("PENDING");
        request.setDeviceCharge(499.0);

        allocation = new FundsAllocation();
        allocation.setId(21L);
        allocation.setManagerId(3L);
        allocation.setStatus("ACTIVE");
        allocation.setAccountStatus("VERIFIED");
        allocation.setChargeManagementEnabled(true);
        allocation.setAllocationAccountId(31L);

        manager = new Admin();
        manager.setAssignedCity("TIRUPATI");

        lenient().when(requestRepository.findById(7L)).thenReturn(Optional.of(request));
        lenient().when(allocationRepository.findById(21L)).thenReturn(Optional.of(allocation));
        lenient().when(adminRepository.findById(3L)).thenReturn(Optional.of(manager));
        lenient().when(requestRepository.save(any(SoundboxRequest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(deviceRepository.save(any(SoundboxDevice.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void approvalRoutesDeviceChargeThroughAllocationChargeService() {
        ChargeTransaction charge = new ChargeTransaction();
        charge.setChargeTransactionId("CT-SOUNDBOX-7");
        when(chargeManagementService.processCharge(
            eq(21L), eq("SOUNDBOX"), anyString(), eq(BigDecimal.valueOf(499.0)),
            eq("CA-100"), eq("Merchant Owner"), eq("CURRENT_ACCOUNT"),
            eq("SOUNDBOX-SBREQ-7"), isNull(), isNull()
        )).thenReturn(charge);

        Map<String, Object> result = service.approveRequest(7L, "Admin", "SB12345678", 100.0, 499.0, 21L);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("chargeTransactionId")).isEqualTo("CT-SOUNDBOX-7");
        assertThat(request.getStatus()).isEqualTo("APPROVED");
        verify(chargeManagementService).processCharge(
            eq(21L), eq("SOUNDBOX"), anyString(), eq(BigDecimal.valueOf(499.0)),
            eq("CA-100"), eq("Merchant Owner"), eq("CURRENT_ACCOUNT"),
            eq("SOUNDBOX-SBREQ-7"), isNull(), isNull()
        );
        verify(currentAccountRepository).findByAccountNumber("CA-100");
        verify(currentAccountRepository, never()).save(any());
    }

    @Test
    void approvalRefusesUnverifiedAllocationBeforeCharging() {
        allocation.setAccountStatus("PENDING");

        Map<String, Object> result = service.approveRequest(7L, "Admin", "SB12345678", 100.0, 499.0, 21L);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("error").toString()).contains("verified linked account");
        verifyNoInteractions(chargeManagementService);
        verify(deviceRepository, never()).save(any(SoundboxDevice.class));
    }

    @Test
    void paymentSubmissionRequiresApprovedLinkAndDoesNotCreditBalance() {
        SoundboxLinkedAccount linkedAccount = new SoundboxLinkedAccount();
        linkedAccount.setSoundboxAccountNumber("CA-100");
        linkedAccount.setLinkedAccountNumber("SA-200");
        linkedAccount.setLinkedCustomerId("CUST-200");
        linkedAccount.setAccountType("SAVINGS");
        linkedAccount.setStatus("APPROVED");
        SoundboxDevice device = new SoundboxDevice();
        device.setStatus("ACTIVE");
        device.setDeviceId("SB-100");
        when(linkedAccountRepository.findBySoundboxAccountNumberAndLinkedAccountNumberAndStatus(
            "CA-100", "SA-200", "APPROVED")).thenReturn(Optional.of(linkedAccount));
        when(deviceRepository.findByAccountNumber("CA-100")).thenReturn(Optional.of(device));

        SoundboxTransaction transaction = new SoundboxTransaction();
        transaction.setSoundboxAccountNumber("CA-100");
        transaction.setAccountNumber("SA-200");
        transaction.setAmount(250.0);
        Map<String, Object> result = service.processPayment(transaction);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("pendingApproval")).isEqualTo(true);
        assertThat(transaction.getStatus()).isEqualTo("PENDING");
        assertThat(transaction.getAccountType()).isEqualTo("SAVINGS");
        verify(accountRepository, never()).save(any());
        verify(currentAccountRepository, never()).save(any());
        verify(salaryAccountRepository, never()).save(any());
        verify(soundboxTransactionRepository).save(transaction);
    }

    @Test
    void paymentSubmissionRejectsAccountWithoutAdminApproval() {
        SoundboxTransaction transaction = new SoundboxTransaction();
        transaction.setSoundboxAccountNumber("CA-100");
        transaction.setAccountNumber("SA-200");
        transaction.setAmount(250.0);
        when(linkedAccountRepository.findBySoundboxAccountNumberAndLinkedAccountNumberAndStatus(
            "CA-100", "SA-200", "APPROVED")).thenReturn(Optional.empty());

        Map<String, Object> result = service.processPayment(transaction);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("error").toString()).contains("not been approved");
        verify(soundboxTransactionRepository, never()).save(any(SoundboxTransaction.class));
        verifyNoInteractions(accountRepository, salaryAccountRepository);
    }

    @Test
    void adminApprovalCreditsOnlyTheIdentityMatchedSavingsAccount() {
        Account savingsAccount = new Account();
        savingsAccount.setAccountNumber("SA-200");
        savingsAccount.setCustomerId("CUST-200");
        savingsAccount.setName("Account Holder");
        savingsAccount.setStatus("ACTIVE");
        savingsAccount.setBalance(25.0);

        SoundboxLinkedAccount linkedAccount = new SoundboxLinkedAccount();
        linkedAccount.setSoundboxAccountNumber("CA-100");
        linkedAccount.setLinkedAccountNumber("SA-200");
        linkedAccount.setLinkedCustomerId("CUST-200");
        linkedAccount.setAccountType("SAVINGS");
        linkedAccount.setStatus("APPROVED");

        SoundboxTransaction transaction = new SoundboxTransaction();
        transaction.setId(55L);
        transaction.setSoundboxAccountNumber("CA-100");
        transaction.setAccountNumber("SA-200");
        transaction.setAmount(250.0);
        transaction.setStatus("PENDING");

        when(soundboxTransactionRepository.findById(55L)).thenReturn(Optional.of(transaction));
        when(linkedAccountRepository.findBySoundboxAccountNumberAndLinkedAccountNumberAndStatus(
            "CA-100", "SA-200", "APPROVED")).thenReturn(Optional.of(linkedAccount));
        when(accountRepository.findByCustomerIdAndAccountNumber("CUST-200", "SA-200")).thenReturn(savingsAccount);

        Map<String, Object> result = service.reviewPayment(55L, "Admin", true);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(transaction.getStatus()).isEqualTo("SUCCESS");
        assertThat(transaction.getApprovedBy()).isEqualTo("Admin");
        assertThat(savingsAccount.getBalance()).isEqualTo(275.0);
        verify(accountRepository).save(savingsAccount);
        verify(currentAccountRepository, never()).save(any());
        verify(salaryAccountRepository, never()).save(any());
    }
}