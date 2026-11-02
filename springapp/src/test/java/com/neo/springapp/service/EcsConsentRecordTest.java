package com.neo.springapp.service;

import com.neo.springapp.model.EcsConsentRecord;
import com.neo.springapp.model.EcsMandate;
import com.neo.springapp.model.RecurringDeposit;
import com.neo.springapp.repository.EcsConsentRecordRepository;
import com.neo.springapp.repository.EcsMandateEventRepository;
import com.neo.springapp.repository.EcsMandateRepository;
import com.neo.springapp.repository.RecurringDepositRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcsConsentRecordTest {

    @Mock private EcsMandateRepository mandateRepository;
    @Mock private EcsMandateEventRepository eventRepository;
    @Mock private EcsConsentRecordRepository consentRecordRepository;
    @Mock private RecurringDepositRepository recurringDepositRepository;
    @Mock private RecurringDepositEcsService recurringDepositEcsService;
    @InjectMocks private EcsMandateService service;

    @Test
    void confirmationPersistsSnapshotOfCustomerConsent() {
        String token = "customer-consent-token";
        EcsMandate mandate = mandate();
        ReflectionTestUtils.setField(mandate, "consentTokenHash",
                ReflectionTestUtils.invokeMethod(service, "sha256", token));
        when(mandateRepository.findByConsentTokenHash(any())).thenReturn(Optional.of(mandate));
        RecurringDeposit deposit = new RecurringDeposit();
        deposit.setId(17L);
        when(recurringDepositRepository.findByRdAccountNumber("RD00000017"))
                .thenReturn(Optional.of(deposit));
        when(mandateRepository.save(any(EcsMandate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRecordRepository.save(any(EcsConsentRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.confirmConsent(token, "Test Customer", true, "127.0.0.1");

        ArgumentCaptor<EcsConsentRecord> captor = ArgumentCaptor.forClass(EcsConsentRecord.class);
        verify(consentRecordRepository).save(captor.capture());
        EcsConsentRecord saved = captor.getValue();
        assertEquals(17L, saved.getMandateDbId());
        assertEquals("RD00000017", saved.getProductAccountNumber());
        assertEquals("123456", saved.getSavingsAccountNumber());
        assertEquals("Test Customer", saved.getConfirmedName());
        assertEquals(1000.0, saved.getMonthlyAmount());
        assertEquals(1000.0, saved.getAmountLimit());
        assertEquals(5, saved.getDebitDay());
        assertEquals("RD_ECS_CONSENT_V1", saved.getConsentVersion());
        assertTrue(saved.getConsentStatement().contains("I authorize NeoBank"));
        assertEquals("127.0.0.1", saved.getRemoteAddress());
        assertNotNull(saved.getAcceptedAt());
        verify(recurringDepositEcsService).activateAfterConsent(17L, "Test Customer");
    }

    @Test
    void adminLookupReturnsSavedRecordAndDoesNotInventLegacyWording() {
        EcsMandate mandate = mandate();
        mandate.setCustomerConsentAt(LocalDateTime.now().minusDays(1));
        mandate.setCustomerConsentName("Test Customer");
        when(mandateRepository.findById(17L)).thenReturn(Optional.of(mandate));
        when(consentRecordRepository.findByMandateDbId(17L)).thenReturn(Optional.empty());

        EcsConsentRecord legacyRecord = service.consentRecord(17L);

        assertNotNull(legacyRecord);
        assertEquals("LEGACY_METADATA_ONLY", legacyRecord.getConsentVersion());
        assertNull(legacyRecord.getConsentStatement());
    }

    private EcsMandate mandate() {
        EcsMandate mandate = new EcsMandate();
        mandate.setId(17L);
        mandate.setMandateId("ECS-17");
        mandate.setProductType("RECURRING_DEPOSIT");
        mandate.setLoanAccountNumber("RD00000017");
        mandate.setSavingsAccountNumber("123456");
        mandate.setCustomerName("Test Customer");
        mandate.setEmiAmount(1000.0);
        mandate.setAmountLimit(1000.0);
        mandate.setDebitDay(5);
        mandate.setStatus("PENDING_CONSENT");
        mandate.setConsentExpiresAt(LocalDateTime.now().plusMinutes(10));
        return mandate;
    }
}
