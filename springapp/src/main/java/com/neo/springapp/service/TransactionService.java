package com.neo.springapp.service;

import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // Save new transaction
    public Transaction saveTransaction(Transaction txn) {
        txn.setId(null); // Force Hibernate to INSERT instead of UPDATE
        return transactionRepository.save(txn);
    }

    // Get all transactions with pagination and sorting
    public Page<Transaction> getAllTransactions(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return transactionRepository.findAll(pageable);
    }

    // Get transactions by account number with pagination
    public Page<Transaction> getTransactionsByAccountNumber(String accountNumber, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByAccountNumberOrderByDateDesc(accountNumber, pageable);
    }

    // Get transactions by user name with pagination
    public Page<Transaction> getTransactionsByUserName(String userName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByUserNameOrderByDateDesc(userName, pageable);
    }

    // Get transactions by type with pagination
    public Page<Transaction> getTransactionsByType(String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByTypeOrderByDateDesc(type, pageable);
    }

    // Get transactions by status with pagination
    public Page<Transaction> getTransactionsByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByStatusOrderByDateDesc(status, pageable);
    }

    // Get transactions by date range with pagination
    public Page<Transaction> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByDateBetweenOrderByDateDesc(startDate, endDate, pageable);
    }

    // Get transactions by merchant with pagination
    public Page<Transaction> getTransactionsByMerchant(String merchant, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByMerchantContainingIgnoreCaseOrderByDateDesc(merchant, pageable);
    }

    // Get transactions by account number and date range
    public Page<Transaction> getTransactionsByAccountAndDateRange(String accountNumber, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByAccountNumberAndDateBetweenOrderByDateDesc(accountNumber, startDate, endDate, pageable);
    }

    // Get mini statement (recent 5 transactions)
    public List<Transaction> getMiniStatement(String accountNumber) {
        Pageable pageable = PageRequest.of(0, 5);
        return transactionRepository.findTop5ByAccountNumberOrderByDateDesc(accountNumber, pageable);
    }

    // Get transaction summary
    public Object[] getTransactionSummary(String accountNumber, String type) {
        return transactionRepository.getTransactionSummaryByAccountAndType(accountNumber, type);
    }

    // Get transactions with custom sorting
    public Page<Transaction> getTransactionsWithSorting(String sortBy, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findAllWithSorting(sortBy, pageable);
    }

    // Search transactions with multiple criteria
    public Page<Transaction> searchTransactions(String accountNumber, String merchant, String type, String status, 
                                              LocalDateTime startDate, LocalDateTime endDate, 
                                              int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // This would need a custom repository method for complex searches
        // For now, return all transactions with pagination
        return transactionRepository.findAll(pageable);
    }
}
