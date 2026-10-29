package com.neo.springapp.service;

import com.neo.springapp.dto.PositivePayCreateRequest;
import com.neo.springapp.dto.PositivePayUpdateRequest;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.PositivePayAuditLog;
import com.neo.springapp.model.PositivePayRequest;
import com.neo.springapp.model.PositivePayStatus;
import com.neo.springapp.model.SavingsChequeRequest;
import com.neo.springapp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositivePayServiceTest {
    @Mock private DemandDraftRepository demandDraftRepository;
    @Mock private PositivePayRequestRepository requestRepository;
    @Mock private PositivePayAuditLogRepository auditRepository;
    @Mock private ChequeRepository chequeRepository;
    @Mock private ChequeRequestRepository salaryChequeRepository;
    @Mock private SavingsChequeRequestRepository savingsChequeRepository;
    @Mock private BusinessChequeRequestRepository businessChequeRepository;
    @Mock private SalaryAccountRepository salaryAccountRepository;
    @Mock private CurrentAccountRepository currentAccountRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private AccountService accountService;
    @Mock private AiSecurityService aiSecurityService;

    private PositivePayService service;
    private Account account;
    private SavingsChequeRequest savingsCheque;

    @BeforeEach
    void setUp() {
        service = new PositivePayService(demandDraftRepository, requestRepository, auditRepository, chequeRepository,
                salaryChequeRepository, savingsChequeRepository, businessChequeRepository, salaryAccountRepository,
                currentAccountRepository, accountRepository, userRepository, accountService, aiSecurityService);
        account = new Account();
        account.setId(81L);
        account.setAccountNumber("SAV-1001");
        account.setName("Test Customer");
        account.setBalance(10000.0);
        savingsCheque = new SavingsChequeRequest();
        savingsCheque.setId(91L);
        savingsCheque.setAccountId(account.getId());
        savingsCheque.setChequeNumber("SCHQ-81-0001");
        savingsCheque.setStatus("AWAITING_POSITIVE_PAY");
        lenient().when(userRepository.findByAccountNumber("SAV-1001")).thenReturn(Optional.empty());
        lenient().when(accountRepository.findByAccountNumber("SAV-1001")).thenReturn(account);
        lenient().when(savingsChequeRepository.findAllByChequeNumber("SCHQ-81-0001")).thenReturn(List.of(savingsCheque));
        lenient().when(accountService.getBalanceByAccountNumber("SAV-1001")).thenReturn(10000.0);
        lenient().when(requestRepository.save(any(PositivePayRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void automaticallyApprovesChequeWhenAiRiskScreeningPasses() {
        when(savingsChequeRepository.findById(91L)).thenReturn(Optional.empty());
        when(requestRepository.findFirstByAccountNumberAndChequeNumberAndStatusIn(
                eq("SAV-1001"), eq("SCHQ-81-0001"), anyList())).thenReturn(Optional.empty());
        when(requestRepository.findByReferenceNumber(anyString())).thenReturn(Optional.empty());
        when(aiSecurityService.analyzeTransaction(anyString(), anyString(), any(), any(), anyString(),
                any(), any(), any(), any())).thenReturn(Map.of("shouldBlock", false));

        var response = service.create(request(), account.getId(), "127.0.0.1");

        assertEquals(PositivePayStatus.APPROVED.name(), response.status());
        assertEquals("AI_SECURITY_ENGINE", response.approvedBy());
    }

    @Test
    void rejectsRegistrationWhenAiRiskScreeningBlocksIt() {
        when(requestRepository.findFirstByAccountNumberAndChequeNumberAndStatusIn(
                eq("SAV-1001"), eq("SCHQ-81-0001"), anyList())).thenReturn(Optional.empty());
        when(requestRepository.findByReferenceNumber(anyString())).thenReturn(Optional.empty());
        when(aiSecurityService.analyzeTransaction(anyString(), anyString(), any(), any(), anyString(),
                any(), any(), any(), any())).thenReturn(Map.of("shouldBlock", true));

        var response = service.create(request(), account.getId(), "127.0.0.1");

        assertEquals(PositivePayStatus.REJECTED.name(), response.status());
    }

    @Test
    void letsTheOwningCustomerEditAnUnpresentedPassedRegistration() {
        PositivePayRequest existing = new PositivePayRequest();
        existing.setReferenceNumber("PP-TEST-1");
        existing.setUserId(account.getId());
        existing.setAccountNumber(account.getAccountNumber());
        existing.setChequeNumber(savingsCheque.getChequeNumber());
        existing.setChequeSource("SAVINGS_CHEQUE_REQUESTS");
        existing.setChequeId(savingsCheque.getId());
        existing.setStatus(PositivePayStatus.APPROVED);
        when(requestRepository.findByReferenceNumber("PP-TEST-1")).thenReturn(Optional.of(existing));
        when(savingsChequeRepository.findById(91L)).thenReturn(Optional.empty());
        when(aiSecurityService.analyzeTransaction(anyString(), anyString(), any(), any(), anyString(),
                any(), any(), any(), any())).thenReturn(Map.of("shouldBlock", false));

        var response = service.update("PP-TEST-1",
                new PositivePayUpdateRequest(LocalDate.now(), new BigDecimal("750.00"), "Updated Payee",
                        null, null, null, null, true),
                account.getId(), "127.0.0.1");

        assertEquals(PositivePayStatus.APPROVED.name(), response.status());
        assertEquals("Updated Payee", response.payeeName());
    }

    @Test
    void allowsAdminToOverrideAiRejectionWithAnAuditRemark() {
        PositivePayRequest existing = new PositivePayRequest();
        existing.setReferenceNumber("PP-TEST-2");
        existing.setAccountNumber(account.getAccountNumber());
        existing.setChequeNumber(savingsCheque.getChequeNumber());
        existing.setChequeSource("SAVINGS_CHEQUE_REQUESTS");
        existing.setChequeId(savingsCheque.getId());
        existing.setStatus(PositivePayStatus.REJECTED);
        existing.setRejectedBy("AI_SECURITY_ENGINE");
        existing.setRejectionReason("AI risk screening identified elevated risk.");
        when(requestRepository.findByReferenceNumber("PP-TEST-2")).thenReturn(Optional.of(existing));
        when(savingsChequeRepository.findById(savingsCheque.getId())).thenReturn(Optional.empty());

        var response = service.approve("PP-TEST-2", "Admin User", "Verified customer details", "127.0.0.1");

        assertEquals(PositivePayStatus.APPROVED.name(), response.request().status());
        assertEquals("Admin User", response.request().approvedBy());
        assertNull(response.request().rejectionReason());
        assertNull(response.request().rejectedBy());
        ArgumentCaptor<PositivePayAuditLog> audit = ArgumentCaptor.forClass(PositivePayAuditLog.class);
        verify(auditRepository).save(audit.capture());
        assertEquals("ADMIN_OVERRIDDEN_AI_REJECTION", audit.getValue().getAction());
        assertEquals("Verified customer details", audit.getValue().getRemarks());
    }

    @Test
    void doesNotAllowAdminToOverrideANonAiRejection() {
        PositivePayRequest existing = new PositivePayRequest();
        existing.setReferenceNumber("PP-TEST-3");
        existing.setStatus(PositivePayStatus.REJECTED);
        existing.setRejectedBy("Admin User");
        when(requestRepository.findByReferenceNumber("PP-TEST-3")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> service.approve("PP-TEST-3", "Another Admin", "Reviewed", "127.0.0.1"));

        verify(requestRepository, never()).save(any(PositivePayRequest.class));
    }

    private PositivePayCreateRequest request() {
        return new PositivePayCreateRequest("SAV-1001", "SCHQ-81-0001", LocalDate.now(),
                new BigDecimal("500.00"), "Payee", null, null, null, null, true);
    }
}
