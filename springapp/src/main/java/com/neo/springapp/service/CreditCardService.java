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

    public CreditCardService(
            CreditCardRepository creditCardRepository,
            CreditCardTransactionRepository transactionRepository,
            CreditCardBillRepository billRepository,
            AccountService accountService,
            TransactionService transactionService,
            GlobalTransactionIdGenerator transactionIdGenerator) {
        this.creditCardRepository = creditCardRepository;
        this.transactionRepository = transactionRepository;
        this.billRepository = billRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.transactionIdGenerator = transactionIdGenerator;
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
        Double accountBalanceAfter = null;
        if ("ACCOUNT".equals(paymentMethod) || "CHEQUE".equals(paymentMethod)) {
            if (debitAccountNumber == null || debitAccountNumber.isBlank()) {
                debitAccountNumber = card.getAccountNumber();
            }
            accountBalanceAfter = accountService.debitBalance(debitAccountNumber, amount);
            if (accountBalanceAfter == null) {
                throw new IllegalArgumentException("Account not found or insufficient balance");
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
        return result;
    }

    public CreditCardTransaction getChequeImageTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .filter(transaction -> transaction.getChequeImage() != null)
                .orElse(null);
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
