package com.neo.springapp.repository;

import com.neo.springapp.model.PositivePayRequest;
import com.neo.springapp.model.PositivePayStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PositivePayRequestRepository extends JpaRepository<PositivePayRequest, Long> {
    Optional<PositivePayRequest> findByReferenceNumber(String referenceNumber);
    List<PositivePayRequest> findByAccountNumberOrderBySubmittedAtDesc(String accountNumber);
    Optional<PositivePayRequest> findFirstByChequeSourceAndChequeIdAndStatusIn(String source, Long chequeId, List<PositivePayStatus> statuses);
    Optional<PositivePayRequest> findFirstByAccountNumberAndChequeNumberAndStatusIn(String accountNumber, String chequeNumber, List<PositivePayStatus> statuses);
    Page<PositivePayRequest> findByStatus(PositivePayStatus status, Pageable pageable);
    Page<PositivePayRequest> findBySubmittedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
    long countByStatus(PositivePayStatus status);
    long countBySubmittedAtBetween(LocalDateTime from, LocalDateTime to);
}
