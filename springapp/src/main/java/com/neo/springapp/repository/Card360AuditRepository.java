package com.neo.springapp.repository;

import com.neo.springapp.model.Card360Audit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface Card360AuditRepository extends JpaRepository<Card360Audit, Long> {
    List<Card360Audit> findTop200ByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    java.util.Optional<Card360Audit> findFirstByAccountNumberAndCardTypeAndCardIdOrderByCreatedAtDesc(
            String accountNumber, String cardType, Long cardId);
}
