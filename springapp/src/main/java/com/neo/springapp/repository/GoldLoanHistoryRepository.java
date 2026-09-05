package com.neo.springapp.repository;

import com.neo.springapp.model.GoldLoanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoldLoanHistoryRepository extends JpaRepository<GoldLoanHistory, Long> {
    List<GoldLoanHistory> findByGoldLoanIdOrderByChangeDateDesc(Long goldLoanId);
    List<GoldLoanHistory> findByLoanAccountNumberOrderByChangeDateDesc(String loanAccountNumber);
}
