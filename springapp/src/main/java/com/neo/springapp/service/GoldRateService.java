package com.neo.springapp.service;

import com.neo.springapp.model.GoldRate;
import com.neo.springapp.repository.GoldRateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GoldRateService {

    private final GoldRateRepository goldRateRepository;

    public GoldRateService(GoldRateRepository goldRateRepository) {
        this.goldRateRepository = goldRateRepository;
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
        
        GoldRate goldRate;
        if (existingRate.isPresent()) {
            goldRate = existingRate.get();
        } else {
            goldRate = new GoldRate();
            goldRate.setDate(today);
        }
        
        goldRate.setRatePerGram(ratePerGram);
        goldRate.setLastUpdated(LocalDateTime.now());
        goldRate.setUpdatedBy(updatedBy);
        
        return goldRateRepository.save(goldRate);
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

