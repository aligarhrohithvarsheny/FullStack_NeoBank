package com.neo.springapp.controller;

import com.neo.springapp.model.GoldRate;
import com.neo.springapp.service.GoldRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gold-rates")
@CrossOrigin(origins = "http://localhost:4200")
public class GoldRateController {

    @Autowired
    private GoldRateService goldRateService;

    // Get current gold rate
    @GetMapping("/current")
    public ResponseEntity<GoldRate> getCurrentGoldRate() {
        GoldRate currentRate = goldRateService.getCurrentGoldRate();
        return ResponseEntity.ok(currentRate);
    }

    // Get gold rate for a specific date
    @GetMapping("/date/{date}")
    public ResponseEntity<GoldRate> getGoldRateByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        GoldRate rate = goldRateService.getGoldRateByDate(localDate);
        return ResponseEntity.ok(rate);
    }

    // Update gold rate (Admin only)
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateGoldRate(
            @RequestParam Double ratePerGram,
            @RequestParam(required = false, defaultValue = "Admin") String updatedBy) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            GoldRate updatedRate = goldRateService.updateGoldRate(ratePerGram, updatedBy);
            response.put("success", true);
            response.put("message", "Gold rate updated successfully");
            response.put("goldRate", updatedRate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update gold rate: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Get all gold rates
    @GetMapping
    public ResponseEntity<List<GoldRate>> getAllGoldRates() {
        List<GoldRate> rates = goldRateService.getAllGoldRates();
        return ResponseEntity.ok(rates);
    }

    // Calculate loan amount for given grams
    @GetMapping("/calculate-loan")
    public ResponseEntity<Map<String, Object>> calculateLoanAmount(@RequestParam Double grams) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            GoldRate currentRate = goldRateService.getCurrentGoldRate();
            Double goldValue = grams * currentRate.getRatePerGram();
            Double loanAmount = goldValue * 0.75; // 75% of gold value
            
            response.put("success", true);
            response.put("grams", grams);
            response.put("goldRatePerGram", currentRate.getRatePerGram());
            response.put("goldValue", goldValue);
            response.put("loanAmount", loanAmount);
            response.put("loanPercentage", 75.0);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to calculate loan amount: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

