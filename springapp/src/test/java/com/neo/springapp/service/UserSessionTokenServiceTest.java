package com.neo.springapp.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserSessionTokenServiceTest {

    private final UserSessionTokenService service = new UserSessionTokenService("test-secret-with-at-least-32-characters");

    @Test
    void standardUserTokenCannotBeUsedAsCards360Token() {
        UserSessionTokenService.SessionPrincipal principal = service.verify(service.issue(7L, "ACC700"));

        assertThat(principal).isNotNull();
        assertThat(principal.userId()).isEqualTo(7L);
        assertThat(principal.accountNumber()).isEqualTo("ACC700");
        assertThat(principal.scope()).isEqualTo("USER");
    }

    @Test
    void cards360TokenCarriesDedicatedScope() {
        UserSessionTokenService.SessionPrincipal principal = service.verify(service.issueCard360(7L, "ACC700"));

        assertThat(principal).isNotNull();
        assertThat(principal.scope()).isEqualTo("CARD360");
    }

    @Test
    void modifiedTokenIsRejected() {
        String token = service.issueCard360(7L, "ACC700");

        assertThat(service.verify(token + "x")).isNull();
    }
}
