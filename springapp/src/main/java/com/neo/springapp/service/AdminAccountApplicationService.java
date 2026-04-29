package com.neo.springapp.service;

import com.neo.springapp.model.AdminAccountApplication;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.AdminAccountApplicationRepository;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AdminAccountApplicationService {

    @Autowired
    private AdminAccountApplicationRepository applicationRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private SalaryAccountService salaryAccountService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordService passwordService;

    // ==================== Application Management ====================

    public AdminAccountApplication createApplication(AdminAccountApplication application) {
        application.setApplicationNumber(generateApplicationNumber());
        application.setStatus("PENDING");
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        return applicationRepository.save(application);
    }

    public Optional<AdminAccountApplication> getById(Long id) {
        return applicationRepository.findById(id);
    }

    public AdminAccountApplication saveApplication(AdminAccountApplication application) {
        return applicationRepository.save(application);
    }

    public Optional<AdminAccountApplication> getByApplicationNumber(String applicationNumber) {
        return applicationRepository.findByApplicationNumber(applicationNumber);
    }

    public Optional<AdminAccountApplication> getByAccountNumber(String accountNumber) {
        return applicationRepository.findByAccountNumber(accountNumber);
    }

    public List<AdminAccountApplication> getAllApplications() {
        return applicationRepository.findAllOrderByCreatedAtDesc();
    }

    public List<AdminAccountApplication> getByStatus(String status) {
        return applicationRepository.findByStatus(status);
    }

    public List<AdminAccountApplication> getByAccountType(String accountType) {
        return applicationRepository.findByAccountType(accountType);
    }

    public List<AdminAccountApplication> getPendingApplications() {
        return applicationRepository.findPendingApplications();
    }

    public List<AdminAccountApplication> getAwaitingManagerApproval() {
        return applicationRepository.findAwaitingManagerApproval();
    }

    public List<AdminAccountApplication> searchApplications(String term) {
        return applicationRepository.searchApplications(term);
    }

    // ==================== Existing Account Lookup by Aadhaar ====================

    public Map<String, Object> findExistingAccountsByAadhar(String aadharNumber) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> existingAccounts = new ArrayList<>();
        List<String> blockedTypes = new ArrayList<>();

        // 1. Check Savings accounts (Account table)
        Account savingsAccount = accountRepository.findByAadharNumber(aadharNumber);
        if (savingsAccount != null) {
            Map<String, Object> acc = new HashMap<>();
            acc.put("type", "Savings");
            acc.put("accountNumber", savingsAccount.getAccountNumber());
            acc.put("status", savingsAccount.getStatus());
            acc.put("customerId", savingsAccount.getCustomerId());

            existingAccounts.add(acc);
            blockedTypes.add("Savings");
        }

        // 2. Check Current accounts
        CurrentAccount currentAccount = currentAccountRepository.findByAadharNumber(aadharNumber);
        if (currentAccount != null) {
            Map<String, Object> acc = new HashMap<>();
            acc.put("type", "Current");
            acc.put("accountNumber", currentAccount.getAccountNumber());
            acc.put("status", currentAccount.getStatus());
            acc.put("customerId", currentAccount.getCustomerId());
            acc.put("businessName", currentAccount.getBusinessName());
            existingAccounts.add(acc);
            blockedTypes.add("Current");
        }

        // 3. Check Salary accounts
        SalaryAccount salaryAccount = salaryAccountRepository.findByAadharNumber(aadharNumber);
        if (salaryAccount != null) {
            Map<String, Object> acc = new HashMap<>();
            acc.put("type", "Salary");
            acc.put("accountNumber", salaryAccount.getAccountNumber());
            acc.put("status", salaryAccount.getStatus());
            acc.put("customerId", salaryAccount.getCustomerId());
            acc.put("companyName", salaryAccount.getCompanyName());
            existingAccounts.add(acc);
            blockedTypes.add("Salary");
        }

        // 4. Check pending/in-progress admin applications (not rejected)
        List<AdminAccountApplication> pendingApps = applicationRepository.findByAadharNumber(aadharNumber);
        for (AdminAccountApplication app : pendingApps) {
            if (!"MANAGER_REJECTED".equals(app.getStatus())) {
                String appType = app.getAccountType();
                if (!blockedTypes.contains(appType)) {
                    Map<String, Object> acc = new HashMap<>();
                    acc.put("type", appType);
                    acc.put("accountNumber", app.getAccountNumber() != null ? app.getAccountNumber() : "Pending");
                    acc.put("status", app.getStatus());
                    acc.put("applicationNumber", app.getApplicationNumber());
                    existingAccounts.add(acc);
                    blockedTypes.add(appType);
                }
            }
        }

        // Determine allowed account types
        List<String> allTypes = Arrays.asList("Savings", "Current", "Salary");
        List<String> allowedTypes = new ArrayList<>();
        for (String type : allTypes) {
            if (!blockedTypes.contains(type)) {
                allowedTypes.add(type);
            }
        }

        result.put("existingAccounts", existingAccounts);
        result.put("blockedTypes", blockedTypes);
        result.put("allowedTypes", allowedTypes);
        result.put("hasExisting", !existingAccounts.isEmpty());

        return result;
    }

    // ==================== Validation ====================

    public boolean isAadharAvailable(String aadharNumber) {
        return !applicationRepository.existsByAadharNumberAndStatusNot(aadharNumber, "MANAGER_REJECTED")
                && accountService.isAadharUnique(aadharNumber);
    }

    public boolean isPanAvailable(String panNumber) {
        return !applicationRepository.existsByPanNumberAndStatusNot(panNumber, "MANAGER_REJECTED")
                && accountService.isPanUnique(panNumber);
    }

    public boolean isPhoneAvailable(String phone) {
        return !applicationRepository.existsByPhoneAndStatusNot(phone, "MANAGER_REJECTED")
                && accountService.isPhoneUnique(phone);
    }

    // ==================== Admin Verification ====================

    @Transactional
    public AdminAccountApplication adminVerify(Long id, String adminName, String remarks) {
        AdminAccountApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Validate all required fields are filled
        List<String> missingFields = new ArrayList<>();
        if (app.getFullName() == null || app.getFullName().trim().isEmpty()) missingFields.add("Full Name");
        if (app.getDateOfBirth() == null || app.getDateOfBirth().trim().isEmpty()) missingFields.add("Date of Birth");
        if (app.getGender() == null || app.getGender().trim().isEmpty()) missingFields.add("Gender");
        if (app.getPhone() == null || app.getPhone().trim().length() < 10) missingFields.add("Phone");
        if (app.getEmail() == null || app.getEmail().trim().isEmpty()) missingFields.add("Email");
        if (app.getAadharNumber() == null || app.getAadharNumber().trim().length() < 12) missingFields.add("Aadhaar Number");
        if (app.getPanNumber() == null || app.getPanNumber().trim().length() < 10) missingFields.add("PAN Number");
        if (app.getAddress() == null || app.getAddress().trim().isEmpty()) missingFields.add("Address");
        if (app.getCity() == null || app.getCity().trim().isEmpty()) missingFields.add("City");
        if (app.getState() == null || app.getState().trim().isEmpty()) missingFields.add("State");
        if (app.getPincode() == null || app.getPincode().trim().isEmpty()) missingFields.add("Pincode");
        if (app.getOccupation() == null || app.getOccupation().trim().isEmpty()) missingFields.add("Occupation");

        // Account type specific validations
        if ("Current".equals(app.getAccountType())) {
            if (app.getBusinessName() == null || app.getBusinessName().trim().isEmpty()) missingFields.add("Business Name");
            if (app.getBusinessType() == null || app.getBusinessType().trim().isEmpty()) missingFields.add("Business Type");
        }
        if ("Salary".equals(app.getAccountType())) {
            if (app.getCompanyName() == null || app.getCompanyName().trim().isEmpty()) missingFields.add("Company Name");
            if (app.getDesignation() == null || app.getDesignation().trim().isEmpty()) missingFields.add("Designation");
        }

        // Signed application document must be uploaded
        if (app.getSignedApplicationPath() == null || app.getSignedApplicationPath().trim().isEmpty()) {
            missingFields.add("Signed Application Document");
        }

        if (!missingFields.isEmpty()) {
            throw new RuntimeException("Cannot verify: Missing required fields — " + String.join(", ", missingFields));
        }

        app.setAdminVerified(true);
        app.setAdminVerifiedBy(adminName);
        app.setAdminVerifiedDate(LocalDateTime.now());
        app.setAdminRemarks(remarks);
        app.setStatus("ADMIN_VERIFIED");
        app.setUpdatedAt(LocalDateTime.now());

        return applicationRepository.save(app);
    }

    // ==================== Manager Approval ====================

    @Transactional
    public AdminAccountApplication managerApprove(Long id, String managerName, String remarks) {
        AdminAccountApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!"ADMIN_VERIFIED".equals(app.getStatus())) {
            throw new RuntimeException("Application must be admin-verified before manager approval");
        }

        app.setManagerApproved(true);
        app.setManagerApprovedBy(managerName);
        app.setManagerApprovedDate(LocalDateTime.now());
        app.setManagerRemarks(remarks);
        app.setStatus("MANAGER_APPROVED");
        app.setUpdatedAt(LocalDateTime.now());

        // For Savings accounts, generate account number and customer ID upfront
        // Current and Salary accounts auto-generate their own via their models/services
        if ("Savings".equals(app.getAccountType())) {
            String accountNumber = accountService.generateUniqueAccountNumberForNewAccount();
            app.setAccountNumber(accountNumber);

            String customerId = generateCustomerId(app.getPanNumber(), app.getDateOfBirth());
            app.setCustomerId(customerId);
        }

        AdminAccountApplication saved = applicationRepository.save(app);

        // Create the actual account based on account type
        createActualAccount(saved);

        return saved;
    }

    @Transactional
    public AdminAccountApplication managerReject(Long id, String managerName, String remarks) {
        AdminAccountApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        app.setManagerApproved(false);
        app.setManagerApprovedBy(managerName);
        app.setManagerApprovedDate(LocalDateTime.now());
        app.setManagerRemarks(remarks);
        app.setStatus("MANAGER_REJECTED");
        app.setUpdatedAt(LocalDateTime.now());

        return applicationRepository.save(app);
    }

    // ==================== Document Upload ====================

    @Transactional
    public AdminAccountApplication uploadSignedApplication(Long id, String filePath) {
        AdminAccountApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        app.setSignedApplicationPath(filePath);
        app.setStatus("DOCUMENTS_UPLOADED");
        app.setUpdatedAt(LocalDateTime.now());

        return applicationRepository.save(app);
    }

    @Transactional
    public AdminAccountApplication uploadAdditionalDocuments(Long id, String filePath) {
        AdminAccountApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        app.setAdditionalDocumentsPath(filePath);
        app.setUpdatedAt(LocalDateTime.now());

        return applicationRepository.save(app);
    }

    @Transactional
    public AdminAccountApplication saveApplicationPdfPath(Long id, String pdfPath) {
        AdminAccountApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        app.setApplicationPdfPath(pdfPath);
        app.setUpdatedAt(LocalDateTime.now());

        return applicationRepository.save(app);
    }

    // ==================== Statistics ====================

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalApplications", applicationRepository.count());
        stats.put("pendingCount", applicationRepository.countByStatus("PENDING"));
        stats.put("adminVerifiedCount", applicationRepository.countByStatus("ADMIN_VERIFIED"));
        stats.put("managerApprovedCount", applicationRepository.countByStatus("MANAGER_APPROVED"));
        stats.put("rejectedCount", applicationRepository.countByStatus("MANAGER_REJECTED"));
        stats.put("savingsCount", applicationRepository.countByAccountType("Savings"));
        stats.put("currentCount", applicationRepository.countByAccountType("Current"));
        stats.put("salaryCount", applicationRepository.countByAccountType("Salary"));
        return stats;
    }

    // ==================== Private Helpers ====================

    private void createActualAccount(AdminAccountApplication app) {
        if ("Savings".equals(app.getAccountType())) {
            Account account = new Account();
            account.setName(app.getFullName());
            account.setDob(app.getDateOfBirth());
            account.setAge(app.getAge() != null ? app.getAge() : 0);
            account.setOccupation(app.getOccupation());
            account.setAccountType("Savings");
            account.setAadharNumber(app.getAadharNumber());
            account.setPan(app.getPanNumber());
            account.setAccountNumber(app.getAccountNumber());
            account.setCustomerId(app.getCustomerId());
            account.setIncome(app.getIncome());
            account.setPhone(app.getPhone());
            account.setAddress(app.getAddress());
            account.setBalance(0.0);
            account.setKycVerified(true);
            account.setVerifiedMatrix(true);
            account.setStatus("ACTIVE");
            account.setAadharVerified(true);
            account.setAadharVerificationStatus("VERIFIED");

            // Create User entity so the account appears in Manage Users and can login
            User user = new User();
            user.setUsername(app.getFullName());
            user.setEmail(app.getEmail());
            // Generate a temporary password from last 4 digits of Aadhaar + first 4 of PAN
            String tempPassword = "";
            if (app.getAadharNumber() != null && app.getAadharNumber().length() >= 4) {
                tempPassword += app.getAadharNumber().substring(app.getAadharNumber().length() - 4);
            }
            if (app.getPanNumber() != null && app.getPanNumber().length() >= 4) {
                tempPassword += app.getPanNumber().substring(0, 4);
            }
            if (tempPassword.isEmpty()) {
                tempPassword = "NeoBank@123";
            }
            user.setPassword(passwordService.encryptPassword(tempPassword));
            user.setStatus("APPROVED");
            user.setAccountNumber(app.getAccountNumber());
            user.setAccount(account);

            userService.saveUser(user);
        } else if ("Current".equals(app.getAccountType())) {
            CurrentAccount ca = new CurrentAccount();
            ca.setOwnerName(app.getFullName());
            ca.setBusinessName(app.getBusinessName() != null ? app.getBusinessName() : app.getFullName());
            ca.setBusinessType(app.getBusinessType() != null ? app.getBusinessType() : "Proprietor");
            ca.setBusinessRegistrationNumber(app.getBusinessRegistrationNumber());
            ca.setGstNumber(app.getGstNumber());
            ca.setMobile(app.getPhone());
            ca.setEmail(app.getEmail());
            ca.setAadharNumber(app.getAadharNumber());
            ca.setPanNumber(app.getPanNumber());
            ca.setShopAddress(app.getShopAddress());
            ca.setCity(app.getCity());
            ca.setState(app.getState());
            ca.setPincode(app.getPincode());
            ca.setBranchName(app.getBranchName() != null ? app.getBranchName() : "NeoBank Main Branch");
            ca.setIfscCode(app.getIfscCode() != null ? app.getIfscCode() : "EZYV000123");
            ca.setBalance(0.0);
            ca.setStatus("ACTIVE");
            ca.setKycVerified(true);
            ca.setKycVerifiedDate(LocalDateTime.now());
            ca.setKycVerifiedBy(app.getAdminVerifiedBy());
            ca.setApprovedAt(LocalDateTime.now());
            ca.setApprovedBy(app.getManagerApprovedBy());
            // accountNumber and customerId are auto-generated by @PrePersist
            CurrentAccount saved = currentAccountRepository.save(ca);
            // Update application with generated account number and customer ID
            app.setAccountNumber(saved.getAccountNumber());
            app.setCustomerId(saved.getCustomerId());
        } else if ("Salary".equals(app.getAccountType())) {
            SalaryAccount sa = new SalaryAccount();
            sa.setEmployeeName(app.getFullName());
            sa.setDob(app.getDateOfBirth());
            sa.setMobileNumber(app.getPhone());
            sa.setEmail(app.getEmail());
            sa.setAadharNumber(app.getAadharNumber());
            sa.setPanNumber(app.getPanNumber());
            sa.setCompanyName(app.getCompanyName());
            sa.setCompanyId(app.getCompanyId());
            sa.setEmployerAddress(app.getEmployerAddress());
            sa.setHrContactNumber(app.getHrContactNumber());
            sa.setMonthlySalary(app.getMonthlySalary() != null ? app.getMonthlySalary() : 0.0);
            sa.setSalaryCreditDate(app.getSalaryCreditDate() != null ? app.getSalaryCreditDate() : 1);
            sa.setDesignation(app.getDesignation());
            sa.setBranchName(app.getBranchName() != null ? app.getBranchName() : "NeoBank Main Branch");
            sa.setIfscCode(app.getIfscCode() != null ? app.getIfscCode() : "EZYV000123");
            sa.setAddress(app.getAddress());
            sa.setBalance(0.0);
            sa.setStatus("Active");
            // accountNumber, customerId, debit card details are generated by SalaryAccountService.createAccount
            SalaryAccount saved = salaryAccountService.createAccount(sa);
            // Update application with generated account number and customer ID
            app.setAccountNumber(saved.getAccountNumber());
            app.setCustomerId(saved.getCustomerId());
        }
        app.setStatus("ACTIVE");
        applicationRepository.save(app);
    }

    private String generateApplicationNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "APP" + timestamp.substring(2) + random;
    }

    private String generateCustomerId(String pan, String dob) {
        String panDigits = (pan != null) ? pan.replaceAll("\\D", "") : "";
        String panPart = "0000";
        if (panDigits.length() >= 4) {
            panPart = panDigits.substring(panDigits.length() - 4);
        } else if (panDigits.length() > 0) {
            panPart = String.format("%4s", panDigits).replace(' ', '0');
        }

        String dobPart = "00000";
        if (dob != null && !dob.isEmpty()) {
            String dobDigits = dob.replaceAll("\\D", "");
            if (dobDigits.length() >= 5) {
                dobPart = dobDigits.substring(0, 5);
            }
        }

        String candidate = panPart + dobPart;
        // Ensure 9 digits
        if (candidate.length() > 9) {
            candidate = candidate.substring(0, 9);
        } else if (candidate.length() < 9) {
            candidate = String.format("%-9s", candidate).replace(' ', '0');
        }
        return candidate;
    }
}
