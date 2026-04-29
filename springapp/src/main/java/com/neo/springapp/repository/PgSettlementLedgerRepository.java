package com.neo.springapp.repository;

import com.neo.springapp.model.PgSettlementLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PgSettlementLedgerRepository extends JpaRepository<PgSettlementLedger, Long> {
    Optional<PgSettlementLedger> findByLedgerId(String ledgerId);
    List<PgSettlementLedger> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
    List<PgSettlementLedger> findByTransactionId(String transactionId);
    List<PgSettlementLedger> findByCreditAccount(String creditAccount);
    List<PgSettlementLedger> findByCreditStatus(String creditStatus);
}
