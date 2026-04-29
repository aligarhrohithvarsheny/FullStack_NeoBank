package com.neo.springapp.repository;

import com.neo.springapp.model.MutualFundForeclosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MutualFundForeclosureRepository extends JpaRepository<MutualFundForeclosure, Long> {
    
    List<MutualFundForeclosure> findByAccountNumber(String accountNumber);
    
    List<MutualFundForeclosure> findByInvestmentId(Long investmentId);
    
    List<MutualFundForeclosure> findByStatus(String status);
    
    List<MutualFundForeclosure> findByAccountNumberAndStatus(String accountNumber, String status);
    
    List<MutualFundForeclosure> findByStatusIn(List<String> statuses);
    
    List<MutualFundForeclosure> findByInvestmentIdAndStatus(Long investmentId, String status);
}
