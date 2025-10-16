package com.neo.springapp.repository;

import com.neo.springapp.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    // JPQL skeleton
    @Query("SELECT a FROM Admin a WHERE a.email = :email AND a.password = :password")
    Admin validateLogin(String email, String password);

    // Spring Data JPA method
    Admin findByEmailAndPassword(String email, String password);
    
    // Find admin by email only
    Admin findByEmail(String email);
}
