package com.neo.springapp.service;

import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // Save new transaction
   
    public Transaction saveTransaction(Transaction txn) {
    txn.setId(null); // ✅ force Hibernate to INSERT instead of UPDATE
    return transactionRepository.save(txn);
}


    // Fetch all transactions
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}
