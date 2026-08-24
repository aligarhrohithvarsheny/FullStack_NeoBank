package com.neo.springapp.repository;

import com.neo.springapp.model.DemandDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DemandDraftRepository extends JpaRepository<DemandDraft, Long> {
    List<DemandDraft> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    Optional<DemandDraft> findByChequeNumberAndAccountNumber(String chequeNumber, String accountNumber);
}
