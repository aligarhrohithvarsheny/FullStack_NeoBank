package com.neo.springapp.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.neo.springapp.config.BankFormCatalog;
import com.neo.springapp.config.BankFormCatalog.FormDefinition;
import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@SuppressWarnings("null")
public class BankFormService {

    private static final String UPLOAD_DIR = "uploads/bank-forms";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    private static final String BANK_NAME = "NeoBank";
    private static final String BANK_TAGLINE = "Relationship Beyond Banking";
    private static final String BANK_IFSC = "NEOB0000001";
    private static final String BANK_BRANCH = "NeoBank Main Branch, Mumbai";
    private static final String BANK_BRANCH_CODE = "NEOB001";
    private static final String BANK_CIN = "U65110MH2026PLC000001";
    private static final String BANK_TOLL_FREE = "1800 103 1906";
    private static final String BANK_EMAIL = "support@neobank.in";
    private static final String BANK_WEBSITE = "www.neobank.in";
    private static final String BANK_REGISTERED_OFFICE =
            "NeoBank Tower, Bandra Kurla Complex, Mumbai - 400051, Maharashtra, India";

    private final BankFormUploadRepository bankFormUploadRepository;
    private final AccountRepository accountRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final LoanRepository loanRepository;
    private final GoldLoanRepository goldLoanRepository;
    private final ChequeRepository chequeRepository;

    public BankFormService(
            BankFormUploadRepository bankFormUploadRepository,
            AccountRepository accountRepository,
            SalaryAccountRepository salaryAccountRepository,
            CurrentAccountRepository currentAccountRepository,
            LoanRepository loanRepository,
            GoldLoanRepository goldLoanRepository,
            ChequeRepository chequeRepository) {
        this.bankFormUploadRepository = bankFormUploadRepository;
        this.accountRepository = accountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.loanRepository = loanRepository;
        this.goldLoanRepository = goldLoanRepository;
        this.chequeRepository = chequeRepository;
    }

    public List<Map<String, Object>> listFormDefinitions() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (FormDefinition f : BankFormCatalog.all()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", f.id());
            row.put("code", f.code());
            row.put("name", f.name());
            row.put("category", f.category());
            row.put("fields", f.fields());
            row.put("commonFields", BankFormCatalog.COMMON_FIELDS);
            row.put("allFields", BankFormCatalog.allFieldsFor(f));
            result.add(row);
        }
        return result;
    }

    public List<String> listCategories() {
        return BankFormCatalog.categories();
    }

    public byte[] buildBlankFormPdf(
            String formCode,
            String adminName,
            String accountNumber,
            String accountType,
            String holderName) throws IOException {
        FormDefinition form = BankFormCatalog.findByCode(formCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown form code: " + formCode));

        String html = buildFormHtml(form, adminName, accountNumber, accountType, holderName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, out);
        return out.toByteArray();
    }

    public Optional<BankFormUpload> findUpload(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return bankFormUploadRepository.findById(id);
    }

    public byte[] readUploadedFile(BankFormUpload upload) throws IOException {
        Path path = Paths.get(upload.getStoredFilePath()).normalize();
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Uploaded file not found on server");
        }
        return Files.readAllBytes(path);
    }

    @Transactional
    public BankFormUpload saveUploadedForm(
            String formCode,
            String accountNumber,
            String accountType,
            MultipartFile file,
            String uploadedByAdmin,
            String remarks) throws IOException {

        FormDefinition form = BankFormCatalog.findByCode(formCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown form code: " + formCode));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file to upload");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 15MB limit");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String ext = extension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Only PDF, JPG, JPEG, PNG, or WEBP files are allowed");
        }

        Map<String, Object> accountInfo = resolveAccount(accountNumber, accountType);
        if (accountInfo == null) {
            throw new IllegalArgumentException("Account not found for the given account number and type");
        }

        Path uploadDir = Paths.get(UPLOAD_DIR).normalize();
        Files.createDirectories(uploadDir);

        String safeAccount = accountNumber.replaceAll("[^a-zA-Z0-9]", "");
        String filename = form.code() + "-" + safeAccount + "-" + System.currentTimeMillis() + "." + ext;
        Path target = uploadDir.resolve(filename).normalize();
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        BankFormUpload upload = new BankFormUpload();
        upload.setFormCode(form.code());
        upload.setFormName(form.name());
        upload.setCategory(form.category());
        upload.setAccountNumber(accountNumber.trim());
        upload.setAccountType(normalizeAccountType(accountType));
        upload.setAccountHolderName(String.valueOf(accountInfo.getOrDefault("holderName", "")));
        upload.setOriginalFileName(originalName);
        upload.setStoredFilePath(UPLOAD_DIR + "/" + filename);
        upload.setContentType(file.getContentType());
        upload.setFileSizeBytes(file.getSize());
        upload.setUploadedByAdmin(uploadedByAdmin);
        upload.setRemarks(remarks);
        upload.setUploadedAt(LocalDateTime.now());

        return bankFormUploadRepository.save(upload);
    }

    @Transactional(readOnly = true)
    public List<BankFormUpload> listUploads(String accountNumber, String formCode) {
        if (accountNumber != null && !accountNumber.isBlank()) {
            return bankFormUploadRepository.findByAccountNumberOrderByUploadedAtDesc(accountNumber.trim());
        }
        if (formCode != null && !formCode.isBlank()) {
            return bankFormUploadRepository.findByFormCodeOrderByUploadedAtDesc(formCode.trim());
        }
        return bankFormUploadRepository.findAllByOrderByUploadedAtDesc();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verifyAccount(String accountNumber, String accountType) {
        Map<String, Object> info = resolveAccount(accountNumber, accountType);
        Map<String, Object> response = new LinkedHashMap<>();
        if (info == null) {
            response.put("success", false);
            response.put("message", "Account not found");
            return response;
        }
        response.put("success", true);
        response.put("account", info);
        return response;
    }

    public Map<String, Object> toUploadDto(BankFormUpload upload) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", upload.getId());
        dto.put("formCode", upload.getFormCode());
        dto.put("formName", upload.getFormName());
        dto.put("category", upload.getCategory());
        dto.put("accountNumber", upload.getAccountNumber());
        dto.put("accountType", upload.getAccountType());
        dto.put("accountHolderName", upload.getAccountHolderName());
        dto.put("originalFileName", upload.getOriginalFileName());
        dto.put("storedFilePath", upload.getStoredFilePath());
        dto.put("contentType", upload.getContentType());
        dto.put("fileSizeBytes", upload.getFileSizeBytes());
        dto.put("uploadedByAdmin", upload.getUploadedByAdmin());
        dto.put("remarks", upload.getRemarks());
        dto.put("uploadedAt", upload.getUploadedAt());
        return dto;
    }

    private Map<String, Object> resolveAccount(String accountNumber, String accountType) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }
        String num = accountNumber.trim();
        String type = normalizeAccountType(accountType);

        switch (type) {
            case "regular" -> {
                Account a = accountRepository.findByAccountNumber(num);
                if (a != null) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("accountNumber", a.getAccountNumber());
                    m.put("holderName", a.getName());
                    m.put("accountType", "regular");
                    m.put("balance", a.getBalance());
                    return m;
                }
            }
            case "salary" -> {
                SalaryAccount a = salaryAccountRepository.findByAccountNumber(num);
                if (a != null) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("accountNumber", a.getAccountNumber());
                    m.put("holderName", a.getEmployeeName());
                    m.put("accountType", "salary");
                    m.put("balance", a.getBalance());
                    return m;
                }
            }
            case "current" -> {
                Optional<CurrentAccount> ca = currentAccountRepository.findByAccountNumber(num);
                if (ca.isPresent()) {
                    CurrentAccount a = ca.get();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("accountNumber", a.getAccountNumber());
                    m.put("holderName", a.getBusinessName() != null ? a.getBusinessName() : a.getOwnerName());
                    m.put("accountType", "current");
                    m.put("balance", a.getBalance());
                    return m;
                }
            }
            case "loan" -> {
                Optional<Loan> byLoanNumber = loanRepository.findByLoanAccountNumber(num);
                if (byLoanNumber.isPresent()) {
                    return loanInfo(byLoanNumber.get());
                }
                List<Loan> byAccount = loanRepository.findByAccountNumber(num);
                if (!byAccount.isEmpty()) {
                    return loanInfo(byAccount.get(0));
                }
            }
            case "goldloan" -> {
                Optional<GoldLoan> byLoanNumber = goldLoanRepository.findByLoanAccountNumber(num);
                if (byLoanNumber.isPresent()) {
                    return goldLoanInfo(byLoanNumber.get());
                }
                List<GoldLoan> byAccount = goldLoanRepository.findByAccountNumber(num);
                if (!byAccount.isEmpty()) {
                    return goldLoanInfo(byAccount.get(0));
                }
            }
            case "cheque" -> {
                List<Cheque> cheques = chequeRepository.findByAccountNumber(num);
                if (!cheques.isEmpty()) {
                    Cheque c = cheques.get(0);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("accountNumber", c.getAccountNumber());
                    m.put("holderName", c.getAccountHolderName());
                    m.put("accountType", "cheque");
                    return m;
                }
            }
            default -> {
                return null;
            }
        }
        return null;
    }

    @Transactional
    public boolean deleteUpload(Long id) {
        if (id == null) return false;
        Optional<BankFormUpload> opt = bankFormUploadRepository.findById(id);
        if (opt.isEmpty()) return false;
        BankFormUpload upload = opt.get();
        try {
            Path path = Paths.get(upload.getStoredFilePath()).normalize();
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // DB record still removed even if file missing
        }
        bankFormUploadRepository.delete(upload);
        return true;
    }

    @Transactional
    public int clearAllUploads() {
        List<BankFormUpload> all = bankFormUploadRepository.findAll();
        for (BankFormUpload upload : all) {
            try {
                Path path = Paths.get(upload.getStoredFilePath()).normalize();
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // continue
            }
        }
        int count = all.size();
        bankFormUploadRepository.deleteAll();
        return count;
    }

    private String buildFormHtml(
            FormDefinition form,
            String adminName,
            String accountNumber,
            String accountType,
            String holderName) {
        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        List<String> common = BankFormCatalog.COMMON_FIELDS;
        List<String> specific = form.fields();

        StringBuilder fieldsHtml = new StringBuilder();
        fieldsHtml.append(sectionHeader("Common Information (All Forms)"));
        int i = 1;
        for (String field : common) {
            fieldsHtml.append(fieldRow(i++, field, prefillValue(field, accountNumber, holderName)));
        }
        fieldsHtml.append(sectionHeader("Form Specific Information"));
        for (String field : specific) {
            fieldsHtml.append(fieldRow(i++, field, prefillValue(field, accountNumber, holderName)));
        }

        String accountBlock = "";
        if (accountNumber != null && !accountNumber.isBlank()) {
            accountBlock = """
                    <table cellspacing="0" cellpadding="6" style="width:100%%;margin:0 0 16px;border:1px solid #cbd5e1;background:#f8fafc;">
                      <tr><td colspan="2" style="font-weight:700;color:#1e3a8a;font-size:13px;padding:8px 10px;background:#e0e7ff;">Linked Account Details</td></tr>
                      <tr><td style="width:35%%;font-size:12px;color:#475569;padding:4px 10px;">Account Number</td><td style="font-size:12px;font-weight:700;color:#111;padding:4px 10px;">%s</td></tr>
                      <tr><td style="font-size:12px;color:#475569;padding:4px 10px;">Account Type</td><td style="font-size:12px;color:#111;padding:4px 10px;">%s</td></tr>
                      <tr><td style="font-size:12px;color:#475569;padding:4px 10px;">Account Holder</td><td style="font-size:12px;color:#111;padding:4px 10px;">%s</td></tr>
                      <tr><td style="font-size:12px;color:#475569;padding:4px 10px;">IFSC Code</td><td style="font-size:12px;color:#111;padding:4px 10px;">%s</td></tr>
                      <tr><td style="font-size:12px;color:#475569;padding:4px 10px;">Branch</td><td style="font-size:12px;color:#111;padding:4px 10px;">%s (%s)</td></tr>
                    </table>
                    """.formatted(
                    escapeHtml(accountNumber.trim()),
                    escapeHtml(accountType != null && !accountType.isBlank() ? accountType : "regular"),
                    escapeHtml(holderName != null && !holderName.isBlank() ? holderName : "________________________"),
                    escapeHtml(BANK_IFSC),
                    escapeHtml(BANK_BRANCH),
                    escapeHtml(BANK_BRANCH_CODE)
            );
        }

        return """
                <!DOCTYPE html><html><head><meta charset="UTF-8"/><style>
                  body { font-family: Arial, Helvetica, sans-serif; margin: 20px; color: #222; font-size: 12px; }
                  .bank-header { border-bottom: 3px solid #1a4b8c; margin-bottom: 14px; padding-bottom: 10px; }
                  .bank-name { font-size: 24px; font-weight: 800; color: #1a4b8c; margin: 0; }
                  .bank-tagline { font-size: 11px; color: #64748b; margin: 2px 0 0; }
                  .logo-box { width: 72px; height: 72px; border: 2px solid #d45113; border-radius: 50%%; text-align: center; background: #fff7ed; }
                  .logo-text { font-size: 10px; font-weight: 800; color: #8b2e0a; line-height: 72px; }
                  .form-title { font-size: 17px; font-weight: 700; color: #111; margin: 0 0 4px; }
                  .form-meta { color: #64748b; margin: 0 0 14px; font-size: 11px; }
                  .terms-box { margin-top: 18px; padding: 10px; border: 1px solid #cbd5e1; background: #f8fafc; }
                  .terms-title { font-size: 12px; font-weight: 700; color: #1e3a8a; margin: 0 0 8px; }
                  .terms-item { margin: 0 0 6px; font-size: 10px; color: #475569; line-height: 1.45; }
                  .footer { margin-top: 16px; font-size: 9px; color: #64748b; border-top: 1px solid #e2e8f0; padding-top: 8px; }
                </style></head><body>
                <table class="bank-header" cellspacing="0" cellpadding="0" style="width:100%%;"><tr>
                  <td style="width:78%%;vertical-align:top;">
                    <div class="bank-name">%s</div>
                    <div class="bank-tagline">%s</div>
                    <div style="font-size:10px;color:#475569;margin-top:6px;line-height:1.5;">
                      Registered Office: %s<br/>
                      Branch: %s &nbsp;|&nbsp; Branch Code: %s &nbsp;|&nbsp; IFSC: %s<br/>
                      Toll Free: %s &nbsp;|&nbsp; Email: %s &nbsp;|&nbsp; Website: %s &nbsp;|&nbsp; CIN: %s
                    </div>
                  </td>
                  <td style="width:22%%;text-align:right;vertical-align:top;">
                    <div class="logo-box"><span class="logo-text">NEO BANK</span></div>
                  </td>
                </tr></table>

                <div class="form-title">%s</div>
                <div class="form-meta">Official Banking Application Form &nbsp;|&nbsp; Form #%d &nbsp;|&nbsp; Code: %s &nbsp;|&nbsp; Category: %s</div>
                %s
                <table cellspacing="0" cellpadding="0" style="width:100%%;border-collapse:collapse;">%s</table>

                <div class="terms-box">
                  <div class="terms-title">NeoBank Terms and Conditions</div>
                  <p class="terms-item">1. I hereby declare that all particulars furnished in this application are true, complete, and correct to the best of my knowledge.</p>
                  <p class="terms-item">2. I authorize %s and its authorized representatives to verify my identity, address, income, and KYC documents including Aadhaar, PAN, and signature from UIDAI, NSDL, or other regulated sources.</p>
                  <p class="terms-item">3. I agree to abide by the Reserve Bank of India guidelines, Banking Regulation Act, 1949, and all applicable NeoBank policies governing account operation, loans, cards, and digital banking services.</p>
                  <p class="terms-item">4. I understand that NeoBank may accept or reject this application at its sole discretion without assigning any reason. Approved services shall be governed by the latest schedule of charges published on %s.</p>
                  <p class="terms-item">5. I consent to receive account-related alerts, OTPs, statements, and service communications on my registered mobile number and email address.</p>
                  <p class="terms-item">6. I accept that furnishing false or misleading information may result in rejection, account closure, recovery proceedings, and reporting to regulatory or law enforcement authorities.</p>
                  <p class="terms-item">7. Disputes shall be subject to the jurisdiction of courts at Mumbai, Maharashtra, India unless otherwise mandated by applicable law.</p>
                  <p style="margin:10px 0 0;font-size:11px;font-weight:700;">☐ I have read, understood, and accept the NeoBank Terms and Conditions stated above.</p>
                </div>

                <table cellspacing="0" cellpadding="0" style="width:100%%;margin-top:24px;">
                  <tr>
                    <td style="width:50%%;vertical-align:top;">
                      <div style="font-size:11px;color:#555;">Customer Signature with Date &amp; Place</div>
                      <div style="width:220px;border-top:1px solid #333;margin-top:42px;"></div>
                    </td>
                    <td style="width:50%%;vertical-align:top;text-align:right;">
                      <div style="font-size:11px;color:#555;">Authorized Bank Official / Branch Manager</div>
                      <div style="width:220px;border-top:1px solid #333;margin-top:42px;margin-left:auto;"></div>
                    </td>
                  </tr>
                </table>

                <div class="footer">
                  Generated on %s IST &nbsp;|&nbsp; Prepared by: %s &nbsp;|&nbsp; %s — %s
                </div>
                </body></html>
                """.formatted(
                escapeHtml(BANK_NAME),
                escapeHtml(BANK_TAGLINE),
                escapeHtml(BANK_REGISTERED_OFFICE),
                escapeHtml(BANK_BRANCH),
                escapeHtml(BANK_BRANCH_CODE),
                escapeHtml(BANK_IFSC),
                escapeHtml(BANK_TOLL_FREE),
                escapeHtml(BANK_EMAIL),
                escapeHtml(BANK_WEBSITE),
                escapeHtml(BANK_CIN),
                escapeHtml(form.name()),
                form.id(),
                escapeHtml(form.code()),
                escapeHtml(form.category()),
                accountBlock,
                fieldsHtml,
                escapeHtml(BANK_NAME),
                escapeHtml(BANK_WEBSITE),
                generatedAt,
                escapeHtml(adminName != null ? adminName : "Admin"),
                escapeHtml(BANK_NAME),
                escapeHtml(BANK_TAGLINE)
        );
    }

    private static String sectionHeader(String title) {
        return "<tr><td colspan=\"2\" style=\"padding:12px 0 6px;font-weight:700;color:#1e3a8a;font-size:13px;border-bottom:1px solid #dbeafe;\">"
                + escapeHtml(title) + "</td></tr>";
    }

    private static String fieldRow(int index, String field, String prefill) {
        String valueLine = prefill != null && !prefill.isBlank()
                ? "<div style=\"font-size:11px;color:#111;padding:4px 0 2px;\">" + escapeHtml(prefill) + "</div>"
                : "<div style=\"border-bottom:1px solid #cbd5e1;height:24px;\"></div>";
        return "<tr><td style=\"width:35px;padding:8px 4px 0;color:#64748b;vertical-align:top;\">" + index + ".</td>"
                + "<td style=\"padding:8px 4px 0;vertical-align:top;\">"
                + "<div style=\"font-weight:600;color:#1a1a2e;font-size:12px;\">" + escapeHtml(field) + "</div>"
                + valueLine
                + "</td></tr>";
    }

    private static String prefillValue(String field, String accountNumber, String holderName) {
        if (field == null) {
            return "";
        }
        String normalized = field.toLowerCase(Locale.ROOT);
        if (normalized.contains("account number") && accountNumber != null && !accountNumber.isBlank()) {
            return accountNumber.trim();
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

    private static Map<String, Object> loanInfo(Loan loan) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accountNumber", loan.getLoanAccountNumber() != null ? loan.getLoanAccountNumber() : loan.getAccountNumber());
        m.put("holderName", loan.getUserName());
        m.put("accountType", "loan");
        m.put("balance", loan.getAmount());
        return m;
    }

    private static Map<String, Object> goldLoanInfo(GoldLoan goldLoan) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accountNumber", goldLoan.getLoanAccountNumber() != null ? goldLoan.getLoanAccountNumber() : goldLoan.getAccountNumber());
        m.put("holderName", goldLoan.getUserName());
        m.put("accountType", "goldloan");
        m.put("balance", goldLoan.getLoanAmount());
        return m;
    }

    private static String normalizeAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return "regular";
        }
        return accountType.trim().toLowerCase();
    }

    private static String extension(String filename) {
        String lower = filename.toLowerCase();
        int dot = lower.lastIndexOf('.');
        return dot >= 0 ? lower.substring(dot + 1) : "";
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
