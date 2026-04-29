package com.neo.springapp.repository;

import com.neo.springapp.model.BankMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankMessageRepository extends JpaRepository<BankMessage, Long> {

    List<BankMessage> findByRecipientAccountNumberOrderByCreatedAtDesc(String accountNumber);

    List<BankMessage> findByRecipientAccountNumberAndIsReadFalseOrderByCreatedAtDesc(String accountNumber);

    List<BankMessage> findByMessageType(String messageType);

    long countByRecipientAccountNumberAndIsReadFalse(String accountNumber);
}
