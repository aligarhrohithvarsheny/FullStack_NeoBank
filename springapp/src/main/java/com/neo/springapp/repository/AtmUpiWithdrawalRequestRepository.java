package com.neo.springapp.repository;

import com.neo.springapp.model.AtmUpiWithdrawalRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AtmUpiWithdrawalRequestRepository extends JpaRepository<AtmUpiWithdrawalRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AtmUpiWithdrawalRequest r WHERE r.requestToken = :token")
    Optional<AtmUpiWithdrawalRequest> findByRequestTokenForUpdate(@Param("token") String token);

    Optional<AtmUpiWithdrawalRequest> findByRequestToken(String requestToken);
}
