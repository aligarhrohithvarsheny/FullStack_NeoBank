package com.neo.springapp.repository;

import com.neo.springapp.model.AccountTracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountTrackingRepository extends JpaRepository<AccountTracking, Long> {
    
    Optional<AccountTracking> findByTrackingId(String trackingId);
    
    Optional<AccountTracking> findByAadharNumber(String aadharNumber);
    
    Optional<AccountTracking> findByMobileNumber(String mobileNumber);
    
    @Query("SELECT at FROM AccountTracking at WHERE at.aadharNumber = :aadharNumber AND at.mobileNumber = :mobileNumber")
    Optional<AccountTracking> findByAadharNumberAndMobileNumber(@Param("aadharNumber") String aadharNumber, @Param("mobileNumber") String mobileNumber);
    
    List<AccountTracking> findByStatus(String status);
    
    Page<AccountTracking> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT at FROM AccountTracking at WHERE at.user.id = :userId")
    Optional<AccountTracking> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT at FROM AccountTracking at WHERE " +
           "LOWER(at.trackingId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(at.aadharNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(at.mobileNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(at.user.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<AccountTracking> searchTracking(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT at FROM AccountTracking at ORDER BY at.createdAt DESC")
    List<AccountTracking> findRecentTracking(Pageable pageable);
}

