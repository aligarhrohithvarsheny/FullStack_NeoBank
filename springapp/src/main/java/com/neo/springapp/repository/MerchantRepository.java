package com.neo.springapp.repository;

import com.neo.springapp.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByMerchantId(String merchantId);
    Optional<Merchant> findByMobile(String mobile);
    Optional<Merchant> findByEmail(String email);
    List<Merchant> findByAgentIdOrderByCreatedAtDesc(String agentId);
    List<Merchant> findByStatusOrderByCreatedAtDesc(String status);
    List<Merchant> findByAgentIdAndStatusOrderByCreatedAtDesc(String agentId, String status);
    List<Merchant> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.status = :status")
    long countByStatus(String status);

    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.agentId = :agentId")
    long countByAgentId(String agentId);
}
