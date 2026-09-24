package com.neo.springapp.repository;

import com.neo.springapp.model.BranchCity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchCityRepository extends JpaRepository<BranchCity, Long> {
    boolean existsByCityIgnoreCase(String city);
    List<BranchCity> findAllByOrderByCityAsc();
}