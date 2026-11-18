package com.neo.springapp.repository;

import com.neo.springapp.model.CustomerActivationCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerActivationCodeRepository extends JpaRepository<CustomerActivationCode, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CustomerActivationCode c WHERE c.customerId = :customerId")
    Optional<CustomerActivationCode> findForUpdate(@Param("customerId") String customerId);

    Optional<CustomerActivationCode> findByCustomerId(String customerId);
}
