package com.neo.springapp.repository;

import com.neo.springapp.model.ChequeBankRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChequeBankRangeRepository extends JpaRepository<ChequeBankRange, Long> {
    
    // Find by salary account ID
    List<ChequeBankRange> findBySalaryAccountId(Long salaryAccountId);
    
    // Find by salary account ID and status
    List<ChequeBankRange> findBySalaryAccountIdAndStatus(Long salaryAccountId, String status);
    
    // Find by cheque book number
    ChequeBankRange findByChequeBookNumber(String chequeBookNumber);
}
