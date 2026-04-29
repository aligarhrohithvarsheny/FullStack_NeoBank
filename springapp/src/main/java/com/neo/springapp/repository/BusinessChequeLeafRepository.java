package com.neo.springapp.repository;

import com.neo.springapp.model.BusinessChequeLeaf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessChequeLeafRepository extends JpaRepository<BusinessChequeLeaf, Long> {

    List<BusinessChequeLeaf> findByCurrentAccountIdAndStatusOrderByLeafNumberAsc(Long currentAccountId, String status);

    List<BusinessChequeLeaf> findByCurrentAccountIdOrderByLeafNumberAsc(Long currentAccountId);

    long countByCurrentAccountId(Long currentAccountId);

    Optional<BusinessChequeLeaf> findByLeafNumberAndCurrentAccountId(String leafNumber, Long currentAccountId);

    Optional<BusinessChequeLeaf> findByLeafNumber(String leafNumber);

    boolean existsByLeafNumber(String leafNumber);
}
