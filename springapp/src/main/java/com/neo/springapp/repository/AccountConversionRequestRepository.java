package com.neo.springapp.repository;

import com.neo.springapp.model.AccountConversionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountConversionRequestRepository extends JpaRepository<AccountConversionRequest, Long> {
    List<AccountConversionRequest> findAllByOrderByRequestedAtDesc();
    List<AccountConversionRequest> findByAccountNumberOrderByRequestedAtDesc(String accountNumber);
}
