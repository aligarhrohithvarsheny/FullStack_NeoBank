package com.neo.springapp.service;

import com.neo.springapp.config.BankFormCatalog;
import com.neo.springapp.config.BankFormCatalog.FormDefinition;
import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

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
    private final HomeLoanRepository homeLoanRepository;
    private final FixedDepositRepository fixedDepositRepository;
    private final RecurringDepositRepository recurringDepositRepository;
    private final EcsMandateRepository ecsMandateRepository;
    private final EmiPaymentRepository emiPaymentRepository;

    @Autowired
    public BankFormService(
            BankFormUploadRepository bankFormUploadRepository,
            BankFormUploadHistoryRepository bankFormUploadHistoryRepository,
            AccountRepository accountRepository,
            SalaryAccountRepository salaryAccountRepository,
            CurrentAccountRepository currentAccountRepository,
            LoanRepository loanRepository,
            GoldLoanRepository goldLoanRepository,
            ChequeRepository chequeRepository,
            HomeLoanRepository homeLoanRepository,
            FixedDepositRepository fixedDepositRepository,
            RecurringDepositRepository recurringDepositRepository,
            EcsMandateRepository ecsMandateRepository,
            EmiPaymentRepository emiPaymentRepository) {
        this.bankFormUploadRepository = bankFormUploadRepository;
        this.bankFormUploadHistoryRepository = bankFormUploadHistoryRepository;
        this.accountRepository = accountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.loanRepository = loanRepository;
        this.goldLoanRepository = goldLoanRepository;
        this.chequeRepository = chequeRepository;
        this.homeLoanRepository = homeLoanRepository;
        this.fixedDepositRepository = fixedDepositRepository;
        this.recurringDepositRepository = recurringDepositRepository;
        this.ecsMandateRepository = ecsMandateRepository;
        this.emiPaymentRepository = emiPaymentRepository;
    }

    public BankFormService(
            BankFormUploadRepository bankFormUploadRepository,
            BankFormUploadHistoryRepository bankFormUploadHistoryRepository,
            AccountRepository accountRepository,
            SalaryAccountRepository salaryAccountRepository,
            CurrentAccountRepository currentAccountRepository,
            LoanRepository loanRepository,
            GoldLoanRepository goldLoanRepository,
            ChequeRepository chequeRepository) {
        this(bankFormUploadRepository, bankFormUploadHistoryRepository, accountRepository,
                salaryAccountRepository, currentAccountRepository, loanRepository, goldLoanRepository,
                chequeRepository, null, null, null, null, null);
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
        return buildBlankFormPdf(formCode, adminName, accountNumber, accountType, holderName,
                customerId, aadhaarNumber, panNumber, phone, email, null, null);
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
            String email,
            String dateOfBirth,
            String address) throws IOException {
        FormDefinition form = BankFormCatalog.findByCode(formCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown form code: " + formCode));

        Map<String, Object> productRecord = isAmbiguousFormRecord(formCode, accountNumber)
                ? null
                : resolveFormRecord(formCode, accountNumber);
        Map<String, String> productDetails = productRecord == null
                ? Map.of()
                : stringMap(productRecord.get("formDetails"));
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
                email,
                dateOfBirth,
                address,
                productDetails);
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
                stringValue(profile, "email"),
                stringValue(profile, "dateOfBirth"),
                stringValue(profile, "address"));
    }

    public Optional<BankFormUpload> findUpload(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return bankFormUploadRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public byte[] readUploadedFileById(Long id) throws IOException {
        Optional<byte[]> content = bankFormUploadRepository.findFileContentById(id);
        if (content.isPresent() && content.get() != null && content.get().length > 0) {
            return content.get();
        }
        BankFormUpload upload = bankFormUploadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Upload record not found"));
        return readUploadedFile(upload);
    }

    @Transactional(readOnly = true)
    public byte[] readUploadedFile(BankFormUpload upload) throws IOException {
        if (upload.getFileContent() != null && upload.getFileContent().length > 0) {
            return upload.getFileContent();
        }
        if (upload.getStoredFilePath() == null || upload.getStoredFilePath().isBlank()
                || upload.getStoredFilePath().startsWith("db://")) {
            throw new IllegalArgumentException("Uploaded file not found on server");
        }
        Path path = resolveStoredPath(upload.getStoredFilePath());
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

        Map<String, Object> accountInfo = resolveFormRecord(formCode, accountNumber);
        if (accountInfo == null) {
            accountInfo = resolveAccount(accountNumber, accountType);
        }
        if (accountInfo == null) {
            throw new IllegalArgumentException("Account not found for the given account number and type");
        }

        String safeAccount = accountNumber.replaceAll("[^a-zA-Z0-9]", "");
        String filename = form.code() + "-" + safeAccount + "-" + System.currentTimeMillis() + "." + ext;
        byte[] fileBytes = file.getBytes();
        String storedPath = "db://" + filename;
        try {
            Path uploadDir = resolveUploadDirectory(UPLOAD_DIR);
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename).normalize();
            Files.write(target, fileBytes);
            storedPath = target.toString();
        } catch (IOException diskError) {
            System.err.println("Bank form upload stored in database only (disk unavailable): " + diskError.getMessage());
        }

        BankFormUpload upload = new BankFormUpload();
        upload.setFormCode(form.code());
        upload.setFormName(form.name());
        upload.setCategory(form.category());
        upload.setAccountNumber(accountNumber.trim());
        upload.setAccountType(normalizeAccountType(accountType));
        upload.setAccountHolderName(String.valueOf(accountInfo.getOrDefault("holderName", "")));
        upload.setOriginalFileName(originalName);
        upload.setStoredFilePath(storedPath);
        upload.setFileContent(fileBytes);
        upload.setContentType(resolveContentType(file.getContentType(), ext));
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

        String safeAccount = upload.getAccountNumber().replaceAll("[^a-zA-Z0-9]", "");
        String filename = upload.getFormCode() + "-" + safeAccount + "-replace-" + System.currentTimeMillis() + "." + ext;
        byte[] fileBytes = file.getBytes();
        String storedPath = "db://" + filename;
        try {
            Path uploadDir = resolveUploadDirectory(UPLOAD_DIR);
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename).normalize();
            Files.write(target, fileBytes);
            storedPath = target.toString();
        } catch (IOException diskError) {
            System.err.println("Bank form replace stored in database only (disk unavailable): " + diskError.getMessage());
        }

        upload.setOriginalFileName(originalName);
        upload.setStoredFilePath(storedPath);
        upload.setFileContent(fileBytes);
        upload.setContentType(resolveContentType(file.getContentType(), ext));
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
    public Map<String, Object> verifyAccount(
            String accountNumber, String accountType, String customerId, String formCode) {
        Map<String, Object> info = null;
        String lookupNumber = accountNumber;
        if (customerId != null && !customerId.isBlank()) {
            info = resolveByCustomerId(customerId.trim());
            if (info != null && formCode != null && !formCode.isBlank()) {
                lookupNumber = stringValue(info, "accountNumber");
                info = resolveFormRecord(formCode, lookupNumber);
            }
        } else if (accountNumber != null && !accountNumber.isBlank()) {
            lookupNumber = accountNumber;
        }
        Map<String, Object> response = new LinkedHashMap<>();
        if (formCode != null && !formCode.isBlank() && isAmbiguousFormRecord(formCode, lookupNumber)) {
            response.put("success", false);
            response.put("message", "More than one product matches this linked account. Enter the specific loan, FD, RD, or mandate account number.");
            return response;
        }
        if (info == null && accountNumber != null && !accountNumber.isBlank()
                && (customerId == null || customerId.isBlank())) {
            info = formCode == null || formCode.isBlank()
                    ? resolveAccount(accountNumber, accountType)
                    : resolveFormRecord(formCode, accountNumber);
        }
        if (info == null) {
            response.put("success", false);
            response.put("message", formCode == null || formCode.isBlank()
                    ? "Account not found for the given account number or customer ID"
                    : "No matching product was found for this form and account number");
            return response;
        }
        response.put("success", true);
        response.put("account", info);
        return response;
    }

    private boolean isAmbiguousFormRecord(String formCode, String reference) {
        if (reference == null || reference.isBlank()) return false;
        String code = formCode.toLowerCase(Locale.ROOT);
        if (code.equals("fixed-deposit-application") && fixedDepositRepository != null) {
            return fixedDepositRepository.findByFdAccountNumber(reference).isEmpty()
                    && fixedDepositRepository.findByAccountNumber(reference).size() > 1;
        }
        if (code.equals("recurring-deposit-application") && recurringDepositRepository != null) {
            return recurringDepositRepository.findByRdAccountNumber(reference).isEmpty()
                    && recurringDepositRepository.findByAccountNumberOrderByCreatedAtDesc(reference).size() > 1;
        }
        if (code.equals("ecs-mandate-authorization") && ecsMandateRepository != null) {
            return ecsMandateRepository.findByLoanAccountNumberOrderByCreatedAtDesc(reference).isEmpty()
                    && ecsMandateRepository.findBySavingsAccountNumberOrderByCreatedAtDesc(reference).size() > 1;
        }
        if (code.equals("gold-loan") && goldLoanRepository != null) {
            return goldLoanRepository.findByLoanAccountNumber(reference).isEmpty()
                    && goldLoanRepository.findByAccountNumber(reference).size() > 1;
        }
        if (Set.of("home-loan", "car-loan", "crop-loan").contains(code) && homeLoanRepository != null
                && homeLoanRepository.findByLoanAccountNumber(reference).isEmpty()) {
            String expectedType = code.equals("home-loan") ? "HOME" : code.equals("car-loan") ? "CAR" : "CROP";
            return homeLoanRepository.findByAccountNumberOrderByApplicationDateDesc(reference).stream()
                    .filter(candidate -> matchesHomeLoanType(candidate, expectedType)).count() > 1;
        }
        if (Set.of("personal-loan", "education-loan", "business-loan").contains(code)
                && loanRepository != null && loanRepository.findByLoanAccountNumber(reference).isEmpty()) {
            String expectedType = code.substring(0, code.indexOf("-loan"));
            return loanRepository.findByAccountNumber(reference).stream()
                    .filter(candidate -> typeMatches(candidate.getType(), expectedType)).count() > 1;
        }
        return false;
    }

    /** Backward-compatible lookup used by existing callers without form context. */
    public Map<String, Object> verifyAccount(String accountNumber, String accountType, String customerId) {
        return verifyAccount(accountNumber, accountType, customerId, null);
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

    private Map<String, Object> resolveFormRecord(String formCode, String reference) {
        if (formCode == null || reference == null || reference.isBlank()) {
            return null;
        }
        String code = formCode.trim().toLowerCase(Locale.ROOT);
        String number = reference.trim();
        if (code.equals("fixed-deposit-application") && fixedDepositRepository != null) {
            FixedDeposit deposit = fixedDepositRepository.findByFdAccountNumber(number).orElse(null);
            if (deposit == null) {
                deposit = fixedDepositRepository.findByAccountNumber(number).stream().findFirst().orElse(null);
            }
            return deposit == null ? null : fixedDepositInfo(deposit);
        }
        if (code.equals("recurring-deposit-application") && recurringDepositRepository != null) {
            RecurringDeposit deposit = recurringDepositRepository.findByRdAccountNumber(number).orElse(null);
            if (deposit == null) {
                deposit = recurringDepositRepository.findByAccountNumberOrderByCreatedAtDesc(number)
                        .stream().findFirst().orElse(null);
            }
            return deposit == null ? null : recurringDepositInfo(deposit);
        }
        if (code.equals("ecs-mandate-authorization") && ecsMandateRepository != null) {
            List<EcsMandate> mandates = ecsMandateRepository.findByLoanAccountNumberOrderByCreatedAtDesc(number);
            if (mandates.isEmpty()) {
                mandates = ecsMandateRepository.findBySavingsAccountNumberOrderByCreatedAtDesc(number);
            }
            return mandates.isEmpty() ? null : ecsMandateInfo(mandates.get(0));
        }
        if (code.equals("gold-loan") && goldLoanRepository != null) {
            GoldLoan loan = goldLoanRepository.findByLoanAccountNumber(number).orElse(null);
            if (loan == null) {
                loan = goldLoanRepository.findByAccountNumber(number).stream().findFirst().orElse(null);
            }
            return loan == null ? null : goldLoanInfo(
                    loan, true, ecsMandatesFor(loan.getLoanAccountNumber(), loan.getAccountNumber()));
        }
        if (Set.of("home-loan", "car-loan", "crop-loan").contains(code) && homeLoanRepository != null) {
            String expectedType = code.equals("home-loan") ? "HOME" : code.equals("car-loan") ? "CAR" : "CROP";
            HomeLoan loan = homeLoanRepository.findByLoanAccountNumber(number)
                    .filter(candidate -> matchesHomeLoanType(candidate, expectedType))
                    .orElseGet(() -> homeLoanRepository.findByAccountNumberOrderByApplicationDateDesc(number)
                            .stream().filter(candidate -> matchesHomeLoanType(candidate, expectedType))
                            .findFirst().orElse(null));
            return loan == null ? null : homeLoanInfo(loan);
        }
        if (Set.of("personal-loan", "education-loan", "business-loan").contains(code)
                && loanRepository != null) {
            String expectedType = code.substring(0, code.indexOf("-loan"));
            Loan loan = loanRepository.findByLoanAccountNumber(number)
                    .filter(candidate -> typeMatches(candidate.getType(), expectedType))
                    .orElseGet(() -> loanRepository.findByAccountNumber(number).stream()
                            .filter(candidate -> typeMatches(candidate.getType(), expectedType))
                            .findFirst().orElse(null));
            return loan == null ? null : generalLoanInfo(
                    loan, ecsMandatesFor(loan.getLoanAccountNumber(), loan.getAccountNumber()));
        }
        if (code.equals("loan-agreement-esign-review")) {
            Map<String, Object> resolved = resolveAnyLoanByNumber(number);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private Map<String, Object> resolveAnyLoanByNumber(String number) {
        if (homeLoanRepository != null) {
            HomeLoan loan = homeLoanRepository.findByLoanAccountNumber(number).orElse(null);
            if (loan != null) return homeLoanInfo(loan);
        }
        if (goldLoanRepository != null) {
            GoldLoan loan = goldLoanRepository.findByLoanAccountNumber(number).orElse(null);
            if (loan != null) return goldLoanInfo(
                    loan, true, ecsMandatesFor(loan.getLoanAccountNumber(), loan.getAccountNumber()));
        }
        if (loanRepository != null) {
            Loan loan = loanRepository.findByLoanAccountNumber(number).orElse(null);
            if (loan != null) return generalLoanInfo(
                    loan, ecsMandatesFor(loan.getLoanAccountNumber(), loan.getAccountNumber()));
        }
        return null;
    }

    private Map<String, Object> homeLoanInfo(HomeLoan loan) {
        Map<String, Object> info = baseProductInfo(
                loan.getLoanAccountNumber(), loan.getAccountNumber(), loan.getUserName(), "homeloan",
                loan.getAmount());
        Map<String, String> details = new LinkedHashMap<>();
        put(details, "Loan Product and Purpose", joinValues(loan.getLoanType(), loan.getPurpose()));
        put(details, "Loan Product and Loan Account Number",
                joinValues(loan.getLoanType(), loan.getLoanAccountNumber()));
        put(details, "Loan Account Number", loan.getLoanAccountNumber());
        put(details, "Application ID", loan.getApplicationId());
        put(details, "Requested Loan Amount", loan.getAmount());
        put(details, "Approved Loan Amount", isApproved(loan.getStatus()) ? loan.getAmount() : null);
        put(details, "Sanctioned Principal", isApproved(loan.getStatus()) ? loan.getAmount() : null);
        put(details, "Tenure and Repayment Frequency", loan.getTenure() == null ? null : loan.getTenure() + " months");
        put(details, "Interest Rate and Tenure", loan.getInterestRate() == null ? null
                : loan.getInterestRate() + "% p.a.; " + loan.getTenure() + " months");
        put(details, "Annual Interest Rate and Rate Type", loan.getInterestRate() == null ? null : loan.getInterestRate() + "% p.a.");
        put(details, "EMI and Repayment Schedule", loan.getEmi());
        put(details, "Complete Repayment Schedule", repaymentSchedule(loan.getLoanAccountNumber()));
        put(details, "Processing Fee, Taxes and Legal Charges",
                joinValues(loan.getProcessingFee(), loan.getGstOnFee(), loan.getLegalCharges()));
        put(details, "Final Approval and Disbursement Details", joinValues(loan.getStatus(), loan.getApprovalDate(), loan.getNetDisbursed()));
        put(details, "Disbursed Amount", loan.getNetDisbursed());
        put(details, "Loan Status", loan.getStatus());
        put(details, "Linked Repayment Account", loan.getAccountNumber());
        put(details, "Purpose and Review Notes", joinValues(loan.getPurpose(), loan.getAdminNotes()));
        if (loan.getLoanType() != null && loan.getLoanType().equalsIgnoreCase("CAR")) {
            put(details, "Vehicle Details", joinValues(loan.getVehicleMakeModel(), loan.getVehicleYear(), loan.getVehicleRegistrationNumber()));
        }
        if (loan.getLoanType() != null && (loan.getLoanType().equalsIgnoreCase("CROP")
                || loan.getLoanType().equalsIgnoreCase("FARMER"))) {
            put(details, "Crop Name and Season", joinValues(loan.getCropName(), loan.getCropSeason()));
            put(details, "Farm Location and Land Area", joinValues(loan.getFarmLocation(), loan.getLandArea(), loan.getLandAreaUnit()));
            put(details, "Cultivation Cost Estimate", loan.getCultivationCostEstimate());
        }
        List<EcsMandate> mandates = ecsMandatesFor(loan.getLoanAccountNumber(), loan.getAccountNumber());
        put(details, "ECS / Auto-debit Mandate Status", mandateSummary(mandates));
        put(details, "Complete ECS Mandate Details", mandateDetails(mandates));
        info.put("formDetails", details);
        info.put("ecsMandates", mandates.stream().map(BankFormService::mandateSummaryMap).toList());
        enrichProductInfo(info, loan.getAccountNumber());
        return info;
    }

    private Map<String, Object> generalLoanInfo(Loan loan, List<EcsMandate> mandates) {
        Map<String, Object> info = baseProductInfo(loan.getLoanAccountNumber(), loan.getAccountNumber(),
                loan.getUserName(), "loan", loan.getAmount());
        Map<String, String> details = new LinkedHashMap<>();
        put(details, "Loan Product and Purpose", joinValues(loan.getType(), loan.getPurpose()));
        put(details, "Loan Product and Loan Account Number", joinValues(loan.getType(), loan.getLoanAccountNumber()));
        put(details, "Loan Account Number", loan.getLoanAccountNumber());
        put(details, "Requested Loan Amount", loan.getAmount());
        put(details, "Approved Loan Amount", isApproved(loan.getStatus()) ? loan.getAmount() : null);
        put(details, "Sanctioned Principal", isApproved(loan.getStatus()) ? loan.getAmount() : null);
        put(details, "Tenure and Repayment Frequency", loan.getTenure() == null ? null : loan.getTenure() + " months");
        put(details, "Interest Rate and Tenure", loan.getInterestRate() == null ? null
                : loan.getInterestRate() + "% p.a.; " + loan.getTenure() + " months");
        put(details, "Annual Interest Rate and Rate Type", loan.getInterestRate() == null ? null : loan.getInterestRate() + "% p.a.");
        put(details, "Loan Status", loan.getStatus());
        put(details, "Linked Repayment Account", loan.getAccountNumber());
        put(details, "ECS / Auto-debit Mandate Status", mandateSummary(mandates));
        put(details, "Complete ECS Mandate Details", mandateDetails(mandates));
        put(details, "Complete Repayment Schedule", repaymentSchedule(loan.getLoanAccountNumber()));
        put(details, "Final Approval and Disbursement Details", joinValues(loan.getStatus(), loan.getApprovalDate()));
        info.put("formDetails", details);
        info.put("ecsMandates", mandates.stream().map(BankFormService::mandateSummaryMap).toList());
        enrichProductInfo(info, loan.getAccountNumber());
        return info;
    }

    private Map<String, Object> goldLoanInfo(GoldLoan loan, boolean detailed, List<EcsMandate> mandates) {
        Map<String, Object> info = baseProductInfo(loan.getLoanAccountNumber(), loan.getAccountNumber(),
                loan.getUserName(), "goldloan", loan.getLoanAmount());
        if (!detailed) return info;
        Map<String, String> details = new LinkedHashMap<>();
        put(details, "Loan Product and Purpose", loan.getPurpose());
        put(details, "Loan Product and Loan Account Number", joinValues("Gold Loan", loan.getLoanAccountNumber()));
        put(details, "Loan Account Number", loan.getLoanAccountNumber());
        put(details, "Requested Loan Amount", loan.getLoanAmount());
        put(details, "Approved Loan Amount", isApproved(loan.getStatus()) ? loan.getLoanAmount() : null);
        put(details, "Sanctioned Principal", isApproved(loan.getStatus()) ? loan.getLoanAmount() : null);
        put(details, "Tenure and Repayment Frequency", loan.getTenure() == null ? null : loan.getTenure() + " months");
        put(details, "Interest Rate and Tenure", loan.getInterestRate() == null ? null
                : loan.getInterestRate() + "% p.a.; " + loan.getTenure() + " months");
        put(details, "Annual Interest Rate and Rate Type", loan.getInterestRate() == null ? null : loan.getInterestRate() + "% p.a.");
        put(details, "Gold Collateral", joinValues(loan.getGoldDescription(), loan.getGoldPurity(), loan.getVerifiedGoldGrams()));
        put(details, "Loan Status", loan.getStatus());
        put(details, "Linked Repayment Account", loan.getAccountNumber());
        put(details, "ECS / Auto-debit Mandate Status", mandateSummary(mandates));
        put(details, "Complete ECS Mandate Details", mandateDetails(mandates));
        put(details, "Complete Repayment Schedule", repaymentSchedule(loan.getLoanAccountNumber()));
        info.put("formDetails", details);
        info.put("ecsMandates", mandates.stream().map(BankFormService::mandateSummaryMap).toList());
        enrichProductInfo(info, loan.getAccountNumber());
        return info;
    }

    private Map<String, Object> fixedDepositInfo(FixedDeposit deposit) {
        Map<String, Object> info = baseProductInfo(deposit.getFdAccountNumber(), deposit.getAccountNumber(),
                deposit.getUserName(), "fixeddeposit", deposit.getPrincipalAmount());
        Map<String, String> details = new LinkedHashMap<>();
        put(details, "Linked Funding Account", deposit.getAccountNumber());
        put(details, "Deposit Scheme", deposit.getSchemeName());
        put(details, "Principal Amount", deposit.getPrincipalAmount());
        put(details, "Tenure", deposit.getTenure() == null ? null : deposit.getTenure() + " months");
        put(details, "Interest Rate", deposit.getInterestRate());
        put(details, "Interest Payout Frequency", deposit.getInterestPayout());
        put(details, "Maturity Date", deposit.getMaturityDate());
        put(details, "FD Status", deposit.getStatus());
        put(details, "Maturity Amount", deposit.getMaturityAmount());
        put(details, "Interest Amount", deposit.getInterestAmount());
        List<EcsMandate> mandates = ecsMandatesFor(deposit.getFdAccountNumber(), deposit.getAccountNumber());
        put(details, "ECS Mandate and Separate Consent Status", mandateSummary(mandates));
        put(details, "Complete ECS Mandate Details", mandateDetails(mandates));
        info.put("formDetails", details);
        info.put("ecsMandates", mandates.stream().map(BankFormService::mandateSummaryMap).toList());
        enrichProductInfo(info, deposit.getAccountNumber());
        return info;
    }

    private Map<String, Object> recurringDepositInfo(RecurringDeposit deposit) {
        Map<String, Object> info = baseProductInfo(deposit.getRdAccountNumber(), deposit.getAccountNumber(),
                deposit.getCustomerName(), "recurringdeposit", deposit.getEstimatedMaturityAmount());
        Map<String, String> details = new LinkedHashMap<>();
        put(details, "Linked Funding Account", deposit.getAccountNumber());
        put(details, "RD Scheme", deposit.getSchemeName());
        put(details, "Monthly Installment", deposit.getMonthlyInstallment());
        put(details, "Annual Interest Rate", deposit.getAnnualInterestRate());
        put(details, "Tenure and Start Date", joinValues(deposit.getTenureMonths() == null ? null : deposit.getTenureMonths() + " months", deposit.getStartDate()));
        put(details, "Calculated Principal", deposit.getEstimatedPrincipal());
        put(details, "Estimated Interest and Maturity Amount", joinValues(deposit.getEstimatedInterest(), deposit.getEstimatedMaturityAmount()));
        put(details, "Maturity Date", deposit.getMaturityDate());
        put(details, "Payout / Allocation Account", deposit.getPayoutInstruction());
        put(details, "RD Status", deposit.getStatus());
        put(details, "Paid Installments", deposit.getPaidInstallments());
        List<EcsMandate> mandates = ecsMandatesFor(deposit.getRdAccountNumber(), deposit.getAccountNumber());
        put(details, "ECS Mandate and Separate Consent Status", mandateSummary(mandates));
        put(details, "Complete ECS Mandate Details", mandateDetails(mandates));
        info.put("formDetails", details);
        info.put("ecsMandates", mandates.stream().map(BankFormService::mandateSummaryMap).toList());
        enrichProductInfo(info, deposit.getAccountNumber());
        return info;
    }

    private Map<String, Object> ecsMandateInfo(EcsMandate mandate) {
        Map<String, Object> info = baseProductInfo(mandate.getLoanAccountNumber(),
                mandate.getSavingsAccountNumber(), mandate.getCustomerName(), "ecs", mandate.getEmiAmount());
        Map<String, String> details = new LinkedHashMap<>();
        put(details, "Product / Loan Account Number", joinValues(mandate.getProductType(), mandate.getLoanAccountNumber()));
        put(details, "Linked Debit Account Number", mandate.getSavingsAccountNumber());
        put(details, "Mandate Reference", mandate.getMandateId());
        put(details, "Authorized Maximum Debit", mandate.getAmountLimit());
        put(details, "Installment Amount", mandate.getEmiAmount());
        put(details, "Debit Frequency and Debit Day", mandate.getDebitDay() == null ? null : "Monthly, day " + mandate.getDebitDay());
        put(details, "Customer Name and Consent Timestamp", joinValues(mandate.getCustomerName(), mandate.getCustomerConsentAt()));
        put(details, "Consent Network Address", mandate.getCustomerConsentIp());
        put(details, "Mandate Status and Audit History", joinValues(mandate.getStatus(), mandate.getLastDebitStatus(), mandate.getSuccessfulDebits(), mandate.getFailedDebits()));
        put(details, "Complete ECS Mandate Details", mandateDetails(List.of(mandate)));
        info.put("formDetails", details);
        info.put("ecsMandates", List.of(mandateSummaryMap(mandate)));
        enrichProductInfo(info, mandate.getSavingsAccountNumber());
        return info;
    }

    private Map<String, Object> baseProductInfo(
            String productNumber, String linkedAccount, String holderName, String type, Object balance) {
        Map<String, Object> info = resolveCustomerAccount(linkedAccount);
        if (info == null) info = new LinkedHashMap<>();
        info.put("accountNumber", firstNonBlank(productNumber, linkedAccount));
        info.put("holderName", firstNonBlank(holderName, stringValue(info, "holderName")));
        info.put("accountType", type);
        info.put("balance", balance);
        info.put("productAccountNumber", productNumber);
        info.put("linkedSavingsAccountNumber", linkedAccount);
        return info;
    }

    private void enrichProductInfo(Map<String, Object> info, String accountNumber) {
        Map<String, Object> linked = resolveCustomerAccount(accountNumber);
        if (linked == null) return;
        for (String key : List.of("customerId", "aadhaarNumber", "panNumber", "phone", "email", "dateOfBirth", "address")) {
            if (info.get(key) == null || String.valueOf(info.get(key)).isBlank()) {
                info.put(key, linked.get(key));
            }
        }
        info.put("linkedAccountDetails", linked);
    }

    private Map<String, Object> resolveCustomerAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return null;
        for (String type : List.of("regular", "salary", "current")) {
            Map<String, Object> account = resolveAccount(accountNumber, type);
            if (account != null) return account;
        }
        return null;
    }

    private List<EcsMandate> ecsMandatesFor(String loanAccountNumber) {
        return ecsMandatesFor(loanAccountNumber, null);
    }

    private List<EcsMandate> ecsMandatesFor(String productAccountNumber, String savingsAccountNumber) {
        if (ecsMandateRepository == null) return List.of();
        Map<String, EcsMandate> byMandate = new LinkedHashMap<>();
        if (productAccountNumber != null && !productAccountNumber.isBlank()) {
            ecsMandateRepository.findByLoanAccountNumberOrderByCreatedAtDesc(productAccountNumber)
                    .forEach(mandate -> byMandate.putIfAbsent(mandateKey(mandate), mandate));
        }
        if (savingsAccountNumber != null && !savingsAccountNumber.isBlank()) {
            ecsMandateRepository.findBySavingsAccountNumberOrderByCreatedAtDesc(savingsAccountNumber)
                    .forEach(mandate -> byMandate.putIfAbsent(mandateKey(mandate), mandate));
        }
        return new ArrayList<>(byMandate.values());
    }

    private static String mandateKey(EcsMandate mandate) {
        if (mandate.getMandateId() != null && !mandate.getMandateId().isBlank()) return mandate.getMandateId();
        return mandate.getId() == null
                ? "instance-" + System.identityHashCode(mandate)
                : "id-" + mandate.getId();
    }

    private String repaymentSchedule(String loanAccountNumber) {
        if (emiPaymentRepository == null || loanAccountNumber == null || loanAccountNumber.isBlank()) return null;
        return emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(loanAccountNumber).stream()
                .map(payment -> joinValues(
                        "EMI " + payment.getEmiNumber(),
                        payment.getDueDate(),
                        "principal " + payment.getPrincipalAmount(),
                        "interest " + payment.getInterestAmount(),
                        "total " + payment.getTotalAmount(),
                        payment.getStatus(),
                        payment.getRemainingPrincipal()))
                .reduce((first, second) -> first + "; " + second)
                .orElse(null);
    }

    private static Map<String, Object> mandateSummaryMap(EcsMandate mandate) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mandateId", mandate.getMandateId());
        summary.put("loanAccountNumber", mandate.getLoanAccountNumber());
        summary.put("savingsAccountNumber", mandate.getSavingsAccountNumber());
        summary.put("status", mandate.getStatus());
        summary.put("emiAmount", mandate.getEmiAmount());
        summary.put("amountLimit", mandate.getAmountLimit());
        summary.put("debitDay", mandate.getDebitDay());
        summary.put("consentAt", mandate.getCustomerConsentAt());
        summary.put("consentIp", mandate.getCustomerConsentIp());
        return summary;
    }

    private static String mandateSummary(List<EcsMandate> mandates) {
        if (mandates == null || mandates.isEmpty()) return "No ECS mandate on record";
        return mandates.stream().map(m -> joinValues(m.getMandateId(), m.getStatus(), m.getEmiAmount()))
                .reduce((first, second) -> first + "; " + second).orElse("");
    }

    private static String mandateDetails(List<EcsMandate> mandates) {
        if (mandates == null || mandates.isEmpty()) return null;
        return mandates.stream().map(m -> joinValues(
                        m.getMandateId(), "status " + m.getStatus(),
                        "debit account " + m.getSavingsAccountNumber(),
                        "limit " + m.getAmountLimit(), "installment " + m.getEmiAmount(),
                        "debit day " + m.getDebitDay(), "consented " + m.getCustomerConsentAt(),
                        "consent IP " + m.getCustomerConsentIp()))
                .reduce((first, second) -> first + "; " + second).orElse(null);
    }

    private static boolean matchesHomeLoanType(HomeLoan loan, String expected) {
        String actual = loan.getLoanType() == null ? "HOME" : loan.getLoanType().toUpperCase(Locale.ROOT);
        return expected.equals(actual) || (expected.equals("CROP") && actual.equals("FARMER"));
    }

    private static boolean typeMatches(String actual, String expected) {
        return actual != null && actual.toLowerCase(Locale.ROOT).contains(expected);
    }

    private static boolean isApproved(String status) {
        return status != null && status.trim().equalsIgnoreCase("approved");
    }

    private static void put(Map<String, String> details, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            details.put(key, String.valueOf(value));
        }
    }

    private static String joinValues(Object... values) {
        return Arrays.stream(values).filter(Objects::nonNull).map(String::valueOf)
                .filter(value -> !value.isBlank()).reduce((first, second) -> first + " | " + second).orElse("");
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, entry) -> {
            if (key != null && entry != null) result.put(String.valueOf(key), String.valueOf(entry));
        });
        return result;
    }

    private Map<String, Object> resolveAccount(String accountNumber, String accountType) {
        if (accountNumber == null || accountNumber.isBlank()) return null;
        String num = accountNumber.trim();
        switch (normalizeAccountType(accountType)) {
            case "regular" -> {
                if (accountRepository == null) return null;
                Account account = accountRepository.findByAccountNumber(num);
                if (account != null) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("accountNumber", account.getAccountNumber());
                    result.put("holderName", account.getName());
                    result.put("accountType", "regular");
                    result.put("balance", account.getBalance());
                    result.put("customerId", account.getCustomerId());
                    result.put("aadhaarNumber", account.getAadharNumber());
                    result.put("panNumber", account.getPan());
                    result.put("phone", account.getPhone());
                    result.put("email", "");
                    result.put("dateOfBirth", account.getDob());
                    result.put("address", account.getAddress());
                    return result;
                }
            }
            case "salary" -> {
                if (salaryAccountRepository == null) return null;
                SalaryAccount account = salaryAccountRepository.findByAccountNumber(num);
                if (account != null) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("accountNumber", account.getAccountNumber());
                    result.put("holderName", account.getEmployeeName());
                    result.put("accountType", "salary");
                    result.put("balance", account.getBalance());
                    result.put("customerId", account.getCustomerId());
                    result.put("aadhaarNumber", account.getAadharNumber());
                    result.put("panNumber", account.getPanNumber());
                    result.put("phone", account.getMobileNumber());
                    result.put("email", account.getEmail());
                    result.put("dateOfBirth", account.getDob());
                    result.put("address", account.getAddress());
                    return result;
                }
            }
            case "current" -> {
                if (currentAccountRepository == null) return null;
                Optional<CurrentAccount> current = currentAccountRepository.findByAccountNumber(num);
                if (current.isPresent()) {
                    CurrentAccount account = current.get();
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("accountNumber", account.getAccountNumber());
                    result.put("holderName", account.getBusinessName() != null
                            ? account.getBusinessName() : account.getOwnerName());
                    result.put("accountType", "current");
                    result.put("balance", account.getBalance());
                    result.put("customerId", account.getCustomerId());
                    result.put("aadhaarNumber", account.getAadharNumber());
                    result.put("panNumber", account.getPanNumber());
                    result.put("phone", account.getMobile());
                    result.put("email", account.getEmail());
                    result.put("address", joinAddress(account.getShopAddress(), account.getCity(),
                            account.getState(), account.getPincode()));
                    return result;
                }
            }
            case "loan" -> {
                if (loanRepository == null) return null;
                Optional<Loan> byLoanNumber = loanRepository.findByLoanAccountNumber(num);
                if (byLoanNumber.isPresent()) return loanInfo(byLoanNumber.get());
                List<Loan> byAccount = loanRepository.findByAccountNumber(num);
                if (!byAccount.isEmpty()) return loanInfo(byAccount.get(0));
            }
            case "goldloan" -> {
                if (goldLoanRepository == null) return null;
                Optional<GoldLoan> byLoanNumber = goldLoanRepository.findByLoanAccountNumber(num);
                if (byLoanNumber.isPresent()) return goldLoanInfo(byLoanNumber.get());
                List<GoldLoan> byAccount = goldLoanRepository.findByAccountNumber(num);
                if (!byAccount.isEmpty()) return goldLoanInfo(byAccount.get(0));
            }
            case "cheque" -> {
                if (chequeRepository == null) return null;
                List<Cheque> cheques = chequeRepository.findByAccountNumber(num);
                if (!cheques.isEmpty()) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("accountNumber", cheques.get(0).getAccountNumber());
                    result.put("holderName", cheques.get(0).getAccountHolderName());
                    result.put("accountType", "cheque");
                    return result;
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
            if (upload.getStoredFilePath() != null && !upload.getStoredFilePath().isBlank()
                    && !upload.getStoredFilePath().startsWith("db://")) {
                Path path = resolveStoredPath(upload.getStoredFilePath());
                Files.deleteIfExists(path);
            }
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
                if (upload.getStoredFilePath() != null && !upload.getStoredFilePath().isBlank()
                        && !upload.getStoredFilePath().startsWith("db://")) {
                    Path path = resolveStoredPath(upload.getStoredFilePath());
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

    public static Path resolveUploadDirectory(String configuredDirectory) {
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            Path configured = Paths.get(configuredDirectory).normalize();
            if (configured.isAbsolute()) {
                return configured;
            }
            return Paths.get(System.getProperty("user.dir")).resolve(configured).normalize();
        }
        return Paths.get(System.getProperty("user.dir"), "uploads", "bank-forms").normalize();
    }

    private Path resolveStoredPath(String storedFilePath) {
        if (storedFilePath == null || storedFilePath.isBlank()) {
            throw new IllegalArgumentException("Uploaded file not found on server");
        }
        Path path = Paths.get(storedFilePath).normalize();
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }
        return path;
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

    private static String joinAddress(String... parts) {
        return Arrays.stream(parts)
                .filter(part -> part != null && !part.isBlank())
                .reduce((first, second) -> first + ", " + second)
                .orElse("");
    }

    private static String extension(String filename) {
        String lower = filename.toLowerCase();
        int dot = lower.lastIndexOf('.');
        return dot >= 0 ? lower.substring(dot + 1) : "";
    }

    public static String resolveContentType(String contentType, String ext) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        return switch (ext.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    public static MediaType resolveMediaType(String contentType, String filename) {
        String ext = extension(filename != null ? filename : "");
        String resolved = resolveContentType(contentType, ext);
        try {
            return MediaType.parseMediaType(resolved);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
