package com.neo.springapp.repository;

import com.neo.springapp.model.GoldRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GoldRateRepository extends JpaRepository<GoldRate, Long> {
    Optional<GoldRate> findByDate(LocalDate date);
    Optional<GoldRate> findFirstByOrderByDateDesc();
}

