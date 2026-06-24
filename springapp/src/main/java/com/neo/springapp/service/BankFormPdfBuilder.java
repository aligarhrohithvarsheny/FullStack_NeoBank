package com.neo.springapp.service;

import com.neo.springapp.config.BankFormCatalog;
import com.neo.springapp.config.BankFormCatalog.FormDefinition;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight PDF generator for bank forms. Uses PDFBox directly instead of html2pdf
 * to avoid OutOfMemoryError on constrained cloud hosts (e.g. Render free tier).
 */
final class BankFormPdfBuilder {

    private static final float MARGIN = 48f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN);

    private static final String BANK_NAME = "NeoBank";
    private static final String BANK_TAGLINE = "Relationship Beyond Banking";
    private static final String BANK_IFSC = "NEOB0000001";
    private static final String BANK_BRANCH = "NeoBank Main Branch, Mumbai";
    private static final String BANK_BRANCH_CODE = "NEOB001";
    private static final String BANK_TOLL_FREE = "1800 103 1906";
    private static final String BANK_EMAIL = "support@neobank.in";
    private static final String BANK_WEBSITE = "www.neobank.in";

    private final PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private PDDocument document;
    private PDPage page;
    private PDPageContentStream stream;
    private float y;

    byte[] build(
            FormDefinition form,
            String adminName,
            String accountNumber,
            String accountType,
            String holderName,
            String customerId,
            String aadhaarNumber,
            String panNumber,
            String phone,
            String email) throws IOException {
        document = new PDDocument();
        try {
            newPage();
            drawHeader();
            drawTitle(form);
            drawAccountBlock(accountNumber, accountType, holderName, customerId, phone, email, aadhaarNumber);
            drawSection("Common Information (All Forms)");
            int index = 1;
            for (String field : BankFormCatalog.COMMON_FIELDS) {
                drawField(index++, field, prefillValue(
                        field, accountNumber, holderName, customerId, aadhaarNumber, panNumber, phone, email));
            }
            drawSection("Form Specific Information");
            for (String field : form.fields()) {
                drawField(index++, field, prefillValue(
                        field, accountNumber, holderName, customerId, aadhaarNumber, panNumber, phone, email));
            }
            drawTerms();
            drawSignatures();
            drawFooter(adminName, form);

            closeStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } finally {
            closeStream();
            if (document != null) {
                document.close();
            }
        }
    }

    private void drawHeader() throws IOException {
        writeLine(BANK_NAME, fontBold, 20, 8);
        writeLine(BANK_TAGLINE, fontRegular, 10, 4);
        writeLine("Branch: " + BANK_BRANCH + "  |  Code: " + BANK_BRANCH_CODE + "  |  IFSC: " + BANK_IFSC,
                fontRegular, 9, 4);
        writeLine("Toll Free: " + BANK_TOLL_FREE + "  |  Email: " + BANK_EMAIL + "  |  Web: " + BANK_WEBSITE,
                fontRegular, 9, 10);
        drawRule();
    }

    private void drawTitle(FormDefinition form) throws IOException {
        writeLine(form.name(), fontBold, 14, 6);
        writeLine("Form #" + form.id() + "  |  Code: " + form.code() + "  |  Category: " + form.category(),
                fontRegular, 9, 12);
    }

    private void drawAccountBlock(
            String accountNumber,
            String accountType,
            String holderName,
            String customerId,
            String phone,
            String email,
            String aadhaarNumber) throws IOException {
        if (accountNumber == null || accountNumber.isBlank()) {
            return;
        }
        drawSection("Linked Account Details");
        drawLabelValue("Account Number", accountNumber.trim());
        drawLabelValue("Account Type", accountType != null && !accountType.isBlank() ? accountType : "regular");
        drawLabelValue("Account Holder", blankToLine(holderName));
        drawLabelValue("Customer ID", blankToLine(customerId));
        drawLabelValue("Mobile", blankToLine(phone));
        drawLabelValue("Email", blankToLine(email));
        drawLabelValue("Aadhaar", maskAadhaar(aadhaarNumber));
        drawLabelValue("IFSC Code", BANK_IFSC);
        drawLabelValue("Branch", BANK_BRANCH + " (" + BANK_BRANCH_CODE + ")");
        y -= 6;
    }

    private void drawSection(String title) throws IOException {
        ensureSpace(24);
        writeLine(title, fontBold, 11, 6);
        drawRule();
    }

    private void drawField(int index, String label, String value) throws IOException {
        ensureSpace(28);
        writeLine(index + ". " + label, fontBold, 10, 2);
        if (value != null && !value.isBlank()) {
            writeWrapped(value, fontRegular, 10, 4);
        } else {
            drawBlankLine();
        }
        y -= 4;
    }

    private void drawLabelValue(String label, String value) throws IOException {
        writeLine(label + ": " + sanitize(value), fontRegular, 9, 3);
    }

    private void drawTerms() throws IOException {
        drawSection("NeoBank Terms and Conditions");
        String[] terms = {
                "1. I declare that all particulars furnished in this application are true, complete, and correct.",
                "2. I authorize NeoBank to verify my identity, address, income, and KYC documents.",
                "3. I agree to abide by RBI guidelines and applicable NeoBank policies.",
                "4. NeoBank may accept or reject this application at its sole discretion.",
                "5. I consent to receive account-related alerts on my registered mobile and email.",
                "6. Furnishing false information may result in rejection or account closure.",
                "7. Disputes shall be subject to courts at Mumbai, Maharashtra, India."
        };
        for (String term : terms) {
            writeWrapped(term, fontRegular, 8, 3);
        }
        writeLine("[ ] I have read, understood, and accept the NeoBank Terms and Conditions.", fontBold, 9, 10);
    }

    private void drawSignatures() throws IOException {
        ensureSpace(60);
        y -= 10;
        writeLine("Customer Signature with Date & Place", fontRegular, 9, 30);
        drawRuleAt(y);
        y -= 20;
        writeLine("Authorized Bank Official / Branch Manager", fontRegular, 9, 30);
        drawRuleAt(y);
        y -= 10;
    }

    private void drawFooter(String adminName, FormDefinition form) throws IOException {
        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        writeLine("Generated on " + generatedAt + " IST  |  Prepared by: "
                        + (adminName != null ? adminName : "Admin")
                        + "  |  " + BANK_NAME + " — " + form.code(),
                fontRegular, 8, 0);
    }

    private void drawRule() throws IOException {
        ensureSpace(8);
        stream.setLineWidth(0.5f);
        stream.moveTo(MARGIN, y);
        stream.lineTo(PAGE_WIDTH - MARGIN, y);
        stream.stroke();
        y -= 8;
    }

    private void drawRuleAt(float ruleY) throws IOException {
        stream.setLineWidth(0.5f);
        stream.moveTo(MARGIN, ruleY);
        stream.lineTo(MARGIN + 220, ruleY);
        stream.stroke();
    }

    private void drawBlankLine() throws IOException {
        ensureSpace(10);
        stream.setLineWidth(0.5f);
        stream.moveTo(MARGIN, y);
        stream.lineTo(PAGE_WIDTH - MARGIN, y);
        stream.stroke();
        y -= 10;
    }

    private void writeLine(String text, PDType1Font font, float size, float gapAfter) throws IOException {
        ensureSpace(size + gapAfter);
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(MARGIN, y);
        stream.showText(sanitize(text));
        stream.endText();
        y -= size + gapAfter;
    }

    private void writeWrapped(String text, PDType1Font font, float size, float gapAfter) throws IOException {
        List<String> lines = wrapText(text, font, size, CONTENT_WIDTH);
        for (String line : lines) {
            ensureSpace(size + 2);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN + 12, y);
            stream.showText(sanitize(line));
            stream.endText();
            y -= size + 2;
        }
        y -= gapAfter;
    }

    private List<String> wrapText(String text, PDType1Font font, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.getStringWidth(sanitize(candidate)) / 1000f * size <= maxWidth) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private void newPage() throws IOException {
        closeStream();
        page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        stream = new PDPageContentStream(document, page);
        y = PAGE_HEIGHT - MARGIN;
    }

    private void ensureSpace(float needed) throws IOException {
        if (y - needed < MARGIN) {
            newPage();
        }
    }

    private void closeStream() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }

    private static String prefillValue(
            String field,
            String accountNumber,
            String holderName,
            String customerId,
            String aadhaarNumber,
            String panNumber,
            String phone,
            String email) {
        if (field == null) {
            return "";
        }
        String normalized = field.toLowerCase(Locale.ROOT);
        if (normalized.contains("account number") && accountNumber != null && !accountNumber.isBlank()) {
            return accountNumber.trim();
        }
        if (normalized.contains("customer id") && customerId != null && !customerId.isBlank()) {
            return customerId.trim();
        }
        if (normalized.contains("aadhaar") && aadhaarNumber != null && !aadhaarNumber.isBlank()) {
            return aadhaarNumber.trim();
        }
        if (normalized.contains("pan") && panNumber != null && !panNumber.isBlank()) {
            return panNumber.trim();
        }
        if ((normalized.contains("mobile") || normalized.contains("phone")) && phone != null && !phone.isBlank()) {
            return phone.trim();
        }
        if (normalized.contains("email") && email != null && !email.isBlank()) {
            return email.trim();
        }
        if ((normalized.equals("name") || normalized.startsWith("name(") || normalized.contains("holder"))
                && holderName != null && !holderName.isBlank()) {
            return holderName.trim();
        }
        if (normalized.contains("bank name")) {
            return BANK_NAME;
        }
        return "";
    }

    private static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) {
            return blankToLine(aadhaar);
        }
        return "XXXX-XXXX-" + aadhaar.substring(aadhaar.length() - 4);
    }

    private static String blankToLine(String value) {
        return value != null && !value.isBlank() ? value.trim() : "________________________";
    }

    private static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (c >= 32 && c <= 255) {
                sb.append(c);
            } else if (c == '\n' || c == '\r') {
                sb.append(' ');
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }
}
