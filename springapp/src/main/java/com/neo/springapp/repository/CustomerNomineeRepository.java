package com.neo.springapp.repository;

import com.neo.springapp.model.CustomerNominee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerNomineeRepository extends JpaRepository<CustomerNominee, Long> {

    List<CustomerNominee> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
