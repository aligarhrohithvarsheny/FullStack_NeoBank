package com.neo.springapp.repository;

import com.neo.springapp.model.FastagLinkedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FastagLinkedAccountRepository extends JpaRepository<FastagLinkedAccount, Long> {

    List<FastagLinkedAccount> findByGmailIdAndStatus(String gmailId, String status);

    Optional<FastagLinkedAccount> findByGmailIdAndAccountNumberAndStatus(String gmailId, String accountNumber, String status);

    Optional<FastagLinkedAccount> findByGmailIdAndAccountNumber(String gmailId, String accountNumber);
}
