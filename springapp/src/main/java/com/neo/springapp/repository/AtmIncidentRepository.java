package com.neo.springapp.repository;

import com.neo.springapp.model.AtmIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtmIncidentRepository extends JpaRepository<AtmIncident, Long> {

    Optional<AtmIncident> findByIncidentRef(String incidentRef);

    Page<AtmIncident> findByAtmIdOrderByCreatedAtDesc(String atmId, Pageable pageable);

    Page<AtmIncident> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<AtmIncident> findByIncidentTypeOrderByCreatedAtDesc(String incidentType, Pageable pageable);

    Page<AtmIncident> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AtmIncident> findByAtmIdAndStatus(String atmId, String status);

    @Query("SELECT COUNT(i) FROM AtmIncident i WHERE i.status = 'OPEN'")
    long countOpenIncidents();

    @Query("SELECT COUNT(i) FROM AtmIncident i WHERE i.status = :status")
    long countByStatus(String status);

    List<AtmIncident> findByAccountNumber(String accountNumber);
}
