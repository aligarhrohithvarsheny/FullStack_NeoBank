package com.neo.springapp.service;

import com.neo.springapp.config.BankFormCatalog;
import com.neo.springapp.config.BankFormCatalog.FormDefinition;
import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@SuppressWarnings("null")
public class BankFormService {

    private static final String UPLOAD_DIR = "uploads/bank-forms";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    private final BankFormUploadRepository bankFormUploadRepository;
    private final BankFormUploadHistoryRepository bankFormUploadHistoryRepository;
    private final AccountRepository accountRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final LoanRepository loanRepository;
    private final GoldLoanRepository goldLoanRepository;
    private final ChequeRepository chequeRepository;

    public BankFormService(
            BankFormUploadRepository bankFormUploadRepository,
            BankFormUploadHistoryRepository bankFormUploadHistoryRepository,
            AccountRepository accountRepository,
            SalaryAccountRepository salaryAccountRepository,
            CurrentAccountRepository currentAccountRepository,
            LoanRepository loanRepository,
            GoldLoanRepository goldLoanRepository,
            ChequeRepository chequeRepository) {
        this.bankFormUploadRepository = bankFormUploadRepository;
        this.bankFormUploadHistoryRepository = bankFormUploadHistoryRepository;
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
            String holderName,
            String customerId,
            String aadhaarNumber,
            String panNumber,
            String phone,
            String email) throws IOException {
        FormDefinition form = BankFormCatalog.findByCode(formCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown form code: " + formCode));

        return new BankFormPdfBuilder().build(
                form,
                adminName,
                accountNumber,
                accountType,
                holderName,
                customerId,
                aadhaarNumber,
                panNumber,
                phone,
                email);
    }

    /** Backward-compatible overload used by existing callers. */
    public byte[] buildBlankFormPdf(
            String formCode,
            String adminName,
            String accountNumber,
            String accountType,
            String holderName) throws IOException {
        Map<String, Object> profile = resolveAccount(accountNumber, accountType);
        return buildBlankFormPdf(
                formCode,
                adminName,
                accountNumber,
                accountType,
                holderName,
                stringValue(profile, "customerId"),
                stringValue(profile, "aadhaarNumber"),
                stringValue(profile, "panNumber"),
                stringValue(profile, "phone"),
                stringValue(profile, "email"));
    }

    public Optional<BankFormUpload> findUpload(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return bankFormUploadRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public byte[] readUploadedFileById(Long id) throws IOException {
        BankFormUpload upload = bankFormUploadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Upload record not found"));
        return readUploadedFile(upload);
    }

    @Transactional(readOnly = true)
    public byte[] readUploadedFile(BankFormUpload upload) throws IOException {
        if (upload.getFileContent() != null && upload.getFileContent().length > 0) {
            return upload.getFileContent();
        }
        if (upload.getStoredFilePath() == null || upload.getStoredFilePath().isBlank()) {
            throw new IllegalArgumentException("Uploaded file not found on server");
        }
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
            throw new IllegalArgumentException("File size exceeds 10MB limit");
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
        byte[] fileBytes = file.getBytes();
        Files.write(target, fileBytes);

        BankFormUpload upload = new BankFormUpload();
        upload.setFormCode(form.code());
        upload.setFormName(form.name());
        upload.setCategory(form.category());
        upload.setAccountNumber(accountNumber.trim());
        upload.setAccountType(normalizeAccountType(accountType));
        upload.setAccountHolderName(String.valueOf(accountInfo.getOrDefault("holderName", "")));
        upload.setOriginalFileName(originalName);
        upload.setStoredFilePath(UPLOAD_DIR + "/" + filename);
        upload.setFileContent(fileBytes);
        upload.setContentType(file.getContentType());
        upload.setFileSizeBytes(file.getSize());
        upload.setUploadedByAdmin(uploadedByAdmin);
        upload.setRemarks(remarks);
        upload.setUploadedAt(LocalDateTime.now());

        BankFormUpload saved = bankFormUploadRepository.save(upload);
        recordHistory(saved.getId(), "UPLOAD", null, saved, uploadedByAdmin, "Initial upload");
        return saved;
    }

    @Transactional
    public BankFormUpload replaceUploadedForm(
            Long uploadId,
            MultipartFile file,
            String replacedByAdmin,
            String remarks) throws IOException {
        BankFormUpload upload = bankFormUploadRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("Upload record not found"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a replacement file");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String ext = extension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Only PDF, JPG, JPEG, PNG, or WEBP files are allowed");
        }

        BankFormUpload previousSnapshot = snapshotUpload(upload);

        Path uploadDir = Paths.get(UPLOAD_DIR).normalize();
        Files.createDirectories(uploadDir);

        String safeAccount = upload.getAccountNumber().replaceAll("[^a-zA-Z0-9]", "");
        String filename = upload.getFormCode() + "-" + safeAccount + "-replace-" + System.currentTimeMillis() + "." + ext;
        Path target = uploadDir.resolve(filename).normalize();
        byte[] fileBytes = file.getBytes();
        Files.write(target, fileBytes);

        upload.setOriginalFileName(originalName);
        upload.setStoredFilePath(UPLOAD_DIR + "/" + filename);
        upload.setFileContent(fileBytes);
        upload.setContentType(file.getContentType());
        upload.setFileSizeBytes(file.getSize());
        upload.setUploadedByAdmin(replacedByAdmin);
        if (remarks != null && !remarks.isBlank()) {
            upload.setRemarks(remarks);
        }
        upload.setUploadedAt(LocalDateTime.now());

        BankFormUpload saved = bankFormUploadRepository.save(upload);
        recordHistory(saved.getId(), "REPLACE", previousSnapshot, saved, replacedByAdmin, remarks);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUploadHistory(Long uploadId) {
        return bankFormUploadHistoryRepository.findByUploadIdOrderByPerformedAtDesc(uploadId).stream()
                .map(this::toHistoryDto)
                .toList();
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
    public Map<String, Object> verifyAccount(String accountNumber, String accountType, String customerId) {
        Map<String, Object> info = null;
        if (customerId != null && !customerId.isBlank()) {
            info = resolveByCustomerId(customerId.trim());
        } else if (accountNumber != null && !accountNumber.isBlank()) {
            info = resolveAccount(accountNumber, accountType);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        if (info == null) {
            response.put("success", false);
            response.put("message", "Account not found for the given account number or customer ID");
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
                    m.put("customerId", a.getCustomerId());
                    m.put("aadhaarNumber", a.getAadharNumber());
                    m.put("panNumber", a.getPan());
                    m.put("phone", a.getPhone());
                    m.put("email", "");
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
                    m.put("customerId", a.getCustomerId());
                    m.put("aadhaarNumber", a.getAadharNumber());
                    m.put("panNumber", a.getPanNumber());
                    m.put("phone", a.getMobileNumber());
                    m.put("email", a.getEmail());
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
                    m.put("customerId", a.getCustomerId());
                    m.put("aadhaarNumber", a.getAadharNumber());
                    m.put("panNumber", a.getPanNumber());
                    m.put("phone", a.getMobile());
                    m.put("email", a.getEmail());
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
        recordHistory(upload.getId(), "DELETE", upload, null, "Admin", "Upload deleted");
        bankFormUploadRepository.delete(upload);
        return true;
    }

    @Transactional
    public int clearAllUploads() {
        List<BankFormUpload> all = bankFormUploadRepository.findAll();
        for (BankFormUpload upload : all) {
            try {
                if (upload.getStoredFilePath() != null && !upload.getStoredFilePath().isBlank()) {
                    Path path = Paths.get(upload.getStoredFilePath()).normalize();
                    Files.deleteIfExists(path);
                }
            } catch (IOException ignored) {
                // continue
            }
        }
        int count = all.size();
        bankFormUploadRepository.deleteAll();
        return count;
    }

    private Map<String, Object> resolveByCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return null;
        }
        String cid = customerId.trim();

        Account regular = accountRepository.findByCustomerId(cid);
        if (regular != null) {
            return resolveAccount(regular.getAccountNumber(), "regular");
        }

        SalaryAccount salary = salaryAccountRepository.findByCustomerId(cid);
        if (salary != null) {
            return resolveAccount(salary.getAccountNumber(), "salary");
        }

        Optional<CurrentAccount> current = currentAccountRepository.findByCustomerId(cid);
        if (current.isPresent()) {
            return resolveAccount(current.get().getAccountNumber(), "current");
        }

        return null;
    }

    private void recordHistory(
            Long uploadId,
            String action,
            BankFormUpload previous,
            BankFormUpload current,
            String performedByAdmin,
            String remarks) {
        BankFormUploadHistory history = new BankFormUploadHistory();
        history.setUploadId(uploadId);
        history.setAction(action);
        if (previous != null) {
            history.setPreviousFileName(previous.getOriginalFileName());
            history.setPreviousStoredPath(previous.getStoredFilePath());
            history.setPreviousContentType(previous.getContentType());
            history.setPreviousFileSizeBytes(previous.getFileSizeBytes());
        }
        if (current != null) {
            history.setNewFileName(current.getOriginalFileName());
            history.setNewStoredPath(current.getStoredFilePath());
            history.setNewContentType(current.getContentType());
            history.setNewFileSizeBytes(current.getFileSizeBytes());
        }
        history.setPerformedByAdmin(performedByAdmin);
        history.setRemarks(remarks);
        bankFormUploadHistoryRepository.save(history);
    }

    private BankFormUpload snapshotUpload(BankFormUpload upload) {
        BankFormUpload copy = new BankFormUpload();
        copy.setOriginalFileName(upload.getOriginalFileName());
        copy.setStoredFilePath(upload.getStoredFilePath());
        copy.setContentType(upload.getContentType());
        copy.setFileSizeBytes(upload.getFileSizeBytes());
        return copy;
    }

    public Map<String, Object> toHistoryDto(BankFormUploadHistory history) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", history.getId());
        dto.put("uploadId", history.getUploadId());
        dto.put("action", history.getAction());
        dto.put("previousFileName", history.getPreviousFileName());
        dto.put("newFileName", history.getNewFileName());
        dto.put("performedByAdmin", history.getPerformedByAdmin());
        dto.put("remarks", history.getRemarks());
        dto.put("performedAt", history.getPerformedAt());
        return dto;
    }

    private static String stringValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
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
}
