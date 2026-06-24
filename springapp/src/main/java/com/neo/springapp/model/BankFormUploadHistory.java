package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "bank_form_upload_history", indexes = {
        @Index(name = "idx_bfuh_upload", columnList = "uploadId")
})
public class BankFormUploadHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long uploadId;

    @Column(nullable = false, length = 32)
    private String action;

    private String previousFileName;
    private String previousStoredPath;
    private String previousContentType;
    private Long previousFileSizeBytes;

    private String newFileName;
    private String newStoredPath;
    private String newContentType;
    private Long newFileSizeBytes;

    private String performedByAdmin;

    @Column(length = 1000)
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    public BankFormUploadHistory() {
        this.performedAt = LocalDateTime.now();
    }
}
