package com.neo.springapp.repository;

import com.neo.springapp.model.NetBankingServiceControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetBankingServiceControlRepository extends JpaRepository<NetBankingServiceControl, Long> {
    Optional<NetBankingServiceControl> findByServiceType(String serviceType);
}
