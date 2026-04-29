package com.neo.springapp.repository;

import com.neo.springapp.model.BusinessChequeSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessChequeSequenceRepository extends JpaRepository<BusinessChequeSequence, Long> {

    BusinessChequeSequence findByCurrentAccountId(Long currentAccountId);

    Optional<BusinessChequeSequence> findByIdAndCurrentAccountId(Long id, Long currentAccountId);
}
