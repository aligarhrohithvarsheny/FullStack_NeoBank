package com.neo.springapp.repository;

import com.neo.springapp.model.Card360ClosureRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Card360ClosureRequestRepository extends JpaRepository<Card360ClosureRequest, Long> {
    Optional<Card360ClosureRequest> findFirstByCreditCardIdAndStatusOrderByRequestedAtDesc(
            Long creditCardId, String status);

    List<Card360ClosureRequest> findByAccountNumberOrderByRequestedAtDesc(String accountNumber);
}
