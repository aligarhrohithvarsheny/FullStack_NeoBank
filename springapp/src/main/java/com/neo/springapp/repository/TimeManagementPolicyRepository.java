package com.neo.springapp.repository;

import com.neo.springapp.model.TimeManagementPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeManagementPolicyRepository extends JpaRepository<TimeManagementPolicy, Long> {
    
    // Find policy by admin ID
    @Query("SELECT p FROM TimeManagementPolicy p WHERE p.adminId = :adminId ORDER BY p.createdAt DESC")
    List<TimeManagementPolicy> findByAdminId(@Param("adminId") String adminId);
    
    // Find active policy for an admin
    @Query("SELECT p FROM TimeManagementPolicy p WHERE p.adminId = :adminId AND p.isActive = true")
    Optional<TimeManagementPolicy> findActiveByAdminId(@Param("adminId") String adminId);
    
    // Find all active policies (paginated)
    @Query("SELECT p FROM TimeManagementPolicy p WHERE p.isActive = true ORDER BY p.adminId ASC")
    Page<TimeManagementPolicy> findAllActive(Pageable pageable);
    
    // Find policies by name pattern
    @Query("SELECT p FROM TimeManagementPolicy p WHERE LOWER(p.policyName) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY p.createdAt DESC")
    List<TimeManagementPolicy> findByPolicyNameContaining(@Param("name") String name);
    
    // Check if policy exists for admin with same name
    @Query("SELECT COUNT(p) > 0 FROM TimeManagementPolicy p WHERE p.adminId = :adminId AND p.policyName = :policyName")
    boolean existsByAdminIdAndPolicyName(@Param("adminId") String adminId, @Param("policyName") String policyName);
    
    // Get all policies with pagination and search
    @Query("SELECT p FROM TimeManagementPolicy p WHERE " +
           "(:search IS NULL OR LOWER(p.policyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.adminId) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<TimeManagementPolicy> findAllWithSearch(@Param("search") String search, Pageable pageable);
}
