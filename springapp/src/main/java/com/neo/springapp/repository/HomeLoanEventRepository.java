package com.neo.springapp.repository;

import com.neo.springapp.model.HomeLoanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeLoanEventRepository extends JpaRepository<HomeLoanEvent, Long> {
    List<HomeLoanEvent> findByHomeLoanIdOrderByEventDateAscIdAsc(Long homeLoanId);
    List<HomeLoanEvent> findByAccountNumberOrderByEventDateAscIdAsc(String accountNumber);
}
