package com.neo.springapp.repository;

import com.neo.springapp.model.BranchDailyAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BranchDailyAllocationRepository extends JpaRepository<BranchDailyAllocation, Long> {
    Optional<BranchDailyAllocation> findByAllocationDate(LocalDate allocationDate);
    List<BranchDailyAllocation> findByAllocationDateBetweenOrderByAllocationDateDesc(LocalDate from, LocalDate to);
}
