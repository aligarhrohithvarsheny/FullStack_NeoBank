package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.AccountConversionRequest;
import com.neo.springapp.model.AdminAuditLog;
import com.neo.springapp.model.Cheque;
import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.DemandDraft;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.AccountConversionRequestRepository;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AdminAuditLogRepository;
import com.neo.springapp.repository.ChequeRepository;
import com.neo.springapp.repository.CreditCardRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.DemandDraftRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.LoanRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AccountConversionService {

    private static final Map<String, Set<String>> ALLOWED_CONVERSIONS = new HashMap<>();

    static {
        ALLOWED_CONVERSIONS.put("Savings", new HashSet<>(Arrays.asList("Salary", "Joint", "Savings")));
        ALLOWED_CONVERSIONS.put("Salary", new HashSet<>(Collections.singletonList("Savings")));
        ALLOWED_CONVERSIONS.put("Minor", new HashSet<>(Collections.singletonList("Savings")));
        ALLOWED_CONVERSIONS.put("Individual", new HashSet<>(Collections.singletonList("Joint")));
    }

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private AccountConversionRequestRepository conversionRepository;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private GoldLoanRepository goldLoanRepository;

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private DemandDraftRepository demandDraftRepository;

    @Autowired
    private ChequeRepository chequeRepository;

    public static String normalizeSourceType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return "Savings";
        }
        String normalized = sourceType.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("salary")) return "Salary";
        if (lower.contains("joint")) return "Joint";
        if (lower.contains("minor")) return "Minor";
        if (lower.contains("individual")) return "Individual";
        if (lower.contains("current")) return "Current";
        return "Savings";
    }

    public static String normalizeTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) return "Savings";
        String normalized = targetType.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("salary")) return "Salary";
        if (lower.contains("joint")) return "Joint";
        if (lower.contains("minor")) return "Minor";
        if (lower.contains("individual")) return "Individual";
        if (lower.contains("current")) return "Current";
        return "Savings";
    }

    public static boolean isSupportedConversion(String sourceType, String targetType) {
        String normalizedSource = normalizeSourceType(sourceType);
        String normalizedTarget = normalizeTargetType(targetType);
        Set<String> allowedTargets = ALLOWED_CONVERSIONS.getOrDefault(normalizedSource, Collections.emptySet());
        return allowedTargets.contains(normalizedTarget);
    }

    public Map<String, Object> lookupAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }

        String normalizedNumber = accountNumber.trim();
        Account savings = accountRepository.findByAccountNumber(normalizedNumber);
        if (savings != null) {
            return toLookupResponse("Savings", savings.getId(), savings.getAccountNumber(), savings.getName(),
                    savings.getPhone(), savings.getAadharNumber(), savings.getPan(), savings.getAddress(),
                    savings.getCustomerId(), savings.getStatus(), savings.getAccountType(), savings.getBalance(),
                    savings.isChildAccount(), savings.getParentAccountId());
        }

        SalaryAccount salary = salaryAccountRepository.findByAccountNumber(normalizedNumber);
        if (salary != null) {
            return toLookupResponse("Salary", salary.getId(), salary.getAccountNumber(), salary.getEmployeeName(),
                    salary.getMobileNumber(), salary.getAadharNumber(), salary.getPanNumber(), salary.getAddress(),
                    salary.getCustomerId(), salary.getStatus(), "Salary", salary.getBalance(), false, null);
        }

        Optional<CurrentAccount> current = currentAccountRepository.findByAccountNumber(normalizedNumber);
        if (current.isPresent()) {
            CurrentAccount c = current.get();
            return toLookupResponse("Current", c.getId(), c.getAccountNumber(), c.getOwnerName(),
                    c.getMobile(), c.getAadharNumber(), c.getPanNumber(), c.getShopAddress(), c.getCustomerId(),
                    c.getStatus(), "Current", c.getBalance(), false, null);
        }

        throw new IllegalArgumentException("Account not found with number: " + normalizedNumber);
    }

    public Map<String, Object> generateApplication(String accountNumber, String targetAccountType, String requestedBy) {
        Map<String, Object> account = lookupAccount(accountNumber);
        String sourceType = String.valueOf(account.getOrDefault("sourceType", "Savings"));
        String normalizedTarget = normalizeTargetType(targetAccountType);
        if (!isSupportedConversion(sourceType, normalizedTarget)) {
            throw new IllegalArgumentException("Unsupported conversion from " + sourceType + " to " + normalizedTarget);
        }

        Map<String, Object> debtSummary = buildDebtSummary(accountNumber);
        String appNumber = "ACV-" + System.currentTimeMillis();
        String content = buildApplicationText(account, normalizedTarget, requestedBy, appNumber);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("applicationNumber", appNumber);
        response.put("accountNumber", accountNumber);
        response.put("sourceType", sourceType);
        response.put("targetType", normalizedTarget);
        response.put("requestedBy", requestedBy != null && !requestedBy.isBlank() ? requestedBy : "Admin");
        response.put("applicationText", content);
        response.put("debtSummary", debtSummary);
        response.put("hasOpenDebt", Boolean.TRUE.equals(debtSummary.get("hasOpenDebt")));
        response.put("canConvert", !Boolean.TRUE.equals(debtSummary.get("hasOpenDebt")));
        response.put("termsAndConditions", "Terms & Conditions: The account holder confirms the conversion request, accepts updated KYC and signature verification, and understands the account will be audited and may be reverted to the original account type with full historical traceability.");
        return response;
    }

    public AccountConversionRequest submitRequest(String accountNumber, String sourceType, String targetType,
                                                String requestedBy, String reason, String termsPath,
                                                String applicationNumber, String applicationContent) {
        Map<String, Object> lookup = lookupAccount(accountNumber);
        String normalizedSource = normalizeSourceType(sourceType);
        String normalizedTarget = normalizeTargetType(targetType);
        if (!isSupportedConversion(normalizedSource, normalizedTarget)) {
            throw new IllegalArgumentException("Unsupported conversion from " + normalizedSource + " to " + normalizedTarget);
        }

        AccountConversionRequest request = new AccountConversionRequest();
        request.setAccountNumber(String.valueOf(lookup.get("accountNumber")));
        request.setAccountHolderName(String.valueOf(lookup.get("name")));
        request.setOriginalAccountType(normalizedSource);
        request.setTargetAccountType(normalizedTarget);
        request.setRequestedBy(requestedBy != null && !requestedBy.isBlank() ? requestedBy : "Admin");
        request.setRequestStatus("PENDING");
        request.setReason(reason);
        request.setTermsAndConditionsPath(termsPath);
        request.setApplicationNumber(applicationNumber != null && !applicationNumber.isBlank() ? applicationNumber : "ACV-" + System.currentTimeMillis());
        request.setApplicationContent(applicationContent);
        request.setPriorAccountType(normalizedSource);
        request.setAuditSummary("Requested account conversion from " + normalizedSource + " to " + normalizedTarget + ". Approval pending.");
        return conversionRepository.save(request);
    }

    public List<AccountConversionRequest> getHistory() {
        return conversionRepository.findAllByOrderByRequestedAtDesc();
    }

    public AccountConversionRequest approveRequest(Long requestId, String approvedBy) {
        AccountConversionRequest request = conversionRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Conversion request not found: " + requestId));

        String previousType = request.getOriginalAccountType();
        String targetType = request.getTargetAccountType();
        String accountNumber = request.getAccountNumber();

        boolean applied = applyAccountTypeChange(accountNumber, previousType, targetType, approvedBy);
        if (!applied) {
            throw new IllegalStateException("Unable to update account type for account " + accountNumber);
        }

        request.setRequestStatus("APPROVED");
        request.setApprovedBy(approvedBy);
        request.setApprovedAt(LocalDateTime.now());
        request.setSignatureVerified(Boolean.TRUE);
        request.setSignatureVerifiedBy(approvedBy);
        request.setSignatureVerifiedAt(LocalDateTime.now());
        request.setAuditSummary("Approved account conversion from " + previousType + " to " + targetType + ".");
        request.setUpdatedAt(LocalDateTime.now());
        AccountConversionRequest saved = conversionRepository.save(request);

        AdminAuditLog audit = new AdminAuditLog();
        audit.setAdminName(approvedBy != null && !approvedBy.isBlank() ? approvedBy : "Admin");
        audit.setActionType("ACCOUNT_CONVERSION_APPROVED");
        audit.setEntityType("ACCOUNT_CONVERSION");
        audit.setEntityId(saved.getId());
        audit.setEntityName(accountNumber);
        audit.setChanges("{\"fromType\":\"" + previousType + "\",\"toType\":\"" + targetType + "\"}");
        audit.setOldValues("{\"previousType\":\"" + previousType + "\"}");
        audit.setNewValues("{\"updatedType\":\"" + targetType + "\"}");
        audit.setDocumentRequired(Boolean.TRUE);
        audit.setDocumentUploaded(saved.getTermsAndConditionsPath() != null && !saved.getTermsAndConditionsPath().isBlank());
        audit.setReasonForChange(saved.getReason());
        audit.setStatus("COMPLETED");
        adminAuditLogRepository.save(audit);
        return saved;
    }

    public AccountConversionRequest revertRequest(Long requestId, String revertedBy) {
        AccountConversionRequest request = conversionRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Conversion request not found: " + requestId));

        String originalType = request.getOriginalAccountType();
        String accountNumber = request.getAccountNumber();
        String currentType = request.getTargetAccountType();

        boolean reverted = applyAccountTypeChange(accountNumber, currentType, originalType, revertedBy);
        if (!reverted) {
            throw new IllegalStateException("Unable to revert account type for account " + accountNumber);
        }

        request.setRequestStatus("REVERTED");
        request.setRevertedToOriginal(Boolean.TRUE);
        request.setApprovedBy(revertedBy);
        request.setApprovedAt(LocalDateTime.now());
        request.setAuditSummary("Reverted account from " + currentType + " back to " + originalType + ".");
        request.setUpdatedAt(LocalDateTime.now());
        return conversionRepository.save(request);
    }

    private Map<String, Object> toLookupResponse(String sourceType, Long id, String accountNumber, String name,
                                                String phone, String aadhar, String pan, String address,
                                                String customerId, String status, String currentType, Double balance,
                                                boolean minor, Long parentAccountId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("sourceType", sourceType);
        map.put("accountNumber", accountNumber);
        map.put("name", name);
        map.put("phone", phone);
        map.put("aadharNumber", aadhar);
        map.put("pan", pan);
        map.put("address", address);
        map.put("customerId", customerId);
        map.put("status", status);
        map.put("currentType", currentType != null ? currentType : sourceType);
        map.put("balance", balance != null ? balance : 0.0);
        map.put("minorAccount", minor);
        map.put("parentAccountId", parentAccountId);
        return map;
    }

    public List<String> getConversionBlockers(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return Collections.emptyList();
        }
        return validateSavingsToSalaryConversion(accountNumber);
    }

    public List<String> validateSavingsToSalaryConversion(String accountNumber) {
        List<String> blockers = new ArrayList<>();
        if (accountNumber == null || accountNumber.isBlank()) {
            return blockers;
        }

        List<Loan> activeLoans = loanRepository.findByAccountNumber(accountNumber).stream()
                .filter(loan -> loan != null && !"Closed".equalsIgnoreCase(loan.getStatus())
                        && !"Rejected".equalsIgnoreCase(loan.getStatus())
                        && !"Paid".equalsIgnoreCase(loan.getStatus())
                        && !"Foreclosed".equalsIgnoreCase(loan.getStatus()))
                .toList();
        if (!activeLoans.isEmpty()) {
            double total = activeLoans.stream().mapToDouble(loan -> loan.getRemainingPrincipal() != null ? loan.getRemainingPrincipal() : 0.0).sum();
            blockers.add("Outstanding loan(s) remain: ₹" + String.format(Locale.US, "%.2f", total) + ". All loans must be closed before salary conversion.");
        }

        List<GoldLoan> activeGoldLoans = goldLoanRepository.findByAccountNumber(accountNumber).stream()
                .filter(loan -> loan != null && !"Closed".equalsIgnoreCase(loan.getStatus())
                        && !"Rejected".equalsIgnoreCase(loan.getStatus())
                        && !"Paid".equalsIgnoreCase(loan.getStatus())
                        && !"Foreclosed".equalsIgnoreCase(loan.getStatus()))
                .toList();
        if (!activeGoldLoans.isEmpty()) {
            double total = activeGoldLoans.stream().mapToDouble(loan -> loan.getRemainingPrincipal() != null ? loan.getRemainingPrincipal() : 0.0).sum();
            blockers.add("Outstanding gold loan(s) remain: ₹" + String.format(Locale.US, "%.2f", total) + ". Clear all gold loans before conversion.");
        }

        List<CreditCard> activeCards = creditCardRepository.findByAccountNumber(accountNumber).stream()
                .filter(card -> card != null && ("Active".equalsIgnoreCase(card.getStatus()) || "Blocked".equalsIgnoreCase(card.getStatus()))
                        && (card.getCurrentBalance() == null || card.getCurrentBalance() > 0 || "Active".equalsIgnoreCase(card.getStatus())))
                .toList();
        if (!activeCards.isEmpty()) {
            double total = activeCards.stream().mapToDouble(card -> card.getCurrentBalance() != null ? card.getCurrentBalance() : 0.0).sum();
            blockers.add("Active credit/debit card debt remains: ₹" + String.format(Locale.US, "%.2f", total) + ". Close and clear all cards before conversion.");
        }

        List<Cheque> activeCheques = chequeRepository.findByAccountNumber(accountNumber).stream()
                .filter(cheque -> cheque != null && !"CANCELLED".equalsIgnoreCase(cheque.getStatus())
                        && !"USED".equalsIgnoreCase(cheque.getStatus())
                        && !"DRAWN".equalsIgnoreCase(cheque.getStatus())
                        && !"BOUNCED".equalsIgnoreCase(cheque.getStatus()))
                .toList();
        if (!activeCheques.isEmpty()) {
            blockers.add("Active cheque books must be permanently closed before salary conversion.");
        }

        List<DemandDraft> activeDrafts = demandDraftRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber).stream()
                .filter(draft -> draft != null && !"CANCELLED".equalsIgnoreCase(draft.getStatus())
                        && !"PAID".equalsIgnoreCase(draft.getStatus())
                        && !"REJECTED".equalsIgnoreCase(draft.getStatus()))
                .toList();
        if (!activeDrafts.isEmpty()) {
            blockers.add("Demand drafts are still active and must be closed before salary conversion.");
        }

        return blockers;
    }

    private Map<String, Object> buildDebtSummary(String accountNumber) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("accountNumber", accountNumber);
        List<String> blockers = getConversionBlockers(accountNumber);
        summary.put("hasOpenDebt", !blockers.isEmpty());
        summary.put("blockers", blockers);
        summary.put("loans", loanRepository.findByAccountNumber(accountNumber).stream().filter(Objects::nonNull).toList());
        summary.put("goldLoans", goldLoanRepository.findByAccountNumber(accountNumber).stream().filter(Objects::nonNull).toList());
        summary.put("creditCards", creditCardRepository.findByAccountNumber(accountNumber).stream().filter(Objects::nonNull).toList());
        summary.put("cheques", chequeRepository.findByAccountNumber(accountNumber).stream().filter(Objects::nonNull).toList());
        summary.put("demandDrafts", demandDraftRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber).stream().filter(Objects::nonNull).toList());
        return summary;
    }

    private String buildApplicationText(Map<String, Object> account, String targetType, String requestedBy, String appNumber) {
        String name = String.valueOf(account.getOrDefault("name", ""));
        String accountNumber = String.valueOf(account.getOrDefault("accountNumber", ""));
        String sourceType = String.valueOf(account.getOrDefault("sourceType", "Savings"));
        return "Account Conversion Application\n"
                + "Application Number: " + appNumber + "\n"
                + "Requested By: " + (requestedBy != null && !requestedBy.isBlank() ? requestedBy : "Admin") + "\n"
                + "Account Number: " + accountNumber + "\n"
                + "Applicant Name: " + name + "\n"
                + "Existing Account Type: " + sourceType + "\n"
                + "Requested New Type: " + targetType + "\n"
                + "Declaration: The customer confirms that all data submitted is true, the account conversion is requested under the bank policy, and the customer accepts the updated product terms and conditions.\n"
                + "Signature verification is required before final approval. The bank may review and revert the account if the account type change is not valid.";
    }

    private void closeSavingsLinkedProducts(String accountNumber, String approvedBy) {
        chequeRepository.findByAccountNumber(accountNumber).forEach(cheque -> {
            if (cheque != null && !"CANCELLED".equalsIgnoreCase(cheque.getStatus())) {
                cheque.setStatus("CANCELLED");
                cheque.setCancelledDate(LocalDateTime.now());
                cheque.setCancelledBy(approvedBy != null && !approvedBy.isBlank() ? approvedBy : "Admin");
                cheque.setCancellationReason("Permanent closure due to savings-to-salary conversion");
                chequeRepository.save(cheque);
            }
        });

        demandDraftRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber).forEach(draft -> {
            if (draft != null && !"CANCELLED".equalsIgnoreCase(draft.getStatus())) {
                draft.setStatus("CANCELLED");
                draft.setUpdatedAt(LocalDateTime.now());
                draft.setApprovedBy(approvedBy != null && !approvedBy.isBlank() ? approvedBy : "Admin");
                draft.setReason(draft.getReason() == null || draft.getReason().isBlank() ? "Closed on salary conversion" : draft.getReason());
                demandDraftRepository.save(draft);
            }
        });

        creditCardRepository.findByAccountNumber(accountNumber).forEach(card -> {
            if (card != null && !"Closed".equalsIgnoreCase(card.getStatus())) {
                card.setStatus("Closed");
                card.setBlocked(true);
                card.setDeactivated(true);
                card.setClosureDate(LocalDateTime.now());
                card.setCurrentBalance(0.0);
                card.setAvailableLimit(0.0);
                card.setUsageLimit(0.0);
                card.setOverdueAmount(0.0);
                creditCardRepository.save(card);
            }
        });
    }

    private void validateSavingsConversionData(SalaryAccount salary) {
        if (salary == null) {
            throw new IllegalArgumentException("Salary account is required for conversion.");
        }
        if (salary.getAccountNumber() == null || salary.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Salary account number is missing.");
        }
        if (salary.getCustomerId() == null || salary.getCustomerId().isBlank()) {
            throw new IllegalArgumentException("Salary account customer ID is missing.");
        }
        if (salary.getEmployeeName() == null || salary.getEmployeeName().isBlank()) {
            throw new IllegalArgumentException("Salary account holder name is missing.");
        }
        if (salary.getMobileNumber() == null || salary.getMobileNumber().isBlank()) {
            throw new IllegalArgumentException("Salary account mobile number is missing.");
        }
        if (salary.getAadharNumber() == null || salary.getAadharNumber().isBlank()) {
            throw new IllegalArgumentException("Salary account Aadhar is missing; conversion to savings is blocked.");
        }
        if (salary.getPanNumber() == null || salary.getPanNumber().isBlank()) {
            throw new IllegalArgumentException("Salary account PAN is missing; conversion to savings is blocked.");
        }
    }

    private boolean applyAccountTypeChange(String accountNumber, String fromType, String toType, String approvedBy) {
        String sourceType = normalizeSourceType(fromType);
        String targetType = normalizeTargetType(toType);

        if ("Savings".equals(sourceType) && "Salary".equals(targetType)) {
            List<String> blockers = validateSavingsToSalaryConversion(accountNumber);
            if (!blockers.isEmpty()) {
                throw new IllegalStateException(String.join("; ", blockers));
            }

            Account account = accountRepository.findByAccountNumber(accountNumber);
            if (account == null) {
                throw new IllegalArgumentException("Savings account not found: " + accountNumber);
            }

            closeSavingsLinkedProducts(accountNumber, approvedBy);

            account.setAccountType("Salary");
            account.setStatus("CONVERTED_TO_SALARY");
            account.setNetBankingEnabled(false);
            account.setLastUpdated(LocalDateTime.now());
            accountRepository.save(account);

            SalaryAccount salary = salaryAccountRepository.findByCustomerId(account.getCustomerId());
            if (salary == null) {
                salary = salaryAccountRepository.findByAccountNumber(accountNumber);
            }
            if (salary == null) {
                salary = new SalaryAccount();
                salary.setAccountNumber(accountNumber);
                salary.setCustomerId(account.getCustomerId());
            }

            salary.setEmployeeName(account.getName());
            salary.setMobileNumber(account.getPhone());
            salary.setAadharNumber(account.getAadharNumber());
            salary.setPanNumber(account.getPan());
            salary.setAddress(account.getAddress());
            salary.setBalance(account.getBalance());
            salary.setStatus("Active");
            salary.setPasswordSet(false);
            salary.setPassword(null);
            salary.setDebitCardStatus("Closed");
            salary.setNetBankingEnabled(true);
            salary.setUpiEnabled(false);
            salary.setOnlineEnabled(false);
            salary.setContactlessEnabled(false);
            salary.setAccountLocked(false);
            salary.setUpdatedAt(LocalDateTime.now());
            salaryAccountRepository.save(salary);
            return true;
        }

        if ("Salary".equals(sourceType) && "Savings".equals(targetType)) {
            SalaryAccount salary = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (salary == null) {
                throw new IllegalArgumentException("Salary account not found: " + accountNumber);
            }
            validateSavingsConversionData(salary);

            Account account = accountRepository.findByAccountNumber(accountNumber);
            if (account == null) {
                account = accountRepository.findByCustomerId(salary.getCustomerId());
            }
            if (account == null) {
                account = new Account();
                account.setAccountNumber(accountNumber);
            }

            account.setName(salary.getEmployeeName());
            account.setPhone(salary.getMobileNumber());
            account.setAadharNumber(salary.getAadharNumber());
            account.setPan(salary.getPanNumber());
            account.setAddress(salary.getAddress());
            account.setCustomerId(salary.getCustomerId());
            account.setStatus("ACTIVE");
            account.setBalance(salary.getBalance() != null ? salary.getBalance() : 0.0);
            account.setAccountType("Savings");
            account.setLastUpdated(LocalDateTime.now());
            accountRepository.save(account);

            salary.setStatus("CONVERTED_TO_SAVINGS");
            salary.setUpdatedAt(LocalDateTime.now());
            salaryAccountRepository.save(salary);
            return true;
        }

        if ("Minor".equals(sourceType) && "Savings".equals(targetType)) {
            Account account = accountRepository.findByAccountNumber(accountNumber);
            if (account == null) {
                throw new IllegalArgumentException("Minor account not found: " + accountNumber);
            }
            account.setAccountType("Savings");
            account.setChildAccount(false);
            account.setParentAccountId(null);
            account.setLastUpdated(LocalDateTime.now());
            accountRepository.save(account);
            return true;
        }

        if ("Individual".equals(sourceType) && "Joint".equals(targetType)) {
            Account account = accountRepository.findByAccountNumber(accountNumber);
            if (account == null) {
                throw new IllegalArgumentException("Individual account not found: " + accountNumber);
            }
            account.setAccountType("Joint");
            account.setStatus("ACTIVE");
            account.setLastUpdated(LocalDateTime.now());
            accountRepository.save(account);
            return true;
        }

        if ("Savings".equals(sourceType) && "Joint".equals(targetType)) {
            Account account = accountRepository.findByAccountNumber(accountNumber);
            if (account == null) {
                throw new IllegalArgumentException("Savings account not found: " + accountNumber);
            }
            account.setAccountType("Joint");
            account.setStatus("ACTIVE");
            account.setLastUpdated(LocalDateTime.now());
            accountRepository.save(account);
            return true;
        }

        return false;
    }
}
