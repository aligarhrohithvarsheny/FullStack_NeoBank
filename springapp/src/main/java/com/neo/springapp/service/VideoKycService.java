package com.neo.springapp.service;

import com.neo.springapp.model.VideoKycSession;
import com.neo.springapp.model.VideoKycAuditLog;
import com.neo.springapp.model.VideoKycSlot;
import com.neo.springapp.model.AdminAccountApplication;
import com.neo.springapp.model.User;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.VideoKycSessionRepository;
import com.neo.springapp.repository.VideoKycAuditLogRepository;
import com.neo.springapp.repository.VideoKycSlotRepository;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AdminAccountApplicationRepository;
import com.neo.springapp.repository.UserRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.SalaryAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
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
    private AccountRepository accountRepository;

    @Autowired
    private AdminAccountApplicationRepository accountApplicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private SalaryAccountService salaryAccountService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

        hydrateSessionFromAccountApplication(session);

        session.setKycStatus("Approved");
        session.setApprovedAt(LocalDateTime.now());
        session.setSessionActive(false);
        session.setSessionEndedAt(LocalDateTime.now());

        if (session.getSessionStartedAt() != null) {
            long seconds = ChronoUnit.SECONDS.between(session.getSessionStartedAt(), session.getSessionEndedAt());
            session.setSessionDurationSeconds((int) seconds);
        }

        String accountType = session.getAccountType() != null ? session.getAccountType() : "Savings";
        String finalAccountNumber;

        if ("Salary".equalsIgnoreCase(accountType)) {
            if ("APPROVED".equalsIgnoreCase(session.getManagerApprovalStatus())) {
                finalAccountNumber = session.getFinalAccountNumber();
            } else {
                SalaryAccount salaryAccount = isBlank(session.getEmail())
                        ? null
                        : salaryAccountRepository.findByEmail(session.getEmail());
                if (salaryAccount != null && !"Closed".equalsIgnoreCase(salaryAccount.getStatus())
                        && !"Frozen".equalsIgnoreCase(salaryAccount.getStatus())) {
                    salaryAccount.setStatus("Pending");
                    salaryAccountRepository.save(salaryAccount);
                }
                session.setManagerApprovalStatus("PENDING");
                finalAccountNumber = null;
            }
        } else if (isUnmaterializedFallbackAccount(session, accountType)) {
            finalAccountNumber = persistFallbackAccount(session, accountType, adminName, session.getFinalAccountNumber());
        } else {
            try {
                finalAccountNumber = activateAccountInNewTransaction(session, accountType, adminName);
            } catch (Exception e) {
                System.out.println("⚠️ Failed to activate " + accountType + " account: " + e.getMessage());
                finalAccountNumber = persistFallbackAccount(session, accountType, adminName, generateUniqueFinalAccountNumber(session));
            }
        }

        session.setFinalAccountNumber(finalAccountNumber);
        VideoKycSession saved = sessionRepository.save(session);

        createAuditLog(sessionId, adminId, adminName, "KYC_APPROVED",
                "Admin " + adminName + " approved KYC for " + session.getFullName() +
                        " (" + accountType + "). Account Number: " + finalAccountNumber);

        return saved;
    }

    public List<Map<String, Object>> getSalaryKycApprovalsPending() {
        List<Map<String, Object>> approvals = new ArrayList<>();
        for (VideoKycSession session : sessionRepository.findSalarySessionsPendingManagerApproval()) {
            AdminAccountApplication application = findAccountApplication(session);
            SalaryAccount account = isBlank(session.getEmail())
                    ? null
                    : salaryAccountRepository.findByEmail(session.getEmail());

            Map<String, Object> approval = new HashMap<>();
            approval.put("id", session.getId());
            approval.put("customerId", session.getCustomerId());
            approval.put("fullName", firstNonBlank(session.getFullName(), application == null ? null : application.getFullName()));
            approval.put("mobileNumber", firstNonBlank(session.getMobileNumber(), application == null ? null : application.getPhone()));
            approval.put("email", firstNonBlank(session.getEmail(), application == null ? null : application.getEmail()));
            approval.put("companyName", application != null ? application.getCompanyName() : account == null ? null : account.getCompanyName());
            approval.put("designation", application != null ? application.getDesignation() : account == null ? null : account.getDesignation());
            approval.put("monthlySalary", application != null ? application.getMonthlySalary() : account == null ? null : account.getMonthlySalary());
            approval.put("salaryCreditDate", application != null ? application.getSalaryCreditDate() : account == null ? null : account.getSalaryCreditDate());
            approval.put("branchName", application != null ? application.getBranchName() : account == null ? null : account.getBranchName());
            approval.put("accountNumber", account == null ? null : account.getAccountNumber());
            approval.put("applicationNumber", application == null ? null : application.getApplicationNumber());
            approval.put("kycApprovedAt", session.getApprovedAt());
            approval.put("managerApprovalStatus", session.getManagerApprovalStatus());
            approvals.add(approval);
        }
        return approvals;
    }

    @Transactional
    public VideoKycSession managerApproveSalaryKyc(Long sessionId, String managerName) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Video KYC session not found"));

        if (!"Approved".equalsIgnoreCase(session.getKycStatus())
                || !"Salary".equalsIgnoreCase(session.getAccountType())) {
            throw new IllegalStateException("Only admin-approved Salary account Video KYC sessions can be approved here.");
        }
        if (!"PENDING".equalsIgnoreCase(session.getManagerApprovalStatus())) {
            throw new IllegalStateException("This Salary account is no longer awaiting manager approval.");
        }

        AdminAccountApplication application = findAccountApplication(session);
        SalaryAccount account = isBlank(session.getEmail())
                ? null
                : salaryAccountRepository.findByEmail(session.getEmail());

        if (account == null) {
            if (application == null) {
                throw new IllegalStateException("Salary account details could not be found for this Video KYC session.");
            }
            account = createSalaryAccountFromApplication(session, application, managerName);
        } else {
            if ("Closed".equalsIgnoreCase(account.getStatus()) || "Frozen".equalsIgnoreCase(account.getStatus())) {
                throw new IllegalStateException("A closed or frozen Salary account cannot be activated.");
            }
            account.setStatus("Active");
            account = salaryAccountRepository.save(account);
        }

        ensureApprovedSalaryUser(session, account);
        if (application != null) {
            LocalDateTime approvedAt = LocalDateTime.now();
            application.setAdminVerified(true);
            application.setAdminVerifiedBy(firstNonBlank(session.getAssignedAdminName(), "Video KYC Admin"));
            if (application.getAdminVerifiedDate() == null) {
                application.setAdminVerifiedDate(session.getApprovedAt() != null ? session.getApprovedAt() : approvedAt);
            }
            application.setManagerApproved(true);
            application.setManagerApprovedBy(managerName);
            application.setManagerApprovedDate(approvedAt);
            application.setManagerRemarks("Approved after successful Video KYC.");
            application.setStatus("ACTIVE");
            application.setAccountNumber(account.getAccountNumber());
            application.setCustomerId(account.getCustomerId());
            application.setUpdatedAt(approvedAt);
            accountApplicationRepository.save(application);
        }

        session.setFinalAccountNumber(account.getAccountNumber());
        session.setManagerApprovalStatus("APPROVED");
        session.setManagerApprovedBy(managerName);
        session.setManagerApprovedAt(LocalDateTime.now());
        VideoKycSession saved = sessionRepository.save(session);
        createAuditLog(sessionId, null, managerName, "SALARY_ACCOUNT_MANAGER_APPROVED",
                "Manager " + managerName + " approved Salary account opening for " +
                        session.getFullName() + ". Account Number: " + account.getAccountNumber());
        return saved;
    }

    private SalaryAccount createSalaryAccountFromApplication(
            VideoKycSession session, AdminAccountApplication application, String managerName) {
        if (isBlank(session.getEmail()) || isBlank(session.getAadharNumber())
                || isBlank(session.getPanNumber()) || isBlank(session.getMobileNumber())) {
            throw new IllegalStateException("Verified email, Aadhaar, PAN, and mobile are required to open this Salary account.");
        }

        SalaryAccount account = new SalaryAccount();
        account.setEmployeeName(firstNonBlank(session.getFullName(), application.getFullName()));
        account.setDob(application.getDateOfBirth());
        account.setMobileNumber(session.getMobileNumber());
        account.setEmail(session.getEmail());
        account.setAadharNumber(session.getAadharNumber());
        account.setPanNumber(session.getPanNumber());
        account.setCompanyName(application.getCompanyName());
        account.setCompanyId(application.getCompanyId());
        account.setEmployerAddress(application.getEmployerAddress());
        account.setHrContactNumber(application.getHrContactNumber());
        account.setMonthlySalary(application.getMonthlySalary() == null ? 0.0 : application.getMonthlySalary());
        account.setSalaryCreditDate(application.getSalaryCreditDate() == null ? 1 : application.getSalaryCreditDate());
        account.setDesignation(application.getDesignation());
        account.setBranchName(application.getBranchName() == null ? "NeoBank Main Branch" : application.getBranchName());
        account.setIfscCode(application.getIfscCode() == null ? "EZYV000123" : application.getIfscCode());
        account.setAddress(buildAddress(session, application));
        account.setStatus("Active");
        account.setPassword(passwordService.encryptPassword(generateTemporaryPassword()));
        return salaryAccountService.createAccount(account, managerName);
    }

    private void ensureApprovedSalaryUser(VideoKycSession session, SalaryAccount account) {
        if (isBlank(session.getEmail())) return;

        User user = userRepository.findByEmail(session.getEmail())
                .orElseGet(() -> {
                    User created = new User();
                    created.setEmail(session.getEmail());
                    created.setUsername(firstNonBlank(session.getFullName(), session.getEmail().split("@")[0]));
                    created.setPassword(passwordService.encryptPassword(generateTemporaryPassword()));
                    created.setPasswordSet(false);
                    return created;
                });
        user.setStatus("APPROVED");
        user.setPasswordSet(false);
        user.setJoinDate(LocalDateTime.now());
        userRepository.save(user);
    }

    private String activateAccountInNewTransaction(VideoKycSession session, String accountType, String adminName) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> {
            switch (accountType) {
                case "Current":
                    return approveCurrentAccount(session, adminName);
                case "Salary":
                    return approveSalaryAccount(session, adminName);
                default:
                    return approveSavingsAccount(session, adminName);
            }
        });
    }

    private void hydrateSessionFromAccountApplication(VideoKycSession session) {
        if (session.getEmail() == null || session.getEmail().isBlank()) return;
        accountApplicationRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(session.getEmail()).stream()
                .filter(application -> session.getAccountType() == null
                        || session.getAccountType().equalsIgnoreCase(application.getAccountType()))
                .findFirst()
                .ifPresent(application -> {
                    if (isBlank(session.getFullName())) session.setFullName(application.getFullName());
                    if (isBlank(session.getMobileNumber())) session.setMobileNumber(application.getPhone());
                    if (isBlank(session.getAadharNumber())) session.setAadharNumber(application.getAadharNumber());
                    if (isBlank(session.getPanNumber())) session.setPanNumber(application.getPanNumber());
                    if (isBlank(session.getAddressCity())) session.setAddressCity(application.getCity());
                    if (isBlank(session.getAddressState())) session.setAddressState(application.getState());
                });
    }

    private String persistFallbackAccount(VideoKycSession session, String accountType, String adminName, String accountNumber) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> {
            switch (accountType) {
                case "Current":
                    return persistFallbackCurrentAccount(session, adminName, accountNumber);
                case "Salary":
                    return persistFallbackSalaryAccount(session, adminName, accountNumber);
                default:
                    return persistFallbackSavingsAccount(session, adminName, accountNumber);
            }
        });
    }

    private String persistFallbackSavingsAccount(VideoKycSession session, String adminName, String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            String aadhar = session.getAadharNumber();
            String pan = session.getPanNumber();
            String phone = session.getMobileNumber();
            if (isBlank(aadhar) || isBlank(pan) || isBlank(phone) || isBlank(session.getEmail())) {
                throw new IllegalStateException("Aadhaar, PAN, mobile, and email are required to activate this Savings account.");
            }

            AdminAccountApplication application = findAccountApplication(session);
            account = new Account();
            account.setName(firstNonBlank(session.getFullName(), application == null ? null : application.getFullName()));
            account.setAadharNumber(aadhar);
            account.setPan(pan);
            account.setPhone(phone);
            account.setAccountNumber(accountNumber);
            account.setAccountType("Savings");
            account.setDob(application != null && !isBlank(application.getDateOfBirth()) ? application.getDateOfBirth() : "1990-01-01");
            account.setAge(application != null && application.getAge() != null ? application.getAge() : 25);
            account.setOccupation(application != null && !isBlank(application.getOccupation()) ? application.getOccupation() : "Employee");
            account.setIncome(application != null && application.getIncome() != null ? application.getIncome() : 0.0);
            account.setAddress(buildAddress(session, application));
            account.setBalance(0.0);
            account.setStatus("ACTIVE");
            account.setKycVerified(true);
            account.setCreatedAt(LocalDateTime.now());
            account.setLastUpdated(LocalDateTime.now());
        }

        User user = userRepository.findByEmail(session.getEmail())
                .orElseGet(() -> {
                    User created = new User();
                    created.setEmail(session.getEmail());
                    created.setUsername(firstNonBlank(session.getFullName(), session.getEmail().split("@")[0]));
                    created.setJoinDate(LocalDateTime.now());
                    created.setPassword(passwordService.encryptPassword(generateTemporaryPassword()));
                    return created;
                });
        if (user.getAccount() != null && !accountNumber.equals(user.getAccount().getAccountNumber())) {
            return user.getAccount().getAccountNumber();
        }
        user.setAccount(account);
        user.setAccountNumber(account.getAccountNumber());
        user.setStatus("APPROVED");
        user.setPasswordSet(false);
        User savedUser = userRepository.save(user);
        session.setUserId(savedUser.getId());
        session.setAccountId(savedUser.getAccount().getId());
        System.out.println("✅ Persisted fallback Savings account for " + adminName + ": " + accountNumber);
        return accountNumber;
    }

    private String persistFallbackCurrentAccount(VideoKycSession session, String adminName, String accountNumber) {
        Optional<CurrentAccount> existing = currentAccountRepository.findByEmail(session.getEmail());
        if (existing.isPresent()) return existing.get().getAccountNumber();

        AdminAccountApplication application = findAccountApplication(session);
        if (application == null || isBlank(application.getBusinessName()) || isBlank(application.getBusinessType())
                || isBlank(session.getAadharNumber()) || isBlank(session.getPanNumber()) || isBlank(session.getMobileNumber())) {
            throw new IllegalStateException("Verified business and identity details are required to activate this Current account.");
        }

        CurrentAccount account = new CurrentAccount();
        account.setAccountNumber(accountNumber);
        account.setBusinessName(application.getBusinessName());
        account.setBusinessType(application.getBusinessType());
        account.setBusinessRegistrationNumber(application.getBusinessRegistrationNumber());
        account.setGstNumber(application.getGstNumber());
        account.setOwnerName(firstNonBlank(session.getFullName(), application.getFullName()));
        account.setMobile(session.getMobileNumber());
        account.setEmail(session.getEmail());
        account.setAadharNumber(session.getAadharNumber());
        account.setPanNumber(session.getPanNumber());
        account.setShopAddress(application.getShopAddress());
        account.setCity(application.getCity());
        account.setState(application.getState());
        account.setPincode(application.getPincode());
        account.setBranchName(application.getBranchName());
        account.setIfscCode(application.getIfscCode());
        account.setBalance(0.0);
        account.setStatus("ACTIVE");
        account.setKycVerified(true);
        account.setKycVerifiedBy(adminName);
        account.setKycVerifiedDate(LocalDateTime.now());
        account.setApprovedBy(adminName);
        account.setApprovedAt(LocalDateTime.now());
        currentAccountRepository.save(account);
        return accountNumber;
    }

    private String persistFallbackSalaryAccount(VideoKycSession session, String adminName, String accountNumber) {
        SalaryAccount existing = salaryAccountRepository.findByEmail(session.getEmail());
        if (existing != null) return existing.getAccountNumber();

        if (isBlank(session.getAadharNumber()) || isBlank(session.getPanNumber()) || isBlank(session.getMobileNumber())) {
            throw new IllegalStateException("Verified identity details are required to activate this Salary account.");
        }
        AdminAccountApplication application = findAccountApplication(session);
        SalaryAccount account = new SalaryAccount();
        account.setEmployeeName(firstNonBlank(session.getFullName(), application == null ? null : application.getFullName()));
        account.setDob(application == null ? null : application.getDateOfBirth());
        account.setMobileNumber(session.getMobileNumber());
        account.setEmail(session.getEmail());
        account.setAadharNumber(session.getAadharNumber());
        account.setPanNumber(session.getPanNumber());
        account.setCompanyName(application == null ? null : application.getCompanyName());
        account.setCompanyId(application == null ? null : application.getCompanyId());
        account.setEmployerAddress(application == null ? null : application.getEmployerAddress());
        account.setHrContactNumber(application == null ? null : application.getHrContactNumber());
        account.setMonthlySalary(application == null ? 0.0 : application.getMonthlySalary());
        account.setSalaryCreditDate(application == null || application.getSalaryCreditDate() == null ? 1 : application.getSalaryCreditDate());
        account.setDesignation(application == null ? null : application.getDesignation());
        account.setAccountNumber(accountNumber);
        account.setBranchName(application == null ? "NeoBank Main Branch" : application.getBranchName());
        account.setIfscCode(application == null ? "EZYV000123" : application.getIfscCode());
        account.setAddress(buildAddress(session, application));
        account.setStatus("Active");
        salaryAccountService.createAccount(account, adminName);
        return accountNumber;
    }

    private AdminAccountApplication findAccountApplication(VideoKycSession session) {
        if (isBlank(session.getEmail())) return null;
        List<AdminAccountApplication> applications = accountApplicationRepository
                .findByEmailIgnoreCaseOrderByCreatedAtDesc(session.getEmail());
        return applications.stream()
                .filter(application -> session.getAccountType() == null
                        || session.getAccountType().equalsIgnoreCase(application.getAccountType()))
                .findFirst()
                .orElse(applications.isEmpty() ? null : applications.get(0));
    }

    private boolean isUnmaterializedFallbackAccount(VideoKycSession session, String accountType) {
        String accountNumber = session.getFinalAccountNumber();
        if (accountNumber == null || !accountNumber.matches("NEOB\\d{10}")) return false;
        return switch (accountType) {
            case "Current" -> currentAccountRepository.findByAccountNumber(accountNumber).isEmpty();
            case "Salary" -> salaryAccountRepository.findByAccountNumber(accountNumber) == null;
            default -> accountRepository.findByAccountNumber(accountNumber) == null;
        };
    }

    private String generateUniqueFinalAccountNumber(VideoKycSession session) {
        for (int attempt = 0; attempt < 50; attempt++) {
            String candidate = generateFinalAccountNumber();
            boolean used = accountRepository.findByAccountNumber(candidate) != null
                    || currentAccountRepository.findByAccountNumber(candidate).isPresent()
                    || salaryAccountRepository.findByAccountNumber(candidate) != null
                    || sessionRepository.findByFinalAccountNumber(candidate)
                            .filter(existing -> !existing.getId().equals(session.getId())).isPresent();
            if (!used) return candidate;
        }
        throw new IllegalStateException("Unable to generate a unique fallback account number.");
    }

    private String buildAddress(VideoKycSession session, AdminAccountApplication application) {
        if (application != null && !isBlank(application.getAddress())) return application.getAddress();
        return firstNonBlank(session.getAddressCity(), "") + ", " + firstNonBlank(session.getAddressState(), "");
    }

    private String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
            
            // ✅ Always require fresh password setup after KYC approval
            user.setPasswordSet(false);

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

            User savedUser = userRepository.save(user);
            session.setUserId(savedUser.getId());
            session.setAccountId(savedUser.getAccount().getId());
            System.out.println("✅ Savings account approved/created for: " + email + " | Account: " + accNumber + " | passwordSet: " + savedUser.isPasswordSet());
            return accNumber;
        }
        throw new IllegalStateException("An email is required to activate a Savings account.");
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
                    // Enforce first-login password setup for approved accounts
                    user.setPasswordSet(false);
                    user.setJoinDate(LocalDateTime.now());
                    userRepository.save(user);
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to create User for Current Account: " + e.getMessage());
                }
                
                System.out.println("✅ Current account approved for: " + email + " | Account: " + currentAccount.getAccountNumber());
                return currentAccount.getAccountNumber();
            }
        }
        throw new IllegalStateException("No Current account record was found for Video KYC approval.");
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
                    // Enforce first-login password setup for approved accounts
                    user.setPasswordSet(false);
                    user.setJoinDate(LocalDateTime.now());
                    userRepository.save(user);
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to create User for Salary Account: " + e.getMessage());
                }
                
                System.out.println("✅ Salary account approved for: " + email + " | Account: " + salaryAccount.getAccountNumber());
                return salaryAccount.getAccountNumber();
            }
        }
        throw new IllegalStateException("No Salary account record was found for Video KYC approval.");
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
        VideoKycSlot saved = slotRepository.save(slot);
        for (VideoKycSession session : sessionRepository.findByBookedSlotId(slotId)) {
            session.setBookedSlotId(null);
            session.setSlotDate(null);
            session.setSlotTime(null);
            session.setSlotEndTime(null);
            session.setKycStatus("Documents Uploaded");
            sessionRepository.save(session);
            createAuditLog(session.getId(), null, null, "SLOT_CANCELLED_BY_ADMIN",
                "Admin cancelled the scheduled Video KYC slot");
        }
        return saved;
    }

    @Transactional
    public VideoKycSlot rescheduleSlot(Long slotId, LocalDate newDate, LocalTime newTime, LocalTime newEndTime) {
        VideoKycSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        slot.setSlotDate(newDate);
        slot.setSlotTime(newTime);
        slot.setSlotEndTime(newEndTime);
        VideoKycSlot saved = slotRepository.save(slot);
        for (VideoKycSession session : sessionRepository.findByBookedSlotId(slotId)) {
            session.setSlotDate(newDate);
            session.setSlotTime(newTime);
            session.setSlotEndTime(newEndTime);
            sessionRepository.save(session);
            createAuditLog(session.getId(), null, null, "SLOT_UPDATED_BY_ADMIN",
                "Admin updated the scheduled Video KYC slot");
        }
        return saved;
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

    @Transactional
    public VideoKycSession rescheduleBooking(Long sessionId, Long newSlotId) {
        VideoKycSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (session.getBookedSlotId() == null) {
            return bookSlot(sessionId, newSlotId);
        }
        if (session.getBookedSlotId().equals(newSlotId)) {
            throw new RuntimeException("Please select a different slot");
        }
        VideoKycSlot newSlot = slotRepository.findById(newSlotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        if (!Boolean.TRUE.equals(newSlot.getIsActive()) || newSlot.getCurrentBookings() >= newSlot.getMaxBookings()) {
            throw new RuntimeException("This slot is no longer available");
        }
        VideoKycSlot oldSlot = slotRepository.findById(session.getBookedSlotId()).orElse(null);
        if (oldSlot != null && oldSlot.getCurrentBookings() > 0) {
            oldSlot.setCurrentBookings(oldSlot.getCurrentBookings() - 1);
            slotRepository.save(oldSlot);
        }
        newSlot.setCurrentBookings(newSlot.getCurrentBookings() + 1);
        slotRepository.save(newSlot);
        session.setBookedSlotId(newSlot.getId());
        session.setSlotDate(newSlot.getSlotDate());
        session.setSlotTime(newSlot.getSlotTime());
        session.setSlotEndTime(newSlot.getSlotEndTime());
        session.setKycStatus("Scheduled");
        VideoKycSession saved = sessionRepository.save(session);
        createAuditLog(sessionId, null, null, "SLOT_RESCHEDULED",
                "Slot changed to " + newSlot.getSlotDate() + " " + newSlot.getSlotTime());
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
