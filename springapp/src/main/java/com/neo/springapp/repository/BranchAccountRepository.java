package com.neo.springapp.repository;

import com.neo.springapp.model.BranchAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchAccountRepository extends JpaRepository<BranchAccount, Long> {
}
