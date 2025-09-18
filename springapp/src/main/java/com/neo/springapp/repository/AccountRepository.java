package com.neo.springapp.repository;

import com.neo.springapp.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByPan(String pan);                  // Lookup by PAN
    Account findByAccountNumber(String accountNumber); // Lookup by Account Number
}
