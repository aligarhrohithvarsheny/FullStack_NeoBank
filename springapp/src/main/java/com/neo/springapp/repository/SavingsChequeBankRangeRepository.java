package com.neo.springapp.repository;

import com.neo.springapp.model.SavingsChequeBankRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsChequeBankRangeRepository extends JpaRepository<SavingsChequeBankRange, Long> {

    List<SavingsChequeBankRange> findByAccountId(Long accountId);

    List<SavingsChequeBankRange> findByAccountIdAndStatus(Long accountId, String status);

    SavingsChequeBankRange findByChequeBookNumber(String chequeBookNumber);
}
