package com.neo.springapp.service;

import com.neo.springapp.config.BankFormCatalog;
import com.neo.springapp.model.EcsMandate;
import com.neo.springapp.model.FixedDeposit;
import com.neo.springapp.model.HomeLoan;
import com.neo.springapp.model.RecurringDeposit;
import com.neo.springapp.repository.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    void carLoanFormResolvesLoanAndLinkedSavingsAccountAndPrefillsPdf() throws Exception {
        HomeLoanRepository homeLoans = mock(HomeLoanRepository.class);
        HomeLoan loan = new HomeLoan();
        loan.setLoanAccountNumber("CAR-LOAN-100");
        loan.setAccountNumber("SAV-100");
        loan.setUserName("Neha Rao");
        loan.setLoanType("CAR");
        loan.setAmount(850000.0);
        loan.setTenure(60);
        loan.setInterestRate(9.5);
        loan.setEmi(17823.0);
        loan.setVehicleMakeModel("Neo Sedan");
        loan.setStatus("Approved");
        loan.setNetDisbursed(820000.0);
        when(homeLoans.findByLoanAccountNumber("CAR-LOAN-100")).thenReturn(Optional.of(loan));
        BankFormService service = productLookupService(homeLoans, null, null, null);

        var result = service.verifyAccount("CAR-LOAN-100", "homeloan", null, "car-loan");
        assertEquals(true, result.get("success"));
        var details = (java.util.Map<?, ?>) ((java.util.Map<?, ?>) result.get("account")).get("formDetails");
        assertEquals("SAV-100", ((java.util.Map<?, ?>) result.get("account")).get("linkedSavingsAccountNumber"));
        assertEquals("850000.0", details.get("Approved Loan Amount"));
        assertEquals("820000.0", details.get("Disbursed Amount"));

        byte[] pdf = service.buildBlankFormPdf(
                "car-loan", "Admin", "CAR-LOAN-100", "homeloan", "Neha Rao",
                null, null, null, null, null);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Neo Sedan"));
            assertTrue(text.contains("SAV-100"));
            assertTrue(text.contains("820000.0"));
        }
    }

    @Test
    void fixedAndRecurringDepositFormsIncludeProductAndEcsDetails() {
        FixedDepositRepository fixedDeposits = mock(FixedDepositRepository.class);
        FixedDeposit fixed = new FixedDeposit();
        fixed.setFdAccountNumber("FD-200");
        fixed.setAccountNumber("SAV-200");
        fixed.setUserName("Arun Kumar");
        fixed.setSchemeName("Neo Growth");
        fixed.setPrincipalAmount(50000.0);
        fixed.setInterestRate(7.5);
        fixed.setTenure(24);
        fixed.setMaturityAmount(58000.0);
        when(fixedDeposits.findByFdAccountNumber("FD-200")).thenReturn(Optional.of(fixed));

        RecurringDepositRepository recurringDeposits = mock(RecurringDepositRepository.class);
        RecurringDeposit recurring = new RecurringDeposit();
        recurring.setRdAccountNumber("RD-300");
        recurring.setAccountNumber("SAV-300");
        recurring.setCustomerName("Meera Shah");
        recurring.setMonthlyInstallment(2000.0);
        recurring.setAnnualInterestRate(6.5);
        recurring.setTenureMonths(12);
        recurring.setEstimatedPrincipal(24000.0);
        recurring.setEstimatedInterest(800.0);
        recurring.setEstimatedMaturityAmount(24800.0);
        when(recurringDeposits.findByRdAccountNumber("RD-300")).thenReturn(Optional.of(recurring));

        EcsMandateRepository mandates = mock(EcsMandateRepository.class);
        EcsMandate mandate = new EcsMandate();
        mandate.setMandateId("ECS-500");
        mandate.setLoanAccountNumber("FD-200");
        mandate.setSavingsAccountNumber("SAV-200");
        mandate.setStatus("ACTIVE");
        mandate.setEmiAmount(2000.0);
        when(mandates.findByLoanAccountNumberOrderByCreatedAtDesc("FD-200")).thenReturn(List.of(mandate));
        when(mandates.findBySavingsAccountNumberOrderByCreatedAtDesc("SAV-200")).thenReturn(List.of(mandate));
        when(mandates.findByLoanAccountNumberOrderByCreatedAtDesc("RD-300")).thenReturn(List.of());
        when(mandates.findBySavingsAccountNumberOrderByCreatedAtDesc("SAV-300")).thenReturn(List.of());

        BankFormService service = productLookupService(null, fixedDeposits, recurringDeposits, mandates);
        var fdResult = service.verifyAccount("FD-200", "fixeddeposit", null, "fixed-deposit-application");
        var fdAccount = (java.util.Map<?, ?>) fdResult.get("account");
        assertEquals("SAV-200", fdAccount.get("linkedSavingsAccountNumber"));
        assertEquals(1, ((List<?>) fdAccount.get("ecsMandates")).size());
        assertEquals("Neo Growth",
                ((java.util.Map<?, ?>) fdAccount.get("formDetails")).get("Deposit Scheme"));

        var rdResult = service.verifyAccount("RD-300", "recurringdeposit", null, "recurring-deposit-application");
        var rdAccount = (java.util.Map<?, ?>) rdResult.get("account");
        var rdDetails = (java.util.Map<?, ?>) rdAccount.get("formDetails");
        assertEquals("24800.0", rdDetails.get("Estimated Interest and Maturity Amount").toString().split(" \\| ")[1]);
        assertEquals("SAV-300", rdAccount.get("linkedSavingsAccountNumber"));
    }

    private static BankFormService productLookupService(
            HomeLoanRepository homeLoans,
            FixedDepositRepository fixedDeposits,
            RecurringDepositRepository recurringDeposits,
            EcsMandateRepository mandates) {
        return new BankFormService(
                mock(BankFormUploadRepository.class),
                mock(BankFormUploadHistoryRepository.class),
                mock(AccountRepository.class),
                mock(SalaryAccountRepository.class),
                mock(CurrentAccountRepository.class),
                mock(LoanRepository.class),
                mock(GoldLoanRepository.class),
                mock(ChequeRepository.class),
                homeLoans,
                fixedDeposits,
                recurringDeposits,
                mandates,
                mock(EmiPaymentRepository.class));
    }
}
