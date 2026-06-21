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
    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024L;

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

    public byte[] buildBlankFormPdf(String formCode, String adminName) throws IOException {
        FormDefinition form = BankFormCatalog.findByCode(formCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown form code: " + formCode));

        String html = buildFormHtml(form, adminName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, out);
        return out.toByteArray();
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
                List<Loan> loans = loanRepository.findAll();
                for (Loan l : loans) {
                    if (num.equals(l.getLoanAccountNumber()) || num.equals(l.getAccountNumber())) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("accountNumber", l.getLoanAccountNumber() != null ? l.getLoanAccountNumber() : l.getAccountNumber());
                        m.put("holderName", l.getUserName());
                        m.put("accountType", "loan");
                        m.put("balance", l.getAmount());
                        return m;
                    }
                }
            }
            case "goldloan" -> {
                List<GoldLoan> goldLoans = goldLoanRepository.findAll();
                for (GoldLoan g : goldLoans) {
                    if (num.equals(g.getLoanAccountNumber()) || num.equals(g.getAccountNumber())) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("accountNumber", g.getLoanAccountNumber() != null ? g.getLoanAccountNumber() : g.getAccountNumber());
                        m.put("holderName", g.getUserName());
                        m.put("accountType", "goldloan");
                        m.put("balance", g.getLoanAmount());
                        return m;
                    }
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

    private String buildFormHtml(FormDefinition form, String adminName) {
        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        List<String> common = BankFormCatalog.COMMON_FIELDS;
        List<String> specific = form.fields();

        StringBuilder fieldsHtml = new StringBuilder();
        fieldsHtml.append("<tr><td colspan=\"2\" style=\"padding:12px 0 6px;font-weight:700;color:#1e3a8a;font-size:14px;\">Common Information (All Forms)</td></tr>");
        int i = 1;
        for (String field : common) {
            fieldsHtml.append(fieldRow(i++, field));
        }
        fieldsHtml.append("<tr><td colspan=\"2\" style=\"padding:16px 0 6px;font-weight:700;color:#1e3a8a;font-size:14px;\">Form Specific Information</td></tr>");
        for (String field : specific) {
            fieldsHtml.append(fieldRow(i++, field));
        }

        return """
                <!DOCTYPE html><html><head><meta charset="UTF-8"/></head>
                <body style="font-family:Arial,sans-serif;margin:24px;color:#222;">
                <div style="border-bottom:3px solid #1e3a8a;padding-bottom:12px;margin-bottom:20px;">
                  <h1 style="margin:0;color:#1e3a8a;font-size:22px;">NeoBank</h1>
                  <p style="margin:4px 0 0;color:#666;font-size:12px;">Official Banking Form — %s</p>
                </div>
                <h2 style="color:#111;font-size:18px;margin:0 0 4px;">%s</h2>
                <p style="color:#666;margin:0 0 18px;font-size:13px;">Form #%d &nbsp;|&nbsp; Category: %s</p>
                <table style="width:100%%;border-collapse:collapse;">%s</table>
                <div style="margin-top:24px;padding:12px;border:1px solid #ddd;border-radius:6px;background:#f8fafc;">
                  <p style="margin:0;font-size:11px;color:#475569;line-height:1.5;">
                    <strong>Terms and Conditions:</strong> By submitting this form I confirm that all information provided is true and correct.
                    I authorize NeoBank to verify my identity using Aadhaar and other KYC documents. I accept NeoBank's terms of service,
                    privacy policy, and applicable banking regulations. False information may lead to account closure and legal action.
                  </p>
                  <p style="margin:10px 0 0;font-size:12px;">☐ I Accept Terms and Conditions</p>
                </div>
                <div style="margin-top:28px;display:flex;justify-content:space-between;">
                  <div><p style="margin:0;font-size:12px;color:#555;">Customer Signature</p><div style="width:180px;border-top:1px solid #333;margin-top:40px;"></div></div>
                  <div><p style="margin:0;font-size:12px;color:#555;">Bank Official</p><div style="width:180px;border-top:1px solid #333;margin-top:40px;"></div></div>
                </div>
                <p style="margin-top:28px;font-size:11px;color:#888;">Generated: %s IST &nbsp;|&nbsp; Prepared by: %s</p>
                </body></html>
                """.formatted(
                escapeHtml(form.category()),
                escapeHtml(form.name()),
                form.id(),
                escapeHtml(form.category()),
                fieldsHtml,
                generatedAt,
                escapeHtml(adminName != null ? adminName : "Admin")
        );
    }

    private static String fieldRow(int index, String field) {
        return "<tr><td style=\"width:35px;padding:8px 0;color:#555;\">" + index + ".</td>"
                + "<td style=\"padding:8px 0;font-weight:600;color:#1a1a2e;\">" + escapeHtml(field)
                + "</td></tr>"
                + "<tr><td></td><td style=\"border-bottom:1px solid #ccc;height:28px;\"></td></tr>";
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
