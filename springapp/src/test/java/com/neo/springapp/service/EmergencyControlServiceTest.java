package com.neo.springapp.service;

import com.neo.springapp.model.EmergencyControl;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.EmergencyControlAuditRepository;
import com.neo.springapp.repository.EmergencyControlRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyControlServiceTest {
    @Mock
    private EmergencyControlRepository controlRepository;
    @Mock
    private EmergencyControlAuditRepository auditRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private SalaryAccountRepository salaryAccountRepository;
    @InjectMocks
    private EmergencyControlService service;

    @Test
    void globalRailPauseBlocksTransfersFromAnyAccount() {
        EmergencyControl pausedRail = new EmergencyControl();
        pausedRail.setActive(true);
        when(controlRepository.findByScopeTypeAndScopeKey("RAIL", "NEFT"))
                .thenReturn(Optional.of(pausedRail));

        assertThat(service.isOutboundBlocked("acc100", "neft")).isTrue();
    }

    @Test
    void accountIsolationBlocksEveryOutboundRailButDoesNotPauseOtherAccounts() {
        EmergencyControl isolation = new EmergencyControl();
        isolation.setActive(true);
        when(controlRepository.findByScopeTypeAndScopeKey("RAIL", "IMPS"))
                .thenReturn(Optional.empty());
        when(controlRepository.findByScopeTypeAndScopeKey("ACCOUNT", "ACC200"))
                .thenReturn(Optional.of(isolation));

        assertThat(service.isOutboundBlocked("ACC200", "IMPS")).isTrue();
        assertThat(service.isAccountIsolated("ACC200")).isTrue();
    }

    @Test
    void emergencyFreezeBlocksSalaryDebitCard() {
        SalaryAccount salaryAccount = new SalaryAccount();
        salaryAccount.setAccountNumber("SAL300");
        salaryAccount.setDebitCardStatus("Active");
        EmergencyControl pausedCard = new EmergencyControl();
        pausedCard.setActive(true);
        when(controlRepository.findByScopeTypeAndScopeKey("CARD", "SALARY:SAL300"))
                .thenReturn(Optional.empty(), Optional.of(pausedCard));
        when(controlRepository.save(any(EmergencyControl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(salaryAccountRepository.findByAccountNumber("SAL300")).thenReturn(salaryAccount);

        service.setControl("CARD", "SALARY:SAL300", true, "Incident review", "admin", "Active");

        assertThat(salaryAccount.getDebitCardStatus()).isEqualTo("Blocked");
        assertThat(service.isCardFrozen("SALARY:SAL300")).isTrue();
    }
}
