package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.HomeLoan;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.EmiPaymentRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.HomeLoanRepository;
import com.neo.springapp.repository.LoanRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LoanRepaymentService {
    private final LoanRepository loanRepository;
    private final GoldLoanRepository goldLoanRepository;
    private final HomeLoanRepository homeLoanRepository;
    private final EmiPaymentRepository emiPaymentRepository;
    private final AccountRepository accountRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final EmiService emiService;
    private final HomeLoanService homeLoanService;
    private final AccountService accountService;
    private final PdfService pdfService;

    public LoanRepaymentService(
            LoanRepository loanRepository,
            GoldLoanRepository goldLoanRepository,
            HomeLoanRepository homeLoanRepository,
            EmiPaymentRepository emiPaymentRepository,
            AccountRepository accountRepository,
            SalaryAccountRepository salaryAccountRepository,
            CurrentAccountRepository currentAccountRepository,
            EmiService emiService,
            HomeLoanService homeLoanService,
            AccountService accountService,
            PdfService pdfService) {
        this.loanRepository = loanRepository;
        this.goldLoanRepository = goldLoanRepository;
        this.homeLoanRepository = homeLoanRepository;
        this.emiPaymentRepository = emiPaymentRepository;
        this.accountRepository = accountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.emiService = emiService;
        this.homeLoanService = homeLoanService;
        this.accountService = accountService;
        this.pdfService = pdfService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> lookup(String loanAccountNumber, String dob) {
        LoanDetails details = verify(loanAccountNumber, dob);
        return details.toResponse();
    }

    @Transactional(rollbackFor = IOException.class)
    public Map<String, Object> pay(
            String loanAccountNumber,
            String dob,
            String paymentMode,
            Integer emiNumber,
            Double amount,
            String adjustment) throws IOException {
        LoanDetails details = verify(loanAccountNumber, dob);
        String mode = paymentMode == null ? "" : paymentMode.trim().toUpperCase(Locale.ROOT);
        Transaction transaction;
        double paidAmount;
        double remaining;

        if ("EMI".equals(mode)) {
            if ("HOME".equals(details.kind)) {
                if (emiNumber != null && emiNumber != details.homeLoan.getPaidEmis() + 1) {
                    throw new IllegalArgumentException("Only the next scheduled EMI can be paid.");
                }
                HomeLoanService.RepaymentResult result = homeLoanService.payEmiFromAccountWithResult(
                        details.homeLoan.getId(), details.accountNumber, "Customer");
                transaction = result.transaction();
                paidAmount = transaction.getAmount();
                remaining = value(result.loan().getRemainingPrincipal());
            } else {
                EmiPayment emi = nextPendingEmi(details, emiNumber);
                Map<String, Object> result = emiService.payEmi(emi.getId(), details.accountNumber);
                if (!Boolean.TRUE.equals(result.get("success"))) {
                    throw new IllegalStateException(String.valueOf(result.get("message")));
                }
                transaction = (Transaction) result.get("transaction");
                paidAmount = emi.getTotalAmount();
                remaining = emi.getRemainingPrincipal();
            }
        } else if ("PREPAYMENT".equals(mode) && "HOME".equals(details.kind)) {
            if (amount == null || !Double.isFinite(amount) || amount <= 0
                    || amount >= value(details.homeLoan.getRemainingPrincipal())) {
                throw new IllegalArgumentException(
                        "Enter a valid prepayment amount below the outstanding principal.");
            }
            HomeLoanService.RepaymentResult result = homeLoanService.prepayFromAccountWithResult(
                    details.homeLoan.getId(), details.accountNumber, amount, normalizeAdjustment(adjustment), "Customer");
            transaction = result.transaction();
            paidAmount = transaction.getAmount();
            remaining = value(result.loan().getRemainingPrincipal());
        } else {
            throw new IllegalArgumentException("This loan does not support the selected payment option.");
        }

        if (transaction == null) {
            throw new IllegalStateException("Payment was processed without a transaction record.");
        }

        Double balanceAfter = accountService.getBalanceByAccountNumber(details.accountNumber);
        if (balanceAfter == null) {
            throw new IllegalStateException("Unable to read the linked account balance after payment.");
        }
        String maskedAccount = maskAccountNumber(details.accountNumber);
        byte[] receipt = pdfService.generateLoanPaymentReceipt(
                details.maskedName,
                maskAccountNumber(details.loanAccountNumber),
                details.loanType,
                mode,
                paidAmount,
                remaining,
                maskedAccount,
                transaction.getTransactionId(),
                transaction.getDate(),
                balanceAfter);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Loan payment completed successfully.");
        response.put("paymentMode", mode);
        response.put("amount", paidAmount);
        response.put("remainingPrincipal", remaining);
        response.put("newBalance", balanceAfter);
        Map<String, Object> transactionSummary = new LinkedHashMap<>();
        transactionSummary.put("transactionId", transaction.getTransactionId());
        transactionSummary.put("amount", transaction.getAmount());
        transactionSummary.put("date", transaction.getDate());
        transactionSummary.put("type", transaction.getType());
        transactionSummary.put("status", transaction.getStatus());
        response.put("transaction", transactionSummary);
        response.put("receiptPdfBase64", Base64.getEncoder().encodeToString(receipt));
        return response;
    }

    private LoanDetails verify(String loanAccountNumber, String dob) {
        if (loanAccountNumber == null || loanAccountNumber.isBlank() || dob == null || dob.isBlank()) {
            throw new IllegalArgumentException("Loan number and date of birth are required.");
        }
        String number = loanAccountNumber.trim();
        Loan loan = loanRepository.findByLoanAccountNumber(number).orElse(null);
        if (loan != null) {
            if (!"Approved".equalsIgnoreCase(loan.getStatus())) {
                throw new IllegalArgumentException("Only approved loans can be paid.");
            }
            return verifiedDetails("STANDARD", number, loan.getAccountNumber(), loan.getUserName(),
                    loan.getType(), loan.getStatus(), loan, null, null, dob);
        }

        GoldLoan goldLoan = goldLoanRepository.findByLoanAccountNumber(number).orElse(null);
        if (goldLoan != null) {
            if (!"Approved".equalsIgnoreCase(goldLoan.getStatus())
                    || !Boolean.TRUE.equals(goldLoan.getTermsAccepted())) {
                throw new IllegalArgumentException("Only approved gold loans with accepted terms can be paid.");
            }
            return verifiedDetails("GOLD", number, goldLoan.getAccountNumber(), goldLoan.getUserName(),
                    "Gold Loan", goldLoan.getStatus(), null, goldLoan, null, dob);
        }

        HomeLoan homeLoan = homeLoanRepository.findByLoanAccountNumber(number).orElse(null);
        if (homeLoan != null) {
            if (!"Approved".equalsIgnoreCase(homeLoan.getStatus())) {
                throw new IllegalArgumentException("Only approved home loans can be paid.");
            }
            return verifiedDetails("HOME", number, homeLoan.getAccountNumber(), homeLoan.getUserName(),
                    "Home Loan", homeLoan.getStatus(), null, null, homeLoan, dob);
        }
        throw new IllegalArgumentException("Loan details could not be verified.");
    }

    private LoanDetails verifiedDetails(
            String kind,
            String loanAccountNumber,
            String accountNumber,
            String loanCustomerName,
            String loanType,
            String status,
            Loan loan,
            GoldLoan goldLoan,
            HomeLoan homeLoan,
            String suppliedDob) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        String storedDob = account == null ? null : account.getDob();
        String customerName = account == null ? loanCustomerName : account.getName();
        if (storedDob == null) {
            SalaryAccount salary = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (salary != null) {
                storedDob = salary.getDob();
                customerName = salary.getEmployeeName();
            }
        }
        if (storedDob == null) {
            Optional<CurrentAccount> current = currentAccountRepository.findByAccountNumber(accountNumber);
            if (current.isPresent()) {
                storedDob = current.get().getOwnerDob();
                customerName = current.get().getOwnerName();
            }
        }
        if (!sameDate(suppliedDob, storedDob)) {
            throw new IllegalArgumentException("Loan details could not be verified.");
        }

        LoanDetails details = new LoanDetails(kind, loanAccountNumber, accountNumber,
                customerName == null ? loanCustomerName : customerName, loanType, status, loan, goldLoan, homeLoan);
        List<Map<String, Object>> emis = new ArrayList<>();
        double outstanding;
        if (homeLoan != null) {
            outstanding = value(homeLoan.getRemainingPrincipal());
            Map<String, Object> next = homeLoanService.emiDue(homeLoan.getId());
            if (next != null && next.get("emiNumber") != null) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("emiNumber", next.get("emiNumber"));
                entry.put("dueDate", next.get("dueDate"));
                entry.put("amount", next.get("totalAmount"));
                emis.add(entry);
            }
            details.prepaymentSupported = true;
            details.prepaymentChargePercent = 1.0;
        } else {
            List<EmiPayment> pending = emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(loanAccountNumber)
                    .stream()
                    .filter(emi -> "Pending".equalsIgnoreCase(emi.getStatus()))
                    .sorted(Comparator.comparing(EmiPayment::getEmiNumber))
                    .toList();
            for (EmiPayment emi : pending.stream().limit(1).toList()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("emiNumber", emi.getEmiNumber());
                entry.put("dueDate", emi.getDueDate());
                entry.put("amount", emi.getTotalAmount());
                emis.add(entry);
            }
            if (!pending.isEmpty()) {
                outstanding = pending.stream().mapToDouble(emi -> value(emi.getPrincipalAmount())).sum();
            } else if (loan != null) {
                outstanding = value(loan.getRemainingPrincipal() == null ? loan.getAmount() : loan.getRemainingPrincipal());
            } else {
                outstanding = value(goldLoan.getRemainingPrincipal() == null
                        ? goldLoan.getLoanAmount() : goldLoan.getRemainingPrincipal());
            }
        }
        details.emis = emis;
        details.outstandingPrincipal = outstanding;
        details.maskedName = maskName(details.customerName);
        return details;
    }

    private EmiPayment nextPendingEmi(LoanDetails details, Integer requestedNumber) {
        List<EmiPayment> pending = emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(details.loanAccountNumber)
                .stream()
                .filter(emi -> "Pending".equalsIgnoreCase(emi.getStatus()))
                .sorted(Comparator.comparing(EmiPayment::getEmiNumber))
                .toList();
        if (pending.isEmpty()) {
            throw new IllegalArgumentException("There are no pending EMIs for this loan.");
        }
        EmiPayment next = pending.get(0);
        if (requestedNumber != null && !requestedNumber.equals(next.getEmiNumber())) {
            throw new IllegalArgumentException("Please pay the next pending EMI first.");
        }
        return next;
    }

    private static String normalizeAdjustment(String adjustment) {
        return "REDUCE_EMI".equalsIgnoreCase(adjustment) ? "REDUCE_EMI" : "REDUCE_TENURE";
    }

    private static boolean sameDate(String supplied, String stored) {
        if (supplied == null || stored == null) return false;
        LocalDate suppliedDate = parseDate(supplied);
        LocalDate storedDate = parseDate(stored);
        if (suppliedDate != null && storedDate != null) return suppliedDate.equals(storedDate);
        return supplied.replaceAll("[^0-9]", "").equals(stored.replaceAll("[^0-9]", ""));
    }

    private static LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.BASIC_ISO_DATE)) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported date representation.
            }
        }
        return null;
    }

    private static double value(Double value) {
        return value == null ? 0.0 : value;
    }

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) return "****";
        return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) return "Customer";
        return java.util.Arrays.stream(name.trim().split("\\s+"))
                .map(part -> part.substring(0, 1) + "*".repeat(Math.max(2, part.length() - 1)))
                .collect(Collectors.joining(" "));
    }

    private static final class LoanDetails {
        private final String kind;
        private final String loanAccountNumber;
        private final String accountNumber;
        private final String customerName;
        private final String loanType;
        private final String status;
        private final Loan loan;
        private final GoldLoan goldLoan;
        private final HomeLoan homeLoan;
        private List<Map<String, Object>> emis = List.of();
        private double outstandingPrincipal;
        private boolean prepaymentSupported;
        private double prepaymentChargePercent;
        private String maskedName;

        private LoanDetails(String kind, String loanAccountNumber, String accountNumber, String customerName,
                String loanType, String status, Loan loan, GoldLoan goldLoan, HomeLoan homeLoan) {
            this.kind = kind;
            this.loanAccountNumber = loanAccountNumber;
            this.accountNumber = accountNumber;
            this.customerName = customerName;
            this.loanType = loanType;
            this.status = status;
            this.loan = loan;
            this.goldLoan = goldLoan;
            this.homeLoan = homeLoan;
        }

        private Map<String, Object> toResponse() {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("maskedName", maskedName);
            response.put("loanType", loanType);
            response.put("maskedLoanAccountNumber", maskAccountNumber(loanAccountNumber));
            response.put("linkedAccountNumber", maskAccountNumber(accountNumber));
            response.put("status", status);
            response.put("outstandingPrincipal", outstandingPrincipal);
            response.put("emis", emis);
            response.put("prepaymentSupported", prepaymentSupported);
            if (prepaymentSupported) {
                response.put("prepaymentChargePercent", prepaymentChargePercent);
                response.put("prepaymentGstPercent", 18.0);
                response.put("prepaymentAdjustmentOptions", List.of("REDUCE_TENURE", "REDUCE_EMI"));
            }
            return response;
        }
    }
}
