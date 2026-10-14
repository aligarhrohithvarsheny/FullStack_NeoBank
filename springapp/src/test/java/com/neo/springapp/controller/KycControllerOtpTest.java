package com.neo.springapp.controller;

import com.neo.springapp.service.EmailService;
import com.neo.springapp.service.KycService;
import com.neo.springapp.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycControllerOtpTest {

    @Mock private KycService kycService;
    @Mock private EmailService emailService;
    @Mock private OtpService otpService;

    private KycController controller;

    @BeforeEach
    void setUp() {
        controller = new KycController();
        ReflectionTestUtils.setField(controller, "kycService", kycService);
        ReflectionTestUtils.setField(controller, "emailService", emailService);
        ReflectionTestUtils.setField(controller, "otpService", otpService);
    }

    @Test
    void failedEmailDeliveryReturnsServiceUnavailableWithoutStoringOtp() {
        Map<String, String> request = Map.of(
                "userEmail", "customer@example.com",
                "userAccountNumber", "1234567890");
        when(kycService.hasExistingKycRequests("1234567890")).thenReturn(true);
        when(otpService.generateOtp()).thenReturn("123456");
        when(emailService.sendKycUpdateOtpEmail("customer@example.com", "123456")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.sendKycOtp(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("success", false);
        verify(otpService, never()).storeOtp("customer@example.com", "123456");
    }

    @Test
    void successfulEmailDeliveryStoresOtpAndReturnsOk() {
        Map<String, String> request = Map.of(
                "userEmail", "customer@example.com",
                "userAccountNumber", "1234567890");
        when(kycService.hasExistingKycRequests("1234567890")).thenReturn(true);
        when(otpService.generateOtp()).thenReturn("123456");
        when(emailService.sendKycUpdateOtpEmail("customer@example.com", "123456")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.sendKycOtp(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(otpService).storeOtp("customer@example.com", "123456");
    }
}
