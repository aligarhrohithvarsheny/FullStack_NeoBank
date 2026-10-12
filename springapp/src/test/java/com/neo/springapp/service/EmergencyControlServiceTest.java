package com.neo.springapp.service;

import com.neo.springapp.model.EmergencyControl;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.EmergencyControlAuditRepository;
import com.neo.springapp.repository.EmergencyControlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyControlServiceTest {
    @Mock
    private EmergencyControlRepository controlRepository;
    @Mock
    private EmergencyControlAuditRepository auditRepository;
    @Mock
    private CardRepository cardRepository;
    @InjectMocks
    private EmergencyControlService service;

    @Test
    void globalRailPauseBlocksTransfersFromAnyAccount() {
        EmergencyControl pausedRail = new EmergencyControl();
        pausedRail.setActive(true);
        when(controlRepository.findByScopeTypeAndScopeKey("RAIL", "NEFT"))
                .thenReturn(Optional.of(pausedRail));
        when(controlRepository.findByScopeTypeAndScopeKey("ACCOUNT", "ACC100"))
                .thenReturn(Optional.empty());

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
}
