package com.neo.springapp.service;

import com.neo.springapp.repository.UserLoginHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLoginHistoryServiceTest {

    @Mock
    private UserLoginHistoryRepository loginHistoryRepository;

    @InjectMocks
    private UserLoginHistoryService loginHistoryService;

    @Test
    void recentHistoryUsesDatabaseLimitAndCapsRequestedRows() {
        when(loginHistoryRepository.findAllByOrderByLoginTimeDesc(PageRequest.of(0, 100)))
                .thenReturn(List.of());

        assertThat(loginHistoryService.getRecentLoginHistory(500)).isEmpty();

        verify(loginHistoryRepository).findAllByOrderByLoginTimeDesc(PageRequest.of(0, 100));
    }

    @Test
    void zeroLimitReturnsNoRowsWithoutQueryingDatabase() {
        assertThat(loginHistoryService.getRecentLoginHistory(0)).isEmpty();
        verifyNoInteractions(loginHistoryRepository);
    }
}
