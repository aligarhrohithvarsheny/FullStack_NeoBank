package com.neo.springapp.service;

import com.neo.springapp.model.EducationLoanApplication;
import com.neo.springapp.repository.EducationLoanApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EducationLoanApplicationService {

    @Autowired
    private EducationLoanApplicationRepository applicationRepository;

    private static final String UPLOAD_DIR = "uploads/education-loan-documents/";

    /**
     * Create new education loan application
     */
    @Transactional
    public EducationLoanApplication createApplication(EducationLoanApplication application) {
        // Validate age
        if (!application.isValidAge()) {
            throw new RuntimeException("Child age must be between 18 and 26 years");
        }
        
        // Set calculated age
        application.setChildAge(application.calculateAge());
        application.setApplicationDate(LocalDateTime.now());
        application.setLastUpdatedDate(LocalDateTime.now());
        application.setApplicationStatus("Pending");
        
        return applicationRepository.save(application);
    }

    /**
     * Get all applications
     */
    public List<EducationLoanApplication> getAllApplications() {
        return applicationRepository.findAll();
    }

    /**
     * Get application by ID
     */
    public EducationLoanApplication getApplicationById(Long id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Education loan application not found"));
    }

    /**
     * Get applications by account number
     */
    public List<EducationLoanApplication> getApplicationsByAccountNumber(String accountNumber) {
        return applicationRepository.findByApplicantAccountNumber(accountNumber);
    }

    /**
     * Get application by loan ID
     */
    public EducationLoanApplication getApplicationByLoanId(Long loanId) {
        return applicationRepository.findByLoanId(loanId)
            .orElse(null);
    }

    /**
     * Get pending applications
     */
    public List<EducationLoanApplication> getPendingApplications() {
        return applicationRepository.findPendingApplications();
    }

    /**
     * Update application (admin can edit)
     */
    @Transactional
    public EducationLoanApplication updateApplication(Long id, EducationLoanApplication updatedApplication) {
        EducationLoanApplication existing = getApplicationById(id);
        
        // Update all fields
        if (updatedApplication.getChildName() != null) {
            existing.setChildName(updatedApplication.getChildName());
        }
        if (updatedApplication.getChildDateOfBirth() != null) {
            existing.setChildDateOfBirth(updatedApplication.getChildDateOfBirth());
            existing.setChildAge(existing.calculateAge());
            if (!existing.isValidAge()) {
                throw new RuntimeException("Child age must be between 18 and 26 years");
            }
        }
        if (updatedApplication.getChildPlaceOfBirth() != null) {
            existing.setChildPlaceOfBirth(updatedApplication.getChildPlaceOfBirth());
        }
        
        // Update education details
        if (updatedApplication.getTenthSchoolName() != null) {
            existing.setTenthSchoolName(updatedApplication.getTenthSchoolName());
        }
        if (updatedApplication.getTenthBoard() != null) {
            existing.setTenthBoard(updatedApplication.getTenthBoard());
        }
        if (updatedApplication.getTenthPassingYear() != null) {
            existing.setTenthPassingYear(updatedApplication.getTenthPassingYear());
        }
        if (updatedApplication.getTenthPercentage() != null) {
            existing.setTenthPercentage(updatedApplication.getTenthPercentage());
        }
        
        if (updatedApplication.getTwelfthSchoolName() != null) {
            existing.setTwelfthSchoolName(updatedApplication.getTwelfthSchoolName());
        }
        if (updatedApplication.getTwelfthBoard() != null) {
            existing.setTwelfthBoard(updatedApplication.getTwelfthBoard());
        }
        if (updatedApplication.getTwelfthPassingYear() != null) {
            existing.setTwelfthPassingYear(updatedApplication.getTwelfthPassingYear());
        }
        if (updatedApplication.getTwelfthPercentage() != null) {
            existing.setTwelfthPercentage(updatedApplication.getTwelfthPercentage());
        }
        
        if (updatedApplication.getUgCollegeName() != null) {
            existing.setUgCollegeName(updatedApplication.getUgCollegeName());
        }
        if (updatedApplication.getUgUniversity() != null) {
            existing.setUgUniversity(updatedApplication.getUgUniversity());
        }
        if (updatedApplication.getUgCourse() != null) {
            existing.setUgCourse(updatedApplication.getUgCourse());
        }
        if (updatedApplication.getUgCurrentCGPA() != null) {
            existing.setUgCurrentCGPA(updatedApplication.getUgCurrentCGPA());
        }
        
        // Update college details
        if (updatedApplication.getCollegeType() != null) {
            existing.setCollegeType(updatedApplication.getCollegeType());
        }
        if (updatedApplication.getCollegeName() != null) {
            existing.setCollegeName(updatedApplication.getCollegeName());
        }
        if (updatedApplication.getCollegeState() != null) {
            existing.setCollegeState(updatedApplication.getCollegeState());
        }
        if (updatedApplication.getCollegeCity() != null) {
            existing.setCollegeCity(updatedApplication.getCollegeCity());
        }
        if (updatedApplication.getCollegeCourse() != null) {
            existing.setCollegeCourse(updatedApplication.getCollegeCourse());
        }
        if (updatedApplication.getCollegeFeeAmount() != null) {
            existing.setCollegeFeeAmount(updatedApplication.getCollegeFeeAmount());
        }
        
        // Update college account details
        if (updatedApplication.getCollegeAccountNumber() != null) {
            existing.setCollegeAccountNumber(updatedApplication.getCollegeAccountNumber());
        }
        if (updatedApplication.getCollegeBankName() != null) {
            existing.setCollegeBankName(updatedApplication.getCollegeBankName());
        }
        if (updatedApplication.getCollegeIFSCCode() != null) {
            existing.setCollegeIFSCCode(updatedApplication.getCollegeIFSCCode());
        }
        
        // Update admin notes
        if (updatedApplication.getAdminNotes() != null) {
            existing.setAdminNotes(updatedApplication.getAdminNotes());
        }
        
        existing.setLastUpdatedDate(LocalDateTime.now());
        
        return applicationRepository.save(existing);
    }

    /**
     * Update application status
     */
    @Transactional
    public EducationLoanApplication updateStatus(Long id, String status, String reviewedBy, String notes) {
        EducationLoanApplication application = getApplicationById(id);
        application.setApplicationStatus(status);
        application.setReviewedBy(reviewedBy);
        application.setReviewedDate(LocalDateTime.now());
        application.setAdminNotes(notes);
        application.setLastUpdatedDate(LocalDateTime.now());
        
        return applicationRepository.save(application);
    }

    /**
     * Reject application
     */
    @Transactional
    public EducationLoanApplication rejectApplication(Long id, String reviewedBy, String rejectionReason) {
        EducationLoanApplication application = getApplicationById(id);
        application.setApplicationStatus("Rejected");
        application.setReviewedBy(reviewedBy);
        application.setReviewedDate(LocalDateTime.now());
        application.setRejectionReason(rejectionReason);
        application.setLastUpdatedDate(LocalDateTime.now());
        
        return applicationRepository.save(application);
    }

    /**
     * Save uploaded file
     */
    public String saveFile(MultipartFile file, String fileName) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return UPLOAD_DIR + uniqueFileName;
    }

    /**
     * Get file content
     */
    public byte[] getFileContent(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            return Files.readAllBytes(path);
        }
        return null;
    }
}





