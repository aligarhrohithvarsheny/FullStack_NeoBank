package com.neo.springapp.repository;

import com.neo.springapp.model.ChequeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChequeRequestRepository extends JpaRepository<ChequeRequest, Long> {
    
    // Find by salary account
    Page<ChequeRequest> findBySalaryAccountIdOrderByCreatedAtDesc(Long salaryAccountId, Pageable pageable);
    
    // Find by status
    Page<ChequeRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    
    // Find by status and cheque number
    Page<ChequeRequest> findByStatusAndChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(
            String status, String chequeNumber, Pageable pageable);
    
    // Find by cheque number containing
    Page<ChequeRequest> findByChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(String chequeNumber, Pageable pageable);
    
    // Find by cheque number containing (list)
    List<ChequeRequest> findByChequeNumberContainingIgnoreCase(String chequeNumber);
    
    // Find all ordered by created date
    Page<ChequeRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Count by status
    long countByStatus(String status);
    
    // Find by status (list)
    List<ChequeRequest> findByStatus(String status);

    // Count pending/approved cheques for a salary account
    long countBySalaryAccountIdAndStatusIn(Long salaryAccountId, List<String> statuses);
}
