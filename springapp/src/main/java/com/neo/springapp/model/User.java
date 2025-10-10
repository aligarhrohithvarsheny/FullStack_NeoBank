package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String email;
    private LocalDateTime joinDate;
    private String status = "PENDING"; // PENDING, APPROVED, CLOSED

    @Column(unique = true)
    private String accountNumber;
    
    // Account lock fields
    private int failedLoginAttempts = 0;
    private boolean accountLocked = false;
    private LocalDateTime lastFailedLoginTime;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id")
    private Account account;

    // Constructors
    public User() {
        this.joinDate = LocalDateTime.now();
    }

    public User(String username, String password, String email) {
        this();
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // Convenience methods to access account fields
    public String getName() {
        return account != null ? account.getName() : null;
    }

    public String getPhone() {
        return account != null ? account.getPhone() : null;
    }

    public String getAddress() {
        return account != null ? account.getAddress() : null;
    }

    public String getDob() {
        return account != null ? account.getDob() : null;
    }

    public String getOccupation() {
        return account != null ? account.getOccupation() : null;
    }

    public Double getIncome() {
        return account != null ? account.getIncome() : null;
    }

    public String getPan() {
        return account != null ? account.getPan() : null;
    }

    public String getAadhar() {
        return account != null ? account.getAadharNumber() : null;
    }

    public String getAccountType() {
        return account != null ? account.getAccountType() : null;
    }

    public Double getBalance() {
        return account != null ? account.getBalance() : null;
    }
}
