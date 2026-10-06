package com.neo.springapp.repository;

import com.neo.springapp.model.SavingsChequeLeaf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsChequeLeafRepository extends JpaRepository<SavingsChequeLeaf, Long> {

    List<SavingsChequeLeaf> findByAccountIdAndStatusOrderByLeafNumberAsc(Long accountId, String status);

    List<SavingsChequeLeaf> findByAccountIdOrderByLeafNumberAsc(Long accountId);

    long countByAccountId(Long accountId);

    Optional<SavingsChequeLeaf> findByLeafNumberAndAccountId(String leafNumber, Long accountId);

    Optional<SavingsChequeLeaf> findByLeafNumber(String leafNumber);

    boolean existsByLeafNumber(String leafNumber);
}
