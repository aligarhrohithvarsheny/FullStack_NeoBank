package com.neo.springapp.controller;

import com.neo.springapp.model.AdminAccountApplication;
import com.neo.springapp.service.AdminAccountApplicationService;
import com.neo.springapp.service.AdminAccountApplicationPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin-account-applications")
public class AdminAccountApplicationController {

    @Autowired
    private AdminAccountApplicationService applicationService;

    @Autowired
    private AdminAccountApplicationPdfService pdfService;

    private static final String UPLOAD_DIR = "uploads/admin-account-applications/";

    // ==================== CRUD ====================

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createApplication(@RequestBody AdminAccountApplication application) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Validate uniqueness
            if (!applicationService.isAadharAvailable(application.getAadharNumber())) {
                response.put("success", false);
                response.put("error", "Aadhaar number is already registered or has a pending application.");
                return ResponseEntity.badRequest().body(response);
            }
            if (!applicationService.isPanAvailable(application.getPanNumber())) {
                response.put("success", false);
                response.put("error", "PAN number is already registered or has a pending application.");
                return ResponseEntity.badRequest().body(response);
            }
            if (!applicationService.isPhoneAvailable(application.getPhone())) {
                response.put("success", false);
                response.put("error", "Phone number is already registered or has a pending application.");
                return ResponseEntity.badRequest().body(response);
            }

            AdminAccountApplication saved = applicationService.createApplication(application);
            response.put("success", true);
            response.put("application", saved);
            response.put("message", "Application created successfully. Application Number: " + saved.getApplicationNumber());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to create application: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<AdminAccountApplication>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminAccountApplication> getById(@PathVariable Long id) {
        return applicationService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{applicationNumber}")
    public ResponseEntity<AdminAccountApplication> getByApplicationNumber(@PathVariable String applicationNumber) {
        return applicationService.getByApplicationNumber(applicationNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AdminAccountApplication>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(applicationService.getByStatus(status));
    }

    @GetMapping("/type/{accountType}")
    public ResponseEntity<List<AdminAccountApplication>> getByAccountType(@PathVariable String accountType) {
        return ResponseEntity.ok(applicationService.getByAccountType(accountType));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AdminAccountApplication>> getPendingApplications() {
        return ResponseEntity.ok(applicationService.getPendingApplications());
    }

    @GetMapping("/awaiting-manager")
    public ResponseEntity<List<AdminAccountApplication>> getAwaitingManagerApproval() {
        return ResponseEntity.ok(applicationService.getAwaitingManagerApproval());
    }

    @GetMapping("/search")
    public ResponseEntity<List<AdminAccountApplication>> searchApplications(@RequestParam String term) {
        return ResponseEntity.ok(applicationService.searchApplications(term));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(applicationService.getStatistics());
    }

    // ==================== Existing Account Check ====================

    @GetMapping("/check-existing-accounts/{aadhar}")
    public ResponseEntity<Map<String, Object>> checkExistingAccounts(@PathVariable String aadhar) {
        try {
            Map<String, Object> result = applicationService.findExistingAccountsByAadhar(aadhar);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== Validation ====================

    @GetMapping("/validate/aadhar/{aadhar}")
    public ResponseEntity<Map<String, Object>> validateAadhar(@PathVariable String aadhar) {
        Map<String, Object> response = new HashMap<>();
        response.put("available", applicationService.isAadharAvailable(aadhar));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/pan/{pan}")
    public ResponseEntity<Map<String, Object>> validatePan(@PathVariable String pan) {
        Map<String, Object> response = new HashMap<>();
        response.put("available", applicationService.isPanAvailable(pan));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/phone/{phone}")
    public ResponseEntity<Map<String, Object>> validatePhone(@PathVariable String phone) {
        Map<String, Object> response = new HashMap<>();
        response.put("available", applicationService.isPhoneAvailable(phone));
        return ResponseEntity.ok(response);
    }

    // ==================== Update Application Details ====================

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateApplication(
            @PathVariable Long id,
            @RequestBody AdminAccountApplication updatedData) {
        Map<String, Object> response = new HashMap<>();
        try {
            AdminAccountApplication app = applicationService.getById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));

            // Only allow editing before manager approval
            if ("MANAGER_APPROVED".equals(app.getStatus()) || "ACTIVE".equals(app.getStatus())) {
                response.put("success", false);
                response.put("error", "Cannot edit an already approved/active application");
                return ResponseEntity.badRequest().body(response);
            }

            // Update editable fields
            if (updatedData.getFullName() != null) app.setFullName(updatedData.getFullName());
            if (updatedData.getDateOfBirth() != null) app.setDateOfBirth(updatedData.getDateOfBirth());
            if (updatedData.getAge() != null) app.setAge(updatedData.getAge());
            if (updatedData.getGender() != null) app.setGender(updatedData.getGender());
            if (updatedData.getOccupation() != null) app.setOccupation(updatedData.getOccupation());
            if (updatedData.getIncome() != null) app.setIncome(updatedData.getIncome());
            if (updatedData.getPhone() != null) app.setPhone(updatedData.getPhone());
            if (updatedData.getEmail() != null) app.setEmail(updatedData.getEmail());
            if (updatedData.getAddress() != null) app.setAddress(updatedData.getAddress());
            if (updatedData.getCity() != null) app.setCity(updatedData.getCity());
            if (updatedData.getState() != null) app.setState(updatedData.getState());
            if (updatedData.getPincode() != null) app.setPincode(updatedData.getPincode());
            if (updatedData.getAadharNumber() != null) app.setAadharNumber(updatedData.getAadharNumber());
            if (updatedData.getPanNumber() != null) app.setPanNumber(updatedData.getPanNumber());

            // Business Details (Current Account)
            if (updatedData.getBusinessName() != null) app.setBusinessName(updatedData.getBusinessName());
            if (updatedData.getBusinessType() != null) app.setBusinessType(updatedData.getBusinessType());
            if (updatedData.getBusinessRegistrationNumber() != null) app.setBusinessRegistrationNumber(updatedData.getBusinessRegistrationNumber());
            if (updatedData.getGstNumber() != null) app.setGstNumber(updatedData.getGstNumber());
            if (updatedData.getShopAddress() != null) app.setShopAddress(updatedData.getShopAddress());

            // Salary Details (Salary Account)
            if (updatedData.getCompanyName() != null) app.setCompanyName(updatedData.getCompanyName());
            if (updatedData.getCompanyId() != null) app.setCompanyId(updatedData.getCompanyId());
            if (updatedData.getDesignation() != null) app.setDesignation(updatedData.getDesignation());
            if (updatedData.getMonthlySalary() != null) app.setMonthlySalary(updatedData.getMonthlySalary());
            if (updatedData.getSalaryCreditDate() != null) app.setSalaryCreditDate(updatedData.getSalaryCreditDate());
            if (updatedData.getHrContactNumber() != null) app.setHrContactNumber(updatedData.getHrContactNumber());
            if (updatedData.getEmployerAddress() != null) app.setEmployerAddress(updatedData.getEmployerAddress());

            // Bank Details
            if (updatedData.getBranchName() != null) app.setBranchName(updatedData.getBranchName());
            if (updatedData.getIfscCode() != null) app.setIfscCode(updatedData.getIfscCode());

            AdminAccountApplication saved = applicationService.saveApplication(app);
            response.put("success", true);
            response.put("application", saved);
            response.put("message", "Application updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to update: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== Admin Verification ====================

    @PutMapping("/admin-verify/{id}")
    public ResponseEntity<Map<String, Object>> adminVerify(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String adminName = body.getOrDefault("adminName", "Admin");
            String remarks = body.getOrDefault("remarks", "");
            AdminAccountApplication app = applicationService.adminVerify(id, adminName, remarks);
            response.put("success", true);
            response.put("application", app);
            response.put("message", "Application verified by admin successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== Manager Approval ====================

    @PutMapping("/manager-approve/{id}")
    public ResponseEntity<Map<String, Object>> managerApprove(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String managerName = body.getOrDefault("managerName", "Manager");
            String remarks = body.getOrDefault("remarks", "");
            AdminAccountApplication app = applicationService.managerApprove(id, managerName, remarks);
            response.put("success", true);
            response.put("application", app);
            response.put("message", "Application approved by manager. Account Number: " + app.getAccountNumber());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/manager-reject/{id}")
    public ResponseEntity<Map<String, Object>> managerReject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String managerName = body.getOrDefault("managerName", "Manager");
            String remarks = body.getOrDefault("remarks", "");
            AdminAccountApplication app = applicationService.managerReject(id, managerName, remarks);
            response.put("success", true);
            response.put("application", app);
            response.put("message", "Application rejected by manager");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== Document Upload ====================

    @PostMapping("/upload-signed/{id}")
    public ResponseEntity<Map<String, Object>> uploadSignedApplication(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            String fileName = "signed_" + id + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path uploadPath = Paths.get(UPLOAD_DIR + "signed/");
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            AdminAccountApplication app = applicationService.uploadSignedApplication(id, filePath.toString());
            response.put("success", true);
            response.put("application", app);
            response.put("message", "Signed application uploaded successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/upload-documents/{id}")
    public ResponseEntity<Map<String, Object>> uploadAdditionalDocuments(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            String fileName = "docs_" + id + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path uploadPath = Paths.get(UPLOAD_DIR + "documents/");
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            AdminAccountApplication app = applicationService.uploadAdditionalDocuments(id, filePath.toString());
            response.put("success", true);
            response.put("application", app);
            response.put("message", "Additional documents uploaded successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== PDF Generation ====================

    @GetMapping("/download-application/{id}")
    public ResponseEntity<byte[]> downloadApplication(@PathVariable Long id) {
        try {
            AdminAccountApplication app = applicationService.getById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            byte[] pdfBytes = pdfService.generateApplicationForm(app);

            // Save PDF path
            String fileName = "application_" + app.getApplicationNumber() + ".pdf";
            Path uploadPath = Paths.get(UPLOAD_DIR + "pdfs/");
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, pdfBytes);
            applicationService.saveApplicationPdfPath(id, filePath.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("NeoBank_Application_" + app.getApplicationNumber() + ".pdf")
                    .build());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== Signature Document Lookup ====================

    @GetMapping("/view-signed-by-id/{id}")
    public ResponseEntity<byte[]> viewSignedDocumentById(@PathVariable Long id) {
        try {
            Optional<AdminAccountApplication> appOpt = applicationService.getById(id);
            if (appOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            AdminAccountApplication app = appOpt.get();
            if (app.getSignedApplicationPath() == null || app.getSignedApplicationPath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Path filePath = Paths.get(app.getSignedApplicationPath());
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] fileBytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDisposition(ContentDisposition.builder("inline")
                    .filename(filePath.getFileName().toString())
                    .build());
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/view-additional-docs-by-id/{id}")
    public ResponseEntity<byte[]> viewAdditionalDocumentsById(@PathVariable Long id) {
        try {
            Optional<AdminAccountApplication> appOpt = applicationService.getById(id);
            if (appOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            AdminAccountApplication app = appOpt.get();
            if (app.getAdditionalDocumentsPath() == null || app.getAdditionalDocumentsPath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Path filePath = Paths.get(app.getAdditionalDocumentsPath());
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] fileBytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDisposition(ContentDisposition.builder("inline")
                    .filename(filePath.getFileName().toString())
                    .build());
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/signed-document-info/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getSignedDocumentInfo(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<AdminAccountApplication> appOpt = applicationService.getByAccountNumber(accountNumber);
            if (appOpt.isEmpty()) {
                response.put("found", false);
                response.put("message", "No application found for this account");
                return ResponseEntity.ok(response);
            }
            AdminAccountApplication app = appOpt.get();
            if (app.getSignedApplicationPath() == null || app.getSignedApplicationPath().isEmpty()) {
                response.put("found", false);
                response.put("message", "No signed document uploaded for this account");
                return ResponseEntity.ok(response);
            }
            response.put("found", true);
            response.put("applicationId", app.getId());
            response.put("applicationNumber", app.getApplicationNumber());
            response.put("fullName", app.getFullName());
            response.put("accountType", app.getAccountType());
            response.put("accountNumber", app.getAccountNumber());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("found", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/view-signed-document/{accountNumber}")
    public ResponseEntity<byte[]> viewSignedDocument(@PathVariable String accountNumber) {
        try {
            Optional<AdminAccountApplication> appOpt = applicationService.getByAccountNumber(accountNumber);
            if (appOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            AdminAccountApplication app = appOpt.get();
            if (app.getSignedApplicationPath() == null || app.getSignedApplicationPath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Path filePath = Paths.get(app.getSignedApplicationPath());
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] fileBytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDisposition(ContentDisposition.builder("inline")
                    .filename(filePath.getFileName().toString())
                    .build());
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
