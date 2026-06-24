package com.neo.springapp.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.neo.springapp.model.AdminAccountApplication;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class AdminAccountApplicationPdfGenerationTest {

    private final AdminAccountApplicationPdfService pdfService = new AdminAccountApplicationPdfService();

    @Test
    void generateApplicationFormProducesValidPdf() throws Exception {
        AdminAccountApplication app = new AdminAccountApplication();
        app.setApplicationNumber("APP-2026-0001");
        app.setAccountType("Savings");
        app.setFullName("Test User");
        app.setDateOfBirth("1990-01-01");
        app.setAge(36);
        app.setGender("Male");
        app.setOccupation("Engineer");
        app.setIncome(850000.0);
        app.setPhone("9876543210");
        app.setEmail("test@example.com");
        app.setAddress("123 Main Street");
        app.setCity("Mumbai");
        app.setState("Maharashtra");
        app.setPincode("400001");
        app.setAadharNumber("123456789012");
        app.setPanNumber("ABCDE1234F");
        app.setBranchName("NeoBank Main Branch");
        app.setIfscCode("EZYV000123");
        app.setCreatedBy("Admin");

        byte[] pdf = pdfService.generateApplicationForm(app);
        assertNotNull(pdf);
        assertTrue(pdf.length > 500);
        assertTrue(new String(pdf, 0, Math.min(5, pdf.length)).startsWith("%PDF"));
    }

    @Test
    void tableWidthAttributeDoesNotBreakHtmlConverter() throws Exception {
        String html = """
                <!DOCTYPE html><html><head><meta charset="UTF-8"/></head><body>
                <table width="100%" cellspacing="0" cellpadding="0">
                  <tr><td>Name</td><td>John Doe</td></tr>
                </table>
                </body></html>
                """;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, out);
        assertTrue(out.size() > 100);
    }
}
