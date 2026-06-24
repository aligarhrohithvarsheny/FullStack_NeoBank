package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "bank_form_uploads", indexes = {
        @Index(name = "idx_bfu_account", columnList = "accountNumber"),
        @Index(name = "idx_bfu_form_code", columnList = "formCode")
})
public class BankFormUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String formCode;

    @Column(nullable = false)
    private String formName;

    @Column(nullable = false, length = 64)
    private String category;

    @Column(nullable = false, length = 32)
    private String accountNumber;

    @Column(nullable = false, length = 32)
    private String accountType;

    private String accountHolderName;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFilePath;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_content")
    private byte[] fileContent;

    private String contentType;

    private Long fileSizeBytes;

    private String uploadedByAdmin;

    @Column(length = 1000)
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    public BankFormUpload() {
        this.uploadedAt = LocalDateTime.now();
    }
}
