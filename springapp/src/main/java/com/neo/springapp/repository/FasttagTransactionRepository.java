package com.neo.springapp.repository;

import com.neo.springapp.model.FasttagTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FasttagTransactionRepository extends JpaRepository<FasttagTransaction, Long> {
    List<FasttagTransaction> findByFasttagIdOrderByCreatedAtDesc(Long fasttagId);
    List<FasttagTransaction> findByFasttagNumberOrderByCreatedAtDesc(String fasttagNumber);
}
