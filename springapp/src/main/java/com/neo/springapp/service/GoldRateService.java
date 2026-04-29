package com.neo.springapp.service;

import com.neo.springapp.model.GoldRate;
import com.neo.springapp.model.GoldRateHistory;
import com.neo.springapp.repository.GoldRateRepository;
import com.neo.springapp.repository.GoldRateHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GoldRateService {

    private final GoldRateRepository goldRateRepository;
    private final GoldRateHistoryRepository goldRateHistoryRepository;

    public GoldRateService(GoldRateRepository goldRateRepository, 
                          GoldRateHistoryRepository goldRateHistoryRepository) {
        this.goldRateRepository = goldRateRepository;
        this.goldRateHistoryRepository = goldRateHistoryRepository;
    }

    // Get current gold rate (latest rate)
    public GoldRate getCurrentGoldRate() {
        Optional<GoldRate> latestRate = goldRateRepository.findFirstByOrderByDateDesc();
        if (latestRate.isPresent()) {
            return latestRate.get();
        }
        // Return default rate if no rate exists
        GoldRate defaultRate = new GoldRate();
        defaultRate.setDate(LocalDate.now());
        defaultRate.setRatePerGram(11790.0); // Default rate: ₹11,790 per gram (22K Gold)
        defaultRate.setLastUpdated(LocalDateTime.now());
        return defaultRate;
    }

    // Get gold rate for a specific date
    public GoldRate getGoldRateByDate(LocalDate date) {
        Optional<GoldRate> rate = goldRateRepository.findByDate(date);
        if (rate.isPresent()) {
            return rate.get();
        }
        // If no rate for specific date, return current rate
        return getCurrentGoldRate();
    }

    // Update or create gold rate for today
    public GoldRate updateGoldRate(Double ratePerGram, String updatedBy) {
        LocalDate today = LocalDate.now();
        Optional<GoldRate> existingRate = goldRateRepository.findByDate(today);
        
        Double previousRate = null;
        GoldRate goldRate;
        if (existingRate.isPresent()) {
            goldRate = existingRate.get();
            previousRate = goldRate.getRatePerGram();
        } else {
            goldRate = new GoldRate();
            goldRate.setDate(today);
            // Get previous rate from latest rate if available
            Optional<GoldRate> latestRate = goldRateRepository.findFirstByOrderByDateDesc();
            if (latestRate.isPresent() && !latestRate.get().getDate().equals(today)) {
                previousRate = latestRate.get().getRatePerGram();
            }
        }
        
        // Save history before updating
        if (previousRate == null) {
            previousRate = 0.0; // First time setting rate
        }
        
        GoldRateHistory history = new GoldRateHistory();
        history.setRatePerGram(ratePerGram);
        history.setPreviousRate(previousRate);
        history.setChangedBy(updatedBy);
        history.setRateDate(today);
        history.setChangedAt(LocalDateTime.now());
        goldRateHistoryRepository.save(history);
        
        goldRate.setRatePerGram(ratePerGram);
        goldRate.setLastUpdated(LocalDateTime.now());
        goldRate.setUpdatedBy(updatedBy);
        
        return goldRateRepository.save(goldRate);
    }
    
    // Get gold rate history
    public List<GoldRateHistory> getGoldRateHistory() {
        return goldRateHistoryRepository.findAllByOrderByChangedAtDesc();
    }

    // Get all gold rates
    public List<GoldRate> getAllGoldRates() {
        return goldRateRepository.findAll();
    }

    // Calculate loan amount for given grams (75% of gold value)
    public Double calculateLoanAmount(Double grams) {
        GoldRate currentRate = getCurrentGoldRate();
        Double goldValue = grams * currentRate.getRatePerGram();
        return goldValue * 0.75; // 75% of gold value
    }
}

