package com.neo.springapp.repository;

import com.neo.springapp.model.BankingProductConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BankingProductConfigRepository extends JpaRepository<BankingProductConfig, Long> {

    List<BankingProductConfig> findAllByOrderByProductCodeAscEffectiveFromDescCreatedAtDesc();

    List<BankingProductConfig> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByProductCodeIgnoreCaseAndEffectiveFromAndStatus(
            String productCode, LocalDate effectiveFrom, String status);
}
