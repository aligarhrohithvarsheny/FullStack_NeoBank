package com.neo.springapp.repository;

import com.neo.springapp.model.SavingsChequeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsChequeRequestRepository extends JpaRepository<SavingsChequeRequest, Long> {

    Page<SavingsChequeRequest> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    Page<SavingsChequeRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

        Page<SavingsChequeRequest> findByStatusNotOrderByCreatedAtDesc(String status, Pageable pageable);

        Page<SavingsChequeRequest> findByStatusNotInOrderByCreatedAtDesc(java.util.Collection<String> statuses, Pageable pageable);

        Page<SavingsChequeRequest> findByStatusNotInAndChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(
            java.util.Collection<String> statuses, String chequeNumber, Pageable pageable);

        Page<SavingsChequeRequest> findByStatusNotAndChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(
            String status, String chequeNumber, Pageable pageable);

    Page<SavingsChequeRequest> findByStatusAndChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(
            String status, String chequeNumber, Pageable pageable);

    Page<SavingsChequeRequest> findByChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(String chequeNumber, Pageable pageable);

    List<SavingsChequeRequest> findByChequeNumberContainingIgnoreCase(String chequeNumber);

    // Find exact cheque number (used by cross-account-type cheque verification)
    Optional<SavingsChequeRequest> findByChequeNumber(String chequeNumber);

    List<SavingsChequeRequest> findAllByChequeNumber(String chequeNumber);

    Page<SavingsChequeRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(String status);

    List<SavingsChequeRequest> findByStatus(String status);

    long countByAccountIdAndStatusIn(Long accountId, List<String> statuses);
}
