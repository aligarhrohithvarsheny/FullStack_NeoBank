package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Data
@Table(name = "video_kyc_sessions")
public class VideoKycSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String addressCity;
    private String addressState;
    private String accountType = "Savings";
    private String temporaryAccountNumber;
    private String finalAccountNumber;
    private String ifscCode = "NEOB0000001";

    // Verification number
    @Column(unique = true)
    private String verificationNumber;

    // Slot booking
    private Long bookedSlotId;
    private LocalDate slotDate;
    private LocalTime slotTime;
    private LocalTime slotEndTime;

    // Document uploads
    @Lob
    @Column(name = "aadhar_document", columnDefinition = "LONGBLOB")
    private byte[] aadharDocument;
    private String aadharDocumentName;
    private String aadharDocumentType;

    @Lob
    @Column(name = "pan_document", columnDefinition = "LONGBLOB")
    private byte[] panDocument;
    private String panDocumentName;
    private String panDocumentType;

    private String aadharNumber;
    private String panNumber;

    // Video KYC specifics
    @Column(unique = true)
    private String roomId;
    private String kycStatus = "Pending"; // Pending, Under Review, Approved, Rejected
    private String otpCode;
    private Boolean otpVerified = false;

    // Snapshots
    @Lob
    @Column(name = "face_snapshot", columnDefinition = "LONGBLOB")
    private byte[] faceSnapshot;
    private String faceSnapshotType;

    @Lob
    @Column(name = "id_snapshot", columnDefinition = "LONGBLOB")
    private byte[] idSnapshot;
    private String idSnapshotType;

    // Liveness
    private Boolean livenessCheckPassed = false;
    private String livenessCheckType;

    // Rejection
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    // Admin
    private Long assignedAdminId;
    private String assignedAdminName;

    // Attempt tracking
    private Integer kycAttemptCount = 1;
    private Integer maxAttempts = 3;

    // Session control
    private Boolean sessionActive = false;
    private LocalDateTime sessionStartedAt;
    private LocalDateTime sessionEndedAt;
    private Integer sessionDurationSeconds;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;

    // Link to main user account
    private Long userId;
    private Long accountId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
