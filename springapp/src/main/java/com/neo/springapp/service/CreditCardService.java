package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@SuppressWarnings("null")
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final CreditCardTransactionRepository transactionRepository;
    private final CreditCardBillRepository billRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final GlobalTransactionIdGenerator transactionIdGenerator;
    private final ChequeRepository chequeRepository;
    private final BranchAccountService branchAccountService;
    private final CreditCardSettingsRepository settingsRepository;
    private final ChequeRequestRepository chequeRequestRepository;
    private final BusinessChequeRequestRepository businessChequeRequestRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final CreditCardEmiPlanRepository emiPlanRepository;
    private final CreditCardEmiInstallmentRepository emiInstallmentRepository;

    public CreditCardService(
            CreditCardRepository creditCardRepository,
            CreditCardTransactionRepository transactionRepository,
            CreditCardBillRepository billRepository,
            AccountService accountService,
            TransactionService transactionService,
            GlobalTransactionIdGenerator transactionIdGenerator,
            ChequeRepository chequeRepository,
            BranchAccountService branchAccountService,
            CreditCardSettingsRepository settingsRepository,
            ChequeRequestRepository chequeRequestRepository,
            BusinessChequeRequestRepository businessChequeRequestRepository,
            SalaryAccountRepository salaryAccountRepository,
            CurrentAccountRepository currentAccountRepository,
            CreditCardEmiPlanRepository emiPlanRepository,
            CreditCardEmiInstallmentRepository emiInstallmentRepository) {
        this.creditCardRepository = creditCardRepository;
        this.transactionRepository = transactionRepository;
        this.billRepository = billRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.transactionIdGenerator = transactionIdGenerator;
        this.chequeRepository = chequeRepository;
        this.branchAccountService = branchAccountService;
        this.settingsRepository = settingsRepository;
        this.chequeRequestRepository = chequeRequestRepository;
        this.businessChequeRequestRepository = businessChequeRequestRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.emiPlanRepository = emiPlanRepository;
        this.emiInstallmentRepository = emiInstallmentRepository;
    }

    /** Normalized cheque lookup result spanning savings (Cheque), salary (ChequeRequest), and current (BusinessChequeRequest) cheques. */
    private static class ResolvedCheque {
        String source; // SAVINGS, SALARY, CURRENT
        boolean usable; // status allows it to be used for payment
        boolean adminDrawn; // true if already drawn by admin
        String drawnBy;
        String status;
        String accountNumber;
        String accountHolderName;
        Double amount;
        Cheque savingsCheque;
        ChequeRequest salaryChequeRequest;
        BusinessChequeRequest businessChequeRequest;
    }

    private ResolvedCheque resolveChequeForPayment(String chequeNumber) {
        Optional<Cheque> savingsOpt = chequeRepository.findByChequeNumber(chequeNumber);
        if (savingsOpt.isPresent()) {
            Cheque c = savingsOpt.get();
            ResolvedCheque r = new ResolvedCheque();
            r.source = "SAVINGS";
            r.status = c.getStatus();
            r.accountNumber = c.getAccountNumber();
            r.accountHolderName = c.getAccountHolderName();
            r.amount = c.getAmount();
            r.savingsCheque = c;
            r.drawnBy = c.getDrawnBy();

            // Check usability:
            // 1. ACTIVE: not yet drawn, admin can draw and pay it ("if not by admin admin then pay it")
            // 2. DRAWN: already drawn by admin. If not already consumed for another bill/DD, admin can pay it!
            if ("ACTIVE".equalsIgnoreCase(c.getStatus())) {
                r.usable = true;
                r.adminDrawn = false;
            } else if ("DRAWN".equalsIgnoreCase(c.getStatus())) {
                // If it was drawn by admin and not yet consumed for another specific purpose
                if (c.getUsedFor() == null || c.getUsedFor().trim().isEmpty()) {
                    r.usable = true;
                    r.adminDrawn = true;
                } else {
                    r.usable = false; // Already consumed
                    r.adminDrawn = true;
                }
            } else {
                r.usable = false;
                r.adminDrawn = false;
            }
            return r;
        }
        Optional<ChequeRequest> salaryOpt = chequeRequestRepository.findByChequeNumber(chequeNumber);
        if (salaryOpt.isPresent()) {
            ChequeRequest req = salaryOpt.get();
            SalaryAccount sal = salaryAccountRepository.findById(req.getSalaryAccountId()).orElse(null);
            ResolvedCheque r = new ResolvedCheque();
            r.source = "SALARY";
            r.status = req.getStatus();
            r.usable = "APPROVED".equalsIgnoreCase(req.getStatus())
                    || "COMPLETED".equalsIgnoreCase(req.getStatus())
                    || "DRAWN".equalsIgnoreCase(req.getStatus())
                    || "PENDING".equalsIgnoreCase(req.getStatus());
            r.adminDrawn = "DRAWN".equalsIgnoreCase(req.getStatus()) || "COMPLETED".equalsIgnoreCase(req.getStatus());
            r.accountNumber = sal != null ? sal.getAccountNumber() : null;
            r.accountHolderName = req.getPayeeName();
            r.amount = req.getAmount() != null ? req.getAmount().doubleValue() : null;
            r.salaryChequeRequest = req;
            return r;
        }
        Optional<BusinessChequeRequest> businessOpt = businessChequeRequestRepository.findByChequeNumber(chequeNumber);
        if (businessOpt.isPresent()) {
            BusinessChequeRequest req = businessOpt.get();
            CurrentAccount cur = currentAccountRepository.findById(req.getCurrentAccountId()).orElse(null);
            ResolvedCheque r = new ResolvedCheque();
            r.source = "CURRENT";
            r.status = req.getStatus();
            r.usable = "APPROVED".equalsIgnoreCase(req.getStatus())
                    || "COMPLETED".equalsIgnoreCase(req.getStatus())
                    || "DRAWN".equalsIgnoreCase(req.getStatus())
                    || "PENDING".equalsIgnoreCase(req.getStatus());
            r.adminDrawn = "DRAWN".equalsIgnoreCase(req.getStatus()) || "COMPLETED".equalsIgnoreCase(req.getStatus());
            r.accountNumber = cur != null ? cur.getAccountNumber() : null;
            r.accountHolderName = req.getPayeeName();
            r.amount = req.getAmount() != null ? req.getAmount().doubleValue() : null;
            r.businessChequeRequest = req;
            return r;
        }
        return null;
    }

    private void markChequeUsed(ResolvedCheque resolved, String usedReference, String adminName, String chequeHolderName) {
        switch (resolved.source) {
            case "SAVINGS":
                Cheque chq = resolved.savingsCheque;
                chq.setStatus("DRAWN");
                if (chq.getDrawnBy() == null || chq.getDrawnBy().isBlank()) {
                    chq.setDrawnBy(adminName != null ? adminName : "Admin");
                }
                if (chq.getDrawnDate() == null) {
                    chq.setDrawnDate(LocalDateTime.now());
                }
                chq.setUsedFor("CREDIT_CARD_BILL_PAYMENT");
                chq.setUsedReference(usedReference);
                chq.setUsedDate(LocalDateTime.now());
                if (chequeHolderName != null && !chequeHolderName.isBlank()) {
                    chq.setAccountHolderName(chequeHolderName.trim());
                }
                chequeRepository.save(chq);
                break;
            case "SALARY":
                ChequeRequest sReq = resolved.salaryChequeRequest;
                sReq.setStatus("CLEARED");
                sReq.setClearedAt(LocalDateTime.now());
                if (chequeHolderName != null && !chequeHolderName.isBlank()) {
                    sReq.setPayeeName(chequeHolderName.trim());
                }
                chequeRequestRepository.save(sReq);
                break;
            case "CURRENT":
                BusinessChequeRequest bReq = resolved.businessChequeRequest;
                bReq.setStatus("CLEARED");
                bReq.setClearedAt(LocalDateTime.now());
                if (chequeHolderName != null && !chequeHolderName.isBlank()) {
                    bReq.setPayeeName(chequeHolderName.trim());
                }
                businessChequeRequestRepository.save(bReq);
                break;
        }
    }

    // Get all credit cards (for admin)
    public List<CreditCard> getAllCreditCards() {
        return creditCardRepository.findAll();
    }

    // Get credit cards by account number (for user)
    public List<CreditCard> getCreditCardsByAccount(String accountNumber) {
        return creditCardRepository.findByAccountNumber(accountNumber);
    }

    // Get credit card by ID
    public Optional<CreditCard> getCreditCardById(Long id) {
        return creditCardRepository.findById(id);
    }

    // Create credit card from approved request
    @Transactional
    public CreditCard createCreditCardFromRequest(CreditCardRequest request, String cardNumber, String cvv, String expiryDate) {
        CreditCard creditCard = new CreditCard();
        creditCard.setCardNumber(cardNumber);
        creditCard.setCvv(cvv);
        creditCard.setExpiryDate(expiryDate);
        creditCard.setAccountNumber(request.getAccountNumber());
        creditCard.setUserName(request.getUserName());
        creditCard.setUserEmail(request.getUserEmail());
        creditCard.setAppliedDate(request.getRequestDate());
        creditCard.setApprovalDate(LocalDateTime.now());
        creditCard.setApprovedLimit(request.getSuggestedLimit() != null ? request.getSuggestedLimit() : 50000.0);
        creditCard.setCurrentBalance(0.0);
        creditCard.calculateAvailableLimit();
        creditCard.calculateUsageLimit();
        creditCard.setStatus("Active");
        
        return creditCardRepository.save(creditCard);
    }

    // Update credit card
    @Transactional
    public CreditCard updateCreditCard(CreditCard creditCard) {
        creditCard.calculateAvailableLimit();
        creditCard.calculateUsageLimit();
        return creditCardRepository.save(creditCard);
    }

    // Set PIN
    @Transactional
    public boolean setPin(Long creditCardId, String pin) {
        Optional<CreditCard> cardOpt = creditCardRepository.findById(creditCardId);
        if (cardOpt.isPresent()) {
            CreditCard card = cardOpt.get();
            card.setPin(pin);
            card.setPinSet(true);
            creditCardRepository.save(card);
            return true;
        }
        return false;
    }

    // Add transaction
    @Transactional
    public CreditCardTransaction addTransaction(CreditCardTransaction transaction) {
        // Update credit card balance
        Optional<CreditCard> cardOpt = creditCardRepository.findById(transaction.getCreditCardId());
        if (cardOpt.isPresent()) {
            CreditCard card = cardOpt.get();
            if ("Purchase".equals(transaction.getTransactionType())) {
                card.setCurrentBalance(card.getCurrentBalance() + transaction.getAmount());
            } else if ("Payment".equals(transaction.getTransactionType())) {
                card.setCurrentBalance(Math.max(0, card.getCurrentBalance() - transaction.getAmount()));
                card.setLastPaidDate(LocalDateTime.now());
            }
            card.calculateAvailableLimit();
            card.calculateUsageLimit();
            creditCardRepository.save(card);
            
            transaction.setBalanceAfter(card.getCurrentBalance());
        }
        
        return transactionRepository.save(transaction);
    }

    // Get transactions
    public List<CreditCardTransaction> getTransactionsByCardId(Long creditCardId) {
        return transactionRepository.findByCreditCardId(creditCardId);
    }

    public List<CreditCardTransaction> getTransactionsByAccount(String accountNumber) {
        return transactionRepository.findByAccountNumberOrderByTransactionDateDesc(accountNumber);
    }

    // Generate bill
    @Transactional
    public CreditCardBill generateBill(Long creditCardId) {
        Optional<CreditCard> cardOpt = creditCardRepository.findById(creditCardId);
        if (cardOpt.isEmpty()) {
            return null;
        }
        
        CreditCard card = cardOpt.get();

        // Check if bill already exists for this month
        Optional<CreditCardBill> existingBill = billRepository.findFirstByCreditCardIdOrderByBillGenerationDateDesc(creditCardId);
        if (existingBill.isPresent()) {
            CreditCardBill lastBill = existingBill.get();
            LocalDateTime lastBillDate = lastBill.getBillGenerationDate();
            if (lastBillDate.getMonth() == LocalDateTime.now().getMonth() && 
                lastBillDate.getYear() == LocalDateTime.now().getYear()) {
                return lastBill; // Return existing bill for this month
            }
        }
        
        LocalDateTime generatedAt = LocalDateTime.now();
        List<CreditCardEmiPlan> activePlans = emiPlanRepository
                .findByCreditCardIdOrderByCreatedAtDesc(creditCardId).stream()
                .filter(plan -> "Active".equalsIgnoreCase(plan.getStatus()))
                .toList();
        double emiPrincipal = activePlans.stream()
                .mapToDouble(plan -> value(plan.getOutstandingPrincipal())).sum();
        double regularAmount = Math.max(0.0, value(card.getCurrentBalance()) - emiPrincipal);
        List<CreditCardEmiInstallment> dueInstallments = activePlans.stream()
                .flatMap(plan -> emiInstallmentRepository.findByPlanIdOrderByInstallmentNumberAsc(plan.getId()).stream())
                .filter(installment -> "Pending".equalsIgnoreCase(installment.getStatus())
                        && installment.getBillId() == null
                        && !installment.getDueDate().isAfter(generatedAt.toLocalDate()))
                .toList();
        double emiAmount = dueInstallments.stream().mapToDouble(item -> value(item.getTotalAmount())).sum();
        double totalAmount = roundMoney(regularAmount + emiAmount);
        if (totalAmount <= 0.0) {
            throw new IllegalArgumentException("No outstanding balance on this card. Statement not generated.");
        }

        CreditCardBill bill = new CreditCardBill();
        bill.setCreditCardId(creditCardId);
        bill.setCardNumber(card.getCardNumber());
        bill.setAccountNumber(card.getAccountNumber());
        bill.setUserName(card.getUserName());
        bill.setBillGenerationDate(generatedAt);
        bill.setDueDate(generatedAt.plusDays(21));
        bill.setRegularAmount(roundMoney(regularAmount));
        bill.setEmiAmount(roundMoney(emiAmount));
        bill.setTotalAmount(totalAmount);
        bill.setMinimumDue(roundMoney(Math.min(totalAmount + value(card.getFine()) + value(card.getPenalty()),
                Math.max(100.0, (totalAmount + value(card.getFine()) + value(card.getPenalty())) * 0.05))));
        bill.setOverdueAmount(card.getOverdueAmount());
        bill.setFine(card.getFine());
        bill.setPenalty(card.getPenalty());
        bill.setStatus("Generated");
        bill.setBillingPeriod(generatedAt.getMonth().toString() + " " + generatedAt.getYear());
        CreditCardBill savedBill = billRepository.save(bill);
        for (CreditCardEmiInstallment installment : dueInstallments) {
            installment.setBillId(savedBill.getId());
            emiInstallmentRepository.save(installment);
        }
        return savedBill;
    }

    // Get bills
    public List<CreditCardBill> getBillsByCardId(Long creditCardId) {
        return billRepository.findByCreditCardId(creditCardId);
    }

    public List<CreditCardBill> getBillsByAccount(String accountNumber) {
        return billRepository.findByAccountNumber(accountNumber);
    }

    public Map<String, Object> getEmiSettings() {
        CreditCardSettings settings = emiSettings();
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", settings.getEmiAnnualInterestPercent() != null);
        result.put("annualInterestPercent", settings.getEmiAnnualInterestPercent());
        result.put("processingFeePercent", settings.getEmiProcessingFeePercent() == null
                ? 8.0 : settings.getEmiProcessingFeePercent());
        result.put("minimumTransactionAmount", 3000.0);
        result.put("tenuresMonths", List.of(3, 6, 9, 12));
        result.put("updatedBy", settings.getUpdatedBy());
        result.put("updatedAt", settings.getUpdatedAt());
        return result;
    }

    @Transactional
    public Map<String, Object> updateEmiSettings(Double annualInterestPercent,
                                                  Double processingFeePercent, String updatedBy) {
        if (annualInterestPercent == null || !Double.isFinite(annualInterestPercent)
                || annualInterestPercent <= 0 || annualInterestPercent > 100) {
            throw new IllegalArgumentException("Annual EMI interest rate must be between 0 and 100 percent");
        }
        if (processingFeePercent == null || !Double.isFinite(processingFeePercent)
                || processingFeePercent < 8 || processingFeePercent > 100) {
            throw new IllegalArgumentException("EMI processing fee must be between 8 and 100 percent");
        }
        CreditCardSettings settings = emiSettings();
        settings.setEmiAnnualInterestPercent(annualInterestPercent);
        settings.setEmiProcessingFeePercent(processingFeePercent);
        settings.setUpdatedBy(updatedBy);
        settings.setUpdatedAt(LocalDateTime.now());
        settingsRepository.save(settings);
        return getEmiSettings();
    }

    @Transactional(readOnly = true)
    public List<CreditCardTransaction> getEmiEligibleTransactions(Long creditCardId) {
        CreditCard card = creditCardRepository.findById(creditCardId)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found"));
        LocalDateTime lastBillDate = billRepository
                .findFirstByCreditCardIdOrderByBillGenerationDateDesc(creditCardId)
                .map(CreditCardBill::getBillGenerationDate).orElse(null);
        return transactionRepository.findByCreditCardId(creditCardId).stream()
                .filter(tx -> "Completed".equalsIgnoreCase(tx.getStatus()))
                .filter(tx -> tx.getTransactionType() != null
                        && tx.getTransactionType().toLowerCase().contains("purchase"))
                .filter(tx -> tx.getAmount() != null && tx.getAmount() >= 3000.0)
                .filter(tx -> tx.getEmiPlanId() == null)
                .filter(tx -> lastBillDate == null || (tx.getTransactionDate() != null
                        && tx.getTransactionDate().isAfter(lastBillDate)))
                .sorted(Comparator.comparing(CreditCardTransaction::getTransactionDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public Map<String, Object> convertTransactionsToEmi(Long creditCardId,
                                                         List<Long> transactionIds, Integer tenureMonths) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one eligible transaction");
        }
        if (tenureMonths == null || !Set.of(3, 6, 9, 12).contains(tenureMonths)) {
            throw new IllegalArgumentException("Choose a 3, 6, 9, or 12 month EMI tenure");
        }
        CreditCard card = creditCardRepository.findById(creditCardId)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found"));
        if (!"Active".equalsIgnoreCase(card.getStatus()) || card.isBlocked() || card.isDeactivated()) {
            throw new IllegalArgumentException("Credit card is not active");
        }
        CreditCardSettings settings = emiSettings();
        Double apr = settings.getEmiAnnualInterestPercent();
        if (apr == null || apr <= 0) {
            throw new IllegalArgumentException("EMI conversion is unavailable until an admin configures the annual interest rate");
        }
        List<CreditCardTransaction> eligible = getEmiEligibleTransactions(creditCardId);
        Set<Long> eligibleIds = new HashSet<>();
        eligible.forEach(tx -> eligibleIds.add(tx.getId()));
        if (new HashSet<>(transactionIds).size() != transactionIds.size()
                || !eligibleIds.containsAll(transactionIds)) {
            throw new IllegalArgumentException("One or more selected purchases are no longer eligible for EMI conversion");
        }
        List<CreditCardTransaction> selected = transactionIds.stream()
                .map(id -> transactionRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Selected transaction was not found")))
                .toList();
        double principal = roundMoney(selected.stream().mapToDouble(tx -> value(tx.getAmount())).sum());
        double feePercent = settings.getEmiProcessingFeePercent() == null ? 8.0 : settings.getEmiProcessingFeePercent();
        double feeAmount = roundMoney(principal * feePercent / 100.0);
        int months = tenureMonths;
        double monthlyRate = apr / 1200.0;
        double rawEmi = principal * monthlyRate / (1.0 - Math.pow(1.0 + monthlyRate, -months));
        double regularInstallment = roundMoney(rawEmi);

        CreditCardEmiPlan plan = new CreditCardEmiPlan();
        plan.setCreditCardId(card.getId());
        plan.setAccountNumber(card.getAccountNumber());
        plan.setUserName(card.getUserName());
        plan.setCardNumber(maskCardNumber(card.getCardNumber()));
        plan.setTenureMonths(months);
        plan.setPrincipalAmount(principal);
        plan.setProcessingFeePercent(feePercent);
        plan.setProcessingFeeAmount(feeAmount);
        plan.setAnnualInterestPercent(apr);
        plan.setMonthlyEmi(regularInstallment);
        plan.setOutstandingPrincipal(principal);
        plan.setStatus("Active");
        plan = emiPlanRepository.save(plan);

        double remaining = principal;
        double totalInterest = 0.0;
        double totalRepayment = 0.0;
        LocalDate dueDate = LocalDate.now().plusMonths(1);
        for (int i = 1; i <= months; i++) {
            double interest = roundMoney(remaining * monthlyRate);
            double installmentPrincipal = i == months
                    ? roundMoney(remaining) : Math.min(remaining, roundMoney(regularInstallment - interest));
            double installmentTotal = roundMoney(installmentPrincipal + interest);
            CreditCardEmiInstallment installment = new CreditCardEmiInstallment();
            installment.setPlanId(plan.getId());
            installment.setInstallmentNumber(i);
            installment.setDueDate(dueDate);
            installment.setPrincipalAmount(installmentPrincipal);
            installment.setInterestAmount(interest);
            installment.setTotalAmount(installmentTotal);
            installment.setPaidPrincipal(0.0);
            installment.setPaidInterest(0.0);
            installment.setPaidAmount(0.0);
            installment.setStatus("Pending");
            emiInstallmentRepository.save(installment);
            remaining = roundMoney(Math.max(0.0, remaining - installmentPrincipal));
            totalInterest += interest;
            totalRepayment += installmentTotal;
            dueDate = dueDate.plusMonths(1);
        }
        plan.setTotalInterest(roundMoney(totalInterest));
        plan.setTotalRepayment(roundMoney(totalRepayment));
        emiPlanRepository.save(plan);
        for (CreditCardTransaction transaction : selected) {
            transaction.setEmiPlanId(plan.getId());
            transactionRepository.save(transaction);
        }
        if (feeAmount > 0) {
            card.setCurrentBalance(roundMoney(value(card.getCurrentBalance()) + feeAmount));
            card.calculateAvailableLimit();
            card.calculateUsageLimit();
            creditCardRepository.save(card);
            CreditCardTransaction feeTransaction = new CreditCardTransaction();
            feeTransaction.setCreditCardId(card.getId());
            feeTransaction.setCardNumber(card.getCardNumber());
            feeTransaction.setAccountNumber(card.getAccountNumber());
            feeTransaction.setUserName(card.getUserName());
            feeTransaction.setTransactionType("Fee");
            feeTransaction.setAmount(feeAmount);
            feeTransaction.setDescription("EMI processing fee (" + feePercent + "%) for plan #" + plan.getId());
            feeTransaction.setBalanceAfter(card.getCurrentBalance());
            transactionRepository.save(feeTransaction);
        }
        return planDetails(plan);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEmiPlans(Long creditCardId) {
        creditCardRepository.findById(creditCardId)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found"));
        return emiPlanRepository.findByCreditCardIdOrderByCreatedAtDesc(creditCardId).stream()
                .map(this::planDetails).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllEmiPlans() {
        return emiPlanRepository.findAllByOrderByCreatedAtDesc().stream().map(this::planDetails).toList();
    }

    // Pay bill
    @Transactional
    public CreditCardBill payBill(Long billId, Double amount) {
        Optional<CreditCardBill> billOpt = billRepository.findById(billId);
        if (billOpt.isEmpty()) {
            return null;
        }
        
        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        CreditCardBill bill = billOpt.get();
        Double paidAmount = bill.getPaidAmount() != null ? bill.getPaidAmount() : 0.0;
        double billTotalDue = billTotalDue(bill);
        if (amount > billTotalDue - paidAmount + 0.01) {
            throw new IllegalArgumentException("Payment cannot exceed the remaining bill amount");
        }
        paidAmount += amount;
        bill.setPaidAmount(paidAmount);
        bill.setPaidDate(LocalDateTime.now());
        
        if (paidAmount + 0.01 >= billTotalDue) {
            bill.setStatus("Paid");
        } else {
            bill.setStatus("Partial");
        }
        
        // Update credit card balance
        Optional<CreditCard> cardOpt = creditCardRepository.findById(bill.getCreditCardId());
        if (cardOpt.isPresent()) {
            CreditCard card = cardOpt.get();
            card.setCurrentBalance(Math.max(0, value(card.getCurrentBalance()) - amount));
            card.setLastPaidDate(LocalDateTime.now());
            if ("Paid".equals(bill.getStatus())) {
                card.setOverdueAmount(0.0);
                card.setFine(0.0);
                card.setPenalty(0.0);
            }
            card.calculateAvailableLimit();
            card.calculateUsageLimit();
            creditCardRepository.save(card);
            
            // Add payment transaction
            CreditCardTransaction payment = new CreditCardTransaction();
            payment.setCreditCardId(card.getId());
            payment.setCardNumber(card.getCardNumber());
            payment.setAccountNumber(card.getAccountNumber());
            payment.setUserName(card.getUserName());
            payment.setTransactionType("Payment");
            payment.setPaymentMethod("ACCOUNT");
            payment.setBillId(billId);
            payment.setAmount(amount);
            payment.setDescription("Bill Payment");
            payment.setBalanceAfter(card.getCurrentBalance());
            transactionRepository.save(payment);
        }
        applyEmiPayment(bill, emiPaymentForBill(bill, paidAmount - amount, amount));
        return billRepository.save(bill);
    }

    @Transactional
    public Map<String, Object> payBillAsAdmin(Long billId, AdminCreditCardPaymentRequest request) {
        if (request == null || request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        String paymentMethod = request.getPaymentMethod() == null
                ? "" : request.getPaymentMethod().trim().toUpperCase();
        if (!List.of("CASH", "ACCOUNT", "CHEQUE").contains(paymentMethod)) {
            throw new IllegalArgumentException("Payment method must be CASH, ACCOUNT, or CHEQUE");
        }
        if ("CHEQUE".equals(paymentMethod)
                && (request.getChequeNumber() == null || request.getChequeNumber().isBlank())) {
            throw new IllegalArgumentException("Cheque payment number is required");
        }

        CreditCardBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Credit card bill not found"));
        CreditCard card = creditCardRepository.findById(bill.getCreditCardId())
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found"));

        double paidAmount = bill.getPaidAmount() == null ? 0.0 : bill.getPaidAmount();
        double billDue = billTotalDue(bill) - paidAmount;
        double amount = request.getAmount();
        if (amount > billDue + 0.01) {
            throw new IllegalArgumentException("Payment cannot exceed the outstanding bill amount of ₹"
                    + String.format("%.2f", Math.max(0, billDue)));
        }

        String debitAccountNumber = request.getDebitAccountNumber();
        ResolvedCheque resolvedCheque = null;
        if ("CHEQUE".equals(paymentMethod)) {
            resolvedCheque = resolveChequeForPayment(request.getChequeNumber().trim());
            if (resolvedCheque == null) {
                throw new IllegalArgumentException("Cheque number not found");
            }
            if (debitAccountNumber == null || debitAccountNumber.isBlank()) {
                debitAccountNumber = resolvedCheque.accountNumber;
            }
            if (!resolvedCheque.usable) {
                throw new IllegalArgumentException("Cheque is already used or unavailable. Status: " + resolvedCheque.status);
            }
            // Only the amount entered by the admin/user is locked against the cheque, not the cheque's own face amount
            if (resolvedCheque.savingsCheque != null) resolvedCheque.savingsCheque.setAmount(amount);
        }

        Double accountBalanceAfter = null;
        if ("ACCOUNT".equals(paymentMethod)) {
            if (debitAccountNumber == null || debitAccountNumber.isBlank()) {
                debitAccountNumber = card.getAccountNumber();
            }
            accountBalanceAfter = debitAnyAccount(debitAccountNumber, amount);
            if (accountBalanceAfter == null) {
                throw new IllegalArgumentException("Account not found or insufficient balance");
            }
        } else if ("CHEQUE".equals(paymentMethod)) {
            if (debitAccountNumber == null || debitAccountNumber.isBlank()) {
                debitAccountNumber = resolvedCheque != null && resolvedCheque.accountNumber != null
                        ? resolvedCheque.accountNumber : card.getAccountNumber();
            }
            if (resolvedCheque != null && resolvedCheque.adminDrawn) {
                // Cheque was already drawn by admin (funds debited during drawCheque).
                // Do not debit account a second time.
                accountBalanceAfter = getAccountBalance(debitAccountNumber);
            } else {
                // Cheque was not drawn by admin yet -> Admin pays it now: debit the account
                accountBalanceAfter = debitAnyAccount(debitAccountNumber, amount);
                if (accountBalanceAfter == null) {
                    throw new IllegalArgumentException("Account not found or insufficient balance to honour cheque");
                }
            }
            if (resolvedCheque != null) {
                markChequeUsed(resolvedCheque, "CARD-" + card.getId() + "-BILL-" + billId, request.getAdminName(), request.getChequeHolderName());
            }
        }

        double cardBalanceAfter = Math.max(0.0, (card.getCurrentBalance() == null ? 0.0 : card.getCurrentBalance()) - amount);
        card.setCurrentBalance(cardBalanceAfter);
        card.setLastPaidDate(LocalDateTime.now());
        if (paidAmount + amount + 0.01 >= billTotalDue(bill)) {
            card.setOverdueAmount(0.0);
            card.setFine(0.0);
            card.setPenalty(0.0);
        }
        card.calculateAvailableLimit();
        card.calculateUsageLimit();
        creditCardRepository.save(card);

        bill.setPaidAmount(paidAmount + amount);
        bill.setPaidDate(LocalDateTime.now());
        bill.setStatus(bill.getPaidAmount() + 0.01 >= billTotalDue(bill) ? "Paid" : "Partial");
        billRepository.save(bill);
        applyEmiPayment(bill, emiPaymentForBill(bill, paidAmount, amount));

        Long globalSequence = transactionIdGenerator.getNextTransactionId();
        CreditCardTransaction cardTransaction = new CreditCardTransaction();
        cardTransaction.setGlobalTransactionSequence(globalSequence);
        cardTransaction.setCreditCardId(card.getId());
        cardTransaction.setCardNumber(card.getCardNumber());
        cardTransaction.setAccountNumber(card.getAccountNumber());
        cardTransaction.setUserName(card.getUserName());
        cardTransaction.setTransactionType("Payment");
        cardTransaction.setBillId(billId);
        cardTransaction.setPaymentMethod(paymentMethod);
        cardTransaction.setChequeNumber(request.getChequeNumber());
        cardTransaction.setChequeHolderName(request.getChequeHolderName());
        if (request.getChequeImageBase64() != null && !request.getChequeImageBase64().isBlank()) {
            String imageData = request.getChequeImageBase64();
            int comma = imageData.indexOf(',');
            if (comma >= 0) imageData = imageData.substring(comma + 1);
            cardTransaction.setChequeImage(Base64.getDecoder().decode(imageData));
            cardTransaction.setChequeImageName(request.getChequeImageName());
            cardTransaction.setChequeImageType(request.getChequeImageType());
        }
        cardTransaction.setDebitAccountNumber(debitAccountNumber);
        cardTransaction.setProcessedBy(request.getAdminName());
        cardTransaction.setAmount(amount);
        cardTransaction.setDescription("Admin bill payment via " + paymentMethod
                + (request.getChequeNumber() == null ? "" : " - Cheque: " + request.getChequeNumber())
                + (request.getChequeHolderName() == null || request.getChequeHolderName().isBlank() ? "" : " (" + request.getChequeHolderName() + ")"));
        cardTransaction.setBalanceAfter(cardBalanceAfter);
        transactionRepository.save(cardTransaction);

        if (accountBalanceAfter != null) {
            Transaction accountTransaction = new Transaction();
            accountTransaction.setGlobalTransactionSequence(globalSequence);
            accountTransaction.setAccountNumber(debitAccountNumber);
            accountTransaction.setUserName(card.getUserName());
            accountTransaction.setAmount(amount);
            accountTransaction.setType("Debit");
            accountTransaction.setMerchant("Credit Card Bill");
            accountTransaction.setDescription(cardTransaction.getDescription());
            accountTransaction.setBalance(accountBalanceAfter);
            accountTransaction.setStatus("Completed");
            transactionService.saveTransaction(accountTransaction);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bill", bill);
        result.put("card", card);
        result.put("payment", cardTransaction);
        result.put("accountBalanceAfter", accountBalanceAfter);
        result.put("debitAccountNumber", debitAccountNumber);
        result.put("cheque", resolvedCheque != null ? resolvedCheque.status : null);
        return result;
    }

    @Transactional
    public Map<String, Object> payMinimumDueByEcs(Long billId, String debitAccountNumber, String actor) {
        CreditCardBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Credit card bill not found"));
        double minimumDue = bill.getMinimumDue() == null ? 0.0 : bill.getMinimumDue();
        double paidAmount = bill.getPaidAmount() == null ? 0.0 : bill.getPaidAmount();
        double amount = Math.max(0.0, minimumDue - paidAmount);
        if (amount <= 0.0) {
            throw new IllegalArgumentException("Credit card minimum due is already paid");
        }

        AdminCreditCardPaymentRequest request = new AdminCreditCardPaymentRequest();
        request.setAmount(amount);
        request.setPaymentMethod("ACCOUNT");
        request.setDebitAccountNumber(debitAccountNumber);
        request.setAdminName(actor == null ? "ECS" : actor);
        return payBillAsAdmin(billId, request);
    }

    // ---------- User-side credit card bill pay (last 4 digits + linked mobile) ----------

    private static String lastDigits(String value, int n) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        return digits.length() <= n ? digits : digits.substring(digits.length() - n);
    }

    private String getLinkedMobile(String accountNumber) {
        if (accountNumber == null) return null;
        Account savings = accountService.getAccountByNumber(accountNumber);
        if (savings != null) return savings.getPhone();
        CurrentAccount cur = currentAccountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (cur != null) return cur.getMobile();
        SalaryAccount sal = salaryAccountRepository.findByAccountNumber(accountNumber);
        return sal != null ? sal.getMobileNumber() : null;
    }

    private CreditCard findCardForBillPay(String last4, String mobile) {
        String l4 = lastDigits(last4, 4);
        String mob = lastDigits(mobile, 10);
        if (l4.length() != 4 || mob.length() != 10) {
            throw new IllegalArgumentException("Enter the last 4 digits of the card and a valid 10-digit mobile number");
        }
        for (CreditCard card : creditCardRepository.findAll()) {
            if (card.getCardNumber() == null || !"Active".equalsIgnoreCase(card.getStatus())) continue;
            if (!lastDigits(card.getCardNumber(), 4).equals(l4)) continue;
            if (mob.equals(lastDigits(getLinkedMobile(card.getAccountNumber()), 10))) {
                return card;
            }
        }
        throw new IllegalArgumentException("No active credit card found for these details");
    }

    public Map<String, Object> lookupCardForBillPay(String last4, String mobile, String payerAccountNumber) {
        CreditCard card = findCardForBillPay(last4, mobile);
        double outstanding = card.getCurrentBalance() == null ? 0.0 : card.getCurrentBalance();
        double minimumDue = outstanding <= 0 ? 0.0
                : Math.min(outstanding, Math.max(100.0, Math.round(outstanding * 5.0) / 100.0));
        Optional<CreditCardBill> latest = billRepository
                .findFirstByCreditCardIdOrderByBillGenerationDateDesc(card.getId());
        if (latest.isPresent() && latest.get().getMinimumDue() != null && outstanding > 0
                && !"Paid".equalsIgnoreCase(latest.get().getStatus())) {
            double billMin = latest.get().getMinimumDue() - (latest.get().getPaidAmount() == null ? 0.0 : latest.get().getPaidAmount());
            minimumDue = Math.min(outstanding, Math.max(0.0, billMin));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("cardId", card.getId());
        result.put("cardHolderName", card.getUserName());
        result.put("maskedCardNumber", card.getMaskedCardNumber());
        result.put("outstandingBalance", outstanding);
        result.put("minimumDue", Math.round(minimumDue * 100.0) / 100.0);
        result.put("approvedLimit", card.getApprovedLimit());
        result.put("availableLimit", card.getAvailableLimit());
        result.put("dueDate", latest.map(CreditCardBill::getDueDate).orElse(card.getNextBillingDate()));
        result.put("payerAccountNumber", payerAccountNumber);
        result.put("payerAvailableBalance", getAccountBalance(payerAccountNumber));
        return result;
    }

    @Transactional
    public Map<String, Object> payBillFromAccount(String last4, String mobile, String payerAccountNumber, Double amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (payerAccountNumber == null || payerAccountNumber.isBlank()) {
            throw new IllegalArgumentException("Paying account is required");
        }
        CreditCard card = findCardForBillPay(last4, mobile);
        CreditCardBill latestBill = billRepository
                .findFirstByCreditCardIdOrderByBillGenerationDateDesc(card.getId()).orElse(null);
        double outstanding = latestBill != null && !"Paid".equalsIgnoreCase(latestBill.getStatus())
                ? Math.max(0.0, billTotalDue(latestBill) - value(latestBill.getPaidAmount()))
                : value(card.getCurrentBalance());
        if (outstanding <= 0) {
            throw new IllegalArgumentException("No outstanding balance on this card");
        }
        if (amount > outstanding + 0.01) {
            throw new IllegalArgumentException("Payment cannot exceed the outstanding balance of ₹"
                    + String.format("%.2f", outstanding));
        }

        Double accountBalanceAfter = debitAnyAccount(payerAccountNumber, amount);
        if (accountBalanceAfter == null) {
            throw new IllegalArgumentException("Insufficient balance or account unavailable");
        }

        return postCreditCardBillPayment(card, payerAccountNumber, amount, accountBalanceAfter, "USER");
    }

    @Transactional
    public Map<String, Object> payCards360Bill(String customerAccountNumber, Long creditCardId, Double amount) {
        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (customerAccountNumber == null || customerAccountNumber.isBlank()) {
            throw new IllegalArgumentException("Customer account is required");
        }
        if (creditCardId == null) {
            throw new IllegalArgumentException("Credit card is required");
        }
        CreditCard card = creditCardRepository.findById(creditCardId)
                .filter(value -> customerAccountNumber.equals(value.getAccountNumber()))
                .orElseThrow(() -> new IllegalArgumentException("Credit card does not belong to this customer"));
        if (!"Active".equalsIgnoreCase(card.getStatus()) || card.isBlocked() || card.isDeactivated()) {
            throw new IllegalArgumentException("Credit card is not active or has been blocked");
        }
        CreditCardBill latestBill = billRepository
                .findFirstByCreditCardIdOrderByBillGenerationDateDesc(card.getId()).orElse(null);
        double outstanding = latestBill != null && !"Paid".equalsIgnoreCase(latestBill.getStatus())
                ? Math.max(0.0, billTotalDue(latestBill) - value(latestBill.getPaidAmount()))
                : value(card.getCurrentBalance());
        if (outstanding <= 0) {
            throw new IllegalArgumentException("No outstanding balance on this card");
        }
        if (amount > outstanding + 0.01) {
            throw new IllegalArgumentException("Payment cannot exceed the outstanding balance of ₹"
                    + String.format("%.2f", outstanding));
        }

        Account savingsAccount = accountService.getAccountByNumber(customerAccountNumber);
        if (savingsAccount == null || !"ACTIVE".equalsIgnoreCase(savingsAccount.getStatus())) {
            throw new IllegalArgumentException("Primary savings account is unavailable");
        }
        Double accountBalanceAfter = accountService.debitBalance(customerAccountNumber, amount);
        if (accountBalanceAfter == null) {
            throw new IllegalArgumentException("Insufficient balance in the primary savings account");
        }
        return postCreditCardBillPayment(card, customerAccountNumber, amount, accountBalanceAfter, "CARD360");
    }

    private Map<String, Object> postCreditCardBillPayment(CreditCard card, String payerAccountNumber,
                                                           Double amount, Double accountBalanceAfter,
                                                           String processedBy) {
        double outstanding = card.getCurrentBalance() == null ? 0.0 : card.getCurrentBalance();
        double cardBalanceAfter = Math.max(0.0, outstanding - Math.min(amount, value(card.getCurrentBalance())));
        card.setCurrentBalance(cardBalanceAfter);
        card.setLastPaidDate(LocalDateTime.now());
        card.calculateAvailableLimit();
        card.calculateUsageLimit();
        creditCardRepository.save(card);

        CreditCardBill paidBill = billRepository.findFirstByCreditCardIdOrderByBillGenerationDateDesc(card.getId()).orElse(null);
        double priorBillPaid = paidBill == null ? 0.0 : value(paidBill.getPaidAmount());
        if (paidBill != null) {
            CreditCardBill bill = paidBill;
            if (!"Paid".equalsIgnoreCase(bill.getStatus())) {
                double paid = priorBillPaid + amount;
                bill.setPaidAmount(paid);
                bill.setPaidDate(LocalDateTime.now());
                bill.setStatus(paid + 0.01 >= billTotalDue(bill) ? "Paid" : "Partial");
                billRepository.save(bill);
                applyEmiPayment(bill, emiPaymentForBill(bill, priorBillPaid, amount));
                if ("Paid".equalsIgnoreCase(bill.getStatus())) {
                    card.setOverdueAmount(0.0);
                    card.setFine(0.0);
                    card.setPenalty(0.0);
                    creditCardRepository.save(card);
                }
            }
        }

        String description = "Credit card bill payment (" + card.getMaskedCardNumber() + ") from account " + payerAccountNumber;
        Long globalSequence = transactionIdGenerator.getNextTransactionId();

        CreditCardTransaction cardTx = new CreditCardTransaction();
        cardTx.setGlobalTransactionSequence(globalSequence);
        cardTx.setCreditCardId(card.getId());
        cardTx.setCardNumber(card.getCardNumber());
        cardTx.setAccountNumber(card.getAccountNumber());
        cardTx.setUserName(card.getUserName());
        cardTx.setTransactionType("Payment");
        cardTx.setBillId(paidBill == null ? null : paidBill.getId());
        cardTx.setPaymentMethod("ACCOUNT");
        cardTx.setDebitAccountNumber(payerAccountNumber);
        cardTx.setProcessedBy(processedBy);
        cardTx.setAmount(amount);
        cardTx.setDescription(description);
        cardTx.setBalanceAfter(cardBalanceAfter);
        transactionRepository.save(cardTx);

        Transaction accountTx = new Transaction();
        accountTx.setGlobalTransactionSequence(globalSequence);
        accountTx.setAccountNumber(payerAccountNumber);
        accountTx.setUserName(card.getUserName());
        accountTx.setAmount(amount);
        accountTx.setType("Debit");
        accountTx.setMerchant("Credit Card Bill");
        accountTx.setDescription(description);
        accountTx.setBalance(accountBalanceAfter);
        accountTx.setStatus("Completed");
        transactionService.saveTransaction(accountTx);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("paidAmount", amount);
        result.put("remainingOutstanding", cardBalanceAfter);
        result.put("accountBalanceAfter", accountBalanceAfter);
        result.put("transactionId", globalSequence);
        result.put("cardHolderName", card.getUserName());
        return result;
    }

    public Double getAccountBalance(String accountNumber) {
        if (accountNumber == null) return null;
        Account savingsAcc = accountService.getAccountByNumber(accountNumber);
        if (savingsAcc != null) return savingsAcc.getBalance();
        CurrentAccount cur = currentAccountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (cur != null) return cur.getBalance();
        SalaryAccount sal = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (sal != null) return sal.getBalance();
        return null;
    }

    public Map<String, Object> getCards360PaymentAccount(String accountNumber) {
        Account savingsAccount = accountService.getAccountByNumber(accountNumber);
        if (savingsAccount == null || !"ACTIVE".equalsIgnoreCase(savingsAccount.getStatus())) {
            throw new IllegalArgumentException("Primary savings account is unavailable");
        }
        String suffix = accountNumber.length() <= 4
                ? accountNumber : accountNumber.substring(accountNumber.length() - 4);
        Map<String, Object> result = new HashMap<>();
        result.put("maskedAccountNumber", "••••" + suffix);
        result.put("balance", savingsAccount.getBalance() == null ? 0.0 : savingsAccount.getBalance());
        return result;
    }

    /** Debits an account for a credit-card bill payment across savings, current, and salary accounts. */
    private Double debitAnyAccount(String accountNumber, double amount) {
        Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(accountNumber);
        if (currentOpt.isPresent()) {
            CurrentAccount ca = currentOpt.get();
            if (!"ACTIVE".equalsIgnoreCase(ca.getStatus())) return null;
            if (ca.getBalance() == null || ca.getBalance() < amount) return null;
            ca.setBalance(ca.getBalance() - amount);
            currentAccountRepository.save(ca);
            return ca.getBalance();
        }
        SalaryAccount sal = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (sal != null) {
            if (!"ACTIVE".equalsIgnoreCase(sal.getStatus())) return null;
            if (sal.getBalance() == null || sal.getBalance() < amount) return null;
            sal.setBalance(sal.getBalance() - amount);
            salaryAccountRepository.save(sal);
            return sal.getBalance();
        }
        return accountService.debitBalance(accountNumber, amount);
    }

    public CreditCardTransaction getChequeImageTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .filter(transaction -> transaction.getChequeImage() != null)
                .orElse(null);
    }

    /** Verify a cheque number for use in credit card bill payment: existence, ownership, valid/used status.
     *  Checks savings account cheques, salary account cheque draws, and current/business account cheque draws. */
    public Map<String, Object> verifyChequeForCardPayment(String chequeNumber, String debitAccountNumber) {
        Map<String, Object> result = new HashMap<>();
        ResolvedCheque resolved = resolveChequeForPayment(chequeNumber == null ? "" : chequeNumber.trim());
        if (resolved == null) {
            result.put("valid", false);
            result.put("message", "Cheque number not found");
            return result;
        }
        boolean ownerMatches = debitAccountNumber == null || debitAccountNumber.isBlank()
                || debitAccountNumber.trim().equals(resolved.accountNumber);
        boolean valid = resolved.usable;
        result.put("valid", valid);
        result.put("chequeNumber", chequeNumber);
        result.put("accountNumber", resolved.accountNumber);
        result.put("accountHolderName", resolved.accountHolderName);
        result.put("status", resolved.status);
        result.put("adminDrawn", resolved.adminDrawn);
        result.put("drawnBy", resolved.drawnBy);
        result.put("source", resolved.source);
        result.put("ownerMatches", ownerMatches);
        Double availableBalance = null;
        if (resolved.accountNumber != null) {
            availableBalance = getAccountBalance(resolved.accountNumber);
        }
        result.put("availableBalance", availableBalance != null ? availableBalance : 0.0);
        if (!resolved.usable) {
            result.put("message", "Cheque is already used or unavailable. Status: " + resolved.status);
        } else if (resolved.adminDrawn) {
            result.put("message", "Cheque is admin-drawn and available for bill payment");
        } else {
            result.put("message", "Cheque is valid and ready for admin payment");
        }
        return result;
    }

    /** Get/update the admin-configurable credit card transfer fee percentage (default 2%). */
    public double getTransferFeePercent() {
        return settingsRepository.findAll().stream().findFirst().map(CreditCardSettings::getTransferFeePercent).orElse(2.0);
    }

    @Transactional
    public CreditCardSettings updateTransferFeePercent(double feePercent, String updatedBy) {
        if (feePercent < 0 || feePercent > 100) throw new IllegalArgumentException("Fee percent must be between 0 and 100");
        CreditCardSettings settings = settingsRepository.findAll().stream().findFirst().orElseGet(CreditCardSettings::new);
        settings.setTransferFeePercent(feePercent);
        settings.setUpdatedBy(updatedBy);
        settings.setUpdatedAt(LocalDateTime.now());
        return settingsRepository.save(settings);
    }

    private CreditCardSettings emiSettings() {
        CreditCardSettings settings = settingsRepository.findAll().stream().findFirst()
                .orElseGet(CreditCardSettings::new);
        if (settings.getEmiProcessingFeePercent() == null) settings.setEmiProcessingFeePercent(8.0);
        return settings;
    }

    private Map<String, Object> planDetails(CreditCardEmiPlan plan) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", plan.getId());
        result.put("creditCardId", plan.getCreditCardId());
        result.put("accountNumber", plan.getAccountNumber());
        result.put("userName", plan.getUserName());
        result.put("maskedCardNumber", plan.getCardNumber());
        result.put("tenureMonths", plan.getTenureMonths());
        result.put("principalAmount", plan.getPrincipalAmount());
        result.put("processingFeePercent", plan.getProcessingFeePercent());
        result.put("processingFeeAmount", plan.getProcessingFeeAmount());
        result.put("annualInterestPercent", plan.getAnnualInterestPercent());
        result.put("monthlyEmi", plan.getMonthlyEmi());
        result.put("totalInterest", plan.getTotalInterest());
        result.put("totalRepayment", plan.getTotalRepayment());
        result.put("outstandingPrincipal", plan.getOutstandingPrincipal());
        result.put("status", plan.getStatus());
        result.put("createdAt", plan.getCreatedAt());
        result.put("installments", emiInstallmentRepository
                .findByPlanIdOrderByInstallmentNumberAsc(plan.getId()));
        return result;
    }

    private void applyEmiPayment(CreditCardBill bill, double billPayment) {
        double emiPaid = Math.min(billPayment, value(bill.getEmiAmount()));
        if (emiPaid <= 0) return;
        for (CreditCardEmiInstallment installment :
                emiInstallmentRepository.findByBillIdOrderByDueDateAsc(bill.getId())) {
            double remainingDue = Math.max(0.0,
                    value(installment.getTotalAmount()) - value(installment.getPaidAmount()));
            double allocation = Math.min(emiPaid, remainingDue);
            if (allocation <= 0) continue;

            double unpaidInterest = Math.max(0.0,
                    value(installment.getInterestAmount()) - value(installment.getPaidInterest()));
            double interestPaid = Math.min(allocation, unpaidInterest);
            double principalPaid = Math.min(allocation - interestPaid,
                    Math.max(0.0, value(installment.getPrincipalAmount()) - value(installment.getPaidPrincipal())));
            installment.setPaidInterest(roundMoney(value(installment.getPaidInterest()) + interestPaid));
            installment.setPaidPrincipal(roundMoney(value(installment.getPaidPrincipal()) + principalPaid));
            installment.setPaidAmount(roundMoney(value(installment.getPaidAmount()) + allocation));
            if (value(installment.getPaidAmount()) + 0.01 >= value(installment.getTotalAmount())) {
                installment.setStatus("Paid");
                installment.setPaidAt(LocalDateTime.now());
            } else {
                installment.setStatus("Partial");
            }

            emiInstallmentRepository.save(installment);
            emiPlanRepository.findById(installment.getPlanId()).ifPresent(plan -> {
                plan.setOutstandingPrincipal(roundMoney(Math.max(0.0,
                        value(plan.getOutstandingPrincipal()) - principalPaid)));
                boolean complete = emiInstallmentRepository.findByPlanIdOrderByInstallmentNumberAsc(plan.getId())
                        .stream().allMatch(item -> "Paid".equalsIgnoreCase(item.getStatus())
                                || item.getId().equals(installment.getId())
                                && "Paid".equalsIgnoreCase(installment.getStatus()));
                if (complete) plan.setStatus("Completed");
                emiPlanRepository.save(plan);
            });
            emiPaid = roundMoney(emiPaid - allocation);
        }
    }

    private double emiPaymentForBill(CreditCardBill bill, double previouslyPaid, double payment) {
        double charges = value(bill.getFine()) + value(bill.getPenalty());
        double regular = bill.getRegularAmount() == null
                ? Math.max(0.0, value(bill.getTotalAmount()) - value(bill.getEmiAmount()))
                : value(bill.getRegularAmount());
        double emiTotal = value(bill.getEmiAmount());
        double emiPaidBefore = Math.min(emiTotal, Math.max(0.0, previouslyPaid - charges - regular));
        double emiPaidAfter = Math.min(emiTotal, Math.max(0.0, previouslyPaid + payment - charges - regular));
        return roundMoney(Math.max(0.0, emiPaidAfter - emiPaidBefore));
    }

    private double billTotalDue(CreditCardBill bill) {
        return roundMoney(value(bill.getTotalAmount()) + value(bill.getFine()) + value(bill.getPenalty()));
    }

    private double value(Double amount) {
        return amount == null || !Double.isFinite(amount) ? 0.0 : amount;
    }

    private double roundMoney(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String maskCardNumber(String number) {
        if (number == null || number.length() < 4) return "****";
        return "•••• •••• •••• " + number.substring(number.length() - 4);
    }

    /**
     * Transfer money from a credit card's available limit to any savings/salary/current account.
     * A configurable percentage fee is deducted from the transferred amount and credited to the
     * admin-linked branch account in real time; the destination account receives the remainder.
     */
    @Transactional
    public Map<String, Object> transferToAccount(Long creditCardId, String destinationAccountNumber, Double amount, String adminName) {
        if (amount == null || amount <= 0) throw new IllegalArgumentException("Transfer amount must be greater than zero");
        if (destinationAccountNumber == null || destinationAccountNumber.isBlank()) throw new IllegalArgumentException("Destination account number is required");

        CreditCard card = creditCardRepository.findById(creditCardId)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found"));
        if (!"Active".equalsIgnoreCase(card.getStatus())) throw new IllegalArgumentException("Credit card is not active");
        if (card.getAvailableLimit() == null || card.getAvailableLimit() < amount) {
            throw new IllegalArgumentException("Insufficient available credit limit. Available: ₹" + card.getAvailableLimit());
        }

        Account destination = accountService.getAccountByNumber(destinationAccountNumber.trim());
        if (destination == null) throw new IllegalArgumentException("Destination account not found: " + destinationAccountNumber);
        if (!"ACTIVE".equalsIgnoreCase(destination.getStatus())) throw new IllegalArgumentException("Destination account is not active");

        double feePercent = getTransferFeePercent();
        double fee = Math.round(amount * feePercent) / 100.0;
        double creditedAmount = amount - fee;

        // Charge the full amount to the card (increases balance / reduces available limit)
        card.setCurrentBalance((card.getCurrentBalance() == null ? 0.0 : card.getCurrentBalance()) + amount);
        card.setLastUsed(LocalDateTime.now());
        card.calculateAvailableLimit();
        card.calculateUsageLimit();
        creditCardRepository.save(card);

        Double destinationBalanceAfter = accountService.creditBalance(destinationAccountNumber.trim(), creditedAmount);

        Long globalSequence = transactionIdGenerator.getNextTransactionId();
        CreditCardTransaction cardTransaction = new CreditCardTransaction();
        cardTransaction.setGlobalTransactionSequence(globalSequence);
        cardTransaction.setCreditCardId(card.getId());
        cardTransaction.setCardNumber(card.getCardNumber());
        cardTransaction.setAccountNumber(card.getAccountNumber());
        cardTransaction.setUserName(card.getUserName());
        cardTransaction.setTransactionType("Transfer");
        cardTransaction.setPaymentMethod("ACCOUNT");
        cardTransaction.setDebitAccountNumber(destinationAccountNumber.trim());
        cardTransaction.setProcessedBy(adminName);
        cardTransaction.setAmount(amount);
        cardTransaction.setDescription(String.format("Transfer to account %s (fee %.2f%% = ₹%.2f)", destinationAccountNumber.trim(), feePercent, fee));
        cardTransaction.setBalanceAfter(card.getCurrentBalance());
        transactionRepository.save(cardTransaction);

        Transaction creditTxn = new Transaction();
        creditTxn.setGlobalTransactionSequence(globalSequence);
        creditTxn.setAccountNumber(destinationAccountNumber.trim());
        creditTxn.setUserName(destination.getName());
        creditTxn.setAmount(creditedAmount);
        creditTxn.setType("Credit");
        creditTxn.setMerchant("Credit Card Transfer");
        creditTxn.setDescription("Credit card transfer from card ending " + card.getMaskedCardNumber());
        creditTxn.setBalance(destinationBalanceAfter != null ? destinationBalanceAfter : creditedAmount);
        creditTxn.setStatus("Completed");
        transactionService.saveTransaction(creditTxn);

        // Fee is already part of the amount charged to the card; credit it to the admin-linked
        // branch account in real time without any additional debit from the user's account.
        if (fee > 0) {
            String branchAccountNumber = branchAccountService.getDepositAccountNumber();
            Double branchBalanceAfter = accountService.creditBalance(branchAccountNumber, fee);
            Transaction feeTxn = new Transaction();
            feeTxn.setGlobalTransactionSequence(globalSequence);
            feeTxn.setAccountNumber(branchAccountNumber);
            feeTxn.setUserName("NeoBank");
            feeTxn.setAmount(fee);
            feeTxn.setType("Credit");
            feeTxn.setMerchant("Credit Card Transfer Commission - " + card.getAccountNumber());
            feeTxn.setDescription("Commission (" + feePercent + "%) on card transfer to " + destinationAccountNumber.trim() + " (from " + card.getAccountNumber() + ")");
            feeTxn.setSourceAccountNumber(card.getAccountNumber());
            feeTxn.setBalance(branchBalanceAfter != null ? branchBalanceAfter : fee);
            feeTxn.setStatus("Completed");
            transactionService.saveTransaction(feeTxn);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("card", card);
        result.put("transaction", cardTransaction);
        result.put("feePercent", feePercent);
        result.put("fee", fee);
        result.put("creditedAmount", creditedAmount);
        result.put("destinationBalanceAfter", destinationBalanceAfter);
        result.put("branchAccount", branchAccountService.getDepositAccountNumber());
        return result;
    }

    // Get statement
    public List<CreditCardTransaction> getStatement(Long creditCardId, LocalDateTime startDate, LocalDateTime endDate) {
        List<CreditCardTransaction> allTransactions = transactionRepository.findByCreditCardId(creditCardId);
        return allTransactions.stream()
                .filter(t -> t.getTransactionDate().isAfter(startDate) && t.getTransactionDate().isBefore(endDate))
                .toList();
    }

    // Close credit card
    @Transactional
    public boolean closeCreditCard(Long creditCardId) {
        Optional<CreditCard> cardOpt = creditCardRepository.findById(creditCardId);
        if (cardOpt.isPresent()) {
            CreditCard card = cardOpt.get();
            if (value(card.getCurrentBalance()) > 0 || value(card.getOverdueAmount()) > 0
                    || value(card.getFine()) > 0 || value(card.getPenalty()) > 0) {
                return false; // Cannot close with outstanding balance
            }
            boolean hasUnpaidBill = billRepository.findByCreditCardId(creditCardId).stream()
                    .anyMatch(bill -> !Set.of("paid", "completed").contains(
                            Objects.toString(bill.getStatus(), "").toLowerCase(Locale.ROOT))
                            && billTotalDue(bill) - value(bill.getPaidAmount()) > 0.01);
            if (hasUnpaidBill) return false;
            card.setStatus("Closed");
            card.setClosureDate(LocalDateTime.now());
            creditCardRepository.save(card);
            return true;
        }
        return false;
    }

    // Calculate overdue and penalties
    @Transactional
    public void calculateOverdueAndPenalties() {
        List<CreditCardBill> overdueBills = new ArrayList<>();
        overdueBills.addAll(billRepository.findByStatus("Generated"));
        overdueBills.addAll(billRepository.findByStatus("Partial"));
        for (CreditCardBill bill : overdueBills) {
            if (bill.getDueDate().isBefore(LocalDateTime.now())) {
                bill.setStatus("Overdue");
                Optional<CreditCard> cardOpt = creditCardRepository.findById(bill.getCreditCardId());
                if (cardOpt.isPresent()) {
                    CreditCard card = cardOpt.get();
                    long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(bill.getDueDate(), LocalDateTime.now());
                    double penalty = bill.getTotalAmount() * 0.02 * daysOverdue; // 2% per day
                    double fine = 500.0; // Fixed fine
                    card.setOverdueAmount(bill.getTotalAmount());
                    card.setPenalty(penalty);
                    card.setFine(fine);
                    bill.setPenalty(penalty);
                    bill.setFine(fine);
                    creditCardRepository.save(card);
                    billRepository.save(bill);
                }
            }
        }
    }
}
