package com.neo.springapp.repository;

import com.neo.springapp.model.ChequeLeaf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChequeLeafRepository extends JpaRepository<ChequeLeaf, Long> {

    List<ChequeLeaf> findBySalaryAccountIdAndStatusOrderByLeafNumberAsc(Long salaryAccountId, String status);

    List<ChequeLeaf> findBySalaryAccountIdOrderByLeafNumberAsc(Long salaryAccountId);

    long countBySalaryAccountId(Long salaryAccountId);

    Optional<ChequeLeaf> findByLeafNumberAndSalaryAccountId(String leafNumber, Long salaryAccountId);

    Optional<ChequeLeaf> findByLeafNumber(String leafNumber);

    boolean existsByLeafNumber(String leafNumber);
}
