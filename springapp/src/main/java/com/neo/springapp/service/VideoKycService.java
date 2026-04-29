package com.neo.springapp.service;

import com.neo.springapp.model.VideoKycSession;
import com.neo.springapp.model.VideoKycAuditLog;
import com.neo.springapp.model.VideoKycSlot;
import com.neo.springapp.model.User;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.VideoKycSessionRepository;
import com.neo.springapp.repository.VideoKycAuditLogRepository;
import com.neo.springapp.repository.VideoKycSlotRepository;
import com.neo.springapp.repository.UserRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class VideoKycService {

    @Autowired
    private VideoKycSessionRepository sessionRepository;

    @Autowired
    private VideoKycAuditLogRepository auditLogRepository;

    @Autowired
    private VideoKycSlotRepository slotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordService passwordService;

    // ======================== Registration ========================

    @Transactional
    public VideoKycSession createSession(VideoKycSession session) {
        // Generate Customer ID (9-digit unique)
        session.setCustomerId(generateCustomerId());

        // Generate Temporary Account Number
        session.setTemporaryAccountNumber(generateTempAccountNumber());

        // Generate Room ID for WebRTC
        session.setRoomId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));

        session.setKycStatus("Pending");
        session.setKycAttemptCount(1);
        session.setSessionActive(false);

        VideoKycSession saved = sessionRepository.save(session);

        // Log
        createAuditLog(saved.getId(), null, null, "SESSION_CREATED",
                "Video KYC session created for " + saved.getFullName());

        return saved;
    }

    // ======================== Document Upload ========================

    @Transactional
    public VideoKycSession uploadDocuments(Long sessionId, byte[] aadharDoc, String aadharName,
                                           String aadharType, byte[] panDoc, String panName, String panType) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setAadharDocument(aadharDoc);
        session.setAadharDocumentName(aadharName);
        session.setAadharDocumentType(aadharType);
        session.setPanDocument(panDoc);
        session.setPanDocumentName(panName);
        session.setPanDocumentType(panType);
        session.setKycStatus("Documents Uploaded");

        VideoKycSession saved = sessionRepository.save(session);

        createAuditLog(sessionId, null, null, "DOCUMENTS_UPLOADED",
                "Aadhaar and PAN documents uploaded");

        return saved;
    }

    // ======================== Video KYC Session ========================

    @Transactional
    public VideoKycSession startVideoKyc(Long sessionId) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getKycAttemptCount() > session.getMaxAttempts()) {
            throw new RuntimeException("Maximum KYC attempts exceeded. Please contact support.");
        }

        // Generate 4-digit OTP
        String otp = String.format("%04d", new Random().nextInt(10000));
        session.setOtpCode(otp);
        session.setOtpVerified(false);
        session.setSessionActive(true);
        session.setSessionStartedAt(LocalDateTime.now());
        session.setKycStatus("Under Review");

        VideoKycSession saved = sessionRepository.save(session);

        createAuditLog(sessionId, null, null, "VIDEO_KYC_STARTED",
                "Video KYC session started. Room ID: " + session.getRoomId());

        return saved;
    }

    @Transactional
    public VideoKycSession verifyOtp(Long sessionId, String otp) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getOtpCode() != null && session.getOtpCode().equals(otp)) {
            session.setOtpVerified(true);
            return sessionRepository.save(session);
        }
        throw new RuntimeException("Invalid OTP");
    }

    @Transactional
    public VideoKycSession saveFaceSnapshot(Long sessionId, byte[] snapshot, String contentType) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setFaceSnapshot(snapshot);
        session.setFaceSnapshotType(contentType);
        return sessionRepository.save(session);
    }

    @Transactional
    public VideoKycSession saveIdSnapshot(Long sessionId, byte[] snapshot, String contentType) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setIdSnapshot(snapshot);
        session.setIdSnapshotType(contentType);
        return sessionRepository.save(session);
    }

    @Transactional
    public VideoKycSession completeLivenessCheck(Long sessionId, boolean passed, String checkType) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setLivenessCheckPassed(passed);
        session.setLivenessCheckType(checkType);

        VideoKycSession saved = sessionRepository.save(session);

        createAuditLog(sessionId, null, null, "LIVENESS_CHECK",
                "Liveness check " + (passed ? "passed" : "failed") + " (" + checkType + ")");

        return saved;
    }

    @Transactional
    public VideoKycSession endVideoSession(Long sessionId) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setSessionActive(false);
        session.setSessionEndedAt(LocalDateTime.now());

        if (session.getSessionStartedAt() != null) {
            long seconds = ChronoUnit.SECONDS.between(session.getSessionStartedAt(), session.getSessionEndedAt());
            session.setSessionDurationSeconds((int) seconds);
        }

        if (!"Approved".equals(session.getKycStatus()) && !"Rejected".equals(session.getKycStatus())) {
            session.setKycStatus("Under Review");
        }

        // Generate verification number if not already assigned
        if (session.getVerificationNumber() == null) {
            session.setVerificationNumber(generateVerificationNumber());
        }

        return sessionRepository.save(session);
    }

    // ======================== Admin Actions ========================

    @Transactional
    public VideoKycSession adminJoinSession(Long sessionId, Long adminId, String adminName) {
        // Check if admin is already in another active session
        Optional<VideoKycSession> activeSession = sessionRepository.findActiveSessionByAdmin(adminId);
        if (activeSession.isPresent() && !activeSession.get().getId().equals(sessionId)) {
            throw new RuntimeException("Admin is already in another active session");
        }

        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setAssignedAdminId(adminId);
        session.setAssignedAdminName(adminName);
        session.setSessionActive(true);
        if (session.getSessionStartedAt() == null) {
            session.setSessionStartedAt(LocalDateTime.now());
        }
        session.setKycStatus("Under Review");

        VideoKycSession saved = sessionRepository.save(session);

        createAuditLog(sessionId, adminId, adminName, "ADMIN_JOINED",
                "Admin " + adminName + " joined Video KYC session");

        return saved;
    }

    @Transactional
    public VideoKycSession approveKyc(Long sessionId, Long adminId, String adminName) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setKycStatus("Approved");
        session.setApprovedAt(LocalDateTime.now());
        session.setSessionActive(false);
        session.setSessionEndedAt(LocalDateTime.now());

        if (session.getSessionStartedAt() != null) {
            long seconds = ChronoUnit.SECONDS.between(session.getSessionStartedAt(), session.getSessionEndedAt());
            session.setSessionDurationSeconds((int) seconds);
        }

        String accountType = session.getAccountType() != null ? session.getAccountType() : "Savings";
        String finalAccountNumber = null;

        try {
            switch (accountType) {
                case "Current":
                    finalAccountNumber = approveCurrentAccount(session, adminName);
                    break;
                case "Salary":
                    finalAccountNumber = approveSalaryAccount(session, adminName);
                    break;
                default:
                    finalAccountNumber = approveSavingsAccount(session, adminName);
                    break;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Failed to activate " + accountType + " account: " + e.getMessage());
            // Generate a session-level account number as fallback
            finalAccountNumber = generateFinalAccountNumber();
        }

        session.setFinalAccountNumber(finalAccountNumber);
        VideoKycSession saved = sessionRepository.save(session);

        createAuditLog(sessionId, adminId, adminName, "KYC_APPROVED",
                "Admin " + adminName + " approved KYC for " + session.getFullName() +
                        " (" + accountType + "). Account Number: " + finalAccountNumber);

        return saved;
    }

    private String approveSavingsAccount(VideoKycSession session, String adminName) {
        String email = session.getEmail();
        if (email != null && !email.isEmpty()) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            User user;
            
            if (userOpt.isPresent()) {
                // User already exists - just approve it
                user = userOpt.get();
            } else {
                // User doesn't exist - CREATE new user from video KYC data
                System.out.println("📋 Creating new user from video KYC session for: " + email);
                user = new User();
                user.setEmail(email);
                user.setUsername(session.getFullName() != null ? session.getFullName() : email.split("@")[0]);
                user.setJoinDate(LocalDateTime.now());
                
                // Generate a random password - user will be required to set password after approval
                String tempPassword = generateTemporaryPassword();
                // ✅ Encrypt the password
                user.setPassword(passwordService.encryptPassword(tempPassword));
                user.setPasswordSet(false); // ✅ IMPORTANT: User must set password after approval
                user.setStatus("APPROVED"); // Set to APPROVED immediately as video KYC approved
            }

            // Generate account number and approve the user
            String accNumber = accountService.generateUniqueAccountNumberForNewAccount();
            user.setAccountNumber(accNumber);
            user.setStatus("APPROVED"); // Ensure status is APPROVED
            
            // ✅ Ensure passwordSet is false for password setup flow
            if (!user.isPasswordSet()) {
                user.setPasswordSet(false);
            }

            Account account = user.getAccount();
            if (account == null) {
                account = new Account();
                account.setName(session.getFullName() != null ? session.getFullName() : user.getUsername());
                account.setAadharNumber(session.getAadharNumber());
                account.setPan(session.getPanNumber());
                account.setPhone(session.getMobileNumber());
                account.setDob("1990-01-01"); // Default, can be updated later
                account.setAge(25); // Default
                account.setOccupation("Employee"); // Default
                account.setAccountType("Savings");
                account.setIncome(50000.0); // Default
                account.setAddress((session.getAddressCity() != null ? session.getAddressCity() : "") + ", " + 
                                  (session.getAddressState() != null ? session.getAddressState() : ""));
                account.setBalance(0.0);
                account.setStatus("ACTIVE");
                account.setCreatedAt(LocalDateTime.now());
                account.setLastUpdated(LocalDateTime.now());
                user.setAccount(account);
            } else {
                // Update existing account with video KYC data where missing
                if (account.getAadharNumber() == null || account.getAadharNumber().isEmpty()) {
                    account.setAadharNumber(session.getAadharNumber());
                }
                if (account.getPan() == null || account.getPan().isEmpty()) {
                    account.setPan(session.getPanNumber());
                }
                if (account.getPhone() == null || account.getPhone().isEmpty()) {
                    account.setPhone(session.getMobileNumber());
                }
                account.setLastUpdated(LocalDateTime.now());
            }
            
            account.setAccountNumber(accNumber);
            account.setKycVerified(true);

            session.setUserId(user.getId());
            session.setAccountId(account.getId());
            
            try {
                User savedUser = userRepository.save(user);
                System.out.println("✅ Savings account approved/created for: " + email + " | Account: " + accNumber + " | passwordSet: " + savedUser.isPasswordSet());
                return accNumber;
            } catch (Exception e) {
                System.out.println("❌ Error creating/approving user: " + e.getMessage());
                e.printStackTrace();
                return generateFinalAccountNumber();
            }
        }
        System.out.println("⚠️ No email provided - generating session account number");
        return generateFinalAccountNumber();
    }
    
    // Helper method to generate temporary password
    private String generateTemporaryPassword() {
        // Generate a random password that meets security requirements
        // At least 8 chars, uppercase, lowercase, number
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String all = upper + lower + digits;
        
        java.util.Random random = new java.util.Random();
        StringBuilder password = new StringBuilder();
        
        // Ensure at least one uppercase, one lowercase, one digit
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        
        // Fill remaining 5 characters
        for (int i = 3; i < 8; i++) {
            password.append(all.charAt(random.nextInt(all.length())));
        }
        
        // Shuffle
        java.util.List<Character> chars = new java.util.ArrayList<>();
        for (char c : password.toString().toCharArray()) {
            chars.add(c);
        }
        java.util.Collections.shuffle(chars);
        
        StringBuilder shuffled = new StringBuilder();
        for (char c : chars) {
            shuffled.append(c);
        }
        
        return shuffled.toString();
    }

    private String approveCurrentAccount(VideoKycSession session, String adminName) {
        String email = session.getEmail();
        if (email != null && !email.isEmpty()) {
            Optional<CurrentAccount> accOpt = currentAccountRepository.findByEmail(email);
            if (accOpt.isPresent()) {
                CurrentAccount currentAccount = accOpt.get();
                currentAccount.setStatus("ACTIVE");
                currentAccount.setKycVerified(true);
                currentAccount.setKycVerifiedDate(LocalDateTime.now());
                currentAccount.setKycVerifiedBy(adminName);
                currentAccount.setApprovedAt(LocalDateTime.now());
                currentAccount.setApprovedBy(adminName);
                currentAccountRepository.save(currentAccount);
                
                // Also create/update User account for Current Account
                try {
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    User user;
                    if (!userOpt.isPresent()) {
                        user = new User();
                        user.setEmail(email);
                        user.setUsername(session.getFullName() != null ? session.getFullName() : email.split("@")[0]);
                        String tempPassword = generateTemporaryPassword();
                        user.setPassword(passwordService.encryptPassword(tempPassword));
                        user.setPasswordSet(false);
                    } else {
                        user = userOpt.get();
                    }
                    user.setStatus("APPROVED");
                    user.setJoinDate(LocalDateTime.now());
                    userRepository.save(user);
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to create User for Current Account: " + e.getMessage());
                }
                
                System.out.println("✅ Current account approved for: " + email + " | Account: " + currentAccount.getAccountNumber());
                return currentAccount.getAccountNumber();
            }
        }
        System.out.println("⚠️ No Current Account found with email: " + email + " - generating session account number");
        return generateFinalAccountNumber();
    }

    private String approveSalaryAccount(VideoKycSession session, String adminName) {
        String email = session.getEmail();
        if (email != null && !email.isEmpty()) {
            SalaryAccount salaryAccount = salaryAccountRepository.findByEmail(email);
            if (salaryAccount != null) {
                salaryAccount.setStatus("Active");
                salaryAccountRepository.save(salaryAccount);
                
                // Also create/update User account for Salary Account
                try {
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    User user;
                    if (!userOpt.isPresent()) {
                        user = new User();
                        user.setEmail(email);
                        user.setUsername(session.getFullName() != null ? session.getFullName() : email.split("@")[0]);
                        String tempPassword = generateTemporaryPassword();
                        user.setPassword(passwordService.encryptPassword(tempPassword));
                        user.setPasswordSet(false);
                    } else {
                        user = userOpt.get();
                    }
                    user.setStatus("APPROVED");
                    user.setJoinDate(LocalDateTime.now());
                    userRepository.save(user);
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to create User for Salary Account: " + e.getMessage());
                }
                
                System.out.println("✅ Salary account approved for: " + email + " | Account: " + salaryAccount.getAccountNumber());
                return salaryAccount.getAccountNumber();
            }
        }
        System.out.println("⚠️ No Salary Account found with email: " + email + " - generating session account number");
        return generateFinalAccountNumber();
    }

    @Transactional
    public VideoKycSession rejectKyc(Long sessionId, Long adminId, String adminName, String reason) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setKycStatus("Rejected");
        session.setRejectionReason(reason);
        session.setSessionActive(false);
        session.setSessionEndedAt(LocalDateTime.now());

        if (session.getSessionStartedAt() != null && session.getSessionEndedAt() != null) {
            long seconds = ChronoUnit.SECONDS.between(session.getSessionStartedAt(), session.getSessionEndedAt());
            session.setSessionDurationSeconds((int) seconds);
        }

        VideoKycSession saved = sessionRepository.save(session);

        createAuditLog(sessionId, adminId, adminName, "KYC_REJECTED",
                "Admin " + adminName + " rejected KYC for " + session.getFullName() +
                        ". Reason: " + reason);

        return saved;
    }

    @Transactional
    public VideoKycSession reopenSession(Long sessionId, Long adminId, String adminName) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!"Rejected".equals(session.getKycStatus())) {
            throw new RuntimeException("Only rejected sessions can be re-opened");
        }

        if (session.getKycAttemptCount() >= session.getMaxAttempts()) {
            throw new RuntimeException("Maximum KYC attempts exceeded");
        }

        session.setKycStatus("Pending");
        session.setRejectionReason(null);
        session.setKycAttemptCount(session.getKycAttemptCount() + 1);
        session.setSessionActive(false);
        session.setOtpCode(null);
        session.setOtpVerified(false);
        session.setLivenessCheckPassed(false);

        // Generate new Room ID
        session.setRoomId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));

        VideoKycSession saved = sessionRepository.save(session);

        createAuditLog(sessionId, adminId, adminName, "SESSION_REOPENED",
                "Admin " + adminName + " re-opened KYC session for " + session.getFullName() +
                        ". Attempt: " + session.getKycAttemptCount());

        return saved;
    }

    @Transactional
    public VideoKycSession forceReVerification(Long sessionId, Long adminId, String adminName) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setKycStatus("Pending");
        session.setRejectionReason(null);
        session.setSessionActive(false);
        session.setOtpCode(null);
        session.setOtpVerified(false);
        session.setLivenessCheckPassed(false);
        session.setFaceSnapshot(null);
        session.setIdSnapshot(null);

        // Generate new Room ID
        session.setRoomId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));

        VideoKycSession saved = sessionRepository.save(session);

        createAuditLog(sessionId, adminId, adminName, "FORCE_REVERIFICATION",
                "Admin " + adminName + " forced re-verification for " + session.getFullName());

        return saved;
    }

    // ======================== Query Methods ========================

    public VideoKycSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    public VideoKycSession getSessionByRoom(String roomId) {
        return sessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Session not found for room: " + roomId));
    }

    public VideoKycSession getSessionByTempAccount(String tempAccountNumber) {
        return sessionRepository.findByTemporaryAccountNumber(tempAccountNumber)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    public Optional<VideoKycSession> findByMobile(String mobileNumber) {
        return sessionRepository.findByMobileNumber(mobileNumber);
    }

    public Page<VideoKycSession> getAllSessions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return sessionRepository.findAll(pageable);
    }

    public Page<VideoKycSession> getSessionsByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return sessionRepository.findByKycStatus(status, pageable);
    }

    public List<VideoKycSession> getKycQueue() {
        return sessionRepository.findByKycStatusIn(Arrays.asList("Pending", "Under Review"));
    }

    public Page<VideoKycSession> searchSessions(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return sessionRepository.searchSessions(search, pageable);
    }

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", sessionRepository.count());
        stats.put("pending", sessionRepository.countByKycStatus("Pending"));
        stats.put("scheduled", sessionRepository.countByKycStatus("Scheduled"));
        stats.put("underReview", sessionRepository.countByKycStatus("Under Review"));
        stats.put("approved", sessionRepository.countByKycStatus("Approved"));
        stats.put("rejected", sessionRepository.countByKycStatus("Rejected"));
        return stats;
    }

    // ======================== Audit Logs ========================

    public List<VideoKycAuditLog> getAuditLogs(Long sessionId) {
        return auditLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    public Page<VideoKycAuditLog> getAllAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return auditLogRepository.findAll(pageable);
    }

    // ======================== Status Check ========================

    public VideoKycSession checkStatus(String tempAccountNumber) {
        return sessionRepository.findByTemporaryAccountNumber(tempAccountNumber)
                .orElse(null);
    }

    public VideoKycSession checkStatusByMobile(String mobileNumber) {
        return sessionRepository.findByMobileNumber(mobileNumber)
                .orElse(null);
    }

    // ======================== Slot Management ========================

    @Transactional
    public VideoKycSlot createSlot(LocalDate date, LocalTime time, LocalTime endTime, int maxBookings, String createdBy) {
        VideoKycSlot slot = new VideoKycSlot();
        slot.setSlotDate(date);
        slot.setSlotTime(time);
        slot.setSlotEndTime(endTime);
        slot.setMaxBookings(maxBookings);
        slot.setCurrentBookings(0);
        slot.setIsActive(true);
        slot.setCreatedBy(createdBy);
        return slotRepository.save(slot);
    }

    public List<VideoKycSlot> getAvailableSlots() {
        List<VideoKycSlot> slots = slotRepository.findAvailableSlots(LocalDate.now());
        if (slots.isEmpty()) {
            // Auto-generate default slots for the next 7 days
            generateDefaultSlots();
            slots = slotRepository.findAvailableSlots(LocalDate.now());
        }
        return slots;
    }

    @Transactional
    public void generateDefaultSlots() {
        LocalDate today = LocalDate.now();
        LocalTime[] startTimes = {
            LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0),
            LocalTime.of(12, 0), LocalTime.of(14, 0), LocalTime.of(15, 0),
            LocalTime.of(16, 0), LocalTime.of(17, 0)
        };
        LocalTime[] endTimes = {
            LocalTime.of(9, 30), LocalTime.of(10, 30), LocalTime.of(11, 30),
            LocalTime.of(12, 30), LocalTime.of(14, 30), LocalTime.of(15, 30),
            LocalTime.of(16, 30), LocalTime.of(17, 30)
        };

        for (int day = 0; day < 7; day++) {
            LocalDate slotDate = today.plusDays(day);
            // Skip if slots already exist for this date
            if (slotRepository.countByDate(slotDate) > 0) continue;

            for (int i = 0; i < startTimes.length; i++) {
                VideoKycSlot slot = new VideoKycSlot();
                slot.setSlotDate(slotDate);
                slot.setSlotTime(startTimes[i]);
                slot.setSlotEndTime(endTimes[i]);
                slot.setMaxBookings(5);
                slot.setCurrentBookings(0);
                slot.setIsActive(true);
                slot.setCreatedBy("System");
                slotRepository.save(slot);
            }
        }
        System.out.println("✅ Auto-generated default Video KYC slots for next 7 days");
    }

    public List<VideoKycSlot> getAllActiveSlots() {
        return slotRepository.findByIsActiveTrueOrderBySlotDateAscSlotTimeAsc();
    }

    public List<VideoKycSlot> getSlotsByDate(LocalDate date) {
        return slotRepository.findBySlotDateAndIsActiveTrueOrderBySlotTimeAsc(date);
    }

    @Transactional
    public VideoKycSlot cancelSlot(Long slotId) {
        VideoKycSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        slot.setIsActive(false);
        return slotRepository.save(slot);
    }

    @Transactional
    public VideoKycSlot rescheduleSlot(Long slotId, LocalDate newDate, LocalTime newTime, LocalTime newEndTime) {
        VideoKycSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        slot.setSlotDate(newDate);
        slot.setSlotTime(newTime);
        slot.setSlotEndTime(newEndTime);
        return slotRepository.save(slot);
    }

    // ======================== Slot Booking (User) ========================

    @Transactional
    public VideoKycSession bookSlot(Long sessionId, Long slotId) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Check if user already has a booked slot
        if (session.getBookedSlotId() != null) {
            throw new RuntimeException("You already have a scheduled slot. Please cancel the existing one first.");
        }

        VideoKycSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.getIsActive()) {
            throw new RuntimeException("This slot is no longer available");
        }
        if (slot.getCurrentBookings() >= slot.getMaxBookings()) {
            throw new RuntimeException("This slot is fully booked. Please select another slot.");
        }

        // Book it
        slot.setCurrentBookings(slot.getCurrentBookings() + 1);
        slotRepository.save(slot);

        session.setBookedSlotId(slot.getId());
        session.setSlotDate(slot.getSlotDate());
        session.setSlotTime(slot.getSlotTime());
        session.setSlotEndTime(slot.getSlotEndTime());
        session.setKycStatus("Scheduled");

        VideoKycSession saved = sessionRepository.save(session);

        createAuditLog(sessionId, null, null, "SLOT_BOOKED",
                "Slot booked for " + slot.getSlotDate() + " " + slot.getSlotTime());

        return saved;
    }

    @Transactional
    public VideoKycSession cancelBooking(Long sessionId) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getBookedSlotId() == null) {
            throw new RuntimeException("No slot booked for this session");
        }

        // Free up the slot
        VideoKycSlot slot = slotRepository.findById(session.getBookedSlotId()).orElse(null);
        if (slot != null && slot.getCurrentBookings() > 0) {
            slot.setCurrentBookings(slot.getCurrentBookings() - 1);
            slotRepository.save(slot);
        }

        session.setBookedSlotId(null);
        session.setSlotDate(null);
        session.setSlotTime(null);
        session.setSlotEndTime(null);
        session.setKycStatus("Documents Uploaded");

        VideoKycSession saved = sessionRepository.save(session);

        createAuditLog(sessionId, null, null, "SLOT_CANCELLED", "Slot booking cancelled");

        return saved;
    }

    // ======================== Verification Number ========================

    private String generateVerificationNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq = String.format("%03d", new Random().nextInt(999) + 1);
        return "VKYC" + dateStr + seq;
    }

    public VideoKycSession verifyByNumber(String verificationNumber) {
        return sessionRepository.findByVerificationNumber(verificationNumber).orElse(null);
    }

    // ======================== Enhanced Start Video (Slot-aware) ========================

    public boolean canJoinVideoKyc(Long sessionId) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getSlotDate() == null || session.getSlotTime() == null) {
            return true; // No slot booked, allow anytime
        }

        LocalDateTime slotStart = LocalDateTime.of(session.getSlotDate(), session.getSlotTime());
        LocalDateTime slotEnd = LocalDateTime.of(session.getSlotDate(), session.getSlotEndTime());
        LocalDateTime now = LocalDateTime.now();

        // Allow joining 5 minutes before slot time until slot end
        return now.isAfter(slotStart.minusMinutes(5)) && now.isBefore(slotEnd);
    }

    // ======================== Private Helpers ========================

    private void createAuditLog(Long sessionId, Long adminId, String adminName,
                                String action, String details) {
        VideoKycAuditLog log = new VideoKycAuditLog();
        log.setSessionId(sessionId);
        log.setAdminId(adminId);
        log.setAdminName(adminName);
        log.setAction(action);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private String generateCustomerId() {
        return "CUS" + String.format("%06d", new Random().nextInt(999999) + 1);
    }

    private String generateTempAccountNumber() {
        return "TEMP" + System.currentTimeMillis() % 100000000L;
    }

    private String generateFinalAccountNumber() {
        return "NEOB" + String.format("%010d", System.currentTimeMillis() % 10000000000L);
    }
}
