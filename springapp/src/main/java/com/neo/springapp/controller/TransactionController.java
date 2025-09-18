package com.neo.springapp.controller;

import com.neo.springapp.model.Transaction;
import com.neo.springapp.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:4200") // ✅ Angular runs here
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Add new transaction (after transfer fund)
    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction) {
        transaction.setDate(LocalDateTime.now()); // set timestamp
        return transactionService.saveTransaction(transaction);
    }

    // Get all transactions
    @GetMapping
    public List<Transaction> getTransactions() {
        return transactionService.getAllTransactions();
    }
}
