package com.neo.springapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.neo.springapp.model.KycRequest;

@Repository
public interface KycRepository extends JpaRepository<KycRequest, Long> {
    KycRequest findByPanNumber(String panNumber);
}
