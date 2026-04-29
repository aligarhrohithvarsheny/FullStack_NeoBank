package com.neo.springapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neo.springapp.model.Admin;
import com.neo.springapp.model.AdminProfileUpdateRequest;
import com.neo.springapp.repository.AdminProfileUpdateRequestRepository;
import com.neo.springapp.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class AdminProfileUpdateService {

    @Autowired
    private AdminProfileUpdateRequestRepository requestRepository;

    @Autowired
    private AdminRepository adminRepository;

    /**
     * Use Spring Boot's preconfigured ObjectMapper so Java 8 date/time
     * types (LocalDateTime) are handled correctly via JavaTimeModule.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Create a new admin profile update request.
     * Stores full old/new profile snapshots as JSON for manager review.
     */
    @Transactional
    public Map<String, Object> createProfileUpdateRequest(Admin existingAdmin, Admin requestedChanges) {
        Map<String, Object> response = new HashMap<>();

        if (existingAdmin == null || existingAdmin.getId() == null) {
            response.put("success", false);
            response.put("message", "Admin not found");
            return response;
        }

        try {
            // Build plain objects with only profile-relevant fields
            Map<String, Object> oldProfile = extractProfileSnapshot(existingAdmin);
            Map<String, Object> newProfile = new HashMap<>(oldProfile);

            applyRequestedChanges(newProfile, requestedChanges);

            // If nothing actually changed, avoid creating a request
            String oldJson = objectMapper.writeValueAsString(oldProfile);
            String newJson = objectMapper.writeValueAsString(newProfile);
            if (oldJson.equals(newJson)) {
                response.put("success", false);
                response.put("message", "No profile changes detected");
                return response;
            }

            AdminProfileUpdateRequest request = new AdminProfileUpdateRequest();
            request.setAdminId(existingAdmin.getId());
            request.setAdminEmail(existingAdmin.getEmail());
            request.setAdminName(existingAdmin.getName());
            request.setOldProfileJson(oldJson);
            request.setNewProfileJson(newJson);
            request.setStatus("PENDING");
            request.setRequestedAt(LocalDateTime.now());

            AdminProfileUpdateRequest saved = requestRepository.save(request);

            response.put("success", true);
            response.put("pendingApproval", true);
            response.put("message", "Profile update request sent for manager approval");
            response.put("requestId", saved.getId());
            return response;
        } catch (JsonProcessingException e) {
            response.put("success", false);
            response.put("message", "Failed to serialize profile data: " + e.getMessage());
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create profile update request: " + e.getMessage());
            return response;
        }
    }

    private Map<String, Object> extractProfileSnapshot(Admin admin) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", admin.getName());
        map.put("email", admin.getEmail());
        map.put("employeeId", admin.getEmployeeId());
        map.put("address", admin.getAddress());
        map.put("aadharNumber", admin.getAadharNumber());
        map.put("mobileNumber", admin.getMobileNumber());
        map.put("role", admin.getRole());
        map.put("pan", admin.getPan());
        map.put("qualifications", admin.getQualifications());
        map.put("dateOfJoining", admin.getDateOfJoining());
        map.put("branchAccountNumber", admin.getBranchAccountNumber());
        map.put("branchAccountName", admin.getBranchAccountName());
        map.put("branchAccountIfsc", admin.getBranchAccountIfsc());
        map.put("salaryAccountNumber", admin.getSalaryAccountNumber());
        return map;
    }

    private void applyRequestedChanges(Map<String, Object> target, Admin changes) {
        if (changes.getName() != null) {
            target.put("name", changes.getName());
        }
        if (changes.getEmail() != null) {
            target.put("email", changes.getEmail());
        }
        if (changes.getEmployeeId() != null) {
            target.put("employeeId", changes.getEmployeeId());
        }
        if (changes.getAddress() != null) {
            target.put("address", changes.getAddress());
        }
        if (changes.getAadharNumber() != null) {
            target.put("aadharNumber", changes.getAadharNumber());
        }
        if (changes.getMobileNumber() != null) {
            target.put("mobileNumber", changes.getMobileNumber());
        }
        if (changes.getRole() != null) {
            target.put("role", changes.getRole());
        }
        if (changes.getPan() != null) {
            target.put("pan", changes.getPan());
        }
        if (changes.getQualifications() != null) {
            target.put("qualifications", changes.getQualifications());
        }
        if (changes.getDateOfJoining() != null) {
            target.put("dateOfJoining", changes.getDateOfJoining());
        }
        if (changes.getBranchAccountNumber() != null) {
            target.put("branchAccountNumber", changes.getBranchAccountNumber());
        }
        if (changes.getBranchAccountName() != null) {
            target.put("branchAccountName", changes.getBranchAccountName());
        }
        if (changes.getBranchAccountIfsc() != null) {
            target.put("branchAccountIfsc", changes.getBranchAccountIfsc());
        }
        if (changes.getSalaryAccountNumber() != null) {
            target.put("salaryAccountNumber", changes.getSalaryAccountNumber());
        }
    }

    /**
     * Approve a pending admin profile update and apply it to the Admin entity.
     */
    @Transactional
    public Map<String, Object> approveProfileUpdate(Long requestId, String approvedBy) {
        Map<String, Object> response = new HashMap<>();

        Optional<AdminProfileUpdateRequest> opt = requestRepository.findById(requestId);
        if (opt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Admin profile update request not found");
            return response;
        }

        AdminProfileUpdateRequest request = opt.get();
        if (!"PENDING".equals(request.getStatus())) {
            response.put("success", false);
            response.put("message", "Request is not in PENDING status");
            return response;
        }

        Optional<Admin> adminOpt = adminRepository.findById(request.getAdminId());
        if (adminOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Admin not found for this request");
            return response;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> newProfile = objectMapper.readValue(request.getNewProfileJson(), Map.class);
            Admin admin = adminOpt.get();

            applySnapshotToAdmin(admin, newProfile);

            Admin savedAdmin = adminRepository.save(admin);

            request.setStatus("COMPLETED");
            request.setApprovedBy(approvedBy);
            request.setApprovedAt(LocalDateTime.now());
            request.setCompletedAt(LocalDateTime.now());
            requestRepository.save(request);

            response.put("success", true);
            response.put("message", "Admin profile update approved and applied");
            response.put("adminId", savedAdmin.getId());
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to apply admin profile update: " + e.getMessage());
            return response;
        }
    }

    private void applySnapshotToAdmin(Admin admin, Map<String, Object> snapshot) {
        if (snapshot.containsKey("name")) {
            admin.setName((String) snapshot.get("name"));
        }
        if (snapshot.containsKey("email")) {
            admin.setEmail((String) snapshot.get("email"));
        }
        if (snapshot.containsKey("employeeId")) {
            admin.setEmployeeId((String) snapshot.get("employeeId"));
        }
        if (snapshot.containsKey("address")) {
            admin.setAddress((String) snapshot.get("address"));
        }
        if (snapshot.containsKey("aadharNumber")) {
            admin.setAadharNumber((String) snapshot.get("aadharNumber"));
        }
        if (snapshot.containsKey("mobileNumber")) {
          admin.setMobileNumber((String) snapshot.get("mobileNumber"));
        }
        if (snapshot.containsKey("role")) {
            admin.setRole((String) snapshot.get("role"));
        }
        if (snapshot.containsKey("pan")) {
            admin.setPan((String) snapshot.get("pan"));
        }
        if (snapshot.containsKey("qualifications")) {
            admin.setQualifications((String) snapshot.get("qualifications"));
        }
        if (snapshot.containsKey("dateOfJoining")) {
            Object value = snapshot.get("dateOfJoining");
            if (value instanceof String) {
                // Expect ISO-8601 string
                admin.setDateOfJoining(LocalDateTime.parse((String) value));
            } else if (value instanceof LocalDateTime) {
                admin.setDateOfJoining((LocalDateTime) value);
            }
        }
        if (snapshot.containsKey("branchAccountNumber")) {
            admin.setBranchAccountNumber((String) snapshot.get("branchAccountNumber"));
        }
        if (snapshot.containsKey("branchAccountName")) {
            admin.setBranchAccountName((String) snapshot.get("branchAccountName"));
        }
        if (snapshot.containsKey("branchAccountIfsc")) {
            admin.setBranchAccountIfsc((String) snapshot.get("branchAccountIfsc"));
        }
        if (snapshot.containsKey("salaryAccountNumber")) {
            admin.setSalaryAccountNumber((String) snapshot.get("salaryAccountNumber"));
        }
    }

    @Transactional
    public Map<String, Object> rejectProfileUpdate(Long requestId, String rejectedBy, String reason) {
        Map<String, Object> response = new HashMap<>();

        Optional<AdminProfileUpdateRequest> opt = requestRepository.findById(requestId);
        if (opt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Admin profile update request not found");
            return response;
        }

        AdminProfileUpdateRequest request = opt.get();
        if (!"PENDING".equals(request.getStatus())) {
            response.put("success", false);
            response.put("message", "Request is not in PENDING status");
            return response;
        }

        request.setStatus("REJECTED");
        request.setApprovedBy(rejectedBy);
        request.setApprovedAt(LocalDateTime.now());
        request.setRejectionReason(reason);
        requestRepository.save(request);

        response.put("success", true);
        response.put("message", "Admin profile update request rejected");
        return response;
    }

    public List<AdminProfileUpdateRequest> getPendingRequests() {
        return requestRepository.findByStatus("PENDING");
    }

    public List<AdminProfileUpdateRequest> getAllRequests() {
        return requestRepository.findAll();
    }

    public Optional<AdminProfileUpdateRequest> getRequestById(Long id) {
        return requestRepository.findById(id);
    }
}

