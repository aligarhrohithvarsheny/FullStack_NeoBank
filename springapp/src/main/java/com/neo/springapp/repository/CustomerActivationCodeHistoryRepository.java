package com.neo.springapp.repository;

import com.neo.springapp.model.CustomerActivationCodeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerActivationCodeHistoryRepository extends JpaRepository<CustomerActivationCodeHistory, Long> {
    Optional<CustomerActivationCodeHistory> findFirstByCustomerIdAndStatusOrderByIssuedAtDesc(
            String customerId, String status);

    Optional<CustomerActivationCodeHistory> findFirstByCustomerIdAndAccountTypeAndStatusOrderByIssuedAtDesc(
            String customerId, String accountType, String status);

    List<CustomerActivationCodeHistory> findTop200ByOrderByIssuedAtDesc();
}
