package com.neo.springapp.controller;

import com.neo.springapp.model.KycRequest;
import com.neo.springapp.service.KycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kyc")
@CrossOrigin(origins = "http://localhost:4200") // Angular frontend URL
public class KycController {

    @Autowired
    private KycService service;

    // Request to Admin
    @PostMapping("/request")
    public KycRequest requestKyc(@RequestParam String panNumber, @RequestParam String name) {
        return service.requestKyc(panNumber, name);
    }

    // Approve by Admin
    @PutMapping("/approve/{panNumber}")
    public KycRequest approveKyc(@PathVariable String panNumber) {
        return service.approveKyc(panNumber);
    }

    // Get Status
    @GetMapping("/status/{panNumber}")
    public KycRequest getStatus(@PathVariable String panNumber) {
        return service.getStatus(panNumber);
    }
}
