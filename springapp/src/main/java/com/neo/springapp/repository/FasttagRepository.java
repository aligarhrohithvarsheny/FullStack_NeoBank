package com.neo.springapp.repository;

import com.neo.springapp.model.Fasttag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FasttagRepository extends JpaRepository<Fasttag, Long> {
    List<Fasttag> findByUserId(String userId);
    Fasttag findByFasttagNumber(String fasttagNumber);
    Fasttag findByVehicleNumber(String vehicleNumber);
    List<Fasttag> findByVehicleNumberAndStatusIn(String vehicleNumber, List<String> statuses);
    Fasttag findByAssignedAccountId(String accountId);
    List<Fasttag> findByEmail(String email);
}
