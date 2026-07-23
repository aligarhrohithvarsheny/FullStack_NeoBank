package com.neo.springapp.repository;

import com.neo.springapp.model.BankFormUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankFormUploadRepository extends JpaRepository<BankFormUpload, Long> {

    List<BankFormUpload> findByAccountNumberOrderByUploadedAtDesc(String accountNumber);

    List<BankFormUpload> findByFormCodeOrderByUploadedAtDesc(String formCode);

    List<BankFormUpload> findAllByOrderByUploadedAtDesc();

    @Query("SELECT u.fileContent FROM BankFormUpload u WHERE u.id = :id")
    Optional<byte[]> findFileContentById(@Param("id") Long id);
}
