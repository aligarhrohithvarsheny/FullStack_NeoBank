package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditCardProductRepository extends JpaRepository<CreditCardProduct, Long> {
    List<CreditCardProduct> findAllByOrderBySortOrderAscNameAsc();
    Optional<CreditCardProduct> findByProductCodeIgnoreCase(String productCode);
}
