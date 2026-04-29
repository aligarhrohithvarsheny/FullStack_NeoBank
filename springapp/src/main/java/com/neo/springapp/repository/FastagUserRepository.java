package com.neo.springapp.repository;

import com.neo.springapp.model.FastagUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FastagUserRepository extends JpaRepository<FastagUser, Long> {
    Optional<FastagUser> findByGmailId(String gmailId);
}
