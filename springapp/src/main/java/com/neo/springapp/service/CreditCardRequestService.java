package com.neo.springapp.service;

import com.neo.springapp.model.Card;
import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.CreditCardRequest;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.CreditCardRequestRepository;
import com.neo.springapp.repository.CreditCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.neo.springapp.model.Account;

import java.time.LocalDateTime;
import java.util.List;

@Service
@SuppressWarnings("null")
public class CreditCardRequestService {

    private final CreditCardRequestRepository repo;
    private final CreditScorePredictorService predictor;
    private final CardRepository cardRepository;
    private final CreditCardRepository creditCardRepository;
    private final AccountService accountService;

    public CreditCardRequestService(CreditCardRequestRepository repo, CreditScorePredictorService predictor, CardRepository cardRepository, CreditCardRepository creditCardRepository, AccountService accountService) {
        this.repo = repo;
        this.predictor = predictor;
        this.cardRepository = cardRepository;
        this.creditCardRepository = creditCardRepository;
        this.accountService = accountService;
    }

    public CreditCardRequest saveRequest(CreditCardRequest request, Double income) {
        // Get comprehensive prediction including PAN-based limit
        java.util.Map<String, Object> prediction = predictor.getCreditCardPrediction(request.getPan(), income);
        
        request.setPredictedCibil((Integer) prediction.get("predictedCibil"));
        request.setPanBasedLimit((Double) prediction.get("panBasedLimit"));
        request.setSuggestedLimit((Double) prediction.get("suggestedLimit"));
        request.setCardType((String) prediction.get("cardType"));
        request.setEligibility((String) prediction.get("eligibility"));
        request.setInterestRate((Double) prediction.get("interestRate"));
        request.setAnnualFee((Double) prediction.get("annualFee"));
        request.setStatus("Pending");
        request.setRequestDate(LocalDateTime.now());
        return repo.save(request);
    }

    public List<CreditCardRequest> getRequestsByStatus(String status) {
        return repo.findByStatus(status);
    }

    @Transactional
    public CreditCardRequest approveRequest(Long id, String adminName) {
        return repo.findById(id).map(req -> {
            // Ensure target account is active before issuing credit card
            Account account = accountService.getAccountByNumber(req.getAccountNumber());
            if (account == null) {
                throw new RuntimeException("Cannot approve credit card: account not found: " + req.getAccountNumber());
            }
            if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                throw new RuntimeException("Cannot approve credit card: account is not active: " + req.getAccountNumber() + " | status=" + account.getStatus());
            }
            req.setStatus("Approved");
            req.setProcessedBy(adminName);
            req.setProcessedDate(LocalDateTime.now());
            
            // Generate credit card details
            String cardNumber = generateCardNumber();
            String cvv = String.format("%03d", (int)(Math.random()*1000));
            String expiryMonth = String.format("%02d", (int)(Math.random() * 12) + 1);
            String expiryYear = String.valueOf(LocalDateTime.now().getYear() + 3);
            String expiryDate = expiryMonth + "/" + expiryYear.substring(2);
            
            // Create CreditCard entity
            CreditCard creditCard = new CreditCard();
            creditCard.setCardNumber(cardNumber);
            creditCard.setCvv(cvv);
            creditCard.setExpiryDate(expiryDate);
            creditCard.setAccountNumber(req.getAccountNumber());
            creditCard.setUserName(req.getUserName());
            creditCard.setUserEmail(req.getUserEmail());
            creditCard.setAppliedDate(req.getRequestDate());
            creditCard.setApprovalDate(LocalDateTime.now());
            // Use PAN-based limit if available, otherwise use suggested limit
            Double approvedLimit = req.getPanBasedLimit() != null ? req.getPanBasedLimit() : 
                                  (req.getSuggestedLimit() != null ? req.getSuggestedLimit() : 50000.0);
            creditCard.setApprovedLimit(approvedLimit);
            creditCard.setCurrentBalance(0.0);
            creditCard.calculateAvailableLimit();
            creditCard.calculateUsageLimit();
            creditCard.setStatus("Active");
            creditCardRepository.save(creditCard);
            
            // Also create a regular Card entry for compatibility
            Card card = new Card();
            card.setCardNumber(cardNumber);
            card.setCardType("Credit");
            card.setUserName(req.getUserName());
            card.setAccountNumber(req.getAccountNumber());
            card.setUserEmail(req.getUserEmail());
            card.setCvv(cvv);
            card.setStatus("Active");
            cardRepository.save(card);
            
            // Store last 4
            req.setLast4(cardNumber.substring(cardNumber.length()-4));
            return repo.save(req);
        }).orElse(null);
    }

    public CreditCardRequest rejectRequest(Long id, String adminName) {
        return repo.findById(id).map(req -> {
            req.setStatus("Rejected");
            req.setProcessedBy(adminName);
            req.setProcessedDate(LocalDateTime.now());
            return repo.save(req);
        }).orElse(null);
    }

    public int getPredictedCibil(String pan, Double income) {
        return predictor.predictCibilFromPanAndIncome(pan, income);
    }

    public double getSuggestedLimit(int predictedCibil, Double income) {
        return predictor.suggestLimitFromCibil(predictedCibil, income);
    }

    public java.util.Map<String, Object> getCreditCardPrediction(String pan, Double income) {
        return predictor.getCreditCardPrediction(pan, income);
    }

    public double getPanBasedLimit(String pan) {
        return predictor.getPanBasedLimit(pan);
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) sb.append((int)(Math.random()*10));
        return sb.toString();
    }
}
