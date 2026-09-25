package com.neo.springapp.repository;

import com.neo.springapp.model.ChequeBookClosureHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChequeBookClosureHistoryRepository extends JpaRepository<ChequeBookClosureHistory, Long> {

    List<ChequeBookClosureHistory> findByAccountNumberOrderByClosedAtDesc(String accountNumber);

    List<ChequeBookClosureHistory> findAllByOrderByClosedAtDesc();
}
