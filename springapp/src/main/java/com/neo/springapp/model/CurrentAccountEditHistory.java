package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "current_account_edit_history")
public class CurrentAccountEditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String editedBy;

    @Column(nullable = false)
    private LocalDateTime editedAt;

    @Column(columnDefinition = "TEXT")
    private String changesDescription;

    @Column(columnDefinition = "TEXT")
    private String fieldChanges; // JSON string of old->new values

    private String documentPath;

    private String documentName;

    @PrePersist
    protected void onCreate() {
        if (editedAt == null) {
            editedAt = LocalDateTime.now();
        }
    }
}
