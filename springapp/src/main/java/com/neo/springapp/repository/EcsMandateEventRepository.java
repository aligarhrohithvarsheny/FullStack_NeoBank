package com.neo.springapp.repository;

import com.neo.springapp.model.EcsMandateEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EcsMandateEventRepository extends JpaRepository<EcsMandateEvent, Long> {
    List<EcsMandateEvent> findByMandateDbIdOrderByCreatedAtDesc(Long mandateDbId);
}
