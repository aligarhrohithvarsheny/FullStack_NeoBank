package com.neo.springapp.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class CreditScorePredictorService {

    // PAN-based credit limit mapping (simulated based on PAN patterns)
    private static final Map<String, Double> PAN_LIMIT_MAP = new HashMap<>();
    
    static {
        // Initialize PAN-based limits (in real scenario, this would come from external credit bureau)
        // Pattern: First letter of PAN determines base limit category
        PAN_LIMIT_MAP.put("A", 500000.0);  // Individual - High limit
        PAN_LIMIT_MAP.put("B", 300000.0);  // Individual - Medium-High limit
        PAN_LIMIT_MAP.put("C", 200000.0);  // Individual - Medium limit
        PAN_LIMIT_MAP.put("D", 100000.0);  // Individual - Low-Medium limit
        PAN_LIMIT_MAP.put("E", 50000.0);   // Individual - Low limit
        PAN_LIMIT_MAP.put("F", 250000.0);  // Individual - Medium-High limit
        PAN_LIMIT_MAP.put("G", 150000.0);  // Individual - Medium limit
        PAN_LIMIT_MAP.put("H", 400000.0);  // HUF - High limit
        PAN_LIMIT_MAP.put("P", 1000000.0); // Person - Very High limit
        PAN_LIMIT_MAP.put("T", 2000000.0); // Trust - Very High limit
    }

    /**
     * Predict CIBIL score based on PAN card number and income
     * Enhanced algorithm that uses PAN structure for better prediction
     */
    public int predictCibilFromPanAndIncome(String pan, Double income) {
        if (pan == null || pan.length() < 10) {
            return 650; // Default score for invalid PAN
        }
        
        int base = 650;
        
        // Analyze PAN structure: Format is ABCDE1234F
        // First 3 letters (A-Z) - Entity type
        // Next 1 letter (A-Z) - Surname initial
        // Next 1 letter (A-Z) - First name initial
        // Next 4 digits - Sequential number
        // Last letter (A-Z) - Check digit
        
        // Extract first letter (Entity type)
        char entityType = pan.charAt(0);
        char surnameInitial = pan.length() > 3 ? pan.charAt(3) : 'A';
        char firstNameInitial = pan.length() > 4 ? pan.charAt(4) : 'A';
        
        // Calculate base score from PAN structure
        int panScore = 0;
        
        // Entity type influence
        if (entityType == 'P' || entityType == 'T' || entityType == 'H') {
            panScore += 100; // Higher score for Person, Trust, HUF
        } else if (entityType >= 'A' && entityType <= 'G') {
            panScore += 50; // Individual entities
        }
        
        // Surname and first name initial influence (simulated credit history)
        panScore += (surnameInitial % 26) * 2;
        panScore += (firstNameInitial % 26);
        
        // Extract numeric part (last 4 digits before check digit)
        if (pan.length() >= 9) {
            try {
                String numericPart = pan.substring(5, 9);
                int panNumber = Integer.parseInt(numericPart);
                panScore += (panNumber % 100); // Add variation based on PAN number
            } catch (NumberFormatException e) {
                // Ignore if not numeric
            }
        }
        
        base += panScore;
        
        // Income influence
        if (income != null) {
            if (income >= 1000000) {
                base += 150; // Very high income
            } else if (income >= 500000) {
                base += 100; // High income
            } else if (income >= 250000) {
                base += 50; // Medium income
            } else if (income >= 100000) {
                base += 25; // Low-medium income
            }
        }
        
        // Normalize score between 300-900
        if (base > 900) base = 900;
        if (base < 300) base = 300;
        
        return base;
    }

    /**
     * Get PAN-based credit limit
     * Uses PAN structure to determine base credit limit
     */
    public double getPanBasedLimit(String pan) {
        if (pan == null || pan.length() < 1) {
            return 50000.0; // Default limit
        }
        
        char firstLetter = Character.toUpperCase(pan.charAt(0));
        String entityType = String.valueOf(firstLetter);
        
        // Get base limit from PAN entity type
        Double baseLimit = PAN_LIMIT_MAP.getOrDefault(entityType, 100000.0);
        
        // Adjust based on PAN numeric part (simulated credit history)
        if (pan.length() >= 9) {
            try {
                String numericPart = pan.substring(5, 9);
                int panNumber = Integer.parseInt(numericPart);
                // Higher PAN numbers get slightly higher limits (simulated)
                double adjustment = 1.0 + ((panNumber % 1000) / 10000.0);
                baseLimit = baseLimit * adjustment;
            } catch (NumberFormatException e) {
                // Keep base limit if parsing fails
            }
        }
        
        return Math.round(baseLimit);
    }

    /**
     * Suggest credit limit based on CIBIL score and income
     * Enhanced algorithm that considers both CIBIL and PAN-based limit
     */
    public double suggestLimitFromCibil(int cibil, Double income) {
        // Calculate income-based limit
        double incomeBasedLimit = 0.0;
        if (income != null) {
            if (income >= 1000000) {
                incomeBasedLimit = Math.min(500000, income * 0.5);
            } else if (income >= 500000) {
                incomeBasedLimit = Math.min(300000, income * 0.6);
            } else if (income >= 250000) {
                incomeBasedLimit = Math.min(200000, income * 0.7);
            } else if (income >= 100000) {
                incomeBasedLimit = Math.min(100000, income * 0.8);
            } else {
                incomeBasedLimit = Math.min(50000, income * 1.0);
            }
        } else {
            incomeBasedLimit = 50000.0; // Default
        }
        
        // CIBIL-based multiplier
        double cibilMultiplier = 0.0;
        if (cibil >= 800) {
            cibilMultiplier = 1.5; // Excellent credit
        } else if (cibil >= 750) {
            cibilMultiplier = 1.2; // Very good credit
        } else if (cibil >= 700) {
            cibilMultiplier = 1.0; // Good credit
        } else if (cibil >= 650) {
            cibilMultiplier = 0.8; // Fair credit
        } else if (cibil >= 600) {
            cibilMultiplier = 0.6; // Poor credit
        } else {
            cibilMultiplier = 0.4; // Very poor credit
        }
        
        double suggestedLimit = incomeBasedLimit * cibilMultiplier;
        
        // Apply minimum and maximum limits
        suggestedLimit = Math.max(10000, Math.min(2000000, suggestedLimit));
        
        return Math.round(suggestedLimit);
    }

    /**
     * Get comprehensive credit card prediction
     * Returns prediction details including CIBIL, PAN-based limit, and final suggested limit
     */
    public Map<String, Object> getCreditCardPrediction(String pan, Double income) {
        Map<String, Object> prediction = new HashMap<>();
        
        int predictedCibil = predictCibilFromPanAndIncome(pan, income);
        double panBasedLimit = getPanBasedLimit(pan);
        double cibilBasedLimit = suggestLimitFromCibil(predictedCibil, income);
        
        // Final limit is the average of PAN-based and CIBIL-based limits
        double finalLimit = (panBasedLimit + cibilBasedLimit) / 2.0;
        
        // Determine eligibility
        String eligibility = "Approved";
        if (predictedCibil < 600) {
            eligibility = "Rejected";
        } else if (predictedCibil < 650) {
            eligibility = "Under Review";
        }
        
        // Determine card type based on limit
        String cardType = "Standard";
        if (finalLimit >= 500000) {
            cardType = "Platinum";
        } else if (finalLimit >= 300000) {
            cardType = "Gold";
        } else if (finalLimit >= 150000) {
            cardType = "Silver";
        }
        
        prediction.put("predictedCibil", predictedCibil);
        prediction.put("panBasedLimit", panBasedLimit);
        prediction.put("cibilBasedLimit", cibilBasedLimit);
        prediction.put("suggestedLimit", finalLimit);
        prediction.put("eligibility", eligibility);
        prediction.put("cardType", cardType);
        prediction.put("interestRate", getInterestRate(predictedCibil));
        prediction.put("annualFee", getAnnualFee(cardType));
        
        return prediction;
    }

    /**
     * Get interest rate based on CIBIL score
     */
    private double getInterestRate(int cibil) {
        if (cibil >= 800) return 12.0; // Excellent
        if (cibil >= 750) return 14.0; // Very good
        if (cibil >= 700) return 16.0; // Good
        if (cibil >= 650) return 18.0; // Fair
        if (cibil >= 600) return 20.0; // Poor
        return 24.0; // Very poor
    }

    /**
     * Get annual fee based on card type
     */
    private double getAnnualFee(String cardType) {
        switch (cardType) {
            case "Platinum": return 5000.0;
            case "Gold": return 2000.0;
            case "Silver": return 1000.0;
            default: return 500.0;
        }
    }
}
