package com.neo.springapp.repository;

import com.neo.springapp.model.AtmMachine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtmMachineRepository extends JpaRepository<AtmMachine, Long> {

    Optional<AtmMachine> findByAtmId(String atmId);

    List<AtmMachine> findByStatus(String status);

    List<AtmMachine> findByCity(String city);

    List<AtmMachine> findByManagedBy(String managedBy);

    Page<AtmMachine> findByStatusOrderByAtmIdAsc(String status, Pageable pageable);

    @Query("SELECT a FROM AtmMachine a WHERE a.cashAvailable < a.minThreshold AND a.status = 'ACTIVE'")
    List<AtmMachine> findLowCashAtms();

    @Query("SELECT COUNT(a) FROM AtmMachine a WHERE a.status = :status")
    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(a.cashAvailable), 0) FROM AtmMachine a")
    Double getTotalCashAcrossAllAtms();

    @Query("SELECT COALESCE(SUM(a.cashAvailable), 0) FROM AtmMachine a WHERE a.status = 'ACTIVE'")
    Double getTotalCashInActiveAtms();

    List<AtmMachine> findByStatusNot(String status);

    boolean existsByAtmId(String atmId);
}
