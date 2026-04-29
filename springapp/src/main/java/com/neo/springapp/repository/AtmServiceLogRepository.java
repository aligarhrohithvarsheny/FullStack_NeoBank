package com.neo.springapp.repository;

import com.neo.springapp.model.AtmServiceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtmServiceLogRepository extends JpaRepository<AtmServiceLog, Long> {

    Page<AtmServiceLog> findByAtmIdOrderByCreatedAtDesc(String atmId, Pageable pageable);

    Page<AtmServiceLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
