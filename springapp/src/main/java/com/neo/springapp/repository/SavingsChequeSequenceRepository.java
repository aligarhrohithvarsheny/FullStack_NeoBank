package com.neo.springapp.repository;

import com.neo.springapp.model.SavingsChequeSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavingsChequeSequenceRepository extends JpaRepository<SavingsChequeSequence, Long> {

    SavingsChequeSequence findByAccountId(Long accountId);

    Optional<SavingsChequeSequence> findByIdAndAccountId(Long id, Long accountId);
}
