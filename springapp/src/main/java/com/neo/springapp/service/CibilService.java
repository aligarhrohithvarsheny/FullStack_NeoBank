package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CibilService {

    @Autowired
    private AccountService accountService;

    /**
     * Calculate CIBIL score based on PAN and account information
     * This is a simulated CIBIL calculation for demo purposes
     * In real scenario, this would call CIBIL API
     */
    public Integer calculateCibilScore(String pan) {
        if (pan == null || pan.trim().isEmpty()) {
            return 0;
        }

        // Get account details by PAN
        Account account = accountService.getAccountByPan(pan);
        if (account == null) {
            // If account not found, return a default low score
            return 400;
        }

        // Simulate CIBIL score calculation based on various factors
        int baseScore = 500; // Base score
        
        // Factor 1: Account balance (higher balance = better score)
        if (account.getBalance() != null) {
            if (account.getBalance() >= 500000) {
                baseScore += 150;
            } else if (account.getBalance() >= 200000) {
                baseScore += 100;
            } else if (account.getBalance() >= 100000) {
                baseScore += 50;
            } else if (account.getBalance() >= 50000) {
                baseScore += 25;
            }
        }

        // Factor 2: Income (higher income = better score)
        if (account.getIncome() != null) {
            if (account.getIncome() >= 1000000) {
                baseScore += 100;
            } else if (account.getIncome() >= 500000) {
                baseScore += 75;
            } else if (account.getIncome() >= 300000) {
                baseScore += 50;
            } else if (account.getIncome() >= 200000) {
                baseScore += 25;
            }
        }

        // Factor 3: Account age (older account = better score)
        if (account.getCreatedAt() != null) {
            long accountAgeMonths = java.time.temporal.ChronoUnit.MONTHS.between(
                account.getCreatedAt(), 
                java.time.LocalDateTime.now()
            );
            if (accountAgeMonths >= 24) {
                baseScore += 50;
            } else if (accountAgeMonths >= 12) {
                baseScore += 30;
            } else if (accountAgeMonths >= 6) {
                baseScore += 15;
            }
        }

        // Factor 4: KYC verification status
        if (account.isKycVerified()) {
            baseScore += 50;
        }

        // Factor 5: Account status
        if ("ACTIVE".equals(account.getStatus())) {
            baseScore += 25;
        }

        // Add some randomness based on PAN hash for consistency
        int panHash = Math.abs(pan.hashCode() % 100);
        baseScore += panHash;

        // Ensure score is between 300 and 900 (CIBIL range)
        baseScore = Math.max(300, Math.min(900, baseScore));

        return baseScore;
    }

    /**
     * Calculate interest rate based on CIBIL score
     */
    public Double calculateInterestRate(Integer cibilScore) {
        if (cibilScore == null || cibilScore < 300) {
            return 18.0; // High interest for low/no score
        }

        if (cibilScore >= 750) {
            return 8.5; // Excellent score - best rate
        } else if (cibilScore >= 700) {
            return 10.0; // Good score
        } else if (cibilScore >= 650) {
            return 12.0; // Fair score
        } else if (cibilScore >= 600) {
            return 14.0; // Average score
        } else if (cibilScore >= 550) {
            return 16.0; // Below average
        } else {
            return 18.0; // Poor score - high interest
        }
    }

    /**
     * Calculate available credit limit based on CIBIL score and income
     */
    public Double calculateCreditLimit(Integer cibilScore, Double income, Double currentBalance) {
        if (cibilScore == null || cibilScore < 300) {
            return 0.0; // No credit limit for low score
        }

        // Base credit limit calculation
        double baseLimit = 0.0;

        // Factor 1: Income-based limit (typically 10-20x monthly income)
        if (income != null && income > 0) {
            double monthlyIncome = income / 12;
            if (cibilScore >= 750) {
                baseLimit = monthlyIncome * 20; // 20x for excellent score
            } else if (cibilScore >= 700) {
                baseLimit = monthlyIncome * 15; // 15x for good score
            } else if (cibilScore >= 650) {
                baseLimit = monthlyIncome * 12; // 12x for fair score
            } else if (cibilScore >= 600) {
                baseLimit = monthlyIncome * 10; // 10x for average score
            } else if (cibilScore >= 550) {
                baseLimit = monthlyIncome * 8; // 8x for below average
            } else {
                baseLimit = monthlyIncome * 5; // 5x for poor score
            }
        }

        // Factor 2: Current balance bonus (up to 50% of balance)
        if (currentBalance != null && currentBalance > 0) {
            double balanceBonus = currentBalance * 0.5;
            baseLimit += balanceBonus;
        }

        // Factor 3: CIBIL score multiplier
        double cibilMultiplier = 1.0;
        if (cibilScore >= 750) {
            cibilMultiplier = 1.5;
        } else if (cibilScore >= 700) {
            cibilMultiplier = 1.3;
        } else if (cibilScore >= 650) {
            cibilMultiplier = 1.1;
        } else if (cibilScore < 550) {
            cibilMultiplier = 0.7;
        }

        baseLimit = baseLimit * cibilMultiplier;

        // Set minimum and maximum limits
        double minLimit = 50000; // Minimum ₹50,000
        double maxLimit = 5000000; // Maximum ₹50,00,000

        baseLimit = Math.max(minLimit, Math.min(maxLimit, baseLimit));

        // Round to nearest 10,000
        baseLimit = Math.round(baseLimit / 10000) * 10000;

        return baseLimit;
    }

    /**
     * Get CIBIL information (score, interest rate, credit limit) for a PAN
     */
    public java.util.Map<String, Object> getCibilInfo(String pan) {
        java.util.Map<String, Object> cibilInfo = new java.util.HashMap<>();
        
        Integer cibilScore = calculateCibilScore(pan);
        Double interestRate = calculateInterestRate(cibilScore);
        
        // Get account details for credit limit calculation
        Account account = accountService.getAccountByPan(pan);
        Double income = account != null ? account.getIncome() : 0.0;
        Double balance = account != null ? account.getBalance() : 0.0;
        Double creditLimit = calculateCreditLimit(cibilScore, income, balance);
        
        cibilInfo.put("cibilScore", cibilScore);
        cibilInfo.put("interestRate", interestRate);
        cibilInfo.put("creditLimit", creditLimit);
        cibilInfo.put("pan", pan);
        
        // Add score category
        String scoreCategory;
        if (cibilScore >= 750) {
            scoreCategory = "Excellent";
        } else if (cibilScore >= 700) {
            scoreCategory = "Good";
        } else if (cibilScore >= 650) {
            scoreCategory = "Fair";
        } else if (cibilScore >= 600) {
            scoreCategory = "Average";
        } else if (cibilScore >= 550) {
            scoreCategory = "Below Average";
        } else {
            scoreCategory = "Poor";
        }
        cibilInfo.put("scoreCategory", scoreCategory);
        
        return cibilInfo;
    }
}





