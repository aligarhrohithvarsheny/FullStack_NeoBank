package com.neo.springapp.repository;

import com.neo.springapp.model.TransferRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<TransferRecord, Long> {
    
    // JPQL Query to find transfers by sender account number with pagination
    @Query("SELECT t FROM TransferRecord t WHERE t.senderAccountNumber = :accountNumber ORDER BY t.date DESC")
    Page<TransferRecord> findBySenderAccountNumberOrderByDateDesc(@Param("accountNumber") String accountNumber, Pageable pageable);
    
    // JPQL Query to find transfers by recipient account number with pagination
    @Query("SELECT t FROM TransferRecord t WHERE t.recipientAccountNumber = :accountNumber ORDER BY t.date DESC")
    Page<TransferRecord> findByRecipientAccountNumberOrderByDateDesc(@Param("accountNumber") String accountNumber, Pageable pageable);
    
    // JPQL Query to find transfers by sender name with pagination
    @Query("SELECT t FROM TransferRecord t WHERE LOWER(t.senderName) LIKE LOWER(CONCAT('%', :senderName, '%')) ORDER BY t.date DESC")
    Page<TransferRecord> findBySenderNameContainingIgnoreCaseOrderByDateDesc(@Param("senderName") String senderName, Pageable pageable);
    
    // JPQL Query to find transfers by recipient name with pagination
    @Query("SELECT t FROM TransferRecord t WHERE LOWER(t.recipientName) LIKE LOWER(CONCAT('%', :recipientName, '%')) ORDER BY t.date DESC")
    Page<TransferRecord> findByRecipientNameContainingIgnoreCaseOrderByDateDesc(@Param("recipientName") String recipientName, Pageable pageable);
    
    // JPQL Query to find transfers by transfer type with pagination
    @Query("SELECT t FROM TransferRecord t WHERE t.transferType = :transferType ORDER BY t.date DESC")
    Page<TransferRecord> findByTransferTypeOrderByDateDesc(@Param("transferType") TransferRecord.TransferType transferType, Pageable pageable);
    
    // JPQL Query to find transfers by status with pagination
    @Query("SELECT t FROM TransferRecord t WHERE t.status = :status ORDER BY t.date DESC")
    Page<TransferRecord> findByStatusOrderByDateDesc(@Param("status") String status, Pageable pageable);
    
    // JPQL Query to find transfers by date range with pagination
    @Query("SELECT t FROM TransferRecord t WHERE t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    Page<TransferRecord> findByDateBetweenOrderByDateDesc(@Param("startDate") LocalDateTime startDate, 
                                                        @Param("endDate") LocalDateTime endDate, 
                                                        Pageable pageable);
    
    // JPQL Query to find transfers by amount range with pagination
    @Query("SELECT t FROM TransferRecord t WHERE t.amount BETWEEN :minAmount AND :maxAmount ORDER BY t.date DESC")
    Page<TransferRecord> findByAmountBetweenOrderByDateDesc(@Param("minAmount") Double minAmount, 
                                                           @Param("maxAmount") Double maxAmount, 
                                                           Pageable pageable);
    
    // JPQL Query to find transfers by IFSC code with pagination
    @Query("SELECT t FROM TransferRecord t WHERE t.ifsc = :ifsc ORDER BY t.date DESC")
    Page<TransferRecord> findByIfscOrderByDateDesc(@Param("ifsc") String ifsc, Pageable pageable);
    
    // JPQL Query to get transfer summary by account
    @Query("SELECT COUNT(t), SUM(t.amount) FROM TransferRecord t WHERE t.senderAccountNumber = :accountNumber")
    Object[] getTransferSummaryBySenderAccount(@Param("accountNumber") String accountNumber);
    
    // JPQL Query to get recent transfers (mini statement)
    @Query("SELECT t FROM TransferRecord t WHERE t.senderAccountNumber = :accountNumber ORDER BY t.date DESC")
    List<TransferRecord> findTop5BySenderAccountNumberOrderByDateDesc(@Param("accountNumber") String accountNumber, Pageable pageable);
    
    // JPQL Query to find all transfers with sorting options
    @Query("SELECT t FROM TransferRecord t ORDER BY " +
           "CASE WHEN :sortBy = 'date' THEN t.date END DESC, " +
           "CASE WHEN :sortBy = 'amount' THEN t.amount END DESC, " +
           "CASE WHEN :sortBy = 'senderName' THEN t.senderName END ASC")
    Page<TransferRecord> findAllWithSorting(@Param("sortBy") String sortBy, Pageable pageable);
}
