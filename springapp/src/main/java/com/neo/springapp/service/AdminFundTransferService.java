package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.AdminFundTransfer;
import com.neo.springapp.model.BusinessChequeRequest;
import com.neo.springapp.model.Cheque;
import com.neo.springapp.model.ChequeRequest;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.GoldLoanHistory;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.SavingsChequeRequest;
import com.neo.springapp.model.SavingsChequeAuditLog;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AdminFundTransferRepository;
import com.neo.springapp.repository.BusinessChequeRequestRepository;
import com.neo.springapp.repository.ChequeRepository;
import com.neo.springapp.repository.ChequeRequestRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.EmiPaymentRepository;
import com.neo.springapp.repository.GoldLoanHistoryRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.LoanRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SavingsChequeRequestRepository;
import com.neo.springapp.repository.SavingsChequeAuditLogRepository;
import com.neo.springapp.repository.PositivePayRequestRepository;
import com.neo.springapp.model.PositivePayStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminFundTransferService {

    public static final double CHARGE_RATE = 0.005; // 0.5%

    @Autowired
    private AdminFundTransferRepository repository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private GoldLoanRepository goldLoanRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private EmiPaymentRepository emiPaymentRepository;

    @Autowired
    private GoldLoanHistoryRepository goldLoanHistoryRepository;

    @Autowired
    private GoldLoanService goldLoanService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private EmiService emiService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ChequeRepository chequeRepository;

    @Autowired
    private ChequeRequestRepository chequeRequestRepository;

    @Autowired
    private BusinessChequeRequestRepository businessChequeRequestRepository;

    @Autowired
    private SavingsChequeRequestRepository savingsChequeRequestRepository;

    @Autowired
    private SavingsChequeAuditLogRepository savingsChequeAuditLogRepository;

    @Autowired
    private PositivePayRequestRepository positivePayRequestRepository;

    @Value("${positivepay.minimum.amount:10000}")
    private BigDecimal positivePayMinimumAmount;

    public List<AdminFundTransfer> getAll(String search) {
        if (search != null && !search.isBlank()) {
            return repository.search(search.trim());
        }
        return repository.findAllByOrderByPerformedAtDesc();
    }

    private boolean isGoldLoanPrepaymentCheque(SavingsChequeRequest cheque) {
        return "GOLD_LOAN_PREPAYMENT".equalsIgnoreCase(cheque.getChequePurpose());
    }

    private void requirePositivePayApproval(String accountNumber, SavingsChequeRequest cheque) {
        if (cheque.getAmount() == null || cheque.getAmount().compareTo(positivePayMinimumAmount) < 0) return;
        boolean approved = positivePayRequestRepository
                .findFirstByAccountNumberAndChequeNumberAndStatusIn(accountNumber, cheque.getChequeNumber(),
                        List.of(PositivePayStatus.PENDING_ADMIN_APPROVAL, PositivePayStatus.APPROVED,
                                PositivePayStatus.MATCHED, PositivePayStatus.MISMATCH))
                .map(request -> request.getStatus() == PositivePayStatus.APPROVED)
                .orElse(false);
        if (!approved) {
            throw new IllegalArgumentException("Positive Pay must be approved before this cheque can fund a Gold Loan prepayment");
        }
    }

    // Verify sender's cheque number belongs to the given account and names match; also returns the balance
    // so the admin UI can then load the signed signature document for visual verification.
    public Map<String, Object> verifySenderCheque(String accountNumber, String chequeNumber) {
        Map<String, Object> result = new HashMap<>();

        ResolvedCheque resolved = resolveCheque(chequeNumber, accountNumber);
        if (resolved == null) {
            result.put("valid", false);
            result.put("message", "Approved cheque number not found for this account");
            return result;
        }

        Map<String, Object> accInfo = accountService.verifyAccountByNumber(accountNumber.trim());
        if (!Boolean.TRUE.equals(accInfo.get("found"))) {
            result.put("valid", false);
            result.put("message", "Sender account not found in bank records");
            return result;
        }

        String holderName = String.valueOf(accInfo.get("name"));
        boolean nameMatches = resolved.accountHolderName == null
            || holderName.equalsIgnoreCase(resolved.accountHolderName);

        result.put("valid", true);
        result.put("nameMatches", nameMatches);
        result.put("chequeNumber", resolved.chequeNumber);
        result.put("chequeAccountHolderName", resolved.accountHolderName != null
            ? resolved.accountHolderName : holderName);
        result.put("chequeStatus", resolved.status);
        result.put("chequeSource", resolved.source);
        if (resolved.value instanceof SavingsChequeRequest savingsCheque) {
            result.put("chequePurpose", savingsCheque.getChequePurpose());
            result.put("goldLoanAccountNumber", savingsCheque.getGoldLoanAccountNumber());
            result.put("chequeAmount", savingsCheque.getAmount());
        }
        result.put("accountHolderName", holderName);
        result.put("accountNumber", accountNumber);
        result.put("accountType", accInfo.get("accountType"));
        result.put("balance", getBalance(accountNumber, (String) accInfo.get("accountType")));
        result.put("message", nameMatches
                ? "Cheque verified — name and account number match."
                : "Cheque found, but the name on the cheque does not match the account holder name.");
        return result;
    }

    // Fetch receiver's name/details for any account type (savings/current/salary)
    public Map<String, Object> verifyReceiver(String accountNumber) {
        Map<String, Object> accInfo = accountService.verifyAccountByNumber(accountNumber.trim());
        if (!Boolean.TRUE.equals(accInfo.get("found"))) {
            accInfo.put("valid", false);
            return accInfo;
        }
        accInfo.put("valid", true);
        accInfo.put("balance", getBalance(accountNumber, (String) accInfo.get("accountType")));
        return accInfo;
    }

    @Transactional
    public Map<String, Object> processTransfer(String senderAccountNumber, String senderChequeNumber,
            String receiverAccountNumber, Double amount, String description, String performedBy) {
        return processTransfer(senderAccountNumber, senderChequeNumber, receiverAccountNumber, amount,
                description, performedBy, "ACCOUNT_TO_ACCOUNT", null, null, null, null);
    }

    public Map<String, Object> verifyLoan(String loanType, String loanAccountNumber) {
        String type = normalizeLoanType(loanType);
        String number = loanAccountNumber == null ? "" : loanAccountNumber.trim();
        if (number.isBlank()) {
            throw new IllegalArgumentException("Loan account number is required");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("loanType", type);
        result.put("loanAccountNumber", number);
        if ("GOLD_LOAN".equals(type)) {
            GoldLoan loan = goldLoanRepository.findByLoanAccountNumber(number).orElse(null);
            if (loan == null) {
                result.put("found", false);
                result.put("message", "Gold loan account not found");
                return result;
            }
            result.put("found", true);
            result.put("borrowerAccountNumber", loan.getAccountNumber());
            result.put("borrowerName", loan.getUserName());
            result.put("status", loan.getStatus());
            result.put("loan", loan);
        } else {
            Loan loan = loanRepository.findByLoanAccountNumber(number).orElse(null);
            if (loan == null) {
                result.put("found", false);
                result.put("message", "Personal loan account not found");
                return result;
            }
            result.put("found", true);
            result.put("borrowerAccountNumber", loan.getAccountNumber());
            result.put("borrowerName", loan.getUserName());
            result.put("status", loan.getStatus());
            result.put("loan", loan);
        }

        List<EmiPayment> pending = emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(number)
                .stream().filter(emi -> "Pending".equalsIgnoreCase(emi.getStatus())).toList();
        result.put("pendingEmis", pending);
        result.put("nextEmi", pending.isEmpty() ? null : pending.get(0));
        Map<String, Object> foreclosure = "GOLD_LOAN".equals(type)
                ? goldLoanService.calculateForeclosure(number)
                : loanService.calculateForeclosure(number);
        result.put("foreclosure", foreclosure);
        return result;
    }

    @Transactional
    public Map<String, Object> processTransfer(String senderAccountNumber, String senderChequeNumber,
            String receiverAccountNumber, Double amount, String description, String performedBy,
            String transferCategory, String loanPaymentType, String loanAccountNumber,
            Long emiPaymentId, String prepaymentAdjustment) {
        String category = transferCategory == null ? "ACCOUNT_TO_ACCOUNT" : transferCategory.trim().toUpperCase();
        if ("ACCOUNT_TO_ACCOUNT".equals(category)) {
            return processAccountTransfer(senderAccountNumber, senderChequeNumber, receiverAccountNumber,
                    amount, description, performedBy, category);
        }
        if (!"GOLD_LOAN".equals(category) && !"PERSONAL_LOAN".equals(category)) {
            throw new IllegalArgumentException("Unsupported transfer category");
        }
        return processLoanPayment(senderAccountNumber, senderChequeNumber, amount, description, performedBy,
                category, loanPaymentType, loanAccountNumber, emiPaymentId, prepaymentAdjustment);
    }

    private Map<String, Object> processAccountTransfer(String senderAccountNumber, String senderChequeNumber,
            String receiverAccountNumber, Double amount, String description, String performedBy, String category) {
        Map<String, Object> result = new HashMap<>();

        if (amount == null || amount <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }
        if (senderAccountNumber == null || receiverAccountNumber == null) {
            throw new RuntimeException("Sender and receiver account numbers are required");
        }
        if (senderAccountNumber.trim().equalsIgnoreCase(receiverAccountNumber.trim())) {
            throw new RuntimeException("Sender and receiver accounts cannot be the same");
        }

        Map<String, Object> senderInfo = accountService.verifyAccountByNumber(senderAccountNumber.trim());
        if (!Boolean.TRUE.equals(senderInfo.get("found"))) {
            throw new RuntimeException("Sender account not found");
        }
        Map<String, Object> receiverInfo = accountService.verifyAccountByNumber(receiverAccountNumber.trim());
        if (!Boolean.TRUE.equals(receiverInfo.get("found"))) {
            throw new RuntimeException("Receiver account not found");
        }

        String senderType = (String) senderInfo.get("accountType");
        String receiverType = (String) receiverInfo.get("accountType");

        ResolvedCheque resolved = null;
        if (senderChequeNumber != null && !senderChequeNumber.isBlank()) {
            resolved = resolveCheque(senderChequeNumber, senderAccountNumber);
            if (resolved == null) {
                throw new RuntimeException("Approved cheque number not found for sender account");
            }
            if (resolved.value instanceof SavingsChequeRequest savingsCheque
                    && isGoldLoanPrepaymentCheque(savingsCheque)) {
                throw new IllegalArgumentException("This cheque is reserved for prepayment of Gold Loan "
                        + savingsCheque.getGoldLoanAccountNumber());
            }
        }

        double senderBalance = getBalance(senderAccountNumber, senderType);
        double transferCharge = round2(amount * CHARGE_RATE);
        double totalDebit = amount + transferCharge;
        if (senderBalance < totalDebit) {
            throw new RuntimeException("Insufficient sender balance. Need ₹" + totalDebit + " (amount + 0.5% charge)");
        }

        adjustBalance(senderType, senderAccountNumber, -totalDebit);
        adjustBalance(receiverType, receiverAccountNumber, amount);
        double senderBalanceAfter = getBalance(senderAccountNumber.trim(), senderType);
        double receiverBalanceAfter = getBalance(receiverAccountNumber.trim(), receiverType);

        AdminFundTransfer transfer = new AdminFundTransfer();
        transfer.setTransferId("AFT" + System.currentTimeMillis());
        transfer.setSenderAccountNumber(senderAccountNumber.trim());
        transfer.setSenderName((String) senderInfo.get("name"));
        transfer.setSenderAccountType(senderType);
        transfer.setSenderChequeNumber(senderChequeNumber);
        transfer.setReceiverAccountNumber(receiverAccountNumber.trim());
        transfer.setReceiverName((String) receiverInfo.get("name"));
        transfer.setReceiverAccountType(receiverType);
        transfer.setAmount(amount);
        transfer.setTransferCharge(transferCharge);
        transfer.setTransferCategory(category);
        transfer.setDescription(description);
        transfer.setStatus("COMPLETED");
        transfer.setPerformedBy(performedBy != null && !performedBy.isBlank() ? performedBy : "Admin");
        transfer.setPerformedAt(LocalDateTime.now());

        AdminFundTransfer saved = repository.save(transfer);
        if (resolved != null) {
            markChequeUsed(resolved, saved.getTransferId(), senderAccountNumber, performedBy);
        }

        saveLedgerTransaction(senderAccountNumber.trim(), (String) senderInfo.get("name"), amount,
                "Debit", senderBalanceAfter, "Admin transfer to " + receiverAccountNumber.trim()
                        + " | " + saved.getTransferId() + (description == null ? "" : " | " + description));
        saveLedgerTransaction(receiverAccountNumber.trim(), (String) receiverInfo.get("name"), amount,
                "Credit", receiverBalanceAfter, "Admin transfer from " + senderAccountNumber.trim()
                        + " | " + saved.getTransferId() + (description == null ? "" : " | " + description));

        result.put("success", true);
        result.put("message", "Transfer completed. ₹" + transferCharge + " (0.5%) charged as processing fee.");
        result.put("transfer", saved);
        return result;
    }

    private Map<String, Object> processLoanPayment(String senderAccountNumber, String senderChequeNumber,
            Double requestedAmount, String description, String performedBy, String category,
            String loanPaymentType, String loanAccountNumber, Long emiPaymentId, String prepaymentAdjustment) {
        if (senderAccountNumber == null || senderAccountNumber.isBlank() || loanAccountNumber == null || loanAccountNumber.isBlank()) {
            throw new IllegalArgumentException("Sender and loan account numbers are required");
        }
        String normalizedSender = senderAccountNumber.trim();
        String normalizedLoanNumber = loanAccountNumber.trim();
        String type = normalizeLoanType(category);
        String action = loanPaymentType == null ? "" : loanPaymentType.trim().toUpperCase();
        Map<String, Object> loanDetails = verifyLoan(type, normalizedLoanNumber);
        if (!Boolean.TRUE.equals(loanDetails.get("found"))) {
            throw new IllegalArgumentException(String.valueOf(loanDetails.get("message")));
        }
        if (!"Approved".equalsIgnoreCase(String.valueOf(loanDetails.get("status")))) {
            throw new IllegalArgumentException("Only approved loans can receive payments");
        }
        if (!normalizedSender.equalsIgnoreCase(String.valueOf(loanDetails.get("borrowerAccountNumber")))) {
            throw new IllegalArgumentException("The source account must belong to the loan borrower");
        }

        Map<String, Object> senderInfo = accountService.verifyAccountByNumber(normalizedSender);
        if (!Boolean.TRUE.equals(senderInfo.get("found"))) {
            throw new IllegalArgumentException("Sender account not found");
        }
        ResolvedCheque resolved = resolveCheque(senderChequeNumber, normalizedSender);
        if (resolved == null) {
            throw new IllegalArgumentException("Approved cheque number not found for sender account");
        }
        if (resolved.value instanceof SavingsChequeRequest savingsCheque
                && isGoldLoanPrepaymentCheque(savingsCheque)) {
            if (!"GOLD_LOAN".equals(type) || !"PREPAYMENT".equals(action)
                    || !normalizedLoanNumber.equalsIgnoreCase(savingsCheque.getGoldLoanAccountNumber())) {
                throw new IllegalArgumentException("This cheque is reserved only for prepayment of Gold Loan "
                        + savingsCheque.getGoldLoanAccountNumber());
            }
            if (requestedAmount == null
                    || round2(requestedAmount) != round2(savingsCheque.getAmount().doubleValue())) {
                throw new IllegalArgumentException("Payment amount must match the amount written on the Gold Loan prepayment cheque");
            }
            requirePositivePayApproval(normalizedSender, savingsCheque);
        }

        double amount;
        Map<String, Object> paymentResult = null;
        Map<String, Object> paymentDetails = new HashMap<>();
        if ("EMI".equals(action)) {
            if (emiPaymentId == null) {
                throw new IllegalArgumentException("Select a pending EMI to pay");
            }
            EmiPayment emi = emiPaymentRepository.findById(emiPaymentId)
                    .orElseThrow(() -> new IllegalArgumentException("EMI not found"));
            if (!normalizedLoanNumber.equalsIgnoreCase(emi.getLoanAccountNumber())
                    || !"Pending".equalsIgnoreCase(emi.getStatus())) {
                throw new IllegalArgumentException("Selected EMI is not pending for this loan");
            }
            amount = emi.getTotalAmount();
            if (requestedAmount != null && round2(requestedAmount) != round2(amount)) {
                throw new IllegalArgumentException("EMI amount must match the selected installment");
            }
            appendSnapshotBefore(paymentDetails, snapshotLoan(normalizedLoanNumber));
            paymentDetails.put("principalPaid", valueOrZero(emi.getPrincipalAmount()));
            paymentDetails.put("interestPaid", valueOrZero(emi.getInterestAmount()));
            paymentResult = emiService.payEmi(emiPaymentId, normalizedSender);
            if (!Boolean.TRUE.equals(paymentResult.get("success"))) {
                throw new IllegalArgumentException(String.valueOf(paymentResult.get("message")));
            }
            appendSnapshot(paymentDetails, snapshotLoan(normalizedLoanNumber));
            updateLoanPaidStatus(type, normalizedLoanNumber);
        } else if ("FORECLOSURE".equals(action)) {
            Map<String, Object> calculation = loanDetails.get("foreclosure") instanceof Map<?, ?> map
                    ? (Map<String, Object>) map : Map.of();
            if (!Boolean.TRUE.equals(calculation.get("success"))) {
                throw new IllegalArgumentException(String.valueOf(calculation.getOrDefault("message", "Unable to calculate foreclosure")));
            }
            appendSnapshotBefore(paymentDetails, snapshotLoan(normalizedLoanNumber));
            paymentDetails.put("outstandingPrincipalBefore", numberValue(calculation.get("remainingPrincipal")));
            paymentDetails.put("remainingInterestBefore", numberValue(calculation.get("remainingInterest")));
            paymentDetails.put("principalPaid", numberValue(calculation.get("remainingPrincipal")));
            paymentDetails.put("interestPaid", numberValue(calculation.get("remainingInterest")));
            paymentDetails.put("charges", numberValue(calculation.get("foreclosureCharges"))
                    + numberValue(calculation.get("gst")));
            amount = numberValue(calculation.get("totalForeclosureAmount"));
            requireSufficientBalance(normalizedSender, (String) senderInfo.get("accountType"), amount);
            adjustBalance((String) senderInfo.get("accountType"), normalizedSender, -amount);
            Object loan;
            if ("GOLD_LOAN".equals(type)) {
                loan = goldLoanService.processForeclosureWithAmount(normalizedLoanNumber,
                        performedBy == null ? "Admin" : performedBy, amount);
            } else {
                loan = loanService.processForeclosure(normalizedLoanNumber,
                        performedBy == null ? "Admin" : performedBy);
            }
            if (loan == null) {
                throw new IllegalArgumentException("Loan foreclosure could not be completed");
            }
            paymentDetails.put("outstandingPrincipalAfter", 0.0);
            paymentDetails.put("remainingInterestAfter", 0.0);
            paymentDetails.put("emiAmountAfter", 0.0);
            paymentDetails.put("remainingTenureAfter", 0);
            paymentResult = Map.of("success", true, "foreclosure", calculation);
            saveLedgerTransaction(normalizedSender, (String) senderInfo.get("name"), amount, "Debit",
                    getBalance(normalizedSender, (String) senderInfo.get("accountType")),
                    type.replace('_', ' ') + " foreclosure " + normalizedLoanNumber);
        } else if ("PREPAYMENT".equals(action)) {
            if (requestedAmount == null || requestedAmount <= 0) {
                throw new IllegalArgumentException("Prepayment amount must be greater than 0");
            }
            String adjustment = prepaymentAdjustment == null ? "" : prepaymentAdjustment.trim().toUpperCase();
            if (!"REDUCE_EMI".equals(adjustment) && !"REDUCE_TENURE".equals(adjustment)) {
                throw new IllegalArgumentException("Choose whether to reduce future EMI or reduce the loan tenure");
            }
            amount = round2(requestedAmount);
            requireSufficientBalance(normalizedSender, (String) senderInfo.get("accountType"), amount);
            paymentDetails.putAll(applyPrepayment(type, normalizedLoanNumber, amount, adjustment));
            adjustBalance((String) senderInfo.get("accountType"), normalizedSender, -amount);
            saveLedgerTransaction(normalizedSender, (String) senderInfo.get("name"), amount, "Debit",
                    getBalance(normalizedSender, (String) senderInfo.get("accountType")),
                    type.replace('_', ' ') + " prepayment " + normalizedLoanNumber + " (" + adjustment + ")");
            paymentResult = Map.of("success", true, "adjustment", adjustment);
        } else {
            throw new IllegalArgumentException("Choose EMI, prepayment, or foreclosure");
        }

        String receiverName = String.valueOf(loanDetails.get("borrowerName"));
        AdminFundTransfer transfer = new AdminFundTransfer();
        transfer.setTransferId("AFT" + System.currentTimeMillis());
        transfer.setSenderAccountNumber(normalizedSender);
        transfer.setSenderName((String) senderInfo.get("name"));
        transfer.setSenderAccountType((String) senderInfo.get("accountType"));
        transfer.setSenderChequeNumber(senderChequeNumber);
        transfer.setReceiverAccountNumber(normalizedLoanNumber);
        transfer.setReceiverName(receiverName);
        transfer.setReceiverAccountType(type);
        transfer.setAmount(amount);
        transfer.setTransferCharge(0.0);
        transfer.setTransferCategory(type);
        transfer.setLoanAccountNumber(normalizedLoanNumber);
        transfer.setLoanPaymentType(action);
        transfer.setEmiPaymentId(emiPaymentId);
        transfer.setPrepaymentAdjustment(prepaymentAdjustment);
        setTransferPaymentDetails(transfer, paymentDetails);
        transfer.setDescription(description);
        transfer.setStatus("COMPLETED");
        transfer.setPerformedBy(performedBy != null && !performedBy.isBlank() ? performedBy : "Admin");
        transfer.setPerformedAt(LocalDateTime.now());
        AdminFundTransfer saved = repository.save(transfer);
        markChequeUsed(resolved, saved.getTransferId(), normalizedSender, performedBy);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", action + " applied successfully to " + type.replace('_', ' ') + " account.");
        result.put("transfer", saved);
        result.put("payment", paymentResult);
        result.put("newBalance", getBalance(normalizedSender, (String) senderInfo.get("accountType")));
        return result;
    }

    private Map<String, Object> applyPrepayment(String type, String loanAccountNumber, double amount, String adjustment) {
        List<EmiPayment> schedule = emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(loanAccountNumber);
        List<EmiPayment> pending = schedule.stream()
                .filter(emi -> "Pending".equalsIgnoreCase(emi.getStatus()))
                .toList();
        if (pending.isEmpty()) {
            throw new IllegalArgumentException("No pending EMI schedule exists for this loan");
        }

        double outstandingPrincipal = round2(pending.stream()
                .mapToDouble(emi -> emi.getPrincipalAmount() == null ? 0.0 : emi.getPrincipalAmount())
                .sum());
        double outstandingInterest = round2(pending.stream()
                .mapToDouble(emi -> emi.getInterestAmount() == null ? 0.0 : emi.getInterestAmount())
                .sum());
        double emiBefore = pending.isEmpty() ? 0.0 : valueOrZero(pending.get(0).getTotalAmount());
        int tenureBefore = pending.size();
        if (amount >= outstandingPrincipal) {
            throw new IllegalArgumentException("Prepayment must be less than the outstanding principal; use foreclosure to close the loan");
        }

        double principalRemaining = round2(outstandingPrincipal - amount);
        double annualRate = "GOLD_LOAN".equals(type)
                ? valueOrZero(goldLoanRepository.findByLoanAccountNumber(loanAccountNumber).orElseThrow().getInterestRate())
                : valueOrZero(loanRepository.findByLoanAccountNumber(loanAccountNumber).orElseThrow().getInterestRate());
        double monthlyRate = annualRate / 1200.0;
        if ("REDUCE_EMI".equals(adjustment)) {
            recalculateWithReducedEmi(pending, principalRemaining, monthlyRate);
        } else {
            recalculateWithReducedTenure(pending, principalRemaining, monthlyRate);
        }
        emiPaymentRepository.saveAll(schedule);
        List<EmiPayment> remainingEmis = schedule.stream()
                .filter(emi -> "Pending".equalsIgnoreCase(emi.getStatus()))
                .toList();
        double remainingInterest = round2(remainingEmis.stream()
                .mapToDouble(emi -> emi.getInterestAmount() == null ? 0.0 : emi.getInterestAmount())
                .sum());
        double emiAfter = remainingEmis.isEmpty() ? 0.0 : valueOrZero(remainingEmis.get(0).getTotalAmount());
        int tenureAfter = remainingEmis.size();

        if ("GOLD_LOAN".equals(type)) {
            GoldLoan loan = goldLoanRepository.findByLoanAccountNumber(loanAccountNumber).orElseThrow();
            loan.setRemainingPrincipal(principalRemaining);
            loan.setRemainingInterest(remainingInterest);
            loan.setCurrentEmi(emiAfter);
            loan.setRemainingTenure(tenureAfter);
            loan.setPrincipalPaid(round2(valueOrZero(loan.getPrincipalPaid()) + amount));
            goldLoanRepository.save(loan);
            GoldLoanHistory history = new GoldLoanHistory();
            history.setGoldLoanId(loan.getId());
            history.setLoanAccountNumber(loanAccountNumber);
            history.setAction("PREPAYMENT");
            history.setChangedBy("Admin Fund Transfer");
            history.setDetails("Prepayment of " + amount + " applied to principal; outstanding principal "
                    + outstandingPrincipal + " -> " + principalRemaining + "; remaining interest "
                    + outstandingInterest + " -> " + remainingInterest + "; EMI " + emiBefore + " -> " + emiAfter
                    + "; remaining tenure " + tenureBefore + " -> " + tenureAfter + " months; adjustment "
                    + adjustment + "; estimated interest saved " + round2(outstandingInterest - remainingInterest) + ".");
            history.setOldAmount(outstandingPrincipal);
            history.setNewAmount(principalRemaining);
            goldLoanHistoryRepository.save(history);
        } else {
            Loan loan = loanRepository.findByLoanAccountNumber(loanAccountNumber).orElseThrow();
            loan.setRemainingPrincipal(principalRemaining);
            loan.setRemainingInterest(remainingInterest);
            loan.setPrincipalPaid(round2(valueOrZero(loan.getPrincipalPaid()) + amount));
            loanRepository.save(loan);
        }

        Map<String, Object> details = new HashMap<>();
        details.put("outstandingPrincipalBefore", outstandingPrincipal);
        details.put("outstandingPrincipalAfter", principalRemaining);
        details.put("remainingInterestBefore", outstandingInterest);
        details.put("remainingInterestAfter", remainingInterest);
        details.put("emiAmountBefore", emiBefore);
        details.put("emiAmountAfter", emiAfter);
        details.put("remainingTenureBefore", tenureBefore);
        details.put("remainingTenureAfter", tenureAfter);
        details.put("principalPaid", amount);
        details.put("interestPaid", 0.0);
        details.put("charges", 0.0);
        details.put("interestSaved", round2(outstandingInterest - remainingInterest));
        return details;
    }

    private Map<String, Object> snapshotLoan(String loanAccountNumber) {
        List<EmiPayment> activeEmis = emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(loanAccountNumber)
                .stream()
                .filter(emi -> "Pending".equalsIgnoreCase(emi.getStatus())
                        || "Overdue".equalsIgnoreCase(emi.getStatus()))
                .toList();
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("outstandingPrincipal", round2(activeEmis.stream()
                .mapToDouble(emi -> valueOrZero(emi.getPrincipalAmount())).sum()));
        snapshot.put("remainingInterest", round2(activeEmis.stream()
                .mapToDouble(emi -> valueOrZero(emi.getInterestAmount())).sum()));
        snapshot.put("emiAmount", activeEmis.isEmpty() ? 0.0 : valueOrZero(activeEmis.get(0).getTotalAmount()));
        snapshot.put("remainingTenure", activeEmis.size());
        return snapshot;
    }

    private void appendSnapshotBefore(Map<String, Object> details, Map<String, Object> snapshot) {
        details.put("outstandingPrincipalBefore", snapshot.get("outstandingPrincipal"));
        details.put("remainingInterestBefore", snapshot.get("remainingInterest"));
        details.put("emiAmountBefore", snapshot.get("emiAmount"));
        details.put("remainingTenureBefore", snapshot.get("remainingTenure"));
    }

    private void appendSnapshot(Map<String, Object> details, Map<String, Object> snapshot) {
        details.put("outstandingPrincipalAfter", snapshot.get("outstandingPrincipal"));
        details.put("remainingInterestAfter", snapshot.get("remainingInterest"));
        details.put("emiAmountAfter", snapshot.get("emiAmount"));
        details.put("remainingTenureAfter", snapshot.get("remainingTenure"));
    }

    private void setTransferPaymentDetails(AdminFundTransfer transfer, Map<String, Object> details) {
        transfer.setOutstandingPrincipalBefore(numberValue(details.get("outstandingPrincipalBefore")));
        transfer.setOutstandingPrincipalAfter(numberValue(details.get("outstandingPrincipalAfter")));
        transfer.setRemainingInterestBefore(numberValue(details.get("remainingInterestBefore")));
        transfer.setRemainingInterestAfter(numberValue(details.get("remainingInterestAfter")));
        transfer.setEmiAmountBefore(numberValue(details.get("emiAmountBefore")));
        transfer.setEmiAmountAfter(numberValue(details.get("emiAmountAfter")));
        transfer.setRemainingTenureBefore(integerValue(details.get("remainingTenureBefore")));
        transfer.setRemainingTenureAfter(integerValue(details.get("remainingTenureAfter")));
        transfer.setPrincipalPaid(numberValue(details.get("principalPaid")));
        transfer.setInterestPaid(numberValue(details.get("interestPaid")));
        transfer.setCharges(numberValue(details.get("charges")));
        transfer.setInterestSaved(numberValue(details.get("interestSaved")));
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        return value == null ? null : Integer.valueOf(value.toString());
    }

    private void recalculateWithReducedEmi(List<EmiPayment> pending, double principal, double monthlyRate) {
        int remainingMonths = pending.size();
        double payment = monthlyPayment(principal, monthlyRate, remainingMonths);
        double balance = principal;
        for (int i = 0; i < pending.size(); i++) {
            EmiPayment emi = pending.get(i);
            double interest = round2(balance * monthlyRate);
            double principalPart = i == pending.size() - 1 ? balance : round2(payment - interest);
            if (principalPart <= 0) {
                throw new IllegalArgumentException("The recalculated EMI does not cover monthly interest");
            }
            principalPart = Math.min(principalPart, balance);
            balance = round2(Math.max(0.0, balance - principalPart));
            emi.setPrincipalAmount(principalPart);
            emi.setInterestAmount(interest);
            emi.setTotalAmount(round2(principalPart + interest));
            emi.setRemainingPrincipal(balance);
            emi.setUpdatedAt(LocalDateTime.now());
        }
    }

    private void recalculateWithReducedTenure(List<EmiPayment> pending, double principal, double monthlyRate) {
        double installment = valueOrZero(pending.get(0).getTotalAmount());
        double balance = principal;
        int used = 0;
        for (EmiPayment emi : pending) {
            double interest = round2(balance * monthlyRate);
            double principalPart = round2(installment - interest);
            if (principalPart <= 0) {
                throw new IllegalArgumentException("Existing EMI is too small to repay the outstanding principal at this interest rate");
            }
            principalPart = Math.min(principalPart, balance);
            balance = round2(Math.max(0.0, balance - principalPart));
            emi.setPrincipalAmount(principalPart);
            emi.setInterestAmount(interest);
            emi.setTotalAmount(round2(principalPart + interest));
            emi.setRemainingPrincipal(balance);
            emi.setUpdatedAt(LocalDateTime.now());
            used++;
            if (balance <= 0) {
                break;
            }
        }
        if (balance > 0) {
            throw new IllegalArgumentException("Remaining tenure is insufficient for the current EMI amount");
        }
        for (int i = used; i < pending.size(); i++) {
            pending.get(i).setStatus("Cancelled");
            pending.get(i).setUpdatedAt(LocalDateTime.now());
        }
    }

    private double monthlyPayment(double principal, double monthlyRate, int months) {
        if (monthlyRate == 0.0) {
            return round2(principal / months);
        }
        double factor = Math.pow(1.0 + monthlyRate, months);
        return round2(principal * monthlyRate * factor / (factor - 1.0));
    }

    private void updateLoanPaidStatus(String type, String loanAccountNumber) {
        boolean noPendingEmi = emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(loanAccountNumber)
                .stream().noneMatch(emi -> "Pending".equalsIgnoreCase(emi.getStatus()));
        if (!noPendingEmi) return;
        if ("GOLD_LOAN".equals(type)) {
            goldLoanRepository.findByLoanAccountNumber(loanAccountNumber).ifPresent(loan -> {
                loan.setStatus("Paid");
                goldLoanRepository.save(loan);
            });
        } else {
            loanRepository.findByLoanAccountNumber(loanAccountNumber).ifPresent(loan -> {
                loan.setStatus("Paid");
                loanRepository.save(loan);
            });
        }
    }

    private void saveLedgerTransaction(String accountNumber, String userName, double amount, String type,
            double balance, String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        transaction.setAccountNumber(accountNumber);
        transaction.setUserName(userName);
        transaction.setMerchant("Admin Fund Transfer");
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setDate(LocalDateTime.now());
        transaction.setStatus("Completed");
        transaction.setBalance(balance);
        transactionService.saveTransaction(transaction);
    }

    private void requireSufficientBalance(String accountNumber, String accountType, double amount) {
        double available = getBalance(accountNumber, accountType);
        if (available < amount) {
            throw new IllegalArgumentException("Insufficient sender balance. Required: ₹" + amount + ", available: ₹" + available);
        }
    }

    private String normalizeLoanType(String loanType) {
        String type = loanType == null ? "" : loanType.trim().toUpperCase();
        if ("GOLD".equals(type) || "GOLD_LOAN".equals(type)) return "GOLD_LOAN";
        if ("PERSONAL".equals(type) || "PERSONAL_LOAN".equals(type)) return "PERSONAL_LOAN";
        throw new IllegalArgumentException("Loan type must be GOLD_LOAN or PERSONAL_LOAN");
    }

    private double numberValue(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        return value == null ? 0.0 : Double.parseDouble(value.toString());
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private ResolvedCheque resolveCheque(String chequeNumber, String accountNumber) {
        String number = chequeNumber == null ? "" : chequeNumber.trim();
        String account = accountNumber == null ? "" : accountNumber.trim();

        Optional<Cheque> standard = chequeRepository.findByChequeNumber(number);
        if (standard.isPresent()) {
            Cheque cheque = standard.get();
            if ("ACTIVE".equalsIgnoreCase(cheque.getStatus())
                    && account.equalsIgnoreCase(String.valueOf(cheque.getAccountNumber()))) {
                return new ResolvedCheque("CHEQUES", cheque.getChequeNumber(), cheque.getAccountHolderName(), cheque.getStatus(), cheque);
            }
        }

        for (ChequeRequest request : chequeRequestRepository.findAllByChequeNumber(number)) {
            if (!"APPROVED".equalsIgnoreCase(request.getStatus())) {
                continue;
            }
            SalaryAccount salaryAccount = salaryAccountRepository.findById(request.getSalaryAccountId()).orElse(null);
            if (salaryAccount != null && account.equalsIgnoreCase(salaryAccount.getAccountNumber())) {
                return new ResolvedCheque("SALARY_CHEQUE_REQUESTS", request.getChequeNumber(), null, request.getStatus(), request);
            }
        }

        for (BusinessChequeRequest request : businessChequeRequestRepository.findAllByChequeNumber(number)) {
            if (!"APPROVED".equalsIgnoreCase(request.getStatus())) {
                continue;
            }
            CurrentAccount currentAccount = currentAccountRepository.findById(request.getCurrentAccountId()).orElse(null);
            if (currentAccount != null && account.equalsIgnoreCase(currentAccount.getAccountNumber())) {
                return new ResolvedCheque("BUSINESS_CHEQUE_REQUESTS", request.getChequeNumber(), null, request.getStatus(), request);
            }
        }

        for (SavingsChequeRequest request : savingsChequeRequestRepository.findAllByChequeNumber(number)) {
            if (!isGoldLoanPrepaymentCheque(request)
                    || !("PENDING".equalsIgnoreCase(request.getStatus())
                    || "AWAITING_POSITIVE_PAY".equalsIgnoreCase(request.getStatus()))) {
                continue;
            }
            Account savingsAccount = accountRepository.findById(request.getAccountId()).orElse(null);
            if (savingsAccount != null && account.equalsIgnoreCase(savingsAccount.getAccountNumber())) {
                return new ResolvedCheque("SAVINGS_CHEQUE_REQUESTS", request.getChequeNumber(),
                        savingsAccount.getName(), request.getStatus(), request);
            }
        }
        return null;
    }

    private void markChequeUsed(ResolvedCheque resolved, String transferId, String senderAccountNumber, String performedBy) {
        if (resolved.value instanceof Cheque cheque) {
            cheque.markUsed("ADMIN_FUND_TRANSFER", transferId);
            chequeRepository.save(cheque);
        } else if (resolved.value instanceof ChequeRequest request) {
            request.setStatus("COMPLETED");
            request.setTransactionReference(transferId);
            request.setDebitedFromAccount(senderAccountNumber);
            request.setApprovedBy(performedBy != null && !performedBy.isBlank() ? performedBy : request.getApprovedBy());
            request.setUpdatedAt(LocalDateTime.now());
            chequeRequestRepository.save(request);
        } else if (resolved.value instanceof SavingsChequeRequest request) {
            request.setStatus("COMPLETED");
            request.setTransactionReference(transferId);
            request.setDebitedFromAccount(senderAccountNumber);
            request.setCreditedToAccount(request.getGoldLoanAccountNumber());
            request.setApprovedBy(performedBy != null && !performedBy.isBlank() ? performedBy : request.getApprovedBy());
            request.setUpdatedAt(LocalDateTime.now());
            SavingsChequeRequest saved = savingsChequeRequestRepository.save(request);
            SavingsChequeAuditLog audit = new SavingsChequeAuditLog();
            audit.setChequeRequestId(saved.getId());
            audit.setAdminEmail(performedBy != null && !performedBy.isBlank() ? performedBy : "Admin");
            audit.setAction("GOLD_LOAN_PREPAYMENT");
            audit.setRemarks("Reserved cheque used for prepayment of Gold Loan "
                    + saved.getGoldLoanAccountNumber() + " | Transfer: " + transferId);
            audit.setTimestamp(LocalDateTime.now());
            savingsChequeAuditLogRepository.save(audit);
        } else if (resolved.value instanceof BusinessChequeRequest request) {
            request.setStatus("COMPLETED");
            request.setTransactionReference(transferId);
            request.setUpdatedAt(LocalDateTime.now());
            businessChequeRequestRepository.save(request);
        }
    }

    private static class ResolvedCheque {
        private final String source;
        private final String chequeNumber;
        private final String accountHolderName;
        private final String status;
        private final Object value;

        private ResolvedCheque(String source, String chequeNumber, String accountHolderName, String status, Object value) {
            this.source = source;
            this.chequeNumber = chequeNumber;
            this.accountHolderName = accountHolderName;
            this.status = status;
            this.value = value;
        }
    }

    @Transactional
    public Map<String, Object> editTransfer(Long id, String description, String editedBy) {
        AdminFundTransfer transfer = repository.findById(id).orElseThrow(() -> new RuntimeException("Transfer not found"));
        if ("REVERTED".equals(transfer.getStatus())) {
            throw new RuntimeException("Cannot edit a reverted transfer");
        }
        if (description != null) {
            transfer.setDescription(description);
        }
        transfer.setEditedBy(editedBy != null && !editedBy.isBlank() ? editedBy : "Admin");
        transfer.setEditedAt(LocalDateTime.now());
        AdminFundTransfer saved = repository.save(transfer);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Transfer details updated");
        result.put("transfer", saved);
        return result;
    }

    @Transactional
    public Map<String, Object> revertTransfer(Long id, String revertedBy, String reason) {
        AdminFundTransfer transfer = repository.findById(id).orElseThrow(() -> new RuntimeException("Transfer not found"));
        if ("REVERTED".equals(transfer.getStatus())) {
            throw new RuntimeException("This transfer has already been reverted");
        }
        if (!"ACCOUNT_TO_ACCOUNT".equalsIgnoreCase(transfer.getTransferCategory())) {
            throw new RuntimeException("Loan repayments cannot be reverted as account-to-account transfers. Use the loan servicing workflow to correct a loan payment.");
        }
        if (transfer.getPerformedAt() != null && LocalDateTime.now().isAfter(transfer.getPerformedAt().plusHours(24))) {
            throw new RuntimeException("Transfer can only be reverted within 24 hours");
        }

        double revertCharge = round2(transfer.getAmount() * CHARGE_RATE);
        double refundToSender = transfer.getAmount() + transfer.getTransferCharge() - revertCharge;
        double debitFromReceiver = transfer.getAmount();

        double receiverBalance = getBalance(transfer.getReceiverAccountNumber(), transfer.getReceiverAccountType());
        if (receiverBalance < debitFromReceiver) {
            throw new RuntimeException("Receiver account has insufficient balance to revert this transfer");
        }

        adjustBalance(transfer.getReceiverAccountType(), transfer.getReceiverAccountNumber(), -debitFromReceiver);
        adjustBalance(transfer.getSenderAccountType(), transfer.getSenderAccountNumber(), refundToSender);

        transfer.setStatus("REVERTED");
        transfer.setRevertCharge(revertCharge);
        transfer.setRevertedBy(revertedBy != null && !revertedBy.isBlank() ? revertedBy : "Admin");
        transfer.setRevertedAt(LocalDateTime.now());
        transfer.setRevertReason(reason);
        AdminFundTransfer saved = repository.save(transfer);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Transfer reverted in real-time. ₹" + revertCharge + " (0.5%) charged as revert processing fee.");
        result.put("transfer", saved);
        return result;
    }

    private double getBalance(String accountNumber, String accountType) {
        if (accountType == null) return 0.0;
        if (accountType.startsWith("Savings")) {
            Account a = accountRepository.findByAccountNumber(accountNumber);
            return a != null && a.getBalance() != null ? a.getBalance() : 0.0;
        } else if (accountType.startsWith("Current")) {
            return currentAccountRepository.findByAccountNumber(accountNumber)
                    .map(CurrentAccount::getBalance).orElse(0.0);
        } else if (accountType.startsWith("Salary")) {
            SalaryAccount s = salaryAccountRepository.findByAccountNumber(accountNumber);
            return s != null && s.getBalance() != null ? s.getBalance() : 0.0;
        }
        return 0.0;
    }

    private void adjustBalance(String accountType, String accountNumber, double delta) {
        if (accountType == null) {
            throw new RuntimeException("Unknown account type for " + accountNumber);
        }
        if (accountType.startsWith("Savings")) {
            Account a = accountRepository.findByAccountNumber(accountNumber);
            if (a == null) throw new RuntimeException("Savings account not found: " + accountNumber);
            a.setBalance((a.getBalance() != null ? a.getBalance() : 0.0) + delta);
            a.setLastUpdated(LocalDateTime.now());
            accountRepository.save(a);
        } else if (accountType.startsWith("Current")) {
            CurrentAccount ca = currentAccountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new RuntimeException("Current account not found: " + accountNumber));
            ca.setBalance((ca.getBalance() != null ? ca.getBalance() : 0.0) + delta);
            currentAccountRepository.save(ca);
        } else if (accountType.startsWith("Salary")) {
            SalaryAccount sa = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (sa == null) throw new RuntimeException("Salary account not found: " + accountNumber);
            sa.setBalance((sa.getBalance() != null ? sa.getBalance() : 0.0) + delta);
            sa.setUpdatedAt(LocalDateTime.now());
            salaryAccountRepository.save(sa);
        } else {
            throw new RuntimeException("Unsupported account type: " + accountType);
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
