package com.neo.springapp.controller;

import com.neo.springapp.model.CustomerNominee;
import com.neo.springapp.repository.CustomerNomineeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/customer-nominees")
public class CustomerNomineeController {

    private final CustomerNomineeRepository repository;

    public CustomerNomineeController(CustomerNomineeRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/account/{accountNumber}")
    public List<CustomerNominee> byAccount(@PathVariable String accountNumber) {
        return repository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    @PostMapping
    public ResponseEntity<CustomerNominee> add(@RequestBody CustomerNominee nominee) {
        if (nominee == null || nominee.getAccountNumber() == null || nominee.getAccountNumber().isBlank()
                || nominee.getNomineeName() == null || nominee.getNomineeName().isBlank()
                || nominee.getRelationship() == null || nominee.getRelationship().isBlank()
                || nominee.getSharePercentage() == null || !Double.isFinite(nominee.getSharePercentage())
                || nominee.getSharePercentage() <= 0 || nominee.getSharePercentage() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Account, nominee name, relationship, and a share between 0 and 100 are required.");
        }
        nominee.setId(null);
        nominee.setCreatedAt(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(nominee));
    }
}
