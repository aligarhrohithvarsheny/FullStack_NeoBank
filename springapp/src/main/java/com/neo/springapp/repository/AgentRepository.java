package com.neo.springapp.repository;

import com.neo.springapp.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByEmail(String email);
    Optional<Agent> findByMobile(String mobile);
    Optional<Agent> findByAgentId(String agentId);
    Optional<Agent> findByEmailAndPassword(String email, String password);
    Optional<Agent> findByMobileAndPassword(String mobile, String password);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
    List<Agent> findByStatus(String status);
    List<Agent> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
