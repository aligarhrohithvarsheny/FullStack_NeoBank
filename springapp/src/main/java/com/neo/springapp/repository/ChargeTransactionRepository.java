package com.neo.springapp.repository;

import com.neo.springapp.entity.ChargeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

/**
 * Repository for ChargeTransaction
 * Manages persistence of charge transactions (Interest, CIBIL, Soundbox, UPI, Payment Gateway)
 */
@Repository
public interface ChargeTransactionRepository extends JpaRepository<ChargeTransaction, Long> {
    
    // Find by charge transaction ID
    @Query("SELECT c FROM ChargeTransaction c WHERE c.chargeTransactionId = :chargeTransactionId")
    Optional<ChargeTransaction> findByChargeTransactionId(@Param("chargeTransactionId") String chargeTransactionId);
    
    // Find by allocation ID
    @Query("SELECT c FROM ChargeTransaction c WHERE c.allocationId = :allocationId ORDER BY c.createdAt DESC")
    List<ChargeTransaction> findByAllocationId(@Param("allocationId") Long allocationId);
    
    // Find by allocation account ID
    @Query("SELECT c FROM ChargeTransaction c WHERE c.allocationAccountId = :accountId ORDER BY c.createdAt DESC")
    List<ChargeTransaction> findByAllocationAccountId(@Param("accountId") Long accountId);
    
    // Find by user account number
    @Query("SELECT c FROM ChargeTransaction c WHERE c.userAccountNumber = :userAccount ORDER BY c.createdAt DESC")
    List<ChargeTransaction> findByUserAccount(@Param("userAccount") String userAccount);
    
    // Find by charge type
    @Query("SELECT c FROM ChargeTransaction c WHERE c.chargeType = :chargeType ORDER BY c.createdAt DESC")
    List<ChargeTransaction> findByChargeType(@Param("chargeType") String chargeType);
    
    // Find by charge type and allocation
    @Query("SELECT c FROM ChargeTransaction c WHERE c.allocationId = :allocationId AND c.chargeType = :chargeType ORDER BY c.createdAt DESC")
    List<ChargeTransaction> findByAllocationAndChargeType(
        @Param("allocationId") Long allocationId, 
        @Param("chargeType") String chargeType
    );
    
    // Find by status
    @Query("SELECT c FROM ChargeTransaction c WHERE c.status = :status ORDER BY c.createdAt DESC")
    List<ChargeTransaction> findByStatus(@Param("status") String status);
    
    // Find pending credit transactions
    @Query("SELECT c FROM ChargeTransaction c WHERE c.creditStatus = 'PENDING_CREDIT' ORDER BY c.createdAt DESC")
    List<ChargeTransaction> findPendingCreditTransactions();
    
    // Find failed transactions
    @Query("SELECT c FROM ChargeTransaction c WHERE c.status = 'FAILED' ORDER BY c.createdAt DESC")
    List<ChargeTransaction> findFailedTransactions();
    
    // Sum charges by type for allocation
    @Query("SELECT SUM(c.chargeAmount) FROM ChargeTransaction c WHERE c.allocationId = :allocationId AND c.chargeType = :chargeType AND c.status = 'SUCCESS'")
    BigDecimal sumChargesByType(
        @Param("allocationId") Long allocationId, 
        @Param("chargeType") String chargeType
    );
    
    // Sum total charges for allocation
    @Query("SELECT SUM(c.chargeAmount) FROM ChargeTransaction c WHERE c.allocationId = :allocationId AND c.status = 'SUCCESS'")
    BigDecimal sumTotalCharges(@Param("allocationId") Long allocationId);
    
    // Sum credited amount for allocation account
    @Query("SELECT SUM(c.netCreditAmount) FROM ChargeTransaction c WHERE c.allocationAccountId = :accountId AND c.creditStatus = 'CREDITED'")
    BigDecimal sumCreditedAmount(@Param("accountId") Long accountId);
    
    // Count by credit status
    @Query("SELECT COUNT(c) FROM ChargeTransaction c WHERE c.allocationId = :allocationId AND c.creditStatus = :creditStatus")
    Long countByCreditStatus(
        @Param("allocationId") Long allocationId, 
        @Param("creditStatus") String creditStatus
    );
    
    // Find transactions within date range
    @Query("SELECT c FROM ChargeTransaction c WHERE c.allocationId = :allocationId AND c.createdAt BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC")
    List<ChargeTransaction> findByDateRange(
        @Param("allocationId") Long allocationId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Find unreconciled transactions
    @Query("SELECT c FROM ChargeTransaction c WHERE c.reconciliationStatus = 'PENDING' ORDER BY c.createdAt ASC")
    List<ChargeTransaction> findUnreconciledTransactions();
    
    // Find transactions by linked transaction ID
    @Query("SELECT c FROM ChargeTransaction c WHERE c.linkedTransactionId = :linkedTxnId")
    List<ChargeTransaction> findByLinkedTransactionId(@Param("linkedTxnId") String linkedTxnId);
    
    // Find by product type
    @Query("SELECT c FROM ChargeTransaction c WHERE c.userProductType = :productType ORDER BY c.createdAt DESC")
    List<ChargeTransaction> findByProductType(@Param("productType") String productType);
    
    // Get charge breakdown for dashboard
    @Query("SELECT NEW map(c.chargeType as chargeType, COUNT(c) as count, SUM(c.chargeAmount) as total) " +
           "FROM ChargeTransaction c WHERE c.allocationId = :allocationId AND c.status = 'SUCCESS' " +
           "GROUP BY c.chargeType")
    List<Object> getChargeBreakdown(@Param("allocationId") Long allocationId);
}
