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
import org.springframework.beans.factory.annotation.Autowired;

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
    private final GoldLoanService goldLoanService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final PdfService pdfService;
    @Autowired(required = false)
    private CustomerEventNotificationService notificationService;

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
        this(loanRepository, goldLoanRepository, homeLoanRepository, emiPaymentRepository,
            accountRepository, salaryAccountRepository, currentAccountRepository, emiService,
            homeLoanService, null, accountService, null, pdfService);
    }

    @Autowired
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
            GoldLoanService goldLoanService,
            AccountService accountService,
            TransactionService transactionService,
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
        this.goldLoanService = goldLoanService;
        this.accountService = accountService;
        this.transactionService = transactionService;
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
        } else if ("PREPAYMENT".equals(mode) && "GOLD".equals(details.kind)) {
            validatePrepayment(details.outstandingPrincipal, amount);
            double charges = round(amount * 0.01);
            double gst = round(charges * 0.18);
            double totalDebit = round(amount + charges + gst);
            transaction = debitPrepaymentAccount(details.accountNumber, totalDebit,
                        "Gold Loan Prepayment", "Prepayment - " + details.loanAccountNumber
                                + " (principal " + amount + ", charge " + charges + ", GST " + gst + ")");
            if (goldLoanService == null) {
                throw new IllegalStateException("Gold loan repayment service is unavailable.");
            }
            Map<String, Double> balances = goldLoanService.processQuickAction(
                        details.loanAccountNumber, "DEPOSIT", amount, "Customer");
            remaining = balances.get("balanceAfter");
            paidAmount = totalDebit;
        } else if ("PREPAYMENT".equals(mode) && "STANDARD".equals(details.kind)) {
            validatePrepayment(details.outstandingPrincipal, amount);
            double charges = round(amount * 0.01);
            double gst = round(charges * 0.18);
            double totalDebit = round(amount + charges + gst);
            transaction = debitPrepaymentAccount(details.accountNumber, totalDebit,
                        details.loanType + " Prepayment", "Prepayment - " + details.loanAccountNumber
                                + " (principal " + amount + ", charge " + charges + ", GST " + gst + ")");
            remaining = applyStandardPrepayment(details.loan, amount, normalizeAdjustment(adjustment));
            paidAmount = totalDebit;
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
        if (notificationService != null) {
            Map<String, String> notificationDetails = new LinkedHashMap<>();
            notificationDetails.put("Loan account", maskAccountNumber(details.loanAccountNumber));
            notificationDetails.put("Loan type", details.loanType);
            notificationDetails.put("Payment mode", mode);
            notificationDetails.put("Amount paid", String.format(Locale.ROOT, "%.2f", paidAmount));
            notificationDetails.put("Remaining principal", String.format(Locale.ROOT, "%.2f", remaining));
            notificationDetails.put("Linked account balance", String.format(Locale.ROOT, "%.2f", balanceAfter));
            notificationDetails.put("Transaction ID", String.valueOf(transaction.getTransactionId()));
            notificationService.notifyForAccount(details.accountNumber, "Loan " + mode.toLowerCase(Locale.ROOT),
                    "Completed", String.valueOf(transaction.getTransactionId()), notificationDetails);
        }
        return response;
    }

    private LoanDetails verify(String loanAccountNumber, String dob) {
        if (loanAccountNumber == null || loanAccountNumber.isBlank() || dob == null || dob.isBlank()) {
            throw new IllegalArgumentException("Loan number and date of birth are required.");
        }
        String number = loanAccountNumber.trim();
        Loan loan = loanRepository.findByLoanAccountNumber(number).orElseGet(() ->
                loanRepository.findAll().stream()
                        .filter(item -> matchesAccountReference(item.getLoanAccountNumber(), number))
                        .findFirst()
                        .orElse(null));
        if (loan != null) {
            if (!"Approved".equalsIgnoreCase(loan.getStatus())) {
                throw new IllegalArgumentException("Only approved loans can be paid.");
            }
            return verifiedDetails("STANDARD", number, loan.getAccountNumber(), loan.getUserName(),
                    loan.getType(), loan.getStatus(), loan, null, null, dob);
        }

        GoldLoan goldLoan = goldLoanRepository.findByLoanAccountNumber(number).orElseGet(() ->
                goldLoanRepository.findAll().stream()
                        .filter(item -> matchesAccountReference(item.getLoanAccountNumber(), number))
                        .findFirst()
                        .orElse(null));
        if (goldLoan != null) {
            if (!"Approved".equalsIgnoreCase(goldLoan.getStatus())
                    || !Boolean.TRUE.equals(goldLoan.getTermsAccepted())) {
                throw new IllegalArgumentException("Only approved gold loans with accepted terms can be paid.");
            }
            return verifiedDetails("GOLD", number, goldLoan.getAccountNumber(), goldLoan.getUserName(),
                    "Gold Loan", goldLoan.getStatus(), null, goldLoan, null, dob);
        }

        HomeLoan homeLoan = homeLoanRepository.findByLoanAccountNumber(number).orElseGet(() ->
                homeLoanRepository.findAll().stream()
                        .filter(item -> matchesAccountReference(item.getLoanAccountNumber(), number))
                        .findFirst()
                        .orElse(null));
        if (homeLoan == null) {
            homeLoan = homeLoanRepository.findByApplicationId(number).orElseGet(() ->
                    homeLoanRepository.findAll().stream()
                            .filter(item -> matchesAccountReference(item.getApplicationId(), number))
                            .findFirst()
                            .orElse(null));
        }
        if (homeLoan != null) {
            if (!"Approved".equalsIgnoreCase(homeLoan.getStatus())) {
                throw new IllegalArgumentException("Only approved home loans can be paid.");
            }
            String homeLoanLabel = switch (homeLoan.getLoanType() == null ? "" : homeLoan.getLoanType().toUpperCase(Locale.ROOT)) {
                case "CAR" -> "Car Loan";
                case "FARMER" -> "Farmer Loan";
                case "CROP" -> "Crop Loan";
                default -> "Home Loan";
            };
            String canonicalLoanNumber = homeLoan.getLoanAccountNumber();
            String responseLoanNumber = canonicalLoanNumber == null || canonicalLoanNumber.isBlank()
                    ? homeLoan.getApplicationId()
                    : canonicalLoanNumber;
            return verifiedDetails("HOME", responseLoanNumber, homeLoan.getAccountNumber(), homeLoan.getUserName(),
                    homeLoanLabel, homeLoan.getStatus(), null, null, homeLoan, dob);
        }
        throw new IllegalArgumentException("Loan account number was not found. Check the complete number shown in the loan account.");
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
        Account account = findSavingsAccount(accountNumber);
        String storedDob = account == null ? null : account.getDob();
        String customerName = account == null ? loanCustomerName : account.getName();
        if (storedDob == null) {
            SalaryAccount salary = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (salary == null) {
                salary = salaryAccountRepository.findAll().stream()
                        .filter(item -> matchesAccountReference(item.getAccountNumber(), accountNumber))
                        .findFirst()
                        .orElse(null);
            }
            if (salary != null) {
                storedDob = salary.getDob();
                customerName = salary.getEmployeeName();
            }
        }
        if (storedDob == null) {
            Optional<CurrentAccount> current = currentAccountRepository.findByAccountNumber(accountNumber);
            if (current.isEmpty()) {
                current = currentAccountRepository.findAll().stream()
                        .filter(item -> matchesAccountReference(item.getAccountNumber(), accountNumber))
                        .findFirst();
            }
            if (current.isPresent()) {
                storedDob = current.get().getOwnerDob();
                customerName = current.get().getOwnerName();
            }
        }
        if (!sameDate(suppliedDob, storedDob)) {
            throw new IllegalArgumentException(
                    "Date of birth does not match the linked account. Use the DOB registered for account "
                            + maskAccountNumber(accountNumber) + ".");
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
        } else if (goldLoan != null) {
            List<EmiPayment> pending = emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(loanAccountNumber)
                    .stream()
                    .filter(emi -> "Pending".equalsIgnoreCase(emi.getStatus()))
                    .sorted(Comparator.comparing(EmiPayment::getEmiNumber))
                    .limit(1)
                    .toList();
            for (EmiPayment emi : pending) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("emiNumber", emi.getEmiNumber());
                entry.put("dueDate", emi.getDueDate());
                entry.put("amount", emi.getTotalAmount());
                emis.add(entry);
            }
            outstanding = value(goldLoan.getRemainingPrincipal() == null
                    ? goldLoan.getLoanAmount() : goldLoan.getRemainingPrincipal());
            if (outstanding <= 0 && !pending.isEmpty()) {
                outstanding = pending.stream().mapToDouble(emi -> value(emi.getPrincipalAmount())).sum();
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
            if (loan != null) {
                details.prepaymentSupported = true;
                details.prepaymentChargePercent = 1.0;
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

    private void validatePrepayment(double outstanding, Double amount) {
        if (amount == null || !Double.isFinite(amount) || amount <= 0
                || amount >= outstanding) {
            throw new IllegalArgumentException(
                    "Enter a valid prepayment amount below the outstanding principal.");
        }
    }

    private Transaction debitPrepaymentAccount(String accountNumber, double totalDebit,
            String merchant, String description) {
        Double balance = accountService.getBalanceByAccountNumber(accountNumber);
        boolean salaryAccount = false;
        boolean currentAccount = false;
        SalaryAccount salary = null;
        CurrentAccount current = null;

        if (balance == null) {
            salary = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (salary != null && salary.getBalance() != null) {
                balance = salary.getBalance();
                salaryAccount = true;
            }
        }
        if (balance == null) {
            current = currentAccountRepository.findByAccountNumber(accountNumber).orElse(null);
            if (current != null && current.getBalance() != null) {
                balance = current.getBalance();
                currentAccount = true;
            }
        }
        if (balance == null || balance < totalDebit) {
            throw new IllegalArgumentException("Insufficient balance. Required: Rs. " + round(totalDebit)
                    + ", Available: Rs. " + round(balance == null ? 0.0 : balance));
        }

        double newBalance;
        if (salaryAccount && salary != null) {
            newBalance = round(salary.getBalance() - totalDebit);
            salary.setBalance(newBalance);
            salary.setUpdatedAt(LocalDateTime.now());
            salaryAccountRepository.save(salary);
        } else if (currentAccount && current != null) {
            newBalance = round(current.getBalance() - totalDebit);
            current.setBalance(newBalance);
            currentAccountRepository.save(current);
        } else {
            Double debited = accountService.debitBalance(accountNumber, totalDebit);
            if (debited == null) throw new IllegalStateException("Failed to debit the linked account.");
            newBalance = debited;
        }

        Transaction transaction = new Transaction();
        transaction.setAccountNumber(accountNumber);
        transaction.setMerchant(merchant);
        transaction.setAmount(totalDebit);
        transaction.setType("Debit");
        transaction.setDescription(description);
        transaction.setDate(LocalDateTime.now());
        transaction.setStatus("Completed");
        transaction.setBalance(newBalance);
        if (transactionService == null) {
            transaction.setTransactionId("LOAN-" + System.currentTimeMillis());
            return transaction;
        }
        return transactionService.saveTransaction(transaction);
    }

    private double applyStandardPrepayment(Loan loan, double amount, String adjustment) {
        double oldOutstanding = value(loan.getRemainingPrincipal() == null ? loan.getAmount() : loan.getRemainingPrincipal());
        double newOutstanding = round(Math.max(0.0, oldOutstanding - amount));
        loan.setRemainingPrincipal(newOutstanding);
        loan.setPrincipalPaid(round(value(loan.getPrincipalPaid()) + amount));

        List<EmiPayment> pending = emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(loan.getLoanAccountNumber())
                .stream()
                .filter(emi -> "Pending".equalsIgnoreCase(emi.getStatus()))
                .sorted(Comparator.comparing(EmiPayment::getEmiNumber))
                .toList();
        if (!pending.isEmpty()) {
            double monthlyRate = value(loan.getInterestRate()) / 1200.0;
            List<EmiPayment> remainingSchedule = new ArrayList<>(pending);
            if ("REDUCE_TENURE".equalsIgnoreCase(adjustment)) {
                double currentEmi = value(pending.get(0).getTotalAmount());
                int reducedTenure = newRemainingTenure(newOutstanding, monthlyRate, currentEmi, pending.size());
                if (reducedTenure < remainingSchedule.size()) {
                    for (int i = reducedTenure; i < remainingSchedule.size(); i++) {
                        remainingSchedule.get(i).setStatus("Cancelled");
                    }
                    remainingSchedule = new ArrayList<>(remainingSchedule.subList(0, reducedTenure));
                }
            } else {
                double recalculatedEmi = amortizedEmi(newOutstanding, monthlyRate, remainingSchedule.size());
                for (EmiPayment emi : remainingSchedule) {
                    emi.setTotalAmount(round(recalculatedEmi));
                }
            }

            double balance = newOutstanding;
            double remainingInterest = 0.0;
            for (int i = 0; i < remainingSchedule.size(); i++) {
                EmiPayment emi = remainingSchedule.get(i);
                double interest = round(balance * monthlyRate);
                double emiAmount = value(emi.getTotalAmount());
                double principalPart = i == remainingSchedule.size() - 1
                        ? balance : round(Math.max(0.0, Math.min(balance, emiAmount - interest)));
                if (i == remainingSchedule.size() - 1) emiAmount = round(principalPart + interest);
                emi.setPrincipalAmount(round(principalPart));
                emi.setInterestAmount(interest);
                emi.setTotalAmount(round(emiAmount));
                balance = round(Math.max(0.0, balance - principalPart));
                emi.setRemainingPrincipal(balance);
                remainingInterest = round(remainingInterest + interest);
            }
            emiPaymentRepository.saveAll(pending);
        }
        loanRepository.save(loan);
        return newOutstanding;
    }

    private static int newRemainingTenure(double principal, double monthlyRate, double currentEmi, int currentCount) {
        if (principal <= 0) return 0;
        if (monthlyRate <= 0) return Math.max(1, (int) Math.ceil(principal / Math.max(1.0, currentEmi)));
        if (currentEmi <= principal * monthlyRate) return currentCount;
        double months = Math.log(currentEmi / (currentEmi - principal * monthlyRate))
                / Math.log(1 + monthlyRate);
        return Math.max(1, (int) Math.ceil(months));
    }

    private static double amortizedEmi(double principal, double monthlyRate, int months) {
        if (principal <= 0 || months <= 0) return 0.0;
        if (monthlyRate <= 0) return round(principal / months);
        return round(principal * monthlyRate / (1 - Math.pow(1 + monthlyRate, -months)));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Account findSavingsAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account != null) {
            return account;
        }
        return accountRepository.findAll().stream()
                .filter(item -> matchesAccountReference(item.getAccountNumber(), accountNumber))
                .findFirst()
                .orElse(null);
    }

    private static boolean matchesAccountReference(String stored, String supplied) {
        if (stored == null || supplied == null) {
            return false;
        }
        String storedKey = stored.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        String suppliedKey = supplied.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return !storedKey.isEmpty() && storedKey.equals(suppliedKey);
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
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
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
