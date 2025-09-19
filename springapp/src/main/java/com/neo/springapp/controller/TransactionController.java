package com.neo.springapp.controller;

import com.neo.springapp.model.Transaction;
import com.neo.springapp.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:4200")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Add new transaction
    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction) {
        transaction.setDate(LocalDateTime.now());
        return transactionService.saveTransaction(transaction);
    }

    // Get all transactions with pagination and sorting
    @GetMapping
    public Page<Transaction> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return transactionService.getAllTransactions(page, size, sortBy, sortDir);
    }

    // Get transactions by account number with pagination
    @GetMapping("/account/{accountNumber}")
    public Page<Transaction> getTransactionsByAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.getTransactionsByAccountNumber(accountNumber, page, size);
    }

    // Get transactions by user name with pagination
    @GetMapping("/user/{userName}")
    public Page<Transaction> getTransactionsByUser(
            @PathVariable String userName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.getTransactionsByUserName(userName, page, size);
    }

    // Get transactions by type with pagination
    @GetMapping("/type/{type}")
    public Page<Transaction> getTransactionsByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.getTransactionsByType(type, page, size);
    }

    // Get transactions by status with pagination
    @GetMapping("/status/{status}")
    public Page<Transaction> getTransactionsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.getTransactionsByStatus(status, page, size);
    }

    // Get transactions by date range with pagination
    @GetMapping("/date-range")
    public Page<Transaction> getTransactionsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return transactionService.getTransactionsByDateRange(start, end, page, size);
    }

    // Get transactions by merchant with pagination
    @GetMapping("/merchant/{merchant}")
    public Page<Transaction> getTransactionsByMerchant(
            @PathVariable String merchant,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.getTransactionsByMerchant(merchant, page, size);
    }

    // Get mini statement (recent 5 transactions)
    @GetMapping("/mini-statement/{accountNumber}")
    public List<Transaction> getMiniStatement(@PathVariable String accountNumber) {
        return transactionService.getMiniStatement(accountNumber);
    }

    // Get transaction summary
    @GetMapping("/summary/{accountNumber}/{type}")
    public Object[] getTransactionSummary(@PathVariable String accountNumber, @PathVariable String type) {
        return transactionService.getTransactionSummary(accountNumber, type);
    }

    // Search transactions with multiple criteria
    @GetMapping("/search")
    public Page<Transaction> searchTransactions(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;
        
        return transactionService.searchTransactions(accountNumber, merchant, type, status, 
                                                    start, end, page, size, sortBy, sortDir);
    }
}
