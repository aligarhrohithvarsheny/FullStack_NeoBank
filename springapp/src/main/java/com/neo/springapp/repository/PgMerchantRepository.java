package com.neo.springapp.repository;

import com.neo.springapp.model.PgMerchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PgMerchantRepository extends JpaRepository<PgMerchant, Long> {
    Optional<PgMerchant> findByMerchantId(String merchantId);
    Optional<PgMerchant> findByApiKey(String apiKey);
    Optional<PgMerchant> findByBusinessEmail(String businessEmail);
    Optional<PgMerchant> findByAccountNumber(String accountNumber);
    Optional<PgMerchant> findByLinkedAccountNumber(String linkedAccountNumber);
    boolean existsByBusinessEmail(String businessEmail);
    boolean existsByApiKey(String apiKey);
    List<PgMerchant> findByRegistrationStatus(String registrationStatus);
    List<PgMerchant> findByAdminApproved(Boolean adminApproved);
    List<PgMerchant> findByLoginEnabled(Boolean loginEnabled);
}
