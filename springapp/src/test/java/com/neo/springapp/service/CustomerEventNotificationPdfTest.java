package com.neo.springapp.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerEventNotificationPdfTest {

    @Test
    void generatesPdfForCustomerEvent() throws IOException {
        byte[] pdf = new PdfService().generateCustomerEventSummary(
                "Profile update", "Approved", "REQ-123",
                Map.of("Field", "Address", "New value", "<script>alert(1)</script>"));

        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).startsWith("%PDF"));
        assertTrue(pdf.length > 500);
    }
}
