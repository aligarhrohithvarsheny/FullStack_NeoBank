package com.neo.springapp.service;

import com.neo.springapp.config.BankFormCatalog;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

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

    @Test
    void longVerifiedAddressWrapsAndEveryPageHasASeparateFooter() throws Exception {
        BankFormService service = new BankFormService(
                null, null, null, null, null, null, null, null
        );
        String address = "12 Main Street, " + "Mumbai ".repeat(500);
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
                "john@example.com",
                "1990-01-02",
                address
        );

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertTrue(document.getNumberOfPages() > 1);
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                assertTrue(pageText.contains("Page " + page), "missing footer on page " + page);
                if (page == 1) {
                    assertTrue(pageText.contains("1990-01-02"), "missing date of birth");
                }
            }
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            assertTrue(stripper.getText(document).contains("Mumbai"));
        }
    }
}
