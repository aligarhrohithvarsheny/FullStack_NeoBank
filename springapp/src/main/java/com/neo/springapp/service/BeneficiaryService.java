package com.neo.springapp.service;

import com.neo.springapp.model.Beneficiary;
import com.neo.springapp.repository.BeneficiaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class BeneficiaryService {

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private AccountService accountService;

    // Save beneficiary
    public Beneficiary saveBeneficiary(Beneficiary beneficiary) {
        // Verify that recipient account exists
        if (accountService.getAccountByNumber(beneficiary.getRecipientAccountNumber()) != null) {
            beneficiary.setVerified(true);
        }
        return beneficiaryRepository.save(beneficiary);
    }

    // Get all beneficiaries for a sender account
    public List<Beneficiary> getBeneficiariesBySenderAccount(String senderAccountNumber) {
        return beneficiaryRepository.findBySenderAccountNumber(senderAccountNumber);
    }

    // Get beneficiary by ID
    public Optional<Beneficiary> getBeneficiaryById(Long id) {
        return beneficiaryRepository.findById(id);
    }

    // Check if beneficiary exists
    public boolean beneficiaryExists(String senderAccountNumber, String recipientAccountNumber) {
        return beneficiaryRepository.existsBySenderAccountNumberAndRecipientAccountNumber(
            senderAccountNumber, recipientAccountNumber);
    }

    // Delete beneficiary
    public boolean deleteBeneficiary(Long id) {
        if (beneficiaryRepository.existsById(id)) {
            beneficiaryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Update beneficiary
    public Beneficiary updateBeneficiary(Beneficiary beneficiary) {
        if (beneficiaryRepository.existsById(beneficiary.getId())) {
            // Verify that recipient account exists
            if (accountService.getAccountByNumber(beneficiary.getRecipientAccountNumber()) != null) {
                beneficiary.setVerified(true);
            }
            return beneficiaryRepository.save(beneficiary);
        }
        return null;
    }

    // Get all beneficiaries (for admin)
    public List<Beneficiary> getAllBeneficiaries() {
        return beneficiaryRepository.findAll();
    }
}

