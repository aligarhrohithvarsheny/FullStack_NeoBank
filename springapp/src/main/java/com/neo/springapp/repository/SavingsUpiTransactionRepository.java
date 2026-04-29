package com.neo.springapp.repository;

import com.neo.springapp.model.SavingsUpiTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsUpiTransactionRepository extends JpaRepository<SavingsUpiTransaction, Long> {

    List<SavingsUpiTransaction> findBySenderAccountOrderByCreatedAtDesc(String senderAccount);

    List<SavingsUpiTransaction> findBySenderUpiIdOrReceiverUpiIdOrderByCreatedAtDesc(String senderUpiId, String receiverUpiId);

    Optional<SavingsUpiTransaction> findByTransactionRef(String transactionRef);

    @Query("SELECT t FROM SavingsUpiTransaction t ORDER BY t.createdAt DESC")
    List<SavingsUpiTransaction> findAllOrderByCreatedAtDesc();

    @Query("SELECT t FROM SavingsUpiTransaction t WHERE t.fraudFlagged = true ORDER BY t.createdAt DESC")
    List<SavingsUpiTransaction> findFlagged();

    @Query("SELECT t FROM SavingsUpiTransaction t WHERE (t.senderUpiId = :upiId OR t.receiverUpiId = :upiId) AND t.createdAt > :after ORDER BY t.createdAt DESC")
    List<SavingsUpiTransaction> findRecentAfter(String upiId, LocalDateTime after);
}
