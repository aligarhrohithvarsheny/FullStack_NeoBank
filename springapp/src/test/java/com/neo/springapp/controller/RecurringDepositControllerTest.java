package com.neo.springapp.controller;

import com.neo.springapp.service.DepositManagementHistoryService;
import com.neo.springapp.service.RecurringDepositService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecurringDepositControllerTest {

    @Mock private RecurringDepositService service;
    @Mock private DepositManagementHistoryService historyService;
    @InjectMocks private RecurringDepositController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void contributionsRouteDispatchesToRecurringDepositController() throws Exception {
        when(service.getById(1L)).thenReturn(null);

        mockMvc.perform(get("/api/recurring-deposits/1/contributions"))
                .andExpect(status().isNotFound());

        verify(service).getById(1L);
    }
}
