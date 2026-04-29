package com.neo.springapp.repository;

import com.neo.springapp.model.CurrentAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentAccountRepository extends JpaRepository<CurrentAccount, Long> {

    Optional<CurrentAccount> findByAccountNumber(String accountNumber);

    Optional<CurrentAccount> findByCustomerId(String customerId);

    Optional<CurrentAccount> findByGstNumber(String gstNumber);

    Optional<CurrentAccount> findByPanNumber(String panNumber);

    List<CurrentAccount> findByStatus(String status);

    Page<CurrentAccount> findByStatus(String status, Pageable pageable);

    List<CurrentAccount> findByBusinessType(String businessType);

    List<CurrentAccount> findByOwnerName(String ownerName);

    List<CurrentAccount> findByKycVerified(Boolean kycVerified);

    List<CurrentAccount> findByAccountFrozen(Boolean accountFrozen);

    @Query("SELECT c FROM CurrentAccount c WHERE " +
           "LOWER(c.businessName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.ownerName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.accountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.customerId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.gstNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.mobile) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<CurrentAccount> searchAccounts(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT COUNT(c) FROM CurrentAccount c WHERE c.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT SUM(c.balance) FROM CurrentAccount c WHERE c.status = 'ACTIVE'")
    Double getTotalActiveBalance();

    @Query("SELECT c FROM CurrentAccount c WHERE c.overdraftEnabled = true AND c.balance < 0")
    List<CurrentAccount> findOverdrawnAccounts();

    CurrentAccount findByAadharNumber(String aadharNumber);

    boolean existsByGstNumber(String gstNumber);

    boolean existsByPanNumber(String panNumber);

    boolean existsByMobile(String mobile);

    boolean existsByEmail(String email);

    Optional<CurrentAccount> findByEmail(String email);

    Optional<CurrentAccount> findByUpiId(String upiId);
}
