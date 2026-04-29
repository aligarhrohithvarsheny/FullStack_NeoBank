package com.neo.springapp.controller;

import com.neo.springapp.model.Beneficiary;
import com.neo.springapp.service.BeneficiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/beneficiaries")
@CrossOrigin(origins = "*")
public class BeneficiaryController {

    @Autowired
    private BeneficiaryService beneficiaryService;

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getBeneficiariesByAccount(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        List<Beneficiary> beneficiaries = beneficiaryService.getBeneficiariesBySenderAccount(accountNumber);
        response.put("success", true);
        response.put("data", beneficiaries);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBeneficiaryById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        return beneficiaryService.getBeneficiaryById(id)
                .map(b -> {
                    response.put("success", true);
                    response.put("data", b);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("success", false);
                    response.put("message", "Beneficiary not found");
                    return ResponseEntity.ok(response);
                });
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addBeneficiary(@RequestBody Beneficiary beneficiary) {
        Map<String, Object> response = new HashMap<>();
        if (beneficiaryService.beneficiaryExists(beneficiary.getSenderAccountNumber(), beneficiary.getRecipientAccountNumber())) {
            response.put("success", false);
            response.put("message", "Beneficiary already exists");
            return ResponseEntity.ok(response);
        }
        Beneficiary saved = beneficiaryService.saveBeneficiary(beneficiary);
        response.put("success", true);
        response.put("data", saved);
        response.put("message", "Beneficiary added successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateBeneficiary(@PathVariable Long id, @RequestBody Beneficiary beneficiary) {
        Map<String, Object> response = new HashMap<>();
        beneficiary.setId(id);
        Beneficiary updated = beneficiaryService.updateBeneficiary(beneficiary);
        if (updated != null) {
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "Beneficiary updated successfully");
        } else {
            response.put("success", false);
            response.put("message", "Beneficiary not found");
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBeneficiary(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        if (beneficiaryService.deleteBeneficiary(id)) {
            response.put("success", true);
            response.put("message", "Beneficiary deleted successfully");
        } else {
            response.put("success", false);
            response.put("message", "Beneficiary not found");
        }
        return ResponseEntity.ok(response);
    }
}
