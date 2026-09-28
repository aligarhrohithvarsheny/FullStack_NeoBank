package com.neo.springapp.controller;

import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.service.AllocationAccountLinkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationAccountLinkingControllerTest {

    @Mock private AllocationAccountLinkingService accountLinkingService;

    private AllocationAccountLinkingController controller;

    @BeforeEach
    void setUp() {
        controller = new AllocationAccountLinkingController();
        ReflectionTestUtils.setField(controller, "accountLinkingService", accountLinkingService);
    }

    @Test
    void approvalWithMissingNotesDoesNotThrowNullPointerException() {
        Map<String, Object> request = new HashMap<>();
        request.put("allocationId", 12);
        request.put("approved", true);
        request.put("verifiedByAdminId", 4);
        request.put("verificationNotes", null);
        when(accountLinkingService.verifyChequeAndAccount(12L, true, "", 4L, null))
            .thenReturn(new AllocationAccount());

        ResponseEntity<?> response = controller.verifyAccount(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(accountLinkingService).verifyChequeAndAccount(12L, true, "", 4L, null);
    }

    @Test
    void missingAdminIdReturnsBadRequestInsteadOfNullPointerException() {
        Map<String, Object> request = new HashMap<>();
        request.put("allocationId", 12);
        request.put("approved", true);

        ResponseEntity<?> response = controller.verifyAccount(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString()).contains("verifiedByAdminId is required");
    }
}