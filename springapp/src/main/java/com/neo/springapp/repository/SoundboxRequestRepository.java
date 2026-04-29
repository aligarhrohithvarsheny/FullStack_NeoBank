package com.neo.springapp.repository;

import com.neo.springapp.model.SoundboxRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoundboxRequestRepository extends JpaRepository<SoundboxRequest, Long> {

    Optional<SoundboxRequest> findByRequestId(String requestId);

    List<SoundboxRequest> findByAccountNumber(String accountNumber);

    List<SoundboxRequest> findByStatus(String status);

    List<SoundboxRequest> findByStatusOrderByRequestedAtDesc(String status);

    @Query("SELECT COUNT(r) FROM SoundboxRequest r WHERE r.status = 'PENDING'")
    long countPendingRequests();

    boolean existsByAccountNumberAndStatusIn(String accountNumber, List<String> statuses);
}
