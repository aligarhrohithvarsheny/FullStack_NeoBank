package com.neo.springapp.repository;

import com.neo.springapp.model.CurrentAccountBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrentAccountBeneficiaryRepository extends JpaRepository<CurrentAccountBeneficiary, Long> {
    List<CurrentAccountBeneficiary> findByAccountNumber(String accountNumber);
    List<CurrentAccountBeneficiary> findByAccountNumberAndStatus(String accountNumber, String status);
}
