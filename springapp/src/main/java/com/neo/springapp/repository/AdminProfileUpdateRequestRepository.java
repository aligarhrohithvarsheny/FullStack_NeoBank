package com.neo.springapp.repository;

import com.neo.springapp.model.AdminProfileUpdateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminProfileUpdateRequestRepository extends JpaRepository<AdminProfileUpdateRequest, Long> {

    List<AdminProfileUpdateRequest> findByStatus(String status);

    List<AdminProfileUpdateRequest> findByAdminId(Long adminId);
}

