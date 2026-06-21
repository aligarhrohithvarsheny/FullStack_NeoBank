package com.neo.springapp.repository;

import com.neo.springapp.model.BankFormUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankFormUploadRepository extends JpaRepository<BankFormUpload, Long> {

    List<BankFormUpload> findByAccountNumberOrderByUploadedAtDesc(String accountNumber);

    List<BankFormUpload> findByFormCodeOrderByUploadedAtDesc(String formCode);

    List<BankFormUpload> findAllByOrderByUploadedAtDesc();
}
