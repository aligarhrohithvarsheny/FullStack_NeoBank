package com.neo.springapp.repository;

import com.neo.springapp.model.GoldRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoldRateHistoryRepository extends JpaRepository<GoldRateHistory, Long> {
    List<GoldRateHistory> findAllByOrderByChangedAtDesc();
    List<GoldRateHistory> findByRateDateOrderByChangedAtDesc(java.time.LocalDate rateDate);
}

