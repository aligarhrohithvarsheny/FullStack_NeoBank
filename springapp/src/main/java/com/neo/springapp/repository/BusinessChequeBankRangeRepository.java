package com.neo.springapp.repository;

import com.neo.springapp.model.BusinessChequeBankRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessChequeBankRangeRepository extends JpaRepository<BusinessChequeBankRange, Long> {

    List<BusinessChequeBankRange> findByCurrentAccountId(Long currentAccountId);

    List<BusinessChequeBankRange> findByCurrentAccountIdAndStatus(Long currentAccountId, String status);

    BusinessChequeBankRange findByChequeBookNumber(String chequeBookNumber);
}
