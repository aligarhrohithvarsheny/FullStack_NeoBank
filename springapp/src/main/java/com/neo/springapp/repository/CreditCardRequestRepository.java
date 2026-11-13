package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface CreditCardRequestRepository extends JpaRepository<CreditCardRequest, Long> {
    List<CreditCardRequest> findByStatus(String status);
    List<CreditCardRequest> findByAccountNumberOrderByRequestDateDesc(String accountNumber);
    List<CreditCardRequest> findByPanIgnoreCaseAndMobileNumberOrderByRequestDateDesc(String pan, String mobileNumber);
    CreditCardRequest findByApplicationId(String applicationId);
    List<CreditCardRequest> findByDocumentsDeleteAfterBeforeAndDocumentsAvailableTrue(LocalDateTime cutoff);
}
