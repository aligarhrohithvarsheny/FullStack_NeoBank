package com.neo.springapp.controller;

import com.neo.springapp.model.EducationLoanApplication;
import com.neo.springapp.service.EducationLoanApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/education-loan-applications")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class EducationLoanApplicationController {

    @Autowired
    private EducationLoanApplicationService applicationService;

    /**
     * Create new education loan application
     */
    @PostMapping("/create")
    public ResponseEntity<?> createApplication(@RequestBody EducationLoanApplication application) {
        try {
            EducationLoanApplication created = applicationService.createApplication(application);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get all applications
     */
    @GetMapping
    public ResponseEntity<List<EducationLoanApplication>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    /**
     * Get application by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<EducationLoanApplication> getApplicationById(@PathVariable Long id) {
        try {
            EducationLoanApplication application = applicationService.getApplicationById(id);
            return ResponseEntity.ok(application);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get applications by account number
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<EducationLoanApplication>> getApplicationsByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(applicationService.getApplicationsByAccountNumber(accountNumber));
    }

    /**
     * Get application by loan ID
     */
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<EducationLoanApplication> getApplicationByLoanId(@PathVariable Long loanId) {
        EducationLoanApplication application = applicationService.getApplicationByLoanId(loanId);
        if (application != null) {
            return ResponseEntity.ok(application);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get pending applications
     */
    @GetMapping("/pending")
    public ResponseEntity<List<EducationLoanApplication>> getPendingApplications() {
        return ResponseEntity.ok(applicationService.getPendingApplications());
    }

    /**
     * Update application (admin can edit all fields)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateApplication(@PathVariable Long id, @RequestBody EducationLoanApplication application) {
        try {
            EducationLoanApplication updated = applicationService.updateApplication(id, application);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update application status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam String reviewedBy,
            @RequestParam(required = false) String notes) {
        try {
            EducationLoanApplication updated = applicationService.updateStatus(id, status, reviewedBy, notes);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Reject application
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectApplication(
            @PathVariable Long id,
            @RequestParam String reviewedBy,
            @RequestParam String rejectionReason) {
        try {
            EducationLoanApplication rejected = applicationService.rejectApplication(id, reviewedBy, rejectionReason);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Upload college application document
     */
    @PostMapping("/{id}/upload-college-application")
    public ResponseEntity<?> uploadCollegeApplication(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            EducationLoanApplication application = applicationService.getApplicationById(id);
            String filePath = applicationService.saveFile(file, file.getOriginalFilename());
            application.setCollegeApplicationPath(filePath);
            applicationService.updateApplication(id, application);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", filePath);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Download file
     */
    @GetMapping("/download/{id}/{fileType}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long id,
            @PathVariable String fileType) {
        try {
            EducationLoanApplication application = applicationService.getApplicationById(id);
            String filePath = null;
            String fileName = "document";
            
            switch (fileType) {
                case "college-application":
                    filePath = application.getCollegeApplicationPath();
                    fileName = "college_application.pdf";
                    break;
                case "admission-letter":
                    filePath = application.getCollegeAdmissionLetterPath();
                    fileName = "admission_letter.pdf";
                    break;
                case "fee-structure":
                    filePath = application.getCollegeFeeStructurePath();
                    fileName = "fee_structure.pdf";
                    break;
            }
            
            if (filePath == null) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] fileContent = applicationService.getFileContent(filePath);
            if (fileContent == null) {
                return ResponseEntity.notFound().build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}





