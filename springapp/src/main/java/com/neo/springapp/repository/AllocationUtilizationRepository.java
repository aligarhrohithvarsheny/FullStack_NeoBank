package com.neo.springapp.repository;

import com.neo.springapp.model.AllocationUtilization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationUtilizationRepository extends JpaRepository<AllocationUtilization, Long> {

    Optional<AllocationUtilization> findByUtilizationId(String utilizationId);

    Page<AllocationUtilization> findByAllocationId(Long allocationId, Pageable pageable);

    Page<AllocationUtilization> findByAllocationIdOrderByTransactionDateDesc(Long allocationId, Pageable pageable);

    @Query("SELECT COUNT(au) FROM AllocationUtilization au WHERE au.allocationId = :allocationId AND UPPER(au.status) = 'SUCCESS'")
    long countSuccessfulByAllocationId(@Param("allocationId") Long allocationId);

    @Query("SELECT au FROM AllocationUtilization au WHERE au.allocationId = :allocationId AND UPPER(au.status) = 'SUCCESS' ORDER BY au.transactionDate DESC")
    Page<AllocationUtilization> findSuccessfulByAllocationIdOrderByTransactionDateDesc(
            @Param("allocationId") Long allocationId, Pageable pageable);

    Page<AllocationUtilization> findByAllocationIdAndTransactionType(Long allocationId, String transactionType, Pageable pageable);

    Page<AllocationUtilization> findByAllocationIdAndProductType(Long allocationId, String productType, Pageable pageable);

    @Query("SELECT au FROM AllocationUtilization au WHERE au.allocationId = :allocationId AND au.transactionDate BETWEEN :fromDate AND :toDate ORDER BY au.transactionDate DESC")
    Page<AllocationUtilization> findByAllocationIdAndDateRange(
            @Param("allocationId") Long allocationId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(au.amount), 0) FROM AllocationUtilization au WHERE au.allocationId = :allocationId AND au.transactionType = 'DEBIT' AND UPPER(au.status) = 'SUCCESS'")
    Double getTotalDebited(@Param("allocationId") Long allocationId);

    @Query("SELECT COALESCE(SUM(au.amount), 0) FROM AllocationUtilization au WHERE au.allocationId = :allocationId AND au.transactionType = 'CREDIT' AND UPPER(au.status) = 'SUCCESS'")
    Double getTotalCredited(@Param("allocationId") Long allocationId);

    @Query("SELECT COUNT(au) FROM AllocationUtilization au WHERE au.allocationId = :allocationId AND au.transactionType = 'DEBIT' AND UPPER(au.status) = 'SUCCESS'")
    Long getDebitCount(@Param("allocationId") Long allocationId);

    @Query("SELECT COUNT(au) FROM AllocationUtilization au WHERE au.allocationId = :allocationId AND au.transactionType = 'CREDIT' AND UPPER(au.status) = 'SUCCESS'")
    Long getCreditCount(@Param("allocationId") Long allocationId);

    @Query("SELECT COALESCE(SUM(au.amount), 0) FROM AllocationUtilization au WHERE au.allocationId = :allocationId AND au.productType = :productType AND au.transactionType = 'DEBIT' AND UPPER(au.status) = 'SUCCESS'")
    Double getTotalDebitedByProduct(@Param("allocationId") Long allocationId, @Param("productType") String productType);

    @Query("SELECT COALESCE(SUM(au.amount), 0) FROM AllocationUtilization au WHERE au.allocationId = :allocationId AND UPPER(au.transactionType) = UPPER(:transactionType) AND UPPER(au.productType) IN :productTypes AND UPPER(au.status) = 'SUCCESS'")
    Double sumByAllocationAndProductTypes(@Param("allocationId") Long allocationId,
                                          @Param("transactionType") String transactionType,
                                          @Param("productTypes") List<String> productTypes);

    @Query("SELECT COUNT(au) FROM AllocationUtilization au WHERE au.allocationId = :allocationId AND UPPER(au.transactionType) = UPPER(:transactionType) AND UPPER(au.productType) IN :productTypes AND UPPER(au.status) = 'SUCCESS'")
    Long countByAllocationAndProductTypes(@Param("allocationId") Long allocationId,
                                          @Param("transactionType") String transactionType,
                                          @Param("productTypes") List<String> productTypes);

    List<AllocationUtilization> findByLinkedTransactionId(String linkedTransactionId);

    Page<AllocationUtilization> findByUserAccountNumber(String accountNumber, Pageable pageable);

    List<AllocationUtilization> findByAllocationIdOrderByTransactionDateDesc(Long allocationId);

    @Query("SELECT UPPER(au.productType), UPPER(au.transactionType), COUNT(au), COALESCE(SUM(au.amount), 0) " +
           "FROM AllocationUtilization au WHERE au.allocationId = :allocationId AND UPPER(au.status) = 'SUCCESS' " +
           "GROUP BY UPPER(au.productType), UPPER(au.transactionType)")
    List<Object[]> summarizeSuccessfulActivityByProduct(@Param("allocationId") Long allocationId);
}
