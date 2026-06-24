package com.neo.springapp.service;

import com.neo.springapp.config.BankFormCatalog;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class BankFormPdfGenerationTest {

    @Test
    void buildBlankFormPdfProducesNonEmptyBytes() throws Exception {
        BankFormService service = new BankFormService(
                null, null, null, null, null, null, null, null
        );

        for (BankFormCatalog.FormDefinition form : BankFormCatalog.all()) {
            byte[] pdf = service.buildBlankFormPdf(form.code(), "Admin", null, null, null);
            assertNotNull(pdf, form.code());
            assertTrue(pdf.length > 500, form.code() + " pdf too small: " + pdf.length);
            assertTrue(new String(pdf, 0, Math.min(5, pdf.length)).startsWith("%PDF"), form.code());
        }
    }

    @Test
    void buildBlankFormPdfWithVerifiedAccountDetails() throws Exception {
        BankFormService service = new BankFormService(
                null, null, null, null, null, null, null, null
        );
        byte[] pdf = service.buildBlankFormPdf(
                "account-opening",
                "Admin",
                "1234567890",
                "regular",
                "John Doe",
                "123456789",
                "123456789012",
                "ABCDE1234F",
                "9876543210",
                "john@example.com"
        );
        assertTrue(pdf.length > 500);
        assertTrue(new String(pdf, 0, Math.min(5, pdf.length)).startsWith("%PDF"));
    }
}
