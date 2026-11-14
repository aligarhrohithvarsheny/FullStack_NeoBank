package com.neo.springapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorsConfigTest {

    @Test
    void allowsTheProductionVercelFrontendOrigin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admins/login");
        CorsConfiguration cors = new CorsConfig()
                .corsConfigurationSource()
                .getCorsConfiguration(request);

        assertNotNull(cors);
        assertEquals("https://full-stack-neo-bank22.vercel.app",
                cors.checkOrigin("https://full-stack-neo-bank22.vercel.app"));
    }
}
