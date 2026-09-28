package com.neo.springapp.repository;

import com.neo.springapp.entity.FundsAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnhancedFundsAllocationRepository extends JpaRepository<FundsAllocation, Long> {
}