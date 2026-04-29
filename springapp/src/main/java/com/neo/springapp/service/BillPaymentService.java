package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class BillPaymentService {

    private final BillPaymentRepository billPaymentRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardTransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final UserService userService;
    private final GlobalTransactionIdGenerator transactionIdGenerator;

    public BillPaymentService(
            BillPaymentRepository billPaymentRepository,
            CreditCardRepository creditCardRepository,
            CreditCardTransactionRepository transactionRepository,
            CardRepository cardRepository,
            AccountService accountService,
            TransactionService transactionService,
            OtpService otpService,
            EmailService emailService,
            UserService userService,
            GlobalTransactionIdGenerator transactionIdGenerator) {
        this.billPaymentRepository = billPaymentRepository;
        this.creditCardRepository = creditCardRepository;
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.userService = userService;
        this.transactionIdGenerator = transactionIdGenerator;
    }

    /**
     * Process bill payment with credit card
     * Validates card details and processes payment
     */
    @Transactional
    public BillPayment processBillPayment(BillPaymentRequest request) {
        // Validate credit card details
        Optional<CreditCard> cardOpt = creditCardRepository.findById(request.getCreditCardId());
        if (cardOpt.isEmpty()) {
            throw new RuntimeException("Credit card not found");
        }

        CreditCard card = cardOpt.get();

        // Verify card belongs to user
        if (!card.getAccountNumber().equals(request.getAccountNumber())) {
            throw new RuntimeException("Credit card does not belong to this account");
        }

        // Verify card details (card number, CVV, expiry date)
        if (!verifyCardDetails(card, request.getCardNumber(), request.getCvv(), request.getExpiryDate())) {
            throw new RuntimeException("Invalid card details. Please check card number, CVV, and expiry date.");
        }

        // Check if card is active
        if (!"Active".equals(card.getStatus()) || card.isBlocked() || card.isDeactivated()) {
            throw new RuntimeException("Credit card is not active or has been blocked");
        }

        // Check available credit limit
        if (card.getAvailableLimit() < request.getAmount()) {
            throw new RuntimeException("Insufficient credit limit. Available: ₹" + card.getAvailableLimit());
        }

        // Create bill payment record
        BillPayment billPayment = new BillPayment();
        billPayment.setAccountNumber(request.getAccountNumber());
        billPayment.setUserName(request.getUserName());
        billPayment.setUserEmail(request.getUserEmail());
        billPayment.setBillType(request.getBillType());
        billPayment.setNetworkProvider(request.getNetworkProvider());
        billPayment.setCustomerNumber(request.getCustomerNumber());
        billPayment.setAmount(request.getAmount());
        billPayment.setCreditCardId(card.getId());
        billPayment.setCardNumber(card.getMaskedCardNumber());
        billPayment.setCardLastFour(card.getCardNumber().substring(card.getCardNumber().length() - 4));
        billPayment.setStatus("Completed");
        billPayment.setPaymentDate(LocalDateTime.now());
        billPayment.setGlobalTransactionSequence(transactionIdGenerator.getNextTransactionId());
        billPayment.setTransactionId("BILL" + System.currentTimeMillis());
        billPayment.setDescription(String.format("%s Bill Payment - %s (Customer: %s)", 
            request.getBillType(), request.getNetworkProvider(), request.getCustomerNumber()));

        // Update credit card balance
        card.setCurrentBalance(card.getCurrentBalance() + request.getAmount());
        card.calculateAvailableLimit();
        card.calculateUsageLimit();
        card.setLastUsed(LocalDateTime.now());
        creditCardRepository.save(card);

        // Create credit card transaction
        CreditCardTransaction transaction = new CreditCardTransaction();
        transaction.setCreditCardId(card.getId());
        transaction.setCardNumber(card.getCardNumber());
        transaction.setAccountNumber(card.getAccountNumber());
        transaction.setUserName(card.getUserName());
        transaction.setTransactionType("Purchase");
        transaction.setAmount(request.getAmount());
        transaction.setMerchant(request.getNetworkProvider());
        transaction.setDescription(billPayment.getDescription());
        transaction.setBalanceAfter(card.getCurrentBalance());
        transaction.setStatus("Completed");
        transaction.setGlobalTransactionSequence(billPayment.getGlobalTransactionSequence());
        transactionRepository.save(transaction);

        // Save bill payment
        return billPaymentRepository.save(billPayment);
    }

    /**
     * Verify card details match
     */
    private boolean verifyCardDetails(CreditCard card, String providedCardNumber, String providedCvv, String providedExpiryDate) {
        // Remove spaces and dashes from card number
        String cleanCardNumber = providedCardNumber.replaceAll("[\\s-]", "");
        String cleanCardNumberDb = card.getCardNumber().replaceAll("[\\s-]", "");

        // Verify card number (last 4 digits should match)
        if (!cleanCardNumber.endsWith(cleanCardNumberDb.substring(cleanCardNumberDb.length() - 4))) {
            return false;
        }

        // Verify CVV
        if (!card.getCvv().equals(providedCvv)) {
            return false;
        }

        // Verify expiry date (format: MM/YY)
        String cleanExpiry = providedExpiryDate.replaceAll("[\\s/]", "");
        String cleanExpiryDb = card.getExpiryDate().replaceAll("[\\s/]", "");
        
        if (!cleanExpiry.equals(cleanExpiryDb)) {
            return false;
        }

        return true;
    }

    /**
     * Get all bill payments by account number
     */
    public List<BillPayment> getBillPaymentsByAccount(String accountNumber) {
        return billPaymentRepository.findByAccountNumber(accountNumber);
    }

    /**
     * Get all bill payments (for admin)
     */
    public List<BillPayment> getAllBillPayments() {
        return billPaymentRepository.findAllByOrderByPaymentDateDesc();
    }

    /**
     * Get bill payment by ID
     */
    public Optional<BillPayment> getBillPaymentById(Long id) {
        return billPaymentRepository.findById(id);
    }

    /**
     * Get bill payments by credit card ID
     */
    public List<BillPayment> getBillPaymentsByCreditCard(Long creditCardId) {
        return billPaymentRepository.findByCreditCardId(creditCardId);
    }

    /**
     * Get bill payments by bill type
     */
    public List<BillPayment> getBillPaymentsByType(String billType) {
        return billPaymentRepository.findByBillType(billType);
    }

    /**
     * Verify card details and send OTP for bill payment
     * Supports both debit and credit cards
     */
    public Map<String, Object> verifyCardAndSendOtp(String cardNumber, String cvv, String expiryDate, String cardholderName, String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Clean card number
            String cleanCardNumber = cardNumber.replaceAll("[\\s-]", "");
            
            // Try to find credit card first
            Optional<CreditCard> creditCardOpt = creditCardRepository.findByCardNumber(cleanCardNumber);
            Card debitCard = null;
            String cardType = null;
            String cardEmail = null;
            String cardLastFour = cleanCardNumber.substring(Math.max(0, cleanCardNumber.length() - 4));
            
            if (creditCardOpt.isPresent()) {
                CreditCard card = creditCardOpt.get();
                
                // Verify card belongs to account
                if (!card.getAccountNumber().equals(accountNumber)) {
                    response.put("success", false);
                    response.put("message", "Credit card does not belong to this account");
                    return response;
                }
                
                // Verify card details
                if (!verifyCreditCardDetails(card, cleanCardNumber, cvv, expiryDate)) {
                    response.put("success", false);
                    response.put("message", "Invalid card details. Please check card number, CVV, and expiry date.");
                    return response;
                }
                
                // Verify cardholder name matches
                if (!card.getUserName().equalsIgnoreCase(cardholderName.trim())) {
                    response.put("success", false);
                    response.put("message", "Cardholder name does not match");
                    return response;
                }
                
                // Check if card is active
                if (!"Active".equals(card.getStatus()) || card.isBlocked() || card.isDeactivated()) {
                    response.put("success", false);
                    response.put("message", "Credit card is not active or has been blocked");
                    return response;
                }
                
                cardType = "CREDIT";
                cardEmail = card.getUserEmail();
                
            } else {
                // Try to find debit card
                List<Card> cards = cardRepository.findByAccountNumber(accountNumber);
                for (Card card : cards) {
                    String dbCardNumber = card.getCardNumber().replaceAll("[\\s-]", "");
                    if (dbCardNumber.equals(cleanCardNumber)) {
                        debitCard = card;
                        break;
                    }
                }
                
                if (debitCard == null) {
                    response.put("success", false);
                    response.put("message", "Card not found. Please check your card number.");
                    return response;
                }
                
                // Verify card details
                if (!verifyDebitCardDetails(debitCard, cleanCardNumber, cvv, expiryDate)) {
                    response.put("success", false);
                    response.put("message", "Invalid card details. Please check card number, CVV, and expiry date.");
                    return response;
                }
                
                // Verify cardholder name matches
                if (!debitCard.getUserName().equalsIgnoreCase(cardholderName.trim())) {
                    response.put("success", false);
                    response.put("message", "Cardholder name does not match");
                    return response;
                }
                
                // Check if card is active
                if (!"Active".equals(debitCard.getStatus()) || debitCard.isBlocked() || debitCard.isDeactivated()) {
                    response.put("success", false);
                    response.put("message", "Debit card is not active or has been blocked");
                    return response;
                }
                
                cardType = "DEBIT";
                cardEmail = debitCard.getUserEmail();
            }
            
            // Generate and send OTP (valid for 2 minutes)
            String otp = otpService.generateOtp();
            otpService.storeOtp(cardEmail, otp);
            
            // Send OTP email
            boolean emailSent = emailService.sendOtpEmail(cardEmail, otp);
            
            if (!emailSent) {
                response.put("success", false);
                response.put("message", "Failed to send OTP. Please try again.");
                return response;
            }
            
            // Mask email (show last 9 characters)
            String maskedEmail = maskEmail(cardEmail);
            
            response.put("success", true);
            response.put("cardType", cardType);
            response.put("cardLastFour", cardLastFour);
            response.put("maskedEmail", maskedEmail);
            response.put("message", "OTP has been sent to your registered email");
            
            return response;
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error verifying card: " + e.getMessage());
            e.printStackTrace();
            return response;
        }
    }

    /**
     * Verify OTP and process bill payment
     */
    @Transactional
    public BillPayment verifyOtpAndProcessPayment(BillPaymentRequest request, String otp, String cardType) {
        // Verify OTP
        String cardEmail = null;
        if ("CREDIT".equals(cardType)) {
            Optional<CreditCard> cardOpt = creditCardRepository.findById(request.getCreditCardId());
            if (cardOpt.isEmpty()) {
                throw new RuntimeException("Credit card not found");
            }
            cardEmail = cardOpt.get().getUserEmail();
        } else {
            // For debit card, get email from user account
            Optional<com.neo.springapp.model.User> userOpt = userService.getUserByAccountNumber(request.getAccountNumber());
            if (userOpt.isEmpty()) {
                throw new RuntimeException("User not found for this account");
            }
            cardEmail = userOpt.get().getEmail();
        }
        
        if (!otpService.verifyOtp(cardEmail, otp)) {
            throw new RuntimeException("Invalid or expired OTP. Please try again.");
        }
        
        // Process payment based on card type
        if ("CREDIT".equals(cardType)) {
            return processCreditCardPayment(request);
        } else {
            return processDebitCardPayment(request);
        }
    }

    /**
     * Process credit card payment
     */
    @Transactional
    private BillPayment processCreditCardPayment(BillPaymentRequest request) {
        Optional<CreditCard> cardOpt = creditCardRepository.findById(request.getCreditCardId());
        if (cardOpt.isEmpty()) {
            throw new RuntimeException("Credit card not found");
        }

        CreditCard card = cardOpt.get();

        // Verify card belongs to user
        if (!card.getAccountNumber().equals(request.getAccountNumber())) {
            throw new RuntimeException("Credit card does not belong to this account");
        }

        // Check if card is active
        if (!"Active".equals(card.getStatus()) || card.isBlocked() || card.isDeactivated()) {
            throw new RuntimeException("Credit card is not active or has been blocked");
        }

        // Check available credit limit
        if (card.getAvailableLimit() < request.getAmount()) {
            throw new RuntimeException("Insufficient credit limit. Available: ₹" + card.getAvailableLimit());
        }

        // Create bill payment record
        BillPayment billPayment = new BillPayment();
        billPayment.setAccountNumber(request.getAccountNumber());
        billPayment.setUserName(request.getUserName());
        billPayment.setUserEmail(request.getUserEmail());
        billPayment.setBillType(request.getBillType());
        billPayment.setNetworkProvider(request.getNetworkProvider());
        billPayment.setCustomerNumber(request.getCustomerNumber());
        billPayment.setAmount(request.getAmount());
        billPayment.setCreditCardId(card.getId());
        billPayment.setCardNumber(card.getMaskedCardNumber());
        billPayment.setCardLastFour(card.getCardNumber().substring(card.getCardNumber().length() - 4));
        billPayment.setStatus("Completed");
        billPayment.setPaymentDate(LocalDateTime.now());
        billPayment.setGlobalTransactionSequence(transactionIdGenerator.getNextTransactionId());
        billPayment.setTransactionId("BILL" + System.currentTimeMillis());
        billPayment.setDescription(String.format("%s Bill Payment - %s (Customer: %s)", 
            request.getBillType(), request.getNetworkProvider(), request.getCustomerNumber()));

        // Update credit card balance
        card.setCurrentBalance(card.getCurrentBalance() + request.getAmount());
        card.calculateAvailableLimit();
        card.calculateUsageLimit();
        card.setLastUsed(LocalDateTime.now());
        creditCardRepository.save(card);

        // Create credit card transaction
        CreditCardTransaction transaction = new CreditCardTransaction();
        transaction.setCreditCardId(card.getId());
        transaction.setCardNumber(card.getCardNumber());
        transaction.setAccountNumber(card.getAccountNumber());
        transaction.setUserName(card.getUserName());
        transaction.setTransactionType("Purchase");
        transaction.setAmount(request.getAmount());
        transaction.setMerchant(request.getNetworkProvider());
        transaction.setDescription(billPayment.getDescription());
        transaction.setBalanceAfter(card.getCurrentBalance());
        transaction.setStatus("Completed");
        transaction.setGlobalTransactionSequence(billPayment.getGlobalTransactionSequence());
        transactionRepository.save(transaction);

        return billPaymentRepository.save(billPayment);
    }

    /**
     * Process debit card payment
     */
    @Transactional
    private BillPayment processDebitCardPayment(BillPaymentRequest request) {
        Account account = accountService.getAccountByNumber(request.getAccountNumber());
        if (account == null) {
            throw new RuntimeException("Account not found");
        }

        // Check account balance
        if (account.getBalance() < request.getAmount()) {
            throw new RuntimeException("Insufficient balance. Available: ₹" + account.getBalance());
        }

        // Debit amount from account
        Double newBalance = accountService.debitBalance(request.getAccountNumber(), request.getAmount());
        if (newBalance == null) {
            throw new RuntimeException("Failed to debit amount from account");
        }

        // Create bill payment record
        BillPayment billPayment = new BillPayment();
        billPayment.setAccountNumber(request.getAccountNumber());
        billPayment.setUserName(request.getUserName());
        billPayment.setUserEmail(request.getUserEmail());
        billPayment.setBillType(request.getBillType());
        billPayment.setNetworkProvider(request.getNetworkProvider());
        billPayment.setCustomerNumber(request.getCustomerNumber());
        billPayment.setAmount(request.getAmount());
        billPayment.setCardNumber("**** **** **** " + request.getCardNumber().substring(request.getCardNumber().length() - 4));
        billPayment.setCardLastFour(request.getCardNumber().substring(request.getCardNumber().length() - 4));
        billPayment.setStatus("Completed");
        billPayment.setPaymentDate(LocalDateTime.now());
        billPayment.setGlobalTransactionSequence(transactionIdGenerator.getNextTransactionId());
        billPayment.setTransactionId("BILL" + System.currentTimeMillis());
        billPayment.setDescription(String.format("%s Bill Payment - %s (Customer: %s)", 
            request.getBillType(), request.getNetworkProvider(), request.getCustomerNumber()));

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setAccountNumber(request.getAccountNumber());
        transaction.setUserName(request.getUserName());
        transaction.setMerchant(request.getNetworkProvider());
        transaction.setAmount(request.getAmount());
        transaction.setType("Debit");
        transaction.setDescription(billPayment.getDescription());
        transaction.setBalance(newBalance);
        transaction.setStatus("Completed");
        transaction.setGlobalTransactionSequence(billPayment.getGlobalTransactionSequence());
        transactionService.saveTransaction(transaction);

        return billPaymentRepository.save(billPayment);
    }

    /**
     * Verify credit card details
     */
    private boolean verifyCreditCardDetails(CreditCard card, String providedCardNumber, String providedCvv, String providedExpiryDate) {
        String cleanCardNumberDb = card.getCardNumber().replaceAll("[\\s-]", "");
        
        // Verify card number matches
        if (!providedCardNumber.equals(cleanCardNumberDb)) {
            return false;
        }

        // Verify CVV
        if (!card.getCvv().equals(providedCvv)) {
            return false;
        }

        // Verify expiry date
        String cleanExpiry = providedExpiryDate.replaceAll("[\\s/]", "");
        String cleanExpiryDb = card.getExpiryDate().replaceAll("[\\s/]", "");
        
        return cleanExpiry.equals(cleanExpiryDb);
    }

    /**
     * Verify debit card details
     */
    private boolean verifyDebitCardDetails(Card card, String providedCardNumber, String providedCvv, String providedExpiryDate) {
        String cleanCardNumberDb = card.getCardNumber().replaceAll("[\\s-]", "");
        
        // Verify card number matches
        if (!providedCardNumber.equals(cleanCardNumberDb)) {
            return false;
        }

        // Verify CVV
        if (!card.getCvv().equals(providedCvv)) {
            return false;
        }

        // Verify expiry date (format: MM/YY)
        String cleanExpiry = providedExpiryDate.replaceAll("[\\s/]", "");
        String cardExpiry = card.getExpiryDate();
        if (cardExpiry != null) {
            String cleanExpiryDb = cardExpiry.replaceAll("[\\s/]", "");
            return cleanExpiry.equals(cleanExpiryDb);
        }
        
        return false;
    }

    /**
     * Mask email to show last 9 characters
     */
    private String maskEmail(String email) {
        if (email == null || email.length() <= 9) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex == -1) {
            return "*****" + email.substring(email.length() - 9);
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 9) {
            return "*****" + localPart + domain;
        }
        return "*****" + localPart.substring(localPart.length() - 9) + domain;
    }
}
