package com.neo.springapp.service;

import com.neo.springapp.model.KycRequest;
import com.neo.springapp.repository.KycRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KycService {

    @Autowired
    private KycRepository repository;

    // Save new KYC request
    public KycRequest requestKyc(String panNumber, String name) {
        KycRequest existing = repository.findByPanNumber(panNumber);
        if (existing != null) {
            return existing; // already exists
        }
        KycRequest request = new KycRequest(panNumber, name, "Pending Approval");
        return repository.save(request);
    }

    // Approve KYC
    public KycRequest approveKyc(String panNumber) {
        KycRequest request = repository.findByPanNumber(panNumber);
        if (request != null) {
            request.setStatus("Approved ✅");
            return repository.save(request);
        }
        return null;
    }

    // Check KYC status
    public KycRequest getStatus(String panNumber) {
        return repository.findByPanNumber(panNumber);
    }
}
