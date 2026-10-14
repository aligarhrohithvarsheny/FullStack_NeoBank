package com.neo.springapp.config;

import com.neo.springapp.service.SavingsFeatureAccessService;
import com.neo.springapp.service.UserSessionTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SavingsFeatureAccessInterceptorTest {
    private final SavingsFeatureAccessService featureAccessService = mock(SavingsFeatureAccessService.class);
    private final SavingsFeatureAccessInterceptor interceptor =
            new SavingsFeatureAccessInterceptor(featureAccessService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksPaymentLinkRequestsWhenUpiPayIsDisabled() throws Exception {
        var principal = new UserSessionTokenService.SessionPrincipal(1L, "ACC123", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(featureAccessService.isFeatureEnabled("ACC123", "upi-pay")).thenReturn(false);
        var request = new MockHttpServletRequest("POST", "/api/payment-gateway/payment-links");
        var response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(423);
        assertThat(response.getContentAsString()).contains("This feature is disabled");
        verify(featureAccessService).isFeatureEnabled("ACC123", "upi-pay");
    }
}
