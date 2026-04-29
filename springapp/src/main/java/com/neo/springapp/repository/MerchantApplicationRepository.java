package com.neo.springapp.repository;

import com.neo.springapp.model.MerchantApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantApplicationRepository extends JpaRepository<MerchantApplication, Long> {
    Optional<MerchantApplication> findByApplicationId(String applicationId);
    List<MerchantApplication> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
    List<MerchantApplication> findByAgentIdOrderByCreatedAtDesc(String agentId);
    List<MerchantApplication> findByStatusOrderByCreatedAtDesc(String status);
    List<MerchantApplication> findAllByOrderByCreatedAtDesc();
}
