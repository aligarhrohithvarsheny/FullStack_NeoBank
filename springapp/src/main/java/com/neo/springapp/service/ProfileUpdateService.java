package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
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
public class ProfileUpdateService {

    @Autowired
    private ProfileUpdateRequestRepository requestRepository;

    @Autowired
    private ProfileUpdateHistoryRepository historyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomerEventNotificationService notificationService;

    /**
     * Request a profile update. A new email must be verified before admin approval.
     */
    @Transactional
    public Map<String, Object> requestProfileUpdate(Long userId, String field, String newValue) {
        Map<String, Object> response = new HashMap<>();
        ProfileUpdateRequest savedRequest = null;
        
        try {
            String normalizedField = field == null ? "" : field.trim().toUpperCase();
            if (!List.of("ADDRESS", "PHONE", "NAME", "EMAIL").contains(normalizedField)) {
                response.put("success", false);
                response.put("message", "Invalid field. ADDRESS, PHONE, NAME, and EMAIL can be updated.");
                return response;
            }
            if (newValue == null || newValue.isBlank()) {
                response.put("success", false);
                response.put("message", "A new value is required.");
                return response;
            }
            newValue = newValue.trim();

            // Get user
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            User user = userOpt.get();
            Account account = user.getAccount();
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found");
                return response;
            }

            // Get current value
            String oldValue = null;
            if ("ADDRESS".equals(normalizedField)) {
                oldValue = account.getAddress();
            } else if ("PHONE".equals(normalizedField)) {
                oldValue = account.getPhone();
            } else if ("NAME".equals(normalizedField)) {
                oldValue = account.getName();
            } else if ("EMAIL".equals(normalizedField)) {
                oldValue = user.getEmail();
                newValue = UserService.normalizeEmail(newValue);
                if (newValue == null || !newValue.matches("^[A-Za-z0-9+_.-]+@[^\\s@]+\\.[^\\s@]+$")) {
                    response.put("success", false);
                    response.put("message", "Enter a valid email address.");
                    return response;
                }
                if (!userService.isEmailUnique(newValue, userId)) {
                    response.put("success", false);
                    response.put("message", "Email already exists. Please use a different email.");
                    return response;
                }
            }

            // Check if new value is different
            boolean unchanged = "EMAIL".equals(normalizedField)
                    ? oldValue != null && newValue.equalsIgnoreCase(oldValue)
                    : newValue.equals(oldValue);
            if (unchanged) {
                response.put("success", false);
                response.put("message", "New value is the same as current value");
                return response;
            }

            // Check if there's already a pending request
            List<ProfileUpdateRequest> pendingRequests = requestRepository
                .findByUserIdAndStatus(userId, "PENDING");
            if (!pendingRequests.isEmpty()) {
                response.put("success", false);
                response.put("message", "You already have a pending update request. Please wait for admin approval.");
                return response;
            }

            String userEmail = user.getEmail();
            if (userEmail == null || userEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "User email not found.");
                return response;
            }

            // Create update request
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setUserId(userId);
            request.setAccountNumber(user.getAccountNumber());
            request.setUserName(account.getName());
            request.setUserEmail(userEmail);
            request.setFieldToUpdate(normalizedField);
            request.setOldValue(oldValue);
            request.setNewValue(newValue);
            request.setOtpVerified(!"EMAIL".equals(normalizedField));
            request.setStatus("EMAIL".equals(normalizedField) ? "PENDING" : "OTP_VERIFIED");

            savedRequest = requestRepository.save(request);
            if ("EMAIL".equals(normalizedField)) {
                otpService.sendOtp(newValue, "Profile email change verification");
                savedRequest.setOtpSentAt(LocalDateTime.now());
                requestRepository.save(savedRequest);
            }

            notificationService.notifyToEmail(userEmail, "Profile update request", "Submitted",
                    String.valueOf(savedRequest.getId()), Map.of(
                            "Field", normalizedField,
                            "Previous value", oldValue == null ? "Not provided" : oldValue,
                            "Requested value", newValue));

            response.put("success", true);
            response.put("message", "EMAIL".equals(normalizedField)
                    ? "Verification code sent to the new email address."
                    : "Profile update request submitted for admin approval.");
            response.put("requestId", savedRequest.getId());
            
        } catch (Exception e) {
            if (savedRequest != null && "PENDING".equals(savedRequest.getStatus())) {
                requestRepository.delete(savedRequest);
            }
            response.put("success", false);
            response.put("message", "Failed to create update request. Please try again.");
            System.err.println("Profile update request failed: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Verify OTP and submit profile update request
     */
    @Transactional
    public Map<String, Object> verifyOtpAndSubmitRequest(Long requestId, String otp, Long requesterUserId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ProfileUpdateRequest request = requestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                response.put("success", false);
                response.put("message", "Update request not found");
                return response;
            }
            if (!java.util.Objects.equals(request.getUserId(), requesterUserId)) {
                response.put("success", false);
                response.put("message", "You can only verify your own profile update request.");
                return response;
            }

            if (!"PENDING".equals(request.getStatus())) {
                response.put("success", false);
                response.put("message", "Request is not in pending status");
                return response;
            }

            // Verify OTP
            String otpEmail = "EMAIL".equals(request.getFieldToUpdate())
                    ? request.getNewValue() : request.getUserEmail();
            boolean otpValid = otpService.verifyOtp(otpEmail, otp);
            if (!otpValid) {
                response.put("success", false);
                response.put("message", "Invalid or expired OTP");
                return response;
            }

            // Update request status
            request.setOtpVerified(true);
            request.setStatus("OTP_VERIFIED");
            requestRepository.save(request);
            notificationService.notifyToEmail(request.getUserEmail(), "Profile update request", "Submitted",
                    String.valueOf(request.getId()), Map.of(
                            "Field", request.getFieldToUpdate(),
                            "Previous value", request.getOldValue() == null ? "Not provided" : request.getOldValue(),
                            "Requested value", request.getNewValue()));

            response.put("success", true);
            response.put("message", "OTP verified. Update request submitted for admin approval.");
            response.put("request", request);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to verify OTP: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Resend OTP for profile update request
     */
    @Transactional
    public Map<String, Object> resendOtp(Long requestId, Long requesterUserId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ProfileUpdateRequest request = requestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                response.put("success", false);
                response.put("message", "Update request not found");
                return response;
            }
            if (!java.util.Objects.equals(request.getUserId(), requesterUserId)) {
                response.put("success", false);
                response.put("message", "You can only resend a code for your own profile update request.");
                return response;
            }

            if (!"PENDING".equals(request.getStatus())) {
                response.put("success", false);
                response.put("message", "Cannot resend OTP. Request is not in pending status");
                return response;
            }

            // Generate and send new OTP
            String otpEmail = "EMAIL".equals(request.getFieldToUpdate())
                    ? request.getNewValue() : request.getUserEmail();
            otpService.sendOtp(otpEmail, "Profile update verification");

            // Update request with new OTP
            request.setOtp(null);
            request.setOtpSentAt(LocalDateTime.now());
            requestRepository.save(request);

            response.put("success", true);
            response.put("message", "OTP has been resent to your email");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to resend OTP: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Approve profile update request
     */
    @Transactional
    public Map<String, Object> approveProfileUpdate(Long requestId, String approvedBy) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ProfileUpdateRequest request = requestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                response.put("success", false);
                response.put("message", "Update request not found");
                return response;
            }

            if (!"OTP_VERIFIED".equals(request.getStatus())) {
                response.put("success", false);
                response.put("message", "Request is not verified. OTP must be verified first.");
                return response;
            }

            // Get user and account
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            User user = userOpt.get();

            // Enforce digital signature: profile updates require an approved signature
            if (user.getSignatureStatus() == null || !"APPROVED".equalsIgnoreCase(user.getSignatureStatus())) {
                response.put("success", false);
                response.put("message", "Profile update cannot be approved. User must have an APPROVED digital signature before changes are applied.");
                return response;
            }

            Account account = user.getAccount();
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found");
                return response;
            }

            String field = request.getFieldToUpdate();
            String oldEmail = user.getEmail();
            if ("ADDRESS".equals(field)) {
                account.setAddress(request.getNewValue());
            } else if ("PHONE".equals(field)) {
                account.setPhone(request.getNewValue());
            } else if ("NAME".equals(field)) {
                account.setName(request.getNewValue());
            } else if ("EMAIL".equals(field)) {
                userService.retireEmail(user.getId(), oldEmail);
                user.setEmail(request.getNewValue());
                userRepository.save(user);
            }

            account.setLastUpdated(LocalDateTime.now());
            accountService.saveAccount(account);

            // Create update history
            ProfileUpdateHistory history = new ProfileUpdateHistory();
            history.setUserId(request.getUserId());
            history.setAccountNumber(request.getAccountNumber());
            history.setUserName(request.getUserName());
            history.setUserEmail(request.getUserEmail());
            history.setFieldUpdated(request.getFieldToUpdate());
            history.setOldValue(request.getOldValue());
            history.setNewValue(request.getNewValue());
            history.setRequestId(requestId);
            history.setApprovedBy(approvedBy);
            history.setUpdateDate(LocalDateTime.now());
            historyRepository.save(history);

            // Update request status
            request.setStatus("COMPLETED");
            request.setApprovalDate(LocalDateTime.now());
            request.setApprovedBy(approvedBy);
            request.setCompletionDate(LocalDateTime.now());
            requestRepository.save(request);

            Map<String, String> changeDetails = Map.of(
                    "Field", field,
                    "Previous value", request.getOldValue() == null ? "Not provided" : request.getOldValue(),
                    "New value", request.getNewValue(),
                    "Approved by", approvedBy == null ? "Admin" : approvedBy);
            notificationService.notifyToEmail(oldEmail, "Profile update", "Approved",
                    String.valueOf(requestId), changeDetails);
            if ("EMAIL".equals(field) && !oldEmail.equalsIgnoreCase(request.getNewValue())) {
                notificationService.notifyToEmail(request.getNewValue(), "Profile update", "Approved",
                        String.valueOf(requestId), changeDetails);
            }

            response.put("success", true);
            response.put("message", "Profile update approved and completed successfully");
            response.put("history", history);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to approve update: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Reject profile update request
     */
    @Transactional
    public Map<String, Object> rejectProfileUpdate(Long requestId, String rejectedBy, String reason) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ProfileUpdateRequest request = requestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                response.put("success", false);
                response.put("message", "Update request not found");
                return response;
            }

            if (!"OTP_VERIFIED".equals(request.getStatus()) && !"PENDING".equals(request.getStatus())) {
                response.put("success", false);
                response.put("message", "Request cannot be rejected in current status");
                return response;
            }

            request.setStatus("REJECTED");
            request.setApprovalDate(LocalDateTime.now());
            request.setApprovedBy(rejectedBy);
            request.setRejectionReason(reason);
            requestRepository.save(request);

            response.put("success", true);
            response.put("message", "Profile update request rejected");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to reject update: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Get update requests by user ID
     */
    public List<ProfileUpdateRequest> getUpdateRequestsByUser(Long userId) {
        return requestRepository.findByUserId(userId);
    }

    /**
     * Get pending update requests (for admin)
     */
    public List<ProfileUpdateRequest> getPendingUpdateRequests() {
        return requestRepository.findByStatus("OTP_VERIFIED");
    }

    /**
     * Get all update requests (for admin)
     */
    public List<ProfileUpdateRequest> getAllUpdateRequests() {
        return requestRepository.findAll();
    }

    /**
     * Get update history by user ID
     */
    public List<ProfileUpdateHistory> getUpdateHistoryByUser(Long userId) {
        return historyRepository.findByUserIdOrderByUpdateDateDesc(userId);
    }

    /**
     * Get update history by account number
     */
    public List<ProfileUpdateHistory> getUpdateHistoryByAccount(String accountNumber) {
        return historyRepository.findByAccountNumberOrderByUpdateDateDesc(accountNumber);
    }

    /**
     * Get update request by ID
     */
    public Optional<ProfileUpdateRequest> getUpdateRequestById(Long id) {
        return requestRepository.findById(id);
    }
}
