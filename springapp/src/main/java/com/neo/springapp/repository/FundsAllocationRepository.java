package com.neo.springapp.repository;

import com.neo.springapp.model.FundsAllocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FundsAllocationRepository extends JpaRepository<FundsAllocation, Long> {

    Optional<FundsAllocation> findByAllocationId(String allocationId);

    Page<FundsAllocation> findByManagerId(Long managerId, Pageable pageable);

    Page<FundsAllocation> findByStatus(String status, Pageable pageable);

    Page<FundsAllocation> findByAllocatedByAdminId(Long hodAdminId, Pageable pageable);

    Page<FundsAllocation> findByManagerIdAndStatus(Long managerId, String status, Pageable pageable);

    List<FundsAllocation> findByValidTillLessThanAndStatusEquals(LocalDate date, String status);

    @Query("SELECT fa FROM FundsAllocation fa WHERE fa.managerId = :managerId AND fa.status = 'ACTIVE' AND fa.validTill >= CURRENT_DATE")
    List<FundsAllocation> findActiveAllocationsForManager(@Param("managerId") Long managerId);

    @Query("SELECT fa FROM FundsAllocation fa WHERE fa.allocationDate BETWEEN :fromDate AND :toDate")
    Page<FundsAllocation> findByAllocationDateBetween(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Query("SELECT fa FROM FundsAllocation fa WHERE fa.managerId = :managerId AND fa.allocationDate BETWEEN :fromDate AND :toDate")
    List<FundsAllocation> findByManagerIdAndDateRange(
            @Param("managerId") Long managerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT COALESCE(SUM(fa.allocatedAmount), 0) FROM FundsAllocation fa WHERE fa.managerId = :managerId AND fa.status = 'ACTIVE'")
    Double getTotalActiveAllocationForManager(@Param("managerId") Long managerId);

    List<FundsAllocation> findByBranchNameContainingIgnoreCase(String branchName);

    @Query("SELECT fa FROM FundsAllocation fa WHERE fa.productTypes LIKE %:productType% AND fa.status = 'ACTIVE'")
    List<FundsAllocation> findByProductType(@Param("productType") String productType);
}
