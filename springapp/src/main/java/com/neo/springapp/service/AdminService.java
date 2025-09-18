package com.neo.springapp.service;

import com.neo.springapp.model.Admin;
import com.neo.springapp.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    public Admin saveAdmin(Admin admin) {
        return adminRepository.save(admin);
    }

    public Admin getAdminById(Long id) {
        return adminRepository.findById(id).orElse(null);
    }

    public Admin updateAdmin(Long id, Admin adminDetails) {
        Admin admin = adminRepository.findById(id).orElse(null);
        if (admin != null) {
            // Update only non-null fields
            if (adminDetails.getName() != null) {
                admin.setName(adminDetails.getName());
            }
            if (adminDetails.getEmail() != null) {
                admin.setEmail(adminDetails.getEmail());
            }
            if (adminDetails.getPassword() != null) {
                admin.setPassword(adminDetails.getPassword());
            }
            if (adminDetails.getRole() != null) {
                admin.setRole(adminDetails.getRole());
            }
            if (adminDetails.getPan() != null) {
                admin.setPan(adminDetails.getPan());
            }
            return adminRepository.save(admin);
        }
        return null;
    }

    public Admin login(String email, String password) {
        return adminRepository.findByEmailAndPassword(email, password);
    }
}
