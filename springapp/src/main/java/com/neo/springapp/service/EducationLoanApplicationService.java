package com.neo.springapp.service;

import com.neo.springapp.model.EducationLoanApplication;
import com.neo.springapp.model.Loan;
import com.neo.springapp.repository.EducationLoanApplicationRepository;
import com.neo.springapp.repository.LoanRepository;
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
@SuppressWarnings("null")
public class EducationLoanApplicationService {

    @Autowired
    private EducationLoanApplicationRepository applicationRepository;

    @Autowired
    private LoanRepository loanRepository;

    private static final String UPLOAD_DIR = "uploads/education-loan-documents/";

    /**
     * Create new education loan application
     */
    @Transactional
    public EducationLoanApplication createApplication(EducationLoanApplication application) {
        application.setChildAge(application.calculateAge());
        if (application.getChildDateOfBirth() != null && !application.isValidAge()) {
            String note = "Child age is outside 18-26 (age=" + application.getChildAge() + "). Saved for admin review.";
            application.setAdminNotes(application.getAdminNotes() == null ? note : application.getAdminNotes() + " | " + note);
        }
        if (application.getApplicationDate() == null) {
            application.setApplicationDate(LocalDateTime.now());
        }
        application.setLastUpdatedDate(LocalDateTime.now());
        if (application.getApplicationStatus() == null || application.getApplicationStatus().isBlank()) {
            application.setApplicationStatus("Pending");
        }
        if (application.getLoanId() != null) {
            return applicationRepository.findByLoanId(application.getLoanId())
                    .map(existing -> updateApplication(existing.getId(), application))
                    .orElseGet(() -> applicationRepository.save(application));
        }
        return applicationRepository.save(application);
    }

    /**
     * Create a linked education-loan application from a saved Loan (covers historical applies).
     */
    @Transactional
    public EducationLoanApplication createFromLoan(Loan loan, EducationLoanApplication details) {
        if (loan == null || loan.getId() == null) {
            throw new IllegalArgumentException("Saved loan is required");
        }
        return applicationRepository.findByLoanId(loan.getId()).orElseGet(() -> {
            EducationLoanApplication app = details != null ? details : new EducationLoanApplication();
            app.setLoanId(loan.getId());
            app.setLoanAccountNumber(loan.getLoanAccountNumber());
            app.setRequestedLoanAmount(loan.getAmount());
            if (app.getApplicantAccountNumber() == null) {
                app.setApplicantAccountNumber(loan.getAccountNumber());
            }
            if (app.getApplicantName() == null) {
                app.setApplicantName(loan.getUserName());
            }
            if (app.getApplicantEmail() == null) {
                app.setApplicantEmail(loan.getUserEmail());
            }
            if (app.getChildAccountNumber() == null) {
                app.setChildAccountNumber(loan.getChildAccountNumber());
            }
            if (app.getChildName() == null || app.getChildName().isBlank()) {
                app.setChildName(loan.getUserName() != null ? loan.getUserName() : "Education loan applicant");
            }
            return createApplication(app);
        });
    }

    /**
     * Get all applications, including education loans that only exist in the loans table.
     */
    public List<EducationLoanApplication> getAllApplications() {
        List<EducationLoanApplication> apps = new java.util.ArrayList<>(applicationRepository.findAll());
        try {
            List<Loan> educationLoans = loanRepository.findAll().stream()
                    .filter(l -> l.getType() != null && l.getType().toLowerCase().contains("education"))
                    .toList();
            for (Loan loan : educationLoans) {
                boolean exists = apps.stream().anyMatch(a ->
                        (a.getLoanId() != null && a.getLoanId().equals(loan.getId()))
                                || (a.getLoanAccountNumber() != null
                                && a.getLoanAccountNumber().equals(loan.getLoanAccountNumber())));
                if (!exists) {
                    apps.add(createFromLoan(loan, null));
                }
            }
        } catch (Exception e) {
            System.err.println("Education loan backfill skipped: " + e.getMessage());
        }
        apps.sort((a, b) -> {
            if (a.getApplicationDate() == null) return 1;
            if (b.getApplicationDate() == null) return -1;
            return b.getApplicationDate().compareTo(a.getApplicationDate());
        });
        return apps;
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





