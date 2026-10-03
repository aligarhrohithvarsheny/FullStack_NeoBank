package com.neo.springapp.repository;

import com.neo.springapp.model.DemandDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DemandDraftRepository extends JpaRepository<DemandDraft, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT d FROM DemandDraft d WHERE " +
            "LOWER(d.ddNumber) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(d.chequeNumber) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(d.accountNumber) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<DemandDraft> searchByTerm(@org.springframework.data.repository.query.Param("term") String term,
                                   org.springframework.data.domain.Pageable pageable);

    List<DemandDraft> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    Optional<DemandDraft> findByChequeNumberAndAccountNumber(String chequeNumber, String accountNumber);
}
