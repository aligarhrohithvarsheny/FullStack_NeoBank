package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Base64;

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
            CurrentAccountRepository currentAccountRepository) {
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
    }

    /** Normalized cheque lookup result spanning savings (Cheque), salary (ChequeRequest), and current (BusinessChequeRequest) cheques. */
    private static class ResolvedCheque {
        String source; // SAVINGS, SALARY, CURRENT
        boolean usable; // status allows it to be used for payment
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
            r.source = "SAVINGS"; r.usable = c.isAvailable(); r.status = c.getStatus();
            r.accountNumber = c.getAccountNumber(); r.accountHolderName = c.getAccountHolderName();
            r.amount = c.getAmount(); r.savingsCheque = c;
            return r;
        }
        Optional<ChequeRequest> salaryOpt = chequeRequestRepository.findByChequeNumber(chequeNumber);
        if (salaryOpt.isPresent()) {
            ChequeRequest req = salaryOpt.get();
            SalaryAccount sal = salaryAccountRepository.findById(req.getSalaryAccountId()).orElse(null);
            ResolvedCheque r = new ResolvedCheque();
            r.source = "SALARY"; r.status = req.getStatus();
            r.usable = "APPROVED".equals(req.getStatus()) || "COMPLETED".equals(req.getStatus());
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
            r.source = "CURRENT"; r.status = req.getStatus();
            r.usable = "APPROVED".equals(req.getStatus()) || "COMPLETED".equals(req.getStatus());
            r.accountNumber = cur != null ? cur.getAccountNumber() : null;
            r.accountHolderName = req.getPayeeName();
            r.amount = req.getAmount() != null ? req.getAmount().doubleValue() : null;
            r.businessChequeRequest = req;
            return r;
        }
        return null;
    }

    private void markChequeUsed(ResolvedCheque resolved, String usedReference) {
        switch (resolved.source) {
            case "SAVINGS":
                resolved.savingsCheque.markUsed("CREDIT_CARD_BILL_PAYMENT", usedReference);
                chequeRepository.save(resolved.savingsCheque);
                break;
            case "SALARY":
                resolved.salaryChequeRequest.setStatus("CLEARED");
                resolved.salaryChequeRequest.setClearedAt(LocalDateTime.now());
                chequeRequestRepository.save(resolved.salaryChequeRequest);
                break;
            case "CURRENT":
                resolved.businessChequeRequest.setStatus("CLEARED");
                resolved.businessChequeRequest.setClearedAt(LocalDateTime.now());
                businessChequeRequestRepository.save(resolved.businessChequeRequest);
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

        // No outstanding balance means nothing to bill; do not generate a zero-amount statement
        if (card.getCurrentBalance() == null || card.getCurrentBalance() <= 0.0) {
            throw new IllegalArgumentException("No outstanding balance on this card. Statement not generated.");
        }
        
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
        
        CreditCardBill bill = new CreditCardBill();
        bill.setCreditCardId(creditCardId);
        bill.setCardNumber(card.getCardNumber());
        bill.setAccountNumber(card.getAccountNumber());
        bill.setUserName(card.getUserName());
        bill.setBillGenerationDate(LocalDateTime.now());
        bill.setDueDate(LocalDateTime.now().plusDays(21)); // 21 days from bill generation
        bill.setTotalAmount(card.getCurrentBalance());
        bill.setMinimumDue(card.getCurrentBalance() * 0.05); // 5% minimum due
        bill.setOverdueAmount(card.getOverdueAmount());
        bill.setFine(card.getFine());
        bill.setPenalty(card.getPenalty());
        bill.setStatus("Generated");
        bill.setBillingPeriod(LocalDateTime.now().getMonth().toString() + " " + LocalDateTime.now().getYear());
        
        return billRepository.save(bill);
    }

    // Get bills
    public List<CreditCardBill> getBillsByCardId(Long creditCardId) {
        return billRepository.findByCreditCardId(creditCardId);
    }

    public List<CreditCardBill> getBillsByAccount(String accountNumber) {
        return billRepository.findByAccountNumber(accountNumber);
    }

    // Pay bill
    @Transactional
    public CreditCardBill payBill(Long billId, Double amount) {
        Optional<CreditCardBill> billOpt = billRepository.findById(billId);
        if (billOpt.isEmpty()) {
            return null;
        }
        
        CreditCardBill bill = billOpt.get();
        Double paidAmount = bill.getPaidAmount() != null ? bill.getPaidAmount() : 0.0;
        paidAmount += amount;
        bill.setPaidAmount(paidAmount);
        bill.setPaidDate(LocalDateTime.now());
        
        if (paidAmount >= bill.getTotalAmount()) {
            bill.setStatus("Paid");
        } else {
            bill.setStatus("Partial");
        }
        
        // Update credit card balance
        Optional<CreditCard> cardOpt = creditCardRepository.findById(bill.getCreditCardId());
        if (cardOpt.isPresent()) {
            CreditCard card = cardOpt.get();
            card.setCurrentBalance(Math.max(0, card.getCurrentBalance() - amount));
            card.setLastPaidDate(LocalDateTime.now());
            card.setOverdueAmount(0.0);
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
            payment.setAmount(amount);
            payment.setDescription("Bill Payment");
            payment.setBalanceAfter(card.getCurrentBalance());
            transactionRepository.save(payment);
        }
        
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
        double billDue = (bill.getTotalAmount() == null ? 0.0 : bill.getTotalAmount())
                + (bill.getFine() == null ? 0.0 : bill.getFine())
                + (bill.getPenalty() == null ? 0.0 : bill.getPenalty()) - paidAmount;
        double amount = request.getAmount();
        if (amount > billDue + 0.01) {
            throw new IllegalArgumentException("Payment cannot exceed the outstanding bill amount of ₹"
                    + String.format("%.2f", Math.max(0, billDue)));
        }

        String debitAccountNumber = request.getDebitAccountNumber();
        ResolvedCheque resolvedCheque = null;
        if ("CHEQUE".equals(paymentMethod)) {
            if (debitAccountNumber == null || debitAccountNumber.isBlank()) {
                debitAccountNumber = card.getAccountNumber();
            }
            resolvedCheque = resolveChequeForPayment(request.getChequeNumber().trim());
            if (resolvedCheque == null) {
                throw new IllegalArgumentException("Cheque number not found");
            }
            if (!debitAccountNumber.equals(resolvedCheque.accountNumber)) {
                throw new IllegalArgumentException("This cheque belongs to another account and cannot be used here");
            }
            if (!resolvedCheque.usable) {
                throw new IllegalArgumentException("Cheque is already used, drawn, cancelled, or bounced. Status: " + resolvedCheque.status);
            }
            // Only the amount entered by the admin/user is locked against the cheque, not the cheque's own face amount
            if (resolvedCheque.savingsCheque != null) resolvedCheque.savingsCheque.setAmount(amount);
        }

        Double accountBalanceAfter = null;
        if ("ACCOUNT".equals(paymentMethod) || "CHEQUE".equals(paymentMethod)) {
            if (debitAccountNumber == null || debitAccountNumber.isBlank()) {
                debitAccountNumber = card.getAccountNumber();
            }
            accountBalanceAfter = debitAnyAccount(debitAccountNumber, amount);
            if (accountBalanceAfter == null) {
                throw new IllegalArgumentException("Account not found or insufficient balance");
            }
            if (resolvedCheque != null) {
                markChequeUsed(resolvedCheque, "CARD-" + card.getId() + "-BILL-" + billId);
            }
        }

        double cardBalanceAfter = Math.max(0.0, (card.getCurrentBalance() == null ? 0.0 : card.getCurrentBalance()) - amount);
        card.setCurrentBalance(cardBalanceAfter);
        card.setLastPaidDate(LocalDateTime.now());
        card.setOverdueAmount(0.0);
        card.setFine(0.0);
        card.setPenalty(0.0);
        card.calculateAvailableLimit();
        card.calculateUsageLimit();
        creditCardRepository.save(card);

        bill.setPaidAmount(paidAmount + amount);
        bill.setPaidDate(LocalDateTime.now());
        bill.setStatus(bill.getPaidAmount() >= bill.getTotalAmount() ? "Paid" : "Partial");
        billRepository.save(bill);

        Long globalSequence = transactionIdGenerator.getNextTransactionId();
        CreditCardTransaction cardTransaction = new CreditCardTransaction();
        cardTransaction.setGlobalTransactionSequence(globalSequence);
        cardTransaction.setCreditCardId(card.getId());
        cardTransaction.setCardNumber(card.getCardNumber());
        cardTransaction.setAccountNumber(card.getAccountNumber());
        cardTransaction.setUserName(card.getUserName());
        cardTransaction.setTransactionType("Payment");
        cardTransaction.setPaymentMethod(paymentMethod);
        cardTransaction.setChequeNumber(request.getChequeNumber());
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
                + (request.getChequeNumber() == null ? "" : " - Cheque: " + request.getChequeNumber()));
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
        boolean ownerMatches = debitAccountNumber != null && debitAccountNumber.trim().equals(resolved.accountNumber);
        boolean valid = resolved.usable && ownerMatches;
        result.put("valid", valid);
        result.put("chequeNumber", chequeNumber);
        result.put("accountNumber", resolved.accountNumber);
        result.put("accountHolderName", resolved.accountHolderName);
        result.put("status", resolved.status);
        result.put("source", resolved.source);
        result.put("ownerMatches", ownerMatches);
        Double availableBalance = null;
        if (resolved.accountNumber != null) {
            Account savingsAcc = accountService.getAccountByNumber(resolved.accountNumber);
            if (savingsAcc != null) {
                availableBalance = savingsAcc.getBalance();
            } else {
                CurrentAccount cur = currentAccountRepository.findByAccountNumber(resolved.accountNumber).orElse(null);
                if (cur != null) availableBalance = cur.getBalance();
                else {
                    SalaryAccount sal = salaryAccountRepository.findByAccountNumber(resolved.accountNumber);
                    if (sal != null) availableBalance = sal.getBalance();
                }
            }
        }
        result.put("availableBalance", availableBalance != null ? availableBalance : 0.0);
        if (!ownerMatches) result.put("message", "This cheque belongs to another account");
        else if (!resolved.usable) result.put("message", "Cheque is already used or unavailable. Status: " + resolved.status);
        else result.put("message", "Cheque is valid and unused");
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
            if (card.getCurrentBalance() > 0) {
                return false; // Cannot close with outstanding balance
            }
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
        List<CreditCardBill> overdueBills = billRepository.findByStatus("Overdue");
        for (CreditCardBill bill : overdueBills) {
            if (bill.getDueDate().isBefore(LocalDateTime.now()) && bill.getStatus().equals("Generated")) {
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
