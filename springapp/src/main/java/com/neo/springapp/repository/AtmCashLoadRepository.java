package com.neo.springapp.repository;

import com.neo.springapp.model.AtmCashLoad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AtmCashLoadRepository extends JpaRepository<AtmCashLoad, Long> {

    Page<AtmCashLoad> findByAtmIdOrderByLoadedAtDesc(String atmId, Pageable pageable);

    List<AtmCashLoad> findByLoadedBy(String loadedBy);

    List<AtmCashLoad> findByAtmIdAndLoadedAtBetween(String atmId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM AtmCashLoad c WHERE c.atmId = :atmId")
    Double getTotalCashLoadedForAtm(String atmId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM AtmCashLoad c WHERE c.loadedAt >= :since")
    Double getTotalCashLoadedSince(LocalDateTime since);
}
