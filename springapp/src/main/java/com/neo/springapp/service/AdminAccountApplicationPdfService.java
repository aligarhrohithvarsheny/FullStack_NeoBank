package com.neo.springapp.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.neo.springapp.model.AdminAccountApplication;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdminAccountApplicationPdfService {

    private static final String BANK_NAME = "NeoBank";
    private static final String BANK_TAGLINE = "Your Digital Banking Partner";
    private static final String BANK_ADDRESS = "NeoBank Corporate Office, Financial District, Hyderabad - 500032";

    public byte[] generateApplicationForm(AdminAccountApplication app) throws IOException {
        String html = buildApplicationHtml(app);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        return outputStream.toByteArray();
    }

    private String buildApplicationHtml(AdminAccountApplication app) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        String dateOnly = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String accountTypeLabel = getAccountTypeLabel(app.getAccountType());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html><head><meta charset=\"UTF-8\">")
            .append("<title>").append(BANK_NAME).append(" - Account Opening Application</title>")
            .append("<style>")
            .append(getStyles())
            .append("</style></head><body>")

            // Watermark
            .append("<div class=\"watermark\">").append(BANK_NAME).append("</div>")
            .append("<div class=\"watermark-secondary\">CONFIDENTIAL</div>")

            // Main container
            .append("<div class=\"application-container\">")

            // Header with bank logo and details
            .append("<div class=\"header\">")
            .append("<div class=\"bank-logo\">⭐ ").append(BANK_NAME).append("</div>")
            .append("<div class=\"bank-tagline\">").append(BANK_TAGLINE).append("</div>")
            .append("<div class=\"bank-address\">").append(BANK_ADDRESS).append("</div>")
            .append("<div class=\"app-title\">").append(accountTypeLabel).append(" ACCOUNT OPENING APPLICATION FORM</div>")
            .append("<div class=\"app-meta\">")
            .append("<span>Application No: <strong>").append(safe(app.getApplicationNumber())).append("</strong></span>")
            .append("<span>Date: <strong>").append(dateOnly).append("</strong></span>")
            .append("<span>Time: <strong>").append(now).append("</strong></span>")
            .append("</div>")
            .append("</div>")

            // Application type badge
            .append("<div class=\"type-badge-row\">")
            .append("<span class=\"type-badge\">Account Type: ").append(safe(app.getAccountType())).append("</span>")
            .append("<span class=\"type-badge\">Branch: ").append(safe(app.getBranchName())).append("</span>")
            .append("<span class=\"type-badge\">IFSC: ").append(safe(app.getIfscCode())).append("</span>")
            .append("</div>");

        // Section 1: Personal Details
        html.append("<div class=\"section\">")
            .append("<div class=\"section-title\">1. PERSONAL / APPLICANT DETAILS</div>")
            .append("<table class=\"detail-table\">")
            .append(row("Full Name", safe(app.getFullName())))
            .append(row("Date of Birth", safe(app.getDateOfBirth())))
            .append(row("Age", app.getAge() != null ? String.valueOf(app.getAge()) : ""))
            .append(row("Gender", safe(app.getGender())))
            .append(row("Occupation", safe(app.getOccupation())))
            .append(row("Annual Income", app.getIncome() != null ? "₹" + String.format("%,.2f", app.getIncome()) : ""))
            .append(row("Mobile Number", safe(app.getPhone())))
            .append(row("Email", safe(app.getEmail())))
            .append("</table></div>");

        // Section 2: Address
        html.append("<div class=\"section\">")
            .append("<div class=\"section-title\">2. ADDRESS DETAILS</div>")
            .append("<table class=\"detail-table\">")
            .append(row("Address", safe(app.getAddress())))
            .append(row("City", safe(app.getCity())))
            .append(row("State", safe(app.getState())))
            .append(row("Pincode", safe(app.getPincode())))
            .append("</table></div>");

        // Section 3: Identity Documents
        html.append("<div class=\"section\">")
            .append("<div class=\"section-title\">3. IDENTITY DOCUMENTS (KYC)</div>")
            .append("<table class=\"detail-table\">")
            .append(row("Aadhaar Number", maskAadhar(app.getAadharNumber())))
            .append(row("PAN Number", safe(app.getPanNumber())))
            .append(row("KYC Verification", "Admin Verified (Without Video KYC)"))
            .append("</table></div>");

        // Section 4: Business Details (for Current account)
        if ("Current".equalsIgnoreCase(app.getAccountType())) {
            html.append("<div class=\"section\">")
                .append("<div class=\"section-title\">4. BUSINESS DETAILS</div>")
                .append("<table class=\"detail-table\">")
                .append(row("Business Name", safe(app.getBusinessName())))
                .append(row("Business Type", safe(app.getBusinessType())))
                .append(row("Registration Number", safe(app.getBusinessRegistrationNumber())))
                .append(row("GST Number", safe(app.getGstNumber())))
                .append(row("Business Address", safe(app.getShopAddress())))
                .append("</table></div>");
        }

        // Section 4/5: Salary Details (for Salary account)
        if ("Salary".equalsIgnoreCase(app.getAccountType())) {
            html.append("<div class=\"section\">")
                .append("<div class=\"section-title\">4. EMPLOYMENT DETAILS</div>")
                .append("<table class=\"detail-table\">")
                .append(row("Company Name", safe(app.getCompanyName())))
                .append(row("Company ID", safe(app.getCompanyId())))
                .append(row("Designation", safe(app.getDesignation())))
                .append(row("Monthly Salary", app.getMonthlySalary() != null ? "₹" + String.format("%,.2f", app.getMonthlySalary()) : ""))
                .append(row("Salary Credit Date", app.getSalaryCreditDate() != null ? String.valueOf(app.getSalaryCreditDate()) + " of every month" : ""))
                .append(row("Employer Address", safe(app.getEmployerAddress())))
                .append(row("HR Contact", safe(app.getHrContactNumber())))
                .append("</table></div>");
        }

        // Section: Bank Details
        html.append("<div class=\"section\">")
            .append("<div class=\"section-title\">").append("Current".equalsIgnoreCase(app.getAccountType()) || "Salary".equalsIgnoreCase(app.getAccountType()) ? "5" : "4").append(". BANK DETAILS</div>")
            .append("<table class=\"detail-table\">")
            .append(row("Branch Name", safe(app.getBranchName())))
            .append(row("IFSC Code", safe(app.getIfscCode())))
            .append(row("Account Number", app.getAccountNumber() != null ? app.getAccountNumber() : "To be assigned upon approval"))
            .append(row("Customer ID", app.getCustomerId() != null ? app.getCustomerId() : "To be assigned upon approval"))
            .append("</table></div>");

        // Section: Declaration
        String declSection = "Current".equalsIgnoreCase(app.getAccountType()) || "Salary".equalsIgnoreCase(app.getAccountType()) ? "6" : "5";
        html.append("<div class=\"section declaration-section\">")
            .append("<div class=\"section-title\">").append(declSection).append(". DECLARATION</div>")
            .append("<div class=\"declaration-text\">")
            .append("<p>I/We hereby declare that:</p>")
            .append("<ol>")
            .append("<li>The information provided in this application is true, correct, and complete to the best of my/our knowledge and belief.</li>")
            .append("<li>I/We agree to be bound by the terms and conditions of ").append(BANK_NAME).append(" for operating the account.</li>")
            .append("<li>I/We understand that the bank reserves the right to close the account at any time if any information provided is found to be false or misleading.</li>")
            .append("<li>I/We authorize ").append(BANK_NAME).append(" to verify any of the information mentioned in this form from any source it deems appropriate.</li>")
            .append("<li>I/We confirm that no account opening video KYC is required for this application as it is being processed through the bank branch with in-person verification by an authorized bank officer.</li>")
            .append("<li>I/We agree to comply with all regulatory requirements including KYC/AML guidelines issued by RBI from time to time.</li>")
            .append("</ol>")
            .append("</div></div>");

        // Section: Signatures
        String sigSection = "Current".equalsIgnoreCase(app.getAccountType()) || "Salary".equalsIgnoreCase(app.getAccountType()) ? "7" : "6";
        html.append("<div class=\"section signature-section\">")
            .append("<div class=\"section-title\">").append(sigSection).append(". SIGNATURES</div>")
            .append("<div class=\"signature-grid\">")

            // Applicant signature
            .append("<div class=\"signature-box\">")
            .append("<div class=\"signature-line\"></div>")
            .append("<div class=\"signature-label\">Applicant's Signature</div>")
            .append("<div class=\"signature-name\">").append(safe(app.getFullName())).append("</div>")
            .append("<div class=\"signature-date\">Date: ").append(dateOnly).append("</div>")
            .append("</div>")

            // Bank Officer signature
            .append("<div class=\"signature-box\">")
            .append("<div class=\"signature-line\"></div>")
            .append("<div class=\"signature-label\">Bank Officer's Signature</div>")
            .append("<div class=\"signature-name\">").append(safe(app.getBankOfficerSignature() != null ? app.getBankOfficerSignature() : app.getCreatedBy())).append("</div>")
            .append("<div class=\"signature-date\">Date: ").append(dateOnly).append("</div>")
            .append("</div>")

            // Manager signature
            .append("<div class=\"signature-box\">")
            .append("<div class=\"signature-line\"></div>")
            .append("<div class=\"signature-label\">Manager's Signature & Seal</div>")
            .append("<div class=\"signature-name\">").append(safe(app.getManagerApprovedBy() != null ? app.getManagerApprovedBy() : "Pending Approval")).append("</div>")
            .append("<div class=\"signature-date\">Date: ").append(app.getManagerApprovedDate() != null ? app.getManagerApprovedDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "_____________").append("</div>")
            .append("</div>")

            .append("</div></div>");

        // Verification Section
        html.append("<div class=\"section verification-section\">")
            .append("<div class=\"section-title\">VERIFICATION STATUS</div>")
            .append("<table class=\"detail-table\">")
            .append(row("Admin Verified", app.getAdminVerified() != null && app.getAdminVerified() ? "✓ Yes - by " + safe(app.getAdminVerifiedBy()) : "Pending"))
            .append(row("Admin Verification Date", app.getAdminVerifiedDate() != null ? app.getAdminVerifiedDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : "Pending"))
            .append(row("Manager Approved", app.getManagerApproved() != null && app.getManagerApproved() ? "✓ Yes - by " + safe(app.getManagerApprovedBy()) : "Pending"))
            .append(row("Manager Approval Date", app.getManagerApprovedDate() != null ? app.getManagerApprovedDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : "Pending"))
            .append(row("Admin Remarks", safe(app.getAdminRemarks())))
            .append(row("Manager Remarks", safe(app.getManagerRemarks())))
            .append("</table></div>");

        // Footer
        html.append("<div class=\"footer\">")
            .append("<div class=\"footer-bank\">")
            .append("<strong>⭐ ").append(BANK_NAME).append("</strong> | ").append(BANK_TAGLINE)
            .append("</div>")
            .append("<div class=\"footer-address\">").append(BANK_ADDRESS).append("</div>")
            .append("<div class=\"footer-note\">This is a computer-generated document. The application is valid only with authorized signatures and bank seal.</div>")
            .append("<div class=\"footer-ref\">Ref: ").append(safe(app.getApplicationNumber())).append(" | Generated: ").append(now).append("</div>")
            .append("</div>")

            .append("</div>") // close application-container
            .append("</body></html>");

        return html.toString();
    }

    private String getStyles() {
        return "* { margin: 0; padding: 0; box-sizing: border-box; }"
            + "body { font-family: 'Arial', sans-serif; font-size: 11px; color: #333; background: #fff; padding: 15px; position: relative; }"
            + ".watermark { position: fixed; top: 45%; left: 50%; transform: translate(-50%, -50%) rotate(-45deg); font-size: 80px; color: rgba(30, 64, 175, 0.06); font-weight: bold; z-index: -1; pointer-events: none; white-space: nowrap; letter-spacing: 15px; }"
            + ".watermark-secondary { position: fixed; top: 70%; left: 25%; transform: rotate(-30deg); font-size: 50px; color: rgba(200, 0, 0, 0.04); font-weight: bold; z-index: -1; pointer-events: none; letter-spacing: 10px; }"
            + ".application-container { max-width: 750px; margin: 0 auto; position: relative; z-index: 1; }"
            + ".header { text-align: center; padding: 15px; border: 2px solid #1e40af; border-radius: 8px; margin-bottom: 15px; background: linear-gradient(135deg, #f0f4ff, #e8eeff); }"
            + ".bank-logo { font-size: 22px; font-weight: bold; color: #1e40af; margin-bottom: 3px; }"
            + ".bank-tagline { font-size: 11px; color: #4b5563; font-style: italic; margin-bottom: 3px; }"
            + ".bank-address { font-size: 9px; color: #6b7280; margin-bottom: 10px; }"
            + ".app-title { font-size: 14px; font-weight: bold; color: #1e40af; padding: 8px; background: #1e40af; color: white; border-radius: 4px; margin: 8px 0; letter-spacing: 1px; }"
            + ".app-meta { display: flex; justify-content: space-between; font-size: 10px; color: #555; margin-top: 8px; }"
            + ".type-badge-row { display: flex; justify-content: center; gap: 15px; margin-bottom: 15px; }"
            + ".type-badge { background: #e0e7ff; color: #3730a3; padding: 4px 12px; border-radius: 12px; font-size: 10px; font-weight: 600; }"
            + ".section { margin-bottom: 12px; border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; }"
            + ".section-title { background: #1e40af; color: white; padding: 6px 12px; font-size: 11px; font-weight: bold; letter-spacing: 0.5px; }"
            + ".detail-table { width: 100%; border-collapse: collapse; }"
            + ".detail-table td { padding: 5px 12px; border-bottom: 1px solid #f3f4f6; font-size: 10px; }"
            + ".detail-table td:first-child { font-weight: 600; color: #4b5563; width: 35%; background: #f9fafb; }"
            + ".detail-table td:last-child { color: #111; }"
            + ".declaration-section .declaration-text { padding: 12px; font-size: 10px; line-height: 1.6; }"
            + ".declaration-section ol { margin-left: 20px; margin-top: 8px; }"
            + ".declaration-section li { margin-bottom: 4px; }"
            + ".signature-section .signature-grid { display: flex; justify-content: space-between; padding: 15px; gap: 20px; }"
            + ".signature-box { flex: 1; text-align: center; }"
            + ".signature-line { border-bottom: 1px solid #333; width: 100%; height: 50px; margin-bottom: 5px; }"
            + ".signature-label { font-weight: bold; font-size: 10px; color: #1e40af; }"
            + ".signature-name { font-size: 9px; color: #555; margin-top: 2px; }"
            + ".signature-date { font-size: 9px; color: #777; margin-top: 2px; }"
            + ".verification-section { border: 2px solid #059669; }"
            + ".verification-section .section-title { background: #059669; }"
            + ".footer { margin-top: 15px; padding: 12px; text-align: center; border-top: 2px solid #1e40af; font-size: 9px; color: #666; }"
            + ".footer-bank { font-size: 12px; color: #1e40af; margin-bottom: 3px; }"
            + ".footer-address { margin-bottom: 5px; }"
            + ".footer-note { font-style: italic; margin-bottom: 3px; color: #999; }"
            + ".footer-ref { font-size: 8px; color: #aaa; }";
    }

    private String row(String label, String value) {
        return "<tr><td>" + label + "</td><td>" + (value != null ? value : "") + "</td></tr>";
    }

    private String safe(String value) {
        return value != null ? value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;") : "";
    }

    private String maskAadhar(String aadhar) {
        if (aadhar == null || aadhar.length() < 4) return safe(aadhar);
        return "XXXX-XXXX-" + aadhar.substring(aadhar.length() - 4);
    }

    private String getAccountTypeLabel(String accountType) {
        if (accountType == null) return "SAVINGS";
        switch (accountType.toLowerCase()) {
            case "current": return "CURRENT";
            case "salary": return "SALARY";
            default: return "SAVINGS";
        }
    }
}
