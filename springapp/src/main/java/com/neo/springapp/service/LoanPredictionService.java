package com.neo.springapp.service;

import com.neo.springapp.model.LoanPrediction;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.LoanPredictionRepository;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class LoanPredictionService {

    @Autowired
    private LoanPredictionRepository predictionRepository;
    
    @Autowired
    private CibilService cibilService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Predict loan approval based on PAN card and loan details
     * Uses a rule-based ML approach considering CIBIL score, income, loan amount, etc.
     */
    public Map<String, Object> predictLoanApproval(String pan, String loanType, Double requestedAmount, 
                                                    Integer tenure, String accountNumber) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get CIBIL information
            Map<String, Object> cibilInfo = cibilService.getCibilInfo(pan);
            Integer cibilScore = (Integer) cibilInfo.get("cibilScore");
            String scoreCategory = (String) cibilInfo.get("scoreCategory");
            Double creditLimit = (Double) cibilInfo.get("creditLimit");
            Double interestRate = (Double) cibilInfo.get("interestRate");
            
            // Get user account information
            Account account = accountRepository.findByAccountNumber(accountNumber);
            Double currentBalance = account != null ? account.getBalance() : 0.0;
            Double monthlyIncome = account != null ? (account.getIncome() != null ? account.getIncome() / 12 : 0.0) : 0.0;
            String occupation = account != null ? account.getOccupation() : "";
            Integer age = account != null ? account.getAge() : null;
            
            // Get user information
            Optional<User> userOptional = userRepository.findByAccountNumber(accountNumber);
            String userName = userOptional.map(User::getUsername).orElse("");
            String userEmail = userOptional.map(User::getEmail).orElse("");
            
            // ML Prediction Logic
            double approvalProbability = calculateApprovalProbability(
                cibilScore, requestedAmount, monthlyIncome, currentBalance, 
                loanType, tenure, occupation, age
            );
            
            String predictionResult;
            String rejectionReason = "";
            
            // Determine prediction result based on probability and thresholds
            if (approvalProbability >= 0.75) {
                predictionResult = "Approved";
            } else if (approvalProbability >= 0.50) {
                predictionResult = "Pending Review";
                rejectionReason = "Requires manual review by admin";
            } else {
                predictionResult = "Rejected";
                rejectionReason = generateRejectionReason(cibilScore, requestedAmount, monthlyIncome, 
                                                         currentBalance, loanType, approvalProbability);
            }
            
            // Create prediction record
            LoanPrediction prediction = new LoanPrediction();
            prediction.setAccountNumber(accountNumber);
            prediction.setUserName(userName);
            prediction.setUserEmail(userEmail);
            prediction.setPan(pan);
            prediction.setLoanType(loanType);
            prediction.setRequestedAmount(requestedAmount);
            prediction.setTenure(tenure);
            prediction.setInterestRate(interestRate);
            prediction.setCibilScore(cibilScore);
            prediction.setScoreCategory(scoreCategory);
            prediction.setCreditLimit(creditLimit);
            prediction.setPredictionResult(predictionResult);
            prediction.setApprovalProbability(approvalProbability);
            prediction.setRejectionReason(rejectionReason);
            prediction.setMlModelVersion("v1.0");
            prediction.setCurrentBalance(currentBalance);
            prediction.setMonthlyIncome(monthlyIncome);
            prediction.setOccupation(occupation);
            prediction.setAge(age);
            prediction.setPredictionDate(LocalDateTime.now());
            
            // Save prediction
            LoanPrediction savedPrediction = predictionRepository.save(prediction);
            
            // Prepare response
            result.put("success", true);
            result.put("prediction", savedPrediction);
            result.put("predictionResult", predictionResult);
            result.put("approvalProbability", approvalProbability);
            result.put("approvalPercentage", Math.round(approvalProbability * 100));
            result.put("rejectionReason", rejectionReason);
            result.put("cibilScore", cibilScore);
            result.put("scoreCategory", scoreCategory);
            result.put("creditLimit", creditLimit);
            result.put("interestRate", interestRate);
            result.put("recommendations", generateRecommendations(cibilScore, approvalProbability, loanType));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error predicting loan approval: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * Calculate approval probability using ML-like rules
     */
    private double calculateApprovalProbability(Integer cibilScore, Double requestedAmount, 
                                              Double monthlyIncome, Double currentBalance,
                                              String loanType, Integer tenure, 
                                              String occupation, Integer age) {
        double probability = 0.0;
        
        // CIBIL Score weight (40%)
        if (cibilScore != null) {
            if (cibilScore >= 750) {
                probability += 0.40; // Excellent
            } else if (cibilScore >= 700) {
                probability += 0.35; // Good
            } else if (cibilScore >= 650) {
                probability += 0.25; // Fair
            } else if (cibilScore >= 600) {
                probability += 0.15; // Average
            } else {
                probability += 0.05; // Poor
            }
        }
        
        // Income to Loan Ratio (30%)
        if (monthlyIncome > 0 && requestedAmount > 0) {
            double annualIncome = monthlyIncome * 12;
            double incomeToLoanRatio = annualIncome / requestedAmount;
            
            if (incomeToLoanRatio >= 3.0) {
                probability += 0.30; // Very safe
            } else if (incomeToLoanRatio >= 2.0) {
                probability += 0.25; // Safe
            } else if (incomeToLoanRatio >= 1.5) {
                probability += 0.15; // Moderate
            } else if (incomeToLoanRatio >= 1.0) {
                probability += 0.10; // Risky
            } else {
                probability += 0.05; // Very risky
            }
        }
        
        // Current Balance (10%)
        if (currentBalance != null && requestedAmount > 0) {
            double balanceRatio = currentBalance / requestedAmount;
            if (balanceRatio >= 0.5) {
                probability += 0.10;
            } else if (balanceRatio >= 0.25) {
                probability += 0.07;
            } else if (balanceRatio >= 0.10) {
                probability += 0.05;
            } else {
                probability += 0.02;
            }
        }
        
        // Loan Type Risk (10%)
        switch (loanType) {
            case "Home Loan":
                probability += 0.10; // Secured, lower risk
                break;
            case "Car Loan":
                probability += 0.08; // Secured, moderate risk
                break;
            case "Education Loan":
                probability += 0.07; // Moderate risk
                break;
            case "Personal Loan":
                probability += 0.05; // Unsecured, higher risk
                break;
            default:
                probability += 0.05;
        }
        
        // Tenure Risk (5%)
        if (tenure != null) {
            if (tenure <= 12) {
                probability += 0.05; // Short tenure, lower risk
            } else if (tenure <= 36) {
                probability += 0.04; // Medium tenure
            } else {
                probability += 0.03; // Long tenure, higher risk
            }
        }
        
        // Occupation Stability (5%)
        if (occupation != null && !occupation.isEmpty()) {
            String lowerOccupation = occupation.toLowerCase();
            if (lowerOccupation.contains("government") || lowerOccupation.contains("public sector") ||
                lowerOccupation.contains("doctor") || lowerOccupation.contains("engineer") ||
                lowerOccupation.contains("teacher") || lowerOccupation.contains("professor")) {
                probability += 0.05; // Stable occupation
            } else if (lowerOccupation.contains("business") || lowerOccupation.contains("self-employed")) {
                probability += 0.03; // Moderate stability
            } else {
                probability += 0.02; // Variable stability
            }
        }
        
        // Age Factor (for certain loan types)
        if (age != null && loanType.equals("Education Loan")) {
            if (age >= 18 && age <= 26) {
                probability += 0.02; // Ideal age for education loan
            }
        }
        
        // Normalize probability to 0-1 range
        return Math.min(1.0, Math.max(0.0, probability));
    }
    
    /**
     * Generate rejection reason based on factors
     */
    private String generateRejectionReason(Integer cibilScore, Double requestedAmount, 
                                           Double monthlyIncome, Double currentBalance,
                                           String loanType, double probability) {
        StringBuilder reason = new StringBuilder();
        
        if (cibilScore != null && cibilScore < 600) {
            reason.append("Low CIBIL score (").append(cibilScore).append("). ");
        }
        
        if (monthlyIncome > 0 && requestedAmount > 0) {
            double annualIncome = monthlyIncome * 12;
            if (requestedAmount > annualIncome * 2) {
                reason.append("Loan amount exceeds recommended limit based on income. ");
            }
        }
        
        if (currentBalance != null && requestedAmount > 0) {
            if (currentBalance < requestedAmount * 0.1) {
                reason.append("Insufficient account balance. ");
            }
        }
        
        if (probability < 0.3) {
            reason.append("Overall risk assessment indicates high risk. ");
        }
        
        if (reason.length() == 0) {
            reason.append("Does not meet minimum eligibility criteria.");
        }
        
        return reason.toString().trim();
    }
    
    /**
     * Generate recommendations for improving approval chances
     */
    private String[] generateRecommendations(Integer cibilScore, double probability, String loanType) {
        java.util.List<String> recommendations = new java.util.ArrayList<>();
        
        if (cibilScore != null && cibilScore < 700) {
            recommendations.add("Improve your CIBIL score by paying bills on time and reducing credit utilization");
        }
        
        if (probability < 0.75) {
            recommendations.add("Consider reducing the loan amount to improve approval chances");
        }
        
        if (probability < 0.50) {
            recommendations.add("Build a stronger credit history before applying");
            recommendations.add("Maintain a higher account balance");
        }
        
        return recommendations.toArray(new String[0]);
    }
    
    /**
     * Get all predictions for a user
     */
    public java.util.List<LoanPrediction> getUserPredictions(String accountNumber) {
        return predictionRepository.findByAccountNumber(accountNumber);
    }
    
    /**
     * Get all predictions (for admin dashboard)
     */
    public java.util.List<LoanPrediction> getAllPredictions() {
        return predictionRepository.findAllOrderByPredictionDateDesc();
    }
    
    /**
     * Get predictions by date range
     */
    public java.util.List<LoanPrediction> getPredictionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return predictionRepository.findByPredictionDateRange(startDate, endDate);
    }
}

