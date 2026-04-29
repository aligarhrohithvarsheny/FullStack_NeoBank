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

    // Enhanced JPQL Queries for Advanced Search and Filtering
    
    // Multi-field search with JPQL
    @Query("SELECT t FROM TransferRecord t WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(t.senderName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.recipientName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "t.senderAccountNumber LIKE CONCAT('%', :searchTerm, '%') OR " +
           "t.recipientAccountNumber LIKE CONCAT('%', :searchTerm, '%') OR " +
           "t.ifsc LIKE CONCAT('%', :searchTerm, '%') OR " +
           "t.phone LIKE CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY t.date DESC")
    Page<TransferRecord> searchTransfers(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Advanced filtering with multiple criteria
    @Query("SELECT t FROM TransferRecord t WHERE " +
           "(:senderAccountNumber IS NULL OR t.senderAccountNumber = :senderAccountNumber) AND " +
           "(:recipientAccountNumber IS NULL OR t.recipientAccountNumber = :recipientAccountNumber) AND " +
           "(:transferType IS NULL OR t.transferType = :transferType) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
           "(:startDate IS NULL OR t.date >= :startDate) AND " +
           "(:endDate IS NULL OR t.date <= :endDate) " +
           "ORDER BY t.date DESC")
    Page<TransferRecord> findTransfersWithFilters(
            @Param("senderAccountNumber") String senderAccountNumber,
            @Param("recipientAccountNumber") String recipientAccountNumber,
            @Param("transferType") TransferRecord.TransferType transferType,
            @Param("status") String status,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Statistics queries
    @Query("SELECT COUNT(t) FROM TransferRecord t WHERE t.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(t) FROM TransferRecord t WHERE t.transferType = :transferType")
    Long countByTransferType(@Param("transferType") TransferRecord.TransferType transferType);

    @Query("SELECT SUM(t.amount) FROM TransferRecord t WHERE t.senderAccountNumber = :accountNumber AND t.status = 'Completed'")
    Double getTotalTransferAmountBySender(@Param("accountNumber") String accountNumber);

    @Query("SELECT AVG(t.amount) FROM TransferRecord t WHERE t.status = 'Completed'")
    Double getAverageTransferAmount();

    @Query("SELECT COUNT(t), SUM(t.amount) FROM TransferRecord t WHERE t.date >= :startDate AND t.date <= :endDate")
    Object[] getTransferStatisticsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Recent transfers for dashboard
    @Query("SELECT t FROM TransferRecord t WHERE t.senderAccountNumber = :accountNumber ORDER BY t.date DESC")
    List<TransferRecord> findRecentTransfersBySender(@Param("accountNumber") String accountNumber, Pageable pageable);

    @Query("SELECT t FROM TransferRecord t WHERE t.recipientAccountNumber = :accountNumber ORDER BY t.date DESC")
    List<TransferRecord> findRecentTransfersByRecipient(@Param("accountNumber") String accountNumber, Pageable pageable);

    // Pending transfers for processing
    @Query("SELECT t FROM TransferRecord t WHERE t.status = 'Pending' ORDER BY t.date ASC")
    List<TransferRecord> findPendingTransfersForProcessing(Pageable pageable);

    // Failed transfers for retry
    @Query("SELECT t FROM TransferRecord t WHERE t.status = 'Failed' ORDER BY t.date DESC")
    List<TransferRecord> findFailedTransfersForRetry(Pageable pageable);

    // Transfer volume by month
    @Query("SELECT YEAR(t.date) as year, MONTH(t.date) as month, COUNT(t) as count, SUM(t.amount) as total " +
           "FROM TransferRecord t WHERE t.status = 'Completed' " +
           "GROUP BY YEAR(t.date), MONTH(t.date) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getTransferVolumeByMonth();

    // Top transfer recipients
    @Query("SELECT t.recipientName, t.recipientAccountNumber, COUNT(t) as transferCount, SUM(t.amount) as totalAmount " +
           "FROM TransferRecord t WHERE t.senderAccountNumber = :accountNumber AND t.status = 'Completed' " +
           "GROUP BY t.recipientName, t.recipientAccountNumber " +
           "ORDER BY totalAmount DESC")
    List<Object[]> getTopRecipientsBySender(@Param("accountNumber") String accountNumber, Pageable pageable);
}
