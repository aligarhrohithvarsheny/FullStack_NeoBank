package com.neo.springapp.repository;

import com.neo.springapp.model.BusinessChequeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessChequeRequestRepository extends JpaRepository<BusinessChequeRequest, Long> {

    Page<BusinessChequeRequest> findByCurrentAccountIdOrderByCreatedAtDesc(Long currentAccountId, Pageable pageable);

    Page<BusinessChequeRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<BusinessChequeRequest> findByStatusAndChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(
            String status, String chequeNumber, Pageable pageable);

    Page<BusinessChequeRequest> findByChequeNumberContainingIgnoreCaseOrderByCreatedAtDesc(String chequeNumber, Pageable pageable);

    List<BusinessChequeRequest> findByChequeNumberContainingIgnoreCase(String chequeNumber);

    Page<BusinessChequeRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(String status);

    List<BusinessChequeRequest> findByStatus(String status);

    long countByCurrentAccountIdAndStatusIn(Long currentAccountId, List<String> statuses);
}
