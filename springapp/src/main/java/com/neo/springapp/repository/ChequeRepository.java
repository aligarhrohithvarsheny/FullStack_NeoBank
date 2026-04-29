package com.neo.springapp.repository;

import com.neo.springapp.model.Cheque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChequeRepository extends JpaRepository<Cheque, Long> {
    
    // Find by account number
    List<Cheque> findByAccountNumber(String accountNumber);
    Page<Cheque> findByAccountNumber(String accountNumber, Pageable pageable);
    
    // Find by status
    List<Cheque> findByStatus(String status);
    Page<Cheque> findByStatus(String status, Pageable pageable);
    
    // Find by account number and status
    List<Cheque> findByAccountNumberAndStatus(String accountNumber, String status);
    Page<Cheque> findByAccountNumberAndStatus(String accountNumber, String status, Pageable pageable);
    
    // Find by cheque number
    Optional<Cheque> findByChequeNumber(String chequeNumber);
    
    // Find active cheques for an account
    @Query("SELECT c FROM Cheque c WHERE c.accountNumber = :accountNumber AND c.status = 'ACTIVE'")
    List<Cheque> findActiveChequesByAccountNumber(@Param("accountNumber") String accountNumber);
    
    // Find cancelled cheques for an account
    @Query("SELECT c FROM Cheque c WHERE c.accountNumber = :accountNumber AND c.status = 'CANCELLED'")
    List<Cheque> findCancelledChequesByAccountNumber(@Param("accountNumber") String accountNumber);
    
    // Count cheques by status
    Long countByAccountNumberAndStatus(String accountNumber, String status);
    
    // Count all cheques by status
    Long countByStatus(String status);
    
    // Search cheques by cheque number containing
    @Query("SELECT c FROM Cheque c WHERE c.chequeNumber LIKE CONCAT('%', :chequeNumber, '%')")
    Page<Cheque> findByChequeNumberContaining(@Param("chequeNumber") String chequeNumber, Pageable pageable);
    
    // Find cheques by request status
    List<Cheque> findByRequestStatus(String requestStatus);
    Page<Cheque> findByRequestStatus(String requestStatus, Pageable pageable);
    
    // Find pending requests
    @Query("SELECT c FROM Cheque c WHERE c.requestStatus = 'PENDING' ORDER BY c.requestDate ASC")
    List<Cheque> findPendingRequests();
    
    @Query("SELECT c FROM Cheque c WHERE c.requestStatus = 'PENDING' ORDER BY c.requestDate ASC")
    Page<Cheque> findPendingRequests(Pageable pageable);
    
    // Find approved requests
    @Query("SELECT c FROM Cheque c WHERE c.requestStatus = 'APPROVED' ORDER BY c.approvedDate DESC")
    List<Cheque> findApprovedRequests();
    
    @Query("SELECT c FROM Cheque c WHERE c.requestStatus = 'APPROVED' ORDER BY c.approvedDate DESC")
    Page<Cheque> findApprovedRequests(Pageable pageable);
    
    // Count pending requests
    Long countByRequestStatus(String requestStatus);
}

