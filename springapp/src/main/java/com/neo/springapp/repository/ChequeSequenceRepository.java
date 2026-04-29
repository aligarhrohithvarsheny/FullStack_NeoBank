package com.neo.springapp.repository;

import com.neo.springapp.model.ChequeSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChequeSequenceRepository extends JpaRepository<ChequeSequence, Long> {
    
    // Find by salary account ID
    ChequeSequence findBySalaryAccountId(Long salaryAccountId);
    
    // Find optional by salary account ID
    Optional<ChequeSequence> findByIdAndSalaryAccountId(Long id, Long salaryAccountId);
}
