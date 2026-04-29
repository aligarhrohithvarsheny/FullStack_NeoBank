package com.neo.springapp.repository;

import com.neo.springapp.model.FaceAuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaceAuthRepository extends JpaRepository<FaceAuthCredential, Long> {

    List<FaceAuthCredential> findByAdminEmailAndActiveTrue(String adminEmail);

    Optional<FaceAuthCredential> findFirstByAdminEmailAndActiveTrue(String adminEmail);

    void deleteByAdminEmail(String adminEmail);
}
