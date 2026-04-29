package com.neo.springapp.service;

import com.neo.springapp.model.TimeManagementPolicy;
import com.neo.springapp.repository.TimeManagementPolicyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class TimeManagementPolicyService {
    
    @Autowired
    private TimeManagementPolicyRepository policyRepository;

    /**
     * Create a new time management policy
     */
    public TimeManagementPolicy createPolicy(TimeManagementPolicy policy) {
        // Check for duplicate policies
        if (policyRepository.existsByAdminIdAndPolicyName(policy.getAdminId(), policy.getPolicyName())) {
            throw new RuntimeException("Policy with this name already exists for the admin");
        }
        
        log.info("Creating new time policy: {} for admin {}", policy.getPolicyName(), policy.getAdminId());
        return policyRepository.save(policy);
    }

    /**
     * Update existing time management policy
     */
    public TimeManagementPolicy updatePolicy(Long policyId, TimeManagementPolicy updates) {
        TimeManagementPolicy policy = policyRepository.findById(policyId)
            .orElseThrow(() -> new RuntimeException("Policy not found"));
        
        if (updates.getPolicyName() != null) {
            policy.setPolicyName(updates.getPolicyName());
        }
        if (updates.getWorkingHoursPerDay() != null) {
            policy.setWorkingHoursPerDay(updates.getWorkingHoursPerDay());
        }
        if (updates.getCheckInTime() != null) {
            policy.setCheckInTime(updates.getCheckInTime());
        }
        if (updates.getCheckOutTime() != null) {
            policy.setCheckOutTime(updates.getCheckOutTime());
        }
        if (updates.getGracePeriodMinutes() != null) {
            policy.setGracePeriodMinutes(updates.getGracePeriodMinutes());
        }
        if (updates.getMaxWorkingHours() != null) {
            policy.setMaxWorkingHours(updates.getMaxWorkingHours());
        }
        if (updates.getOvertimeMultiplier() != null) {
            policy.setOvertimeMultiplier(updates.getOvertimeMultiplier());
        }
        if (updates.getDescription() != null) {
            policy.setDescription(updates.getDescription());
        }
        
        log.info("Updated time policy: {}", policyId);
        return policyRepository.save(policy);
    }

    /**
     * Get policy by ID
     */
    public TimeManagementPolicy getPolicyById(Long policyId) {
        return policyRepository.findById(policyId)
            .orElseThrow(() -> new RuntimeException("Policy not found"));
    }

    /**
     * Get all policies for an admin
     */
    public List<TimeManagementPolicy> getPoliciesByAdminId(String adminId) {
        return policyRepository.findByAdminId(adminId);
    }

    /**
     * Get active policy for an admin
     */
    public Optional<TimeManagementPolicy> getActivePolicyForAdmin(String adminId) {
        return policyRepository.findActiveByAdminId(adminId);
    }

    /**
     * Get all policies with pagination
     */
    public Page<TimeManagementPolicy> getAllPolicies(Pageable pageable) {
        return policyRepository.findAllActive(pageable);
    }

    /**
     * Search policies by name
     */
    public List<TimeManagementPolicy> searchPoliciesByName(String name) {
        return policyRepository.findByPolicyNameContaining(name);
    }

    /**
     * Search with pagination
     */
    public Page<TimeManagementPolicy> searchPolicies(String search, Pageable pageable) {
        return policyRepository.findAllWithSearch(search, pageable);
    }

    /**
     * Activate a policy
     */
    public TimeManagementPolicy activatePolicy(Long policyId) {
        TimeManagementPolicy policy = getPolicyById(policyId);
        
        // Deactivate other policies for the same admin
        policyRepository.findByAdminId(policy.getAdminId()).forEach(p -> {
            if (!p.getId().equals(policyId)) {
                p.setIsActive(false);
                policyRepository.save(p);
            }
        });
        
        policy.setIsActive(true);
        log.info("Activated policy: {}", policyId);
        return policyRepository.save(policy);
    }

    /**
     * Deactivate a policy
     */
    public TimeManagementPolicy deactivatePolicy(Long policyId) {
        TimeManagementPolicy policy = getPolicyById(policyId);
        policy.setIsActive(false);
        log.info("Deactivated policy: {}", policyId);
        return policyRepository.save(policy);
    }

    /**
     * Toggle policy status
     */
    public TimeManagementPolicy togglePolicyStatus(Long policyId) {
        TimeManagementPolicy policy = getPolicyById(policyId);
        
        if (policy.getIsActive()) {
            return deactivatePolicy(policyId);
        } else {
            return activatePolicy(policyId);
        }
    }

    /**
     * Delete a policy
     */
    public void deletePolicy(Long policyId) {
        TimeManagementPolicy policy = getPolicyById(policyId);
        log.info("Deleting policy: {}", policyId);
        policyRepository.delete(policy);
    }

    /**
     * Validate policy settings
     */
    public boolean validatePolicy(TimeManagementPolicy policy) {
        if (policy.getPolicyName() == null || policy.getPolicyName().isEmpty()) {
            return false;
        }
        if (policy.getWorkingHoursPerDay() == null || policy.getWorkingHoursPerDay() <= 0) {
            return false;
        }
        if (policy.getCheckInTime() == null || policy.getCheckOutTime() == null) {
            return false;
        }
        if (policy.getCheckInTime().isAfter(policy.getCheckOutTime())) {
            return false;
        }
        if (policy.getMaxWorkingHours() == null || policy.getMaxWorkingHours() < policy.getWorkingHoursPerDay()) {
            return false;
        }
        
        return true;
    }
}
